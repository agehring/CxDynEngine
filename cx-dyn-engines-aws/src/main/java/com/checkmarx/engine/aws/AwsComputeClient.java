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

import com.amazonaws.services.ec2.model.Instance;

public interface AwsComputeClient {

	/**
	 * Launches an EC2 instance with the supplied parameters. 
	 *  
	 * @param name of instance
	 * @param instanceType of EC2 instance, e.g. m4.large
	 * @param tags to include with the instance
	 * @return the running EC2 instance
	 */
	Instance launch(String name, String instanceType, Map<String, String> tags);

	/**
	 * Starts an EC2 instance.
	 *  
	 * @param instanceId to start
	 */
	Instance start(String instanceId);

	/**
	 * Stops an EC2 instance
	 * 
	 * @param instanceId to stop
	 */
	void stop(String instanceId);

	/**
	 * Terminates an EC2 instance.
	 * 
	 * @param instanceId to terminate
	 */
	void terminate(String instanceId);
	
	/**
	 * Queries AWS for EC2 instances with the supplied tag info
	 * 
	 * @param tag tag key
	 * @param values tag values to match
	 * @return list of instances matching the supplied tag
	 */
	List<Instance> find(String tag, String... values);

	/**
	 * Describes an EC2 instance
	 * 
	 * @param instanceId to describe
	 * @return the instance
	 */
	Instance describe(String instanceId);
	
	/**
	 * Returns true if the EC2 instance is provisioned.  
	 * Provisioned is defined as exists and not terminated or shutting down for termination.
	 * 
	 * @param instanceId to check
	 * @return <code>true</code> if instance is provisioned
	 */
	boolean isProvisioned(String instanceId);

	/**
	 * Returns true if the EC2 instance is running (started).  
	 * 
	 * @param instanceId to check
	 * @return <code>true</code> if instance is provisioned
	 */
	boolean isRunning(String instanceId);

	/**
	 * @return the current AWS configuration
	 */
	AwsEngineConfig getConfig();

}
