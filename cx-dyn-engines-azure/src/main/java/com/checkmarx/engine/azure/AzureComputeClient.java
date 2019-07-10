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
package com.checkmarx.engine.azure;


import com.microsoft.azure.management.compute.VirtualMachine;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public interface AzureComputeClient {

	/**
	 * Launches an Azure instance with the supplied parameters. 
	 *  
	 * @param name of instance
	 * @param instanceType of Azure instance, e.g. m4.large
	 * @param tags to include with the instance
	 * @return the running Azure instance
	 * @throws Exception 
	 */
    @NotNull
	VirtualMachine launch(@NotBlank String name, @NotBlank String instanceType, @NotNull Map<String, String> tags) throws Exception;

	/**
	 * Starts an Azure instance.
	 *  
	 * @param instanceId to start
	 * @throws InterruptedException if interrupted while waiting for instance to start
	 * @throws Exception 
	 */
    @NotNull
	VirtualMachine start(@NotBlank String instanceId) throws InterruptedException, Exception;

	/**
	 * Stops an Azure instance
	 * 
	 * @param instanceId to stop
	 */
	void stop(@NotBlank String instanceId);

	/**
	 * Terminates an Azure instance.
	 * 
	 * @param instanceId to terminate
	 */
	void terminate(@NotBlank String instanceId);
	
	/**
	 * Queries AWS for Azure instances with the supplied tag info
	 * 
	 * @param tags Map containing tag,value 
	 * @return list of instances matching the supplied tag
	 */
    @NotNull
	List<VirtualMachine> find(@NotNull Map<String, String> tags);

	/**
	 * Describes an Azure instance
	 * 
	 * @param instanceId to describe
	 * @return the instance
	 */
    @NotNull
	VirtualMachine describe(@NotBlank String instanceId);
	
	/**
	 * Updates the tags for the supplied Azure instance.
	 * 
	 * @param instance Azure instance to update tags on
	 * @param tags the tags to set
	 * @return updated Instance
	 */
    @NotNull
	VirtualMachine updateTags(@NotNull VirtualMachine instance, @NotNull Map<String, String> tags);
	
	/**
	 * Returns true if the Azure instance is provisioned.  
	 * Provisioned is defined as exists and not terminated or shutting down for termination.
	 * 
	 * @param instanceId to check
	 * @return <code>true</code> if instance is provisioned
	 * @throws InterruptedException when interrupted while waiting for pending state
	 * @throws Exception 
	 */
	boolean isProvisioned(@NotBlank String instanceId) throws InterruptedException, Exception;

	/**
	 * Returns true if the Azure instance is running (started).  
	 * 
	 * @param instanceId to check
	 * @return <code>true</code> if instance is provisioned
	 * @throws InterruptedException when interrupted while state is pending
	 * @throws Exception 
	 */
	boolean isRunning(@NotBlank String instanceId) throws InterruptedException, Exception;

	/**
	 * @return the current Azure configuration
	 */
	@NotNull
	AzureEngineConfig getConfig();

}
