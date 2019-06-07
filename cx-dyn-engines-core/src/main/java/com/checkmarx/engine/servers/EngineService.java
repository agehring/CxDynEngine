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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.domain.EnginePool;
import com.checkmarx.engine.rest.CxEngineApi;
import com.checkmarx.engine.utils.ExecutorServiceUtils;
import com.google.common.collect.Lists;

@Component
public class EngineService implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(EngineService.class);

	private final CxConfig config;
	private final ScanQueueMonitor scanQueueMonitor;
	private final EngineManager engineManager;

	private final ExecutorService engineManagerExecutor;
	private final ScheduledExecutorService scanQueueExecutor;
	private final List<Future<?>> tasks = Lists.newArrayList();

	public EngineService(CxEngineApi cxClient, CxConfig config, ScanQueueMonitor scanQueueMonitor, 
			EngineManager engineManager, EnginePool enginePool) {
		this.config = config;
		this.scanQueueMonitor = scanQueueMonitor;
		this.engineManager = engineManager;
		this.engineManagerExecutor = ExecutorServiceUtils.buildSingleThreadExecutorService("eng-service-%d", true);
		this.scanQueueExecutor = ExecutorServiceUtils.buildScheduledExecutorService("queue-mon-%d", true);
		
		log.info("ctor(): {}", this.config);
	}
	
	@Override
	public void run() {
		log.info("run()");
	
		final int pollingInterval = config.getQueueIntervalSecs();
		try {
		
		    engineManager.initialize();
		    
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
	
}
