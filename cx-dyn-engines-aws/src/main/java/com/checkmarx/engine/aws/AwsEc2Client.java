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
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;
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
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.AutomationExecution;
import com.amazonaws.services.simplesystemsmanagement.model.GetAutomationExecutionRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetAutomationExecutionResult;
import com.amazonaws.services.simplesystemsmanagement.model.StartAutomationExecutionRequest;
import com.amazonaws.services.simplesystemsmanagement.model.StartAutomationExecutionResult;
import com.checkmarx.engine.aws.Ec2.InstanceState;
import com.checkmarx.engine.utils.TaskManager;
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
	private final AWSSimpleSystemsManagement ssmClient;

	private final AwsEngineConfig config;
	private final TaskManager taskManager;
	
	public AwsEc2Client(@NotNull AwsEngineConfig config, @NotNull TaskManager taskManager) {
		this.client = AmazonEC2ClientBuilder.defaultClient();
		this.ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
		this.config = config;
		this.taskManager = taskManager;

		log.info("ctor(): {}", this);
	}
	
	@Override
	@NotNull
	public AwsEngineConfig getConfig() {
		return config;
	}
	
	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	@NotNull
	public Instance launch(@NotBlank String name, @NotBlank String instanceType, 
	        @NotNull Map<String,String> tags) throws Exception {
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
        } catch (CancellationException | InterruptedException | RejectedExecutionException e) {
            // do not retry, so throw original exception
		    handleLaunchException(e, instance, name);
		    throw new InterruptedException(e.getMessage());
		} catch (Throwable t) {
		    // retry, so throw RuntimeException
		    handleLaunchException(t, instance, name);
			throw new RuntimeException("Failed to launch EC2 instance", t);
		} finally {
			log.info("action={}, success={}; elapsedTime={}s; {}; requestId={}", 
					"launchInstance", success, timer.elapsed(TimeUnit.SECONDS), Ec2.print(instance), requestId);
		}
	}
	
	private void handleLaunchException(Throwable t, Instance instance, String name) {
        log.warn("Failed to launch EC2 instance; name={}; cause={}; message={}", 
                name, t, t.getMessage());
        if (instance != null) {
            final String instanceId = instance.getInstanceId();
            log.warn("Terminating failed instance launch; instanceId={}", instanceId);
            safeTerminate(instanceId);
        }
	}

	@NotNull
	private RunInstancesRequest createRunRequest(@NotBlank String name, @NotBlank String instanceType, @NotNull Map<String, String> tags) {
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

	@NotNull
	private TagSpecification createTagSpec(@NotBlank String name, @NotNull Map<String, String> tags) {
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
			value = { AmazonClientException.class },
					maxAttempts = AwsConstants.RETRY_ATTEMPTS,
					backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	@NotNull
	public Instance start(@NotBlank String instanceId) throws Exception {
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
			throw e;
		} catch (InterruptedException e) {
            log.warn("Failed to start EC2 instance; instanceId={}; cause={}; message={}", 
                    instanceId, e, e.getMessage());
            throw e;
        }
	}


	/**
	 * @Override
	 *        @Retryable(
	 *            value = { RuntimeException.class },
	 * 			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
	 * 			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	 * 	public void stop(@NotBlank String instanceId) {
	 * 		log.trace("stop(): instanceId={}", instanceId);
	 *
	 * 		try {
	 * 			final StopInstancesRequest request = new StopInstancesRequest();
	 * 			request.withInstanceIds(instanceId);
	 *
	 * 			final StopInstancesResult result = client.stopInstances(request);
	 * 			logResult(result, instanceId, "stopInstance", false);
	 *        } catch (AmazonClientException e) {
	 * 			log.warn("Failed to stop EC2 instance; instanceId={}; cause={}; message={}",
	 * 					instanceId, e, e.getMessage());
	 * 			throw new RuntimeException("Failed to stop EC2 instance", e);
	 *        }
	 *    }
	 *
	 *
	 */

	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public void stop(@NotBlank String instanceId) {
		log.trace("stop(): instanceId={}", instanceId);
		
		try {
			StartAutomationExecutionRequest request = new StartAutomationExecutionRequest();
			request.setDocumentName(config.getSsmAutomationDocument());
			request.setParameters(Collections.singletonMap("InstanceId", Collections.singletonList(instanceId)));
			StartAutomationExecutionResult result = ssmClient.startAutomationExecution(request);
			String executionId = result.getAutomationExecutionId();

			AutomationExecution execution = getAutomationExecution(executionId);
			log.debug("action=Stopping EC2 instance via SSM; instanceId={}; status={}", 
			        instanceId, execution.getAutomationExecutionStatus());
			// TODO: monitor In Progress -> Success

			log.debug("Execution id for SSM EC2 Instance shutdown: {}", executionId);
		} catch (AmazonClientException e) {
			log.warn("Failed to stop EC2 instance; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw new RuntimeException("Failed to stop EC2 instance", e);
		}
	}

	private AutomationExecution getAutomationExecution(String executionId){
		GetAutomationExecutionRequest automationExecutionRequest = new GetAutomationExecutionRequest().withAutomationExecutionId(executionId);
		GetAutomationExecutionResult automationExecutionResult = ssmClient.getAutomationExecution(automationExecutionRequest);
		return automationExecutionResult.getAutomationExecution();
	}

	@Override
	@Retryable(
			value = { RuntimeException.class },
			maxAttempts = AwsConstants.RETRY_ATTEMPTS,
			backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
	public void terminate(@NotBlank String instanceId) {
		log.trace("terminate(): instanceId={}", instanceId);
		
		try {
			final TerminateInstancesRequest request = new TerminateInstancesRequest();
			request.withInstanceIds(instanceId);
			
			final TerminateInstancesResult result = client.terminateInstances(request);
			logResult(result, instanceId, "terminateInstance", false);
		} catch (AmazonClientException e) {
			log.warn("Failed to terminate EC2 instance; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw new RuntimeException("Failed to terminate EC2 instance", e);
		}
	}
	
	private void safeTerminate(@NotBlank String instanceId) {
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
	@NotNull
	public List<Instance> find(@NotNull Map<String, String> tags) {
		log.trace("find(): tag={}", tags);
		
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
			logResult(result, "", "findInstances", true);
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
	@NotNull
	public Instance describe(@NotBlank String instanceId) {
		log.trace("describe(): instanceId={}", instanceId);
		
		try {
			final DescribeInstancesRequest request = new DescribeInstancesRequest();
			request.withInstanceIds(instanceId);
			
			final DescribeInstancesResult result = client.describeInstances(request);
			final Instance instance = getFirstInstance(result);
			
			logResult(result, instance.getInstanceId(), "describeInstance", true);
			
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
	 * @throws InterruptedException when interrupted while waiting for pending state
	 * @throws RuntimeException if unable to determine status before timeout
	 */
	@NotNull
	private Instance waitForPendingState(@NotBlank String instanceId, InstanceState skipState) throws Exception {
		log.trace("waitForPendingState() : instanceId={}; skipState={}", instanceId, skipState);
		
		long sleepMs = config.getMonitorPollingIntervalSecs() * 1000;
		
		final TimeoutTask<Instance> task = 
				new TimeoutTask<>("waitForState-"+instanceId, config.getLaunchTimeoutSec(), 
				        TimeUnit.SECONDS, taskManager);
		try {
			return task.execute(() -> {
				TimeUnit.MILLISECONDS.sleep(sleepMs);
				Instance instance = describe(instanceId);
				InstanceState state = Ec2.getState(instance);
				while (state.equals(InstanceState.PENDING) || state.equals(skipState)) {
					log.trace("state={}, waiting to refresh; instanceId={}; sleep={}ms", 
							state, instanceId, sleepMs); 
					if (Thread.currentThread().isInterrupted()) {
					    final String msg = "waitForPendingState() interrupted, exiting"; 
						throw new InterruptedException(msg);
					}
					TimeUnit.MILLISECONDS.sleep(sleepMs);
					instance = describe(instanceId);
					state = Ec2.getState(instance); 
				}
				return instance;
			});
        } catch (CancellationException | InterruptedException | RejectedExecutionException e) {
            log.warn("Failed to determine instance state due to interruption; instanceId={}; message={}", 
                    instanceId, e.getMessage());
            throw e;
		} catch (TimeoutException e) {
			log.warn("Failed to determine instance state due to timeout; instanceId={}; message={}", 
					instanceId, e.getMessage());
			throw new RuntimeException("Timeout waiting for instance state", e);
		} catch (Exception e) {
			log.warn("Failed to determine instance state; instanceId={}; cause={}; message={}", 
					instanceId, e, e.getMessage());
			throw e;
		}
	}
	
	@Override
	public boolean isProvisioned(@NotBlank String instanceId) throws Exception {
		final Instance instance = waitForPendingState(instanceId, null);
		return Ec2.isProvisioned(instance);
	}

	@Override
	public boolean isRunning(@NotBlank String instanceId) throws Exception {
		final Instance instance = waitForPendingState(instanceId, null);
		return Ec2.isRunning(instance);
	}

    @Override
    @Retryable(
            value = { RuntimeException.class },
            maxAttempts = AwsConstants.RETRY_ATTEMPTS,
            backoff = @Backoff(delay = AwsConstants.RETRY_DELAY))
    @NotNull
    public Instance updateTags(@NotNull Instance instance, @NotNull Tag... tags) {
        final String instanceId = instance.getInstanceId();
        log.trace("updateTags(): instance={}", instanceId);
        
        try {
            final CreateTagsRequest request = new CreateTagsRequest()
                    .withTags(tags)
                    .withResources(instanceId);
            final CreateTagsResult result = client.createTags(request);
            logResult(result, instanceId, "createTags", false);
            return describe(instanceId);
        } catch (AmazonClientException e) {
            log.warn("Failed to create tages on EC2 instance; instanceId={}; cause={}; message={}", 
                    instanceId, e, e.getMessage());
            throw new RuntimeException("Failed to create tags on EC2 instance", e);
        }
        
    }

    @NotNull
	private Instance validateRunResult(RunInstancesResult result) {
		if (result == null) throw new RuntimeException("EC2 RunInstanceResult is null");
	
		final Reservation reservation = result.getReservation();
		if (reservation == null) throw new RuntimeException("EC2 RunInstance reservation is null");
		
		final List<Instance> instances = reservation.getInstances();
		if (instances == null || instances.isEmpty()) 
		    throw new RuntimeException("RunInstance instance is empty or null");
		
		return instances.get(0);
	}
	
    @Nullable
	private Instance getFirstInstance(DescribeInstancesResult result) {
		final List<Reservation> reservations = result.getReservations();
		final Reservation reservation = reservations.get(0);
		final List<Instance> instances = reservation.getInstances();
		return instances.get(0);
	}
		
    private void logResult(AmazonWebServiceResult<?> result, String instanceId, @NotBlank String action, boolean debug) {
        if (result == null) return;

        final String requestId = result.getSdkResponseMetadata().getRequestId();
        final int statusCode = result.getSdkHttpMetadata().getHttpStatusCode();
    
        if (debug) {
            log.debug("action={}; instanceId={}; requestId={}; status={}", 
                    action, instanceId, requestId, statusCode);
        } else {
            log.info("action={}; instanceId={}; requestId={}; status={}", 
                    action, instanceId, requestId, statusCode);
        }
    }
    
	private Tag createTag(@NotBlank String key, String value) {
		return new Tag(key, value);
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
