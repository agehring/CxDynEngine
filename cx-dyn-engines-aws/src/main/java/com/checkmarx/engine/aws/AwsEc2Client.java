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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.checkmarx.engine.aws.Ec2.InstanceState;
import com.checkmarx.engine.utils.TimeoutTask;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Launches and terminates EC2 instances from a specified AMI image.
 *  
 * @author randy@checkmarx.com
 *
 */
@Component
@Profile("aws")
public class AwsEc2Client implements AwsComputeClient {
	
	private static final Logger log = LoggerFactory.getLogger(AwsEc2Client.class);
	
	private final AmazonEC2 client;
	private final AwsEngineConfig config;
	
	public AwsEc2Client(AwsEngineConfig config) {
		this.client = AmazonEC2ClientBuilder.defaultClient();
		this.config = config;

		log.info("ctor(): {}", this);
	}
	
	@Override
	public AwsEngineConfig getConfig() {
		return config;
	}
	
	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public Instance launch(String name, String instanceType, Map<String,String> tags) {
		log.trace("launch(): name={}; instanceType={}", name, instanceType);
		
		Instance instance = null;
		String requestId = null;
		boolean success = false;
		final Stopwatch timer = Stopwatch.createStarted();
		try {

			final RunInstancesRequest runRequest = createRunRequest(name, instanceType, tags);
			RunInstancesResult result = client.runInstances(runRequest);
			requestId = result.getSdkResponseMetadata().getRequestId();
			instance = validateRunResult(result);
			
			final String instanceId = instance.getInstanceId();
			
			// wait until instance is running to populate IP addresses
			instance = waitForPendingState(instanceId, null);
			
			success = true;
			return instance;
			
		} catch (Throwable t) {
			log.warn("Failed to launch EC2 instance; name={}; cause={}; message={}", name, t, t.getMessage());
			if (instance != null) {
				final String instanceId = instance.getInstanceId();
				log.warn("Terminating failed instance launch; instanceId={}", instanceId);
				safeTerminate(instanceId);
			}
			throw new RuntimeException("Failed to launch EC2 instance", t);
		} finally {
			log.info("action={}, success={}; elapsedTime={}s; {}; requestId={}", 
					"launchInstance", success, timer.elapsed(TimeUnit.SECONDS), Ec2.print(instance), requestId);
		}
	}

	private RunInstancesRequest createRunRequest(String name, String instanceType, Map<String, String> tags) {
		log.trace("createRunRequest(): name={}; instanceType={}", name, instanceType);

		final TagSpecification tagSpec = createTagSpec(name, tags);
		
		final InstanceNetworkInterfaceSpecification nic = new InstanceNetworkInterfaceSpecification();
		nic.withDeviceIndex(0)
			.withSubnetId(config.getSubnetId())
			.withGroups(config.getSecurityGroup())
			.withAssociatePublicIpAddress(config.isAssignPublicIP());

		
		final RunInstancesRequest runRequest = new RunInstancesRequest();
			runRequest.withInstanceType(instanceType)
			.withImageId(config.getImageId())
			.withKeyName(config.getKeyName())
			.withMinCount(1)
			.withMaxCount(1)
			.withNetworkInterfaces(nic)
			.withTagSpecifications(tagSpec);

		if(config.getIamProfile() != null && !config.getIamProfile().isEmpty()) {
			final IamInstanceProfileSpecification profile = new IamInstanceProfileSpecification();
			profile.withName(config.getIamProfile());
			runRequest.setIamInstanceProfile(profile);
		}

		return runRequest;
	}

	private TagSpecification createTagSpec(String name, Map<String, String> tags) {
		final TagSpecification tagSpec = new TagSpecification();
		tagSpec.getTags().add(createTag("Name", name));
		for (Entry<String,String> tag: tags.entrySet()) {
			tagSpec.getTags().add(createTag(tag.getKey(), tag.getValue()));
		}
		tagSpec.setResourceType("instance");
		return tagSpec;
	}
	
