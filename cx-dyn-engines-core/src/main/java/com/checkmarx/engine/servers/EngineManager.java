/*******************************************************************************
 * Copyright (c) 2017-2019 Checkmarx
 *  
 * This software is licensed for customer's internal use only.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package com.checkmarx.engine.servers;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.domain.DynamicEngine;
import com.checkmarx.engine.domain.DynamicEngine.State;
import com.checkmarx.engine.domain.EnginePool;
import com.checkmarx.engine.domain.EnginePool.IdleEngineMonitor;
import com.checkmarx.engine.domain.EngineSize;
import com.checkmarx.engine.rest.CxEngineApi;
import com.checkmarx.engine.rest.model.EngineServer;
import com.checkmarx.engine.rest.model.ScanRequest;
import com.checkmarx.engine.utils.ExecutorServiceUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

public class EngineManager implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(EngineManager.class);

	private final CxConfig config;
	private final CxEngineApi cxClient;
	private final EnginePool pool;
	private final CxEngines engineProvisioner;
	private final BlockingQueue<ScanRequest> queuedScansQueue;
	private final BlockingQueue<ScanRequest> finshedScansQueue;
	private final BlockingQueue<DynamicEngine> expiredEnginesQueue;

	private final ExecutorService managerExecutor;
	private final ExecutorService scanQueuedExecutor;
	private final ExecutorService scanFinishedExecutor;
	private final ExecutorService engineExpiringExecutor;
	private final ScheduledExecutorService idleEngineExecutor;
	private final List<Future<?>> tasks = Lists.newArrayList();
	
	private final static int MANAGER_THREAD_POOL_SIZE = 3;
	//FIXME: move these to config
	private final static int SCANS_QUEUED_THREAD_POOL_SIZE = 10;
	private final static int SCANS_FINISHED_THREAD_POOL_SIZE = 5;
	private final static int ENGINE_EXPIRING_THREAD_POOL_SIZE = 5;

	/**
	 * map of blocked scans by engine size; key=EngineSize of scan
	 */
	private final Map<EngineSize, Queue<ScanRequest>> blockedScansQueueMap;
	
	/**
	 * map of scans assigned to engines; key=Scan.Id, value=cxEngineId
	 */
	private final Map<Long, Long> engineScans = Maps.newConcurrentMap();
	
	/**
	 * map of registered cx engine servers, key=cxEngineId
	 */
	private Map<Long, DynamicEngine> cxEngines = Maps.newConcurrentMap();
	
	/**
	 * map of active (scanning) cx engine servers, key=cxEngineId
	 */
	private Map<Long, EngineServer> activeEngines = Maps.newConcurrentMap();

	public EngineManager(
			CxConfig config,
			EnginePool pool, 
			CxEngineApi cxClient,
			CxEngines engineProvisioner,
			BlockingQueue<ScanRequest> scansQueued,
			BlockingQueue<ScanRequest> scansFinished) {
		this.pool = pool;
		this.config = config;
		this.cxClient = cxClient;
		this.queuedScansQueue = scansQueued;
		this.finshedScansQueue = scansFinished;
		this.expiredEnginesQueue = new ArrayBlockingQueue<DynamicEngine>(pool.getEngineCount());
		this.blockedScansQueueMap = Maps.newConcurrentMap();
		this.engineProvisioner = engineProvisioner;
		this.managerExecutor = ExecutorServiceUtils.buildPooledExecutorService(MANAGER_THREAD_POOL_SIZE, "engine-mgr-%d", true);
		this.scanQueuedExecutor = ExecutorServiceUtils.buildPooledExecutorService(SCANS_QUEUED_THREAD_POOL_SIZE, "scan-queue-%d", true);
		this.scanFinishedExecutor = ExecutorServiceUtils.buildPooledExecutorService(SCANS_FINISHED_THREAD_POOL_SIZE, "scan-finish-%d", true);
		this.engineExpiringExecutor = ExecutorServiceUtils.buildPooledExecutorService(ENGINE_EXPIRING_THREAD_POOL_SIZE, "engine-kill-%d", true);
		this.idleEngineExecutor = ExecutorServiceUtils.buildScheduledExecutorService("idle-mon-%d", true);
	}

	@Override
	public void run() {
		log.info("run()");
		
		try {
			final IdleEngineMonitor engineMonitor = 
					pool.createIdleEngineMonitor(this.expiredEnginesQueue, config.getExpireEngineBufferMins());
			
			tasks.add(managerExecutor.submit(new ScanLauncher()));
			tasks.add(managerExecutor.submit(new ScanFinisher()));
			tasks.add(managerExecutor.submit(new EngineTerminator()));
			
			final int monitorInterval = config.getIdleMonitorSecs();
			tasks.add(idleEngineExecutor.scheduleAtFixedRate(engineMonitor, 1, monitorInterval, TimeUnit.SECONDS));
		} catch (Throwable t) {
			log.error("Error occurred while launching Engine processes, shutting down; cause={}; message={}", 
					t, t.getMessage(), t); 
			shutdown();
		}
	}
	
	public void stop() {
		log.info("stop()");
		shutdown();
	}
	
	private void shutdown() {
		log.info("shutdown()");

		tasks.forEach((task) -> {
			task.cancel(true);
		});
		
		managerExecutor.shutdown();
		scanQueuedExecutor.shutdown();
		scanFinishedExecutor.shutdown();
		idleEngineExecutor.shutdown();
		
		try {
			if (!managerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				managerExecutor.shutdownNow();
			}
			if (!scanQueuedExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				scanQueuedExecutor.shutdownNow();
			}
			if (!scanFinishedExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				scanFinishedExecutor.shutdownNow();
			}
			if (!idleEngineExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				idleEngineExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			managerExecutor.shutdownNow();
			scanQueuedExecutor.shutdownNow();
			scanFinishedExecutor.shutdownNow();
			idleEngineExecutor.shutdownNow();
		}
	}
	
	private EngineSize calcEngineSize(ScanRequest scan) {
		final EngineSize size = pool.calcEngineSize(scan.getLoc());
		if (size == null) {
			final String msg = String.format("Invalid scan size; %s", scan);
			throw new RuntimeException(msg);
		}
		return size;
	}

	public class ScanLauncher implements Runnable {
		
		private final Logger log = LoggerFactory.getLogger(EngineManager.ScanLauncher.class);

		@Override
		public void run() {
			log.info("run()");
			
			int scansQueuedCount = 0;
			try {
				while (true) {
					log.debug("ScanLauncher: waiting for scan");
					
					// blocks until scan available
					ScanRequest scan = queuedScansQueue.take();
					scansQueuedCount++;
					
					// process scan task using background thread pool
					scanQueuedExecutor.execute(()-> onScanQueued(scan));
				}
			} catch (InterruptedException e) {
				log.info("ScanLauncher interrupted");
			} catch (Throwable t) {
				log.error("Error occurred in ScanLauncher; cause={}; message={}", 
						t, t.getMessage(), t); 
			} finally {
				log.info("ScanLauncher exiting; scansQueuedCount={}", scansQueuedCount);
			}
		}
		
		public void onScanQueued(ScanRequest scan) {
			log.debug("onScanQueued() : {}", scan);

			final EngineSize size = calcEngineSize(scan);
			
			try {
			
				if (allocateIdleEngine(size, scan)) return;
				
				if (checkActiveEngines(size, scan)) return;
				
				if (allocateNewEngine(size, scan)) return;
				
				blockScan(size, scan);
				
			} catch (Throwable t) {
				log.error("Error occurred launching scan; cause={}; message={}", 
						t, t.getMessage(), t); 
				//TODO: add retry logic
				blockScan(size, scan);
			}
		}

		private boolean allocateIdleEngine(EngineSize size, ScanRequest scan) {
			log.trace("allocateIdleEngine(): size={}; {}", size, scan);

			final State state = State.IDLE;
			final DynamicEngine engine = pool.allocateEngine(size, state);
			
			if (engine == null) return false;
			
			registerEngine(state, scan, engine);
			return true;
		}
		
		private boolean allocateNewEngine(EngineSize size, ScanRequest scan) {
			log.trace("allocateNewEngine(): size={}; {}", scan, size);

			final State state = DynamicEngine.State.UNPROVISIONED;
			final DynamicEngine engine = pool.allocateEngine(size, state);
			
			if (engine == null) return false;

			// blocks while engine is spinning up
			engineProvisioner.launch(engine, size, true);
			registerEngine(state, scan, engine);
			return true;
		}
		
		private void blockScan(EngineSize size, ScanRequest scan) {
			log.trace("blockScan(): size={}; {}", size, scan);
			
			Queue<ScanRequest> blockedQueue = blockedScansQueueMap.get(size);
			if (blockedQueue == null) {
				log.debug("Creating blocked queue: size={}; {}", size, scan);
				blockedQueue = Queues.newConcurrentLinkedQueue();
				blockedScansQueueMap.put(size, blockedQueue);
			}
			
			blockedQueue.add(scan);
			log.warn("No engine available, added scan to blocked queue: size={}; {}", size, scan);
		}

		private void registerEngine(State fromState, ScanRequest scan, DynamicEngine dynEngine) {
			log.trace("registerEngine(): fromState={}; {}; {}", fromState, scan, dynEngine);
			
			final Long scanId = scan.getId();
			final String url = dynEngine.getUrl();
			if (Strings.isNullOrEmpty(url)) {
				final String msg = String.format("Cannot register Engine, url is null: %s", dynEngine);
				throw new RuntimeException(msg);
			}
			EngineServer cxEngine = createEngine(dynEngine.getName(), scan, dynEngine.getUrl());
			cxEngine = registerCxEngine(scanId, cxEngine);
			
			trackEngineScan(scan, cxEngine, dynEngine);

			log.info("Engine allocated for scan: fromState={}; engine={}; scan={}", fromState, dynEngine, scan);
		}
		
		@Retryable(value = { RestClientException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
		private EngineServer registerCxEngine(final long scanId, final EngineServer cxServer) {
			log.trace("registerCxEngine()L scanId={}", scanId);
			
			final EngineServer cxEngine = cxClient.registerEngine(cxServer);
			log.info("Engine registered: scanId={}; engine={}", scanId, cxEngine);
			
			return cxEngine;
		}

		private void trackEngineScan(ScanRequest scan, EngineServer cxEngine, DynamicEngine dynEngine) {
			log.debug("trackEngineScan(): {}; {}; {}", scan, cxEngine, dynEngine);
			
			final long engineId = cxEngine.getId();
			final long scanId = scan.getId();
			dynEngine.setScanId(scanId);
			
			cxEngines.put(engineId, dynEngine);
			engineScans.put(scanId, engineId);
			activeEngines.put(engineId, cxEngine);
		}
		
		private EngineServer createEngine(String name, ScanRequest scan, String url) {
			final String prefix = config.getCxEnginePrefix(); //"**";
			final String engineName = String.format("%s%s", prefix, name);
			final int size = scan.getLoc(); 
			
			return new EngineServer(engineName, url, size, size, 1, false);
		}

		private boolean checkActiveEngines(EngineSize size, ScanRequest scan) {
			log.trace("checkActiveEngines() : size={}; {}", size, scan);

			//final State state = State.SCANNING;
			//final DynamicEngine engine = pool.allocateEngine(size, state);
			
			//TODO: check active engines for scan finish time; if within finish window, allocate scan
			return false;
		}

	}

	public class ScanFinisher implements Runnable {
		
		private final Logger log = LoggerFactory.getLogger(EngineManager.ScanFinisher.class);

		@Override
		public void run() {
			log.info("run()");
			
			int scanCount = 0;
			try {
				while (true) {
					log.trace("ScanFinisher: waiting for scan...");
					
					// blocks until scan is finished
					ScanRequest scan = finshedScansQueue.take();
					scanCount++;
					
					// add task to thread pool
					scanFinishedExecutor.execute(()-> onScanFinished(scan));
				}
			} catch (InterruptedException e) {
				log.info("ScanFinisher interrupted");
			} catch (Throwable t) {
				log.error("Error occurred in ScanFinisher; cause={}; message={}", 
						t, t.getMessage(), t);
				throw t;
			} finally {
				log.info("ScanFinisher exiting; scanCount={}", scanCount);
			}

		}

		private void onScanFinished(ScanRequest scan) {
			log.debug("onScanFinished() : {}", scan);
			
			try {
				final EngineSize size = calcEngineSize(scan);
				final long scanId = scan.getId(); 
				final Long engineId = determineEngineId(scan);
				if (engineId == null) {
					if (removeBlockedScan(size, scan)) {
						log.info("Blocked scan was cancelled and removed: {}", scan);
						return;
					}
					log.warn("Untracked scan completed; scanId={}; {}", scanId, scan);
					return;
				}
				
				final DynamicEngine engine = cxEngines.get(engineId);
				unRegisterEngine(engineId);
				pool.idleEngine(engine);
				//engine.setState(State.IDLE);
				
				engineScans.remove(scanId);
				cxEngines.remove(engineId);
				log.info("Scan finished, engine removed: engine={}; scan={}", engine, scan);
				
				// see if we have any scans blocked that can now run
				checkBlockedScans(size);
			} catch (Throwable t) {
				log.error("Error occurred finishing scan; cause={}; message={}", 
						t, t.getMessage(), t); 
			}

		}

		/**
		 * Queues the head scan in the blocked queue, if any 
		 * @param size Engine size to check
		 */
		private void checkBlockedScans(EngineSize size) throws InterruptedException {
			log.trace("checkBlockedScans(): size={}", size);
			
			if (!blockedScansQueueMap.containsKey(size)) return;
			
			final ScanRequest scan = blockedScansQueueMap.get(size).poll();
			if (scan == null) return;
			
			// add scan to the queue
			queuedScansQueue.put(scan);
		}

		private boolean removeBlockedScan(EngineSize size, ScanRequest scan) {
			log.trace("removeBlockedScan(): size={}; {}", size, scan);

			if (!blockedScansQueueMap.containsKey(size)) return false;
			
			return blockedScansQueueMap.get(size).remove(scan);
		}

		@Retryable(value = { HttpClientErrorException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
		private void unRegisterEngine(final Long engineId) {
			log.trace("unRegisterEngine(): engineId={}", engineId);
			cxClient.unregisterEngine(engineId);
		}

		private Long determineEngineId(ScanRequest scan) {
			final Long scanEngineId = scan.getEngineId();
			log.trace("determineEngineId(): scanId={}; engineId={}", scan.getId(), scanEngineId);

			return scanEngineId == null ? engineScans.get(scan.getId()) : scanEngineId;
		}
		
	}
	
	public class EngineTerminator implements Runnable {
		
		private final Logger log = LoggerFactory.getLogger(EngineManager.EngineTerminator.class);

		@Override
		public void run() {
			log.info("run()");
			
			try {
				while (true) {
					log.trace("EngineTerminator: waiting for event...");

					//blocks until an engine expires
					final DynamicEngine engine = expiredEnginesQueue.take();
					engineExpiringExecutor.execute(()-> stopEngine(engine));
				}
			} catch (InterruptedException e) {
				log.info("EngineTerminator interrupted");
			} catch (Throwable t) {
				log.error("Error occurred in EngineTerminator; cause={}; message={}", 
						t, t.getMessage(), t);
				throw t;
			}
		}
		
		private void stopEngine(DynamicEngine engine) {
			log.debug("stopEngine(): {}", engine);
			
			pool.deallocateEngine(engine);
			engineProvisioner.stop(engine);

			log.info("Idle engine expired, engine deallocated: engine={}", engine);
		}
		
	}

}
