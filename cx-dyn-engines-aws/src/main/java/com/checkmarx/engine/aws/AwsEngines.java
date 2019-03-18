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
package com.checkmarx.engine.aws;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.Instance;
import com.checkmarx.engine.domain.DynamicEngine;
import com.checkmarx.engine.domain.EnginePoolConfig;
import com.checkmarx.engine.domain.EngineSize;
import com.checkmarx.engine.domain.Host;
import com.checkmarx.engine.rest.CxEngineClient;
import com.checkmarx.engine.servers.CxEngines;
import com.checkmarx.engine.utils.ExecutorServiceUtils;
import com.checkmarx.engine.utils.ScriptRunner;
import com.checkmarx.engine.utils.TimeoutTask;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * AWS {@code CxEngines} provider.
 * 
 * @author randy@checkmarx.com
 *
 */
@Component
@Profile("aws")
public class AwsEngines implements CxEngines {

	private static final Logger log = LoggerFactory.getLogger(AwsEngines.class);
	
	//TODO: inject scripting ExecutorService via constructor
	private final ExecutorService executor = ExecutorServiceUtils.buildPooledExecutorService(20, "eng-scripts-%d", false);

	private final AwsEngineConfig awsConfig;
	private final EnginePoolConfig poolConfig;
	private final AwsComputeClient ec2Client;
	private final CxEngineClient engineClient;
	private final int pollingMillis;

	/**
	 * Maps engine name to EC2 instance; key=engine name
	 */
	private final Map<String, Instance> provisionedEngines = Maps.newConcurrentMap();
	
	/**
	 * Maps EngineSize to EC2 instanceType;
	 * key=size (name), 
	 * value=ec2 instance type (e.g. m4.large)
	 */
	private final Map<String, String> engineTypeMap;
	
	public AwsEngines(
			EnginePoolConfig poolConfig,
			AwsComputeClient awsClient, 
			CxEngineClient engineClient) {
		
		this.poolConfig = poolConfig;
		this.ec2Client = awsClient;
		this.awsConfig = awsClient.getConfig(); 
		this.engineClient = engineClient;
		this.engineTypeMap = awsConfig.getEngineSizeMap();
		this.pollingMillis = awsConfig.getMonitorPollingIntervalSecs() * 1000;
		
		log.info("ctor(): {}", this);
	}
	
	public static Map<String, String> createCxTags(CxServerRole role, String version) {
		log.trace("createCxTags(): role={}; version={}", role, version);
		
		final Map<String, String> tags = Maps.newHashMap();
		tags.put(CX_ROLE_TAG, role.toString());
		tags.put(CX_VERSION_TAG, version);
		return tags;
	}
	
	private Map<String, String> createEngineTags(String size) {
		log.trace("createEngineTags(): size={}", size);
		final Map<String, String> tags = createCxTags(CxServerRole.ENGINE, awsConfig.getCxVersion());
		tags.put(CX_SIZE_TAG, size);
		// add custom tags from configuration
		awsConfig.getTagMap().forEach(tags::put);
		return tags;
	}

	private Map<String, String> getEngineTags() {
		final Map<String, String> tags = createCxTags(CxServerRole.ENGINE, awsConfig.getCxVersion());
		awsConfig.getTagMap().forEach(tags::put);
		return tags;
	}

	Map<String, Instance> findEngines() {
		log.trace("findEngines()");
		
		final Stopwatch timer = Stopwatch.createStarted(); 
		try {
			final List<Instance> engines = ec2Client.find(getEngineTags());
			engines.forEach((instance) -> {
				if (Ec2.isTerminated(instance)) {
					log.info("Terminated engine found: {}", Ec2.print(instance));
					return;
				}

				final String name = Ec2.getName(instance);
				if (!provisionedEngines.containsKey(name)) {
					provisionedEngines.put(name, instance);
					log.info("Provisioned engine found: {}", Ec2.print(instance));
				}
			});
			
		} finally {
			log.debug("Find Engines: elapsedTime={}ms; count={}", 
					timer.elapsed(TimeUnit.MILLISECONDS), provisionedEngines.size()); 
		}
		return provisionedEngines;
	}

	@Override
	public List<DynamicEngine> listEngines() {
		final Map<String, Instance> engines = findEngines();
		final List<DynamicEngine> dynEngines = Lists.newArrayList();
		engines.forEach((name, instance) -> {
			final DynamicEngine engine = buildDynamicEngine(name, instance);
			dynEngines.add(engine);
		});
		return dynEngines;
	}

	DynamicEngine buildDynamicEngine(String name, Instance instance) {
		final String size = lookupEngineSize(instance);
		final DateTime launchTime = new DateTime(instance.getLaunchTime());
		final boolean isRunning = Ec2.isRunning(instance);
		final DynamicEngine engine = DynamicEngine.fromProvisionedInstance(
				name, size, poolConfig.getEngineExpireIntervalSecs(),
				launchTime, isRunning);
		if (isRunning) {
			engine.setHost(createHost(name, instance));
		}
		return engine;
	}

