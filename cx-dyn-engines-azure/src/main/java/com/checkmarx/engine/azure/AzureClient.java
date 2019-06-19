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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Launches and terminates Azure VM instances from a specified image.
 *  
 * @author ken.mcdonald@checkmarx.com
 *
 */
@Component
@Profile("azure")
public class AzureClient implements AzureComputeClient {
	
	private static final Logger log = LoggerFactory.getLogger(AzureClient.class);


	@Override
	public @NotNull VirtualMachine launch(@NotBlank String name, @NotBlank String instanceType, @NotNull Map<String, String> tags) throws Exception {
		return null;
	}

	@Override
	public @NotNull VirtualMachine start(@NotBlank String instanceId) throws InterruptedException, Exception {
		return null;
	}

	@Override
	public void stop(@NotBlank String instanceId) {

	}

	@Override
	public void terminate(@NotBlank String instanceId) {

	}

	@Override
	public @NotNull List<VirtualMachine> find(@NotNull Map<String, String> tags) {
		return null;
	}

	@Override
	public @NotNull VirtualMachine describe(@NotBlank String instanceId) {
		return null;
	}

	@Override
	public @NotNull VirtualMachine updateTags(@NotNull VirtualMachine instance, @NotNull Map<String, String> tags) {
		return null;
	}

	@Override
	public boolean isProvisioned(@NotBlank String instanceId) throws InterruptedException, Exception {
		return false;
	}

	@Override
	public boolean isRunning(@NotBlank String instanceId) throws InterruptedException, Exception {
		return false;
	}

	@Override
	public @NotNull AzureEngineConfig getConfig() {
		return null;
	}
}
