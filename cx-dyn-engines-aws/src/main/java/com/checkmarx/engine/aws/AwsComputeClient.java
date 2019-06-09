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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

public interface AwsComputeClient {

	/**
	 * Launches an EC2 instance with the supplied parameters. 
	 *  
	 * @param name of instance
	 * @param instanceType of EC2 instance, e.g. m4.large
	 * @param tags to include with the instance
	 * @return the running EC2 instance
	 * @throws Exception 
	 */
    @NotNull
	Instance launch(@NotBlank String name, @NotBlank String instanceType, @NotNull Map<String, String> tags) throws Exception;

	/**
	 * Starts an EC2 instance.
	 *  
	 * @param instanceId to start
	 * @throws InterruptedException if interrupted while waiting for instance to start
	 * @throws Exception 
	 */
    @NotNull
	Instance start(@NotBlank String instanceId) throws InterruptedException, Exception;

	/**
	 * Stops an EC2 instance
	 * 
	 * @param instanceId to stop
	 */
	void stop(@NotBlank String instanceId);

	/**
	 * Terminates an EC2 instance.
	 * 
	 * @param instanceId to terminate
	 */
	void terminate(@NotBlank String instanceId);
	
	/**
	 * Queries AWS for EC2 instances with the supplied tag info
	 * 
	 * @param tags Map containing tag,value 
	 * @return list of instances matching the supplied tag
	 */
    @NotNull
	List<Instance> find(@NotNull Map<String, String> tags);

	/**
	 * Describes an EC2 instance
	 * 
	 * @param instanceId to describe
	 * @return the instance
	 */
    @NotNull
	Instance describe(@NotBlank String instanceId);
	
	/**
	 * Updates the tags for the supplied EC2 instance.
	 * 
	 * @param instance EC2 instance to update tags on
	 * @param tags the tags to set
	 * @return updated Instance
	 */
    @NotNull
    Instance updateTags(@NotNull Instance instance, @NotNull Tag... tags);
	
	/**
	 * Returns true if the EC2 instance is provisioned.  
	 * Provisioned is defined as exists and not terminated or shutting down for termination.
	 * 
	 * @param instanceId to check
	 * @return <code>true</code> if instance is provisioned
	 * @throws InterruptedException when interrupted while waiting for pending state
	 * @throws Exception 
	 */
	boolean isProvisioned(@NotBlank String instanceId) throws InterruptedException, Exception;

	/**
	 * Returns true if the EC2 instance is running (started).  
	 * 
	 * @param instanceId to check
	 * @return <code>true</code> if instance is provisioned
	 * @throws InterruptedException when interrupted while state is pending
	 * @throws Exception 
	 */
	boolean isRunning(@NotBlank String instanceId) throws InterruptedException, Exception;

	/**
	 * @return the current AWS configuration
	 */
	@NotNull
	AwsEngineConfig getConfig();

}