	private String lookupEngineSize(Instance instance) {
		String sizeTag = Ec2.getTag(instance, CX_SIZE_TAG);
		if (!Strings.isNullOrEmpty(sizeTag)) return sizeTag;
		
		final Map<String, String> sizeMap = awsConfig.getEngineSizeMap();
		
		for (Entry<String,String> entry : sizeMap.entrySet()) {
			String instanceType = entry.getValue();
			String size = entry.getKey();
			if (instance.getInstanceType().equals(instanceType))
				return size;
		}
		// if not found, return first size in map
		return Iterables.getFirst(sizeMap.values(), "S"); 
	}

	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public void launch(DynamicEngine engine, EngineSize size, boolean waitForSpinup) {
		log.debug("launch(): {}; size={}; wait={}", engine, size, waitForSpinup);
		
		findEngines();
		
		final String name = engine.getName();
		final String type = engineTypeMap.get(size.getName());
		final Map<String, String> tags = createEngineTags(size.getName());
		
		Instance instance = provisionedEngines.get(name);
		String instanceId = null;
		
		log.info("action=LaunchingEngine; name={}; {}", name, engine); 

		boolean success = false;
		final Stopwatch timer = Stopwatch.createStarted();
		try {
			if (instance == null) {
				instance = launchEngine(engine, name, type, tags);
			}
	
			instanceId = instance.getInstanceId();
			
			if (Ec2.isTerminated(instance)) {
				instance = launchEngine(engine, name, type, tags);
				instanceId = instance.getInstanceId();
			} else if (!Ec2.isRunning(instance)) {
				instance = ec2Client.start(instanceId);
			} else {
				// host is running
			}
			
			final Host host = createHost(name, instance);
			engine.setHost(host);
			
			if (waitForSpinup) {
				pingEngine(host);
			}
			//move this logic into caller
			runScript(awsConfig.getScriptOnLaunch(), engine);
			
			//engine.setState(State.IDLE);
			success = true;
		} catch (Throwable e) {
			log.error("Error occurred while launching AWS EC2 instance; name={}; {}", name, engine, e);
			if (!Strings.isNullOrEmpty(instanceId)) {
				log.warn("Terminating instance due to error; instanceId={}", instanceId);
				ec2Client.terminate(instanceId);
				instance = null;
				throw new RuntimeException("Error launching engine", e);
			}
		} finally {
			log.info("action=LaunchedEngine; success={}; name={}; id={}; elapsedTime={}s; {}", 
					success, name, instanceId, timer.elapsed(TimeUnit.SECONDS), Ec2.print(instance)); 
		}
	}
	
	@Override
	public void stop(DynamicEngine engine) {
		stop(engine, false);
	}
	
	@Override
	public void stop(DynamicEngine engine, boolean forceTerminate) {
		log.debug("stop() : {}", engine);

		final String name = engine.getName();
		Instance instance = provisionedEngines.get(name);
		if (instance == null) {
			throw new RuntimeException("Cannot stop engine, no instance found");
		}
		final String instanceId = instance.getInstanceId();

		String action = "StoppedEngine";
		boolean success = false;
		final Stopwatch timer = Stopwatch.createStarted();
		try {
			
			if (awsConfig.isTerminateOnStop() || forceTerminate) {	
				action = "TerminatedEngine";
				ec2Client.terminate(instanceId);
				provisionedEngines.remove(name);
				//engine.setState(State.UNPROVISIONED);
				runScript(awsConfig.getScriptOnTerminate(), engine);
			} else {
				ec2Client.stop(instanceId);
				instance = ec2Client.describe(instanceId);
				provisionedEngines.put(name, instance);
				//engine.setState(State.IDLE);
			}
			success = true;
			
		} finally {
			log.info("action={}; success={}; name={}; id={}; elapsedTime={}ms; {}", 
					action, success, name, instanceId, timer.elapsed(TimeUnit.MILLISECONDS), Ec2.print(instance)); 
		}
	}

	private Instance launchEngine(final DynamicEngine engine, final String name, 
			final String type, final Map<String, String> tags) {
		log.debug("launchEngine(): name={}; type={}", name, type);
		
		final Instance instance = ec2Client.launch(name, type, tags);
		provisionedEngines.put(name, instance);
		return instance;
	}

	private Host createHost(final String name, final Instance instance) {
		final String ip = instance.getPrivateIpAddress();
		final String publicIp = instance.getPublicIpAddress();
		final String cxIp = awsConfig.isUsePublicUrlForCx() ? publicIp : ip;
		final String monitorIp = awsConfig.isUsePublicUrlForMonitor() ? publicIp : ip;
		final DateTime launchTime = new DateTime(instance.getLaunchTime());
		return new Host(name, ip, publicIp, 
				engineClient.buildEngineServiceUrl(cxIp), 
				engineClient.buildEngineServiceUrl(monitorIp), launchTime);
	}

	private void pingEngine(Host host) throws Exception {
		log.trace("pingEngine(): host={}", host);
		
		final TimeoutTask<Boolean> pingTask = 
				new TimeoutTask<>("pingEngine", awsConfig.getCxEngineTimeoutSec(), TimeUnit.SECONDS);
		final String ip = awsConfig.isUsePublicUrlForMonitor() ? host.getPublicIp(): host.getIp();
		try {
			pingTask.execute(() -> {
				while (!engineClient.pingEngine(ip)) {
					log.trace("Engine ping failed, waiting to retry; sleep={}ms; {}", pollingMillis, host); 
					TimeUnit.MILLISECONDS.sleep(pollingMillis);
				}
				return true;
			});
		} catch (Exception e) {
			log.warn("Failed to ping CxEngine service; {}; cause={}; message={}", 
					host, e.getCause(), e.getMessage());
			throw e;
		}
	}
	
	void runScript(String scriptFile, DynamicEngine engine) {
		log.trace("runScript() : script={}", scriptFile);
		
		if (Strings.isNullOrEmpty(scriptFile)) return;
		
		//TODO: pass a readonly copy of DynamicEngine 
		final ScriptRunner<DynamicEngine> runner = new ScriptRunner<DynamicEngine>();
		if (runner.loadScript(scriptFile)) {
			runner.bindData("engine", engine);
			executor.submit(runner);
		} else {
			log.debug("Script file not found: {}", scriptFile);
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("config", awsConfig)
				.toString();
	}

}