	@Override
	@Retryable(
			value = { RuntimeException.class },
					maxAttempts = AwsConstants.RETRY_ATTEMPTS,
					backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public Instance start(String instanceId) {
		log.trace("start(): instanceId={}", instanceId);
		
		try {
			final StartInstancesRequest request = new StartInstancesRequest();
			request.withInstanceIds(instanceId);
			
			final StartInstancesResult result = client.startInstances(request);
			final String requestId = result.getSdkResponseMetadata().getRequestId();
			final int statusCode = result.getSdkHttpMetadata().getHttpStatusCode();
		
			final Instance instance = waitForPendingState(instanceId, null); 
			
			log.info("action=startInstance; instanceId={}; requestId={}; status={}; {}", 
					instanceId, requestId, statusCode, Ec2.print(instance));
			return instance;
			
		} catch (AmazonClientException e) {
			log.warn("Failed to start EC2 instance; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw new RuntimeException("Failed to start EC2 instance", e);
		}
	}
	
	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public void stop(String instanceId) {
		log.trace("stop(): instanceId={}", instanceId);
		
		try {
			final StopInstancesRequest request = new StopInstancesRequest();
			request.withInstanceIds(instanceId);
			
			final StopInstancesResult result = client.stopInstances(request);
			final String requestId = result.getSdkResponseMetadata().getRequestId();
			final int statusCode = result.getSdkHttpMetadata().getHttpStatusCode();
		
			log.info("action=stopInstance; instanceId={}; requestId={}; status={}", 
						instanceId, requestId, statusCode);
		} catch (AmazonClientException e) {
			log.warn("Failed to stop EC2 instance; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw new RuntimeException("Failed to stop EC2 instance", e);
		}
	}
	
	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public void terminate(String instanceId) {
		log.trace("terminate(): instanceId={}", instanceId);
		
		try {
			final TerminateInstancesRequest request = new TerminateInstancesRequest();
			request.withInstanceIds(instanceId);
			
			final TerminateInstancesResult result = client.terminateInstances(request);
			final String requestId = result.getSdkResponseMetadata().getRequestId();
			final int statusCode = result.getSdkHttpMetadata().getHttpStatusCode();
		
			log.debug("action=terminateInstance; instanceId={}; requestId={}; status={}", 
						instanceId, requestId, statusCode);
		} catch (AmazonClientException e) {
			log.warn("Failed to terminate EC2 instance; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw new RuntimeException("Failed to terminate EC2 instance", e);
		}
	}
	
	private void safeTerminate(String instanceId) {
		log.trace("safeTerminate(): instanceId={}", instanceId);
		try {
			terminate(instanceId);
		} catch (Throwable t) {
			// log and swallow
			log.warn("Error during safeTerminate; instanceId={}; cause={}; message={}", 
					instanceId, t, t.getMessage());
		}
	}
	
	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public List<Instance> find(Map<String, String> tags) {
		log.trace("list(): tag={}", tags);
		
		try {
			final List<Instance> allInstances = Lists.newArrayList();
			final DescribeInstancesRequest request = new DescribeInstancesRequest();
			List<Filter> filters = new ArrayList<>();
			for (Map.Entry<String, String> tag : tags.entrySet()) {
				if(!Strings.isNullOrEmpty(tag.getKey()) && !Strings.isNullOrEmpty(tag.getValue())) {
					filters.add(new Filter("tag:".concat(tag.getKey()), Collections.singletonList(tag.getValue())));
				}
			}

			if (!filters.isEmpty()) {
				request.withFilters(filters);
			}
	
			final DescribeInstancesResult result = client.describeInstances(request);
			for (Reservation reservation : result.getReservations()) {
				allInstances.addAll(reservation.getInstances());
			}

			log.debug("action=findInstances; tags={}; found={}", tags, allInstances.size());
			
			return allInstances;
		} catch (AmazonClientException e) {
			log.warn("Failed to find EC2 instances; cause={}; message={}", e, e.getMessage());
			throw new RuntimeException("Failed to find EC2 instances", e);
		}
	}

	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public Instance describe(String instanceId) {
		log.trace("describe(): instanceId={}", instanceId);
		
		try {
			final DescribeInstancesRequest request = new DescribeInstancesRequest();
			request.withInstanceIds(instanceId);
			
			final DescribeInstancesResult result = client.describeInstances(request);
			final Instance instance = validateDescribeResult(result);
			if (instance == null) return null;
			
			final String requestId = result.getSdkResponseMetadata().getRequestId();
			final int statusCode = result.getSdkHttpMetadata().getHttpStatusCode();
			
			log.debug("action=describeInstance; {}; requestId={}; status={}", 
					Ec2.print(instance), requestId, statusCode);
			
			return instance;
		} catch (AmazonClientException e) {
			log.warn("Failed to describe EC2 instance; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw new RuntimeException("Failed to describe EC2 instance", e);
		}
	}
	
	/**
	 * Calls <code>describe</code> until instance <code>state</code> is not Pending, 
	 * and optionally is not the state supplied.
	 * <br/><br/>  
	 * Times out after <code>config.getLaunchTimeoutSec()</code>.
	 * 
	 * @param instanceId
	 * @param skipState state to avoid, can be null
	 * @return instance or <null/> if not valid
	 * @throws RuntimeException if unable to determine status before timeout
	 */
	private Instance waitForPendingState(String instanceId, InstanceState skipState) {
		log.trace("waitForPendingState() : instanceId={}; skipState={}", instanceId, skipState);
		
		long sleepMs = config.getMonitorPollingIntervalSecs() * 1000;
		
		final TimeoutTask<Instance> task = 
				new TimeoutTask<>("waitForState", config.getLaunchTimeoutSec(), TimeUnit.SECONDS);
		try {
			return task.execute(() -> {
				TimeUnit.MILLISECONDS.sleep(sleepMs);
				Instance instance = describe(instanceId);
				if (instance == null) return null;
				
				InstanceState state = Ec2.getState(instance);
				while (state.equals(InstanceState.PENDING) || state.equals(skipState)) {
					log.trace("state={}, waiting to refresh; instanceId={}; sleep={}ms", 
							state, instanceId, sleepMs); 
					if (Thread.currentThread().isInterrupted()) {
						log.info("waitForPendingState(): thread interrupted, exiting");
						break;
					}
					TimeUnit.MILLISECONDS.sleep(sleepMs);
					instance = describe(instanceId);
					state = Ec2.getState(instance); 
				}
				return instance;
			});
		} catch (TimeoutException e) {
			log.warn("Failed to determine instance state due to timeout; instanceId={}; message={}", 
					instanceId, e.getMessage());
			throw new RuntimeException("Timeout waiting for instance state", e);
		} catch (Throwable t) {
			log.warn("Failed to determine instance state; instanceId={}; cause={}; message={}", 
					instanceId, t, t.getMessage());
			throw new RuntimeException("Failed to determine instance state", t);
		}
	}
	
	@Override
	public boolean isProvisioned(String instanceId) {
		final Instance instance = waitForPendingState(instanceId, null);
		return Ec2.isProvisioned(instance);
	}

	@Override
	public boolean isRunning(String instanceId) {
		final Instance instance = waitForPendingState(instanceId, null);
		return Ec2.isRunning(instance);
	}

	private Instance validateRunResult(RunInstancesResult result) {
		if (result == null) return null;
		
		final Reservation reservation = result.getReservation();
		if (reservation == null) return null;
		
		final List<Instance> instances = reservation.getInstances();
		if (instances == null || instances.size() == 0) return null;
		
		return instances.get(0);
	}
		
	private Instance validateDescribeResult(DescribeInstancesResult result) {
		if (result == null) return null;
		
		final List<Reservation> reservations = result.getReservations();
		if (reservations == null || reservations.size() == 0) return null;
		
		final Reservation reservation = reservations.get(0);
		final List<Instance> instances = reservation.getInstances();
		if (instances == null || instances.size() == 0) return null;
		
		return instances.get(0);
	}
		
	private Tag createTag(String key, String value) {
		Tag tag = new Tag();
		tag.setKey(key);
		tag.setValue(value);
		return tag;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("config", config)
				.add("retryAttempts", AwsConstants.RETRY_ATTEMPTS)
				.add("retryDelay", AwsConstants.RETRY_DELAY)
				.toString();
	}

}
