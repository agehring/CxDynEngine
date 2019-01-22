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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

//import jdk.nashorn.internal.parser.JSONParser;
//import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.domain.DynamicEngine;
import com.checkmarx.engine.domain.EnginePool;
import com.checkmarx.engine.rest.CxEngineApi;
import com.checkmarx.engine.rest.model.EngineServer;
import com.checkmarx.engine.utils.ExecutorServiceUtils;
import com.google.common.collect.Lists;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class EngineService implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(EngineService.class);

	private final CxEngineApi cxClient;
	private final CxConfig config;
	private final EnginePool enginePool;
	private final CxEngines engineProvisioner;
	private final ScanQueueMonitor scanQueueMonitor;
	private final EngineManager engineManager;

	private final ExecutorService engineManagerExecutor;
	private final ScheduledExecutorService scanQueueExecutor;
	private final List<Future<?>> tasks = Lists.newArrayList();

	public EngineService(CxEngineApi cxClient, CxEngines engineProvisioner, CxConfig config,
			ScanQueueMonitor scanQueueMonitor, EngineManager engineManager, EnginePool enginePool) {
		this.cxClient = cxClient;
		this.config = config;
		this.engineProvisioner = engineProvisioner;
		this.scanQueueMonitor = scanQueueMonitor;
		this.engineManager = engineManager;
		this.enginePool = enginePool;
		this.engineManagerExecutor = ExecutorServiceUtils.buildSingleThreadExecutorService("eng-service-%d", true);
		this.scanQueueExecutor = ExecutorServiceUtils.buildScheduledExecutorService("queue-mon-%d", true);
		
		log.info("ctor(): {}; {}; {}", this.enginePool, this.cxClient, this.config);
	}
	
	@Override
	public void run() {
		log.info("run()");
	
		final int pollingInterval = config.getQueueIntervalSecs();
		try {
		
			initialize();

			log.info("Launching EngineManager...");
			tasks.add(engineManagerExecutor.submit(engineManager));
			
			log.info("Launching ScanQueueMonitor; pollingInterval={}s", pollingInterval);
			tasks.add(scanQueueExecutor.scheduleAtFixedRate(scanQueueMonitor, 0L, pollingInterval, TimeUnit.SECONDS));

		} catch (Throwable t) {
			log.error("Error occurred while launching Engine services, shutting down; cause={}; message={}", 
					t, t.getMessage(), t); 
			shutdown();
		}
	}

	private void initialize() {
		log.trace("initialize()");
		
		log.info("Logging into CxManager; url={}, user={}", config.getRestUrl(), config.getUserName()); 
		if (!cxClient.login()) {
			throw new RuntimeException("Unable to login to CxManager");
		}

		updateHostedEngines();
		checkCxEngines();
		registerQueuingEngine();
	}
	
	public void registerQueuingEngine() {
		log.debug("registerQueueEngine()");
		
		final String engineName = config.getQueueingEngineName();
		final EngineServer engine = cxClient.blockEngine(engineName);

		log.info("Queueing engine registered: {}", engine);
	}
	
	/**
	 * Checks registered engines, removes idle dynamic engines
	 */
	private void checkCxEngines() {
		log.debug("checkCxEngines()");
		try {
			final List<EngineServer> engines = cxClient.getEngines();
			engines.forEach((engine) -> {
				boolean isDynamic = isDynamicEngine(engine);
				log.info("CxEngine found; engine={}; isAlive={}; isBlocked={}; isDynamic={}",
						engine.getName(), engine.isAlive(), engine.isBlocked(), isDynamic);
				if (isDynamic) {
					//FIXME: once the engine API supports engine state, add active engines to registered engine list
					// for now, unregister any dynamic engines found
					log.warn("Dynamic engine found, unregistering; engine={}; {}", engine.getName(), engine);
					cxClient.unregisterEngine(engine.getId());
				}
			});
		} catch (HttpClientErrorException e){
			log.error("Error while checking CxEngines; cause={}; message={}", e, e.getMessage(), e);
			//(HttpStatusCodeException) e).getResponseBodyAsString()) -> messageDetails contains "busy (scanning)"  TODO rogue engine monitor??
		} catch (Exception e) {
			// log and swallow
			log.error("Error while checking CxEngines; cause={}; message={}", e, e.getMessage(), e); 
		}
	}

	private boolean isDynamicEngine(EngineServer engine) {
		return engine.getName().startsWith(config.getCxEnginePrefix());
	}

	@PreDestroy
	public void stop() {
		log.info("stop()");
		shutdown();
	}
	
	private void shutdown() {
		log.info("shutdown()");

		engineManager.stop();
		
		tasks.forEach((task) -> {
			task.cancel(true);
		});
		engineManagerExecutor.shutdown();
		scanQueueExecutor.shutdown();
		try {
			if (!engineManagerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				engineManagerExecutor.shutdownNow();
			}
			if (!scanQueueExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
				scanQueueExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			engineManagerExecutor.shutdownNow();
			scanQueueExecutor.shutdownNow();
		}
	}
	
	private void updateHostedEngines() {
		log.debug("updateHostedEngines()");

		final List<DynamicEngine> provisionedEngines = engineProvisioner.listEngines();
		provisionedEngines.forEach((engine) -> {
			final String name = engine.getName();
			final DynamicEngine existingEngine = enginePool.getEngineByName(name);
			if (existingEngine != null) {
				enginePool.addExistingEngine(engine);
			} else {
				// terminate unknown engines
				engineProvisioner.stop(engine, true);
			}
		});
		
	}
	
}
