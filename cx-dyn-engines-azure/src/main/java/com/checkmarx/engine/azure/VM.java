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

import com.google.common.base.MoreObjects;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class VM {
	
	public enum InstanceState {
		PENDING(PowerState.STARTING.toString()),
		RUNNING(PowerState.RUNNING.toString()),
		SHUTTING_DOWN(PowerState.STOPPING.toString()),
		TERMINATED(PowerState.STOPPED.toString()),
		STOPPING(PowerState.DEALLOCATING.toString()),
		STOPPED(PowerState.STOPPED.toString()),
		UNKNOWN(PowerState.UNKNOWN.toString());
		
		
		private String code;
		public String getCode() {
			return code;
		}

		InstanceState(String code) {
			this.code = code;
		}
		
		public static InstanceState from(String code) {
			for (InstanceState state : InstanceState.values()) {
				if (code.equalsIgnoreCase(state.code))
					return state;
			}
			return InstanceState.UNKNOWN;
		}
	}

	public static String getName(@NotNull VirtualMachine instance) {
		//return getTag(instance, AzureConstants.NAME_TAG);
		return instance.name();
	}

	public static String getTag(@NotNull VirtualMachine instance, String key) {
		final Map<String, String> tags = instance.tags();
		if (tags == null) return null;

		return tags.getOrDefault(key, null);
	}
	
	public static InstanceState getState(VirtualMachine instance) {
		return instance == null ? InstanceState.TERMINATED 
				: InstanceState.from(instance.powerState().toString());
	}
	
	public static boolean isProvisioned(@NotNull VirtualMachine instance) {
		return !isTerminated(instance);
	}

	public static boolean isStopping(@NotNull VirtualMachine instance) {
		final InstanceState state = getState(instance);
		return InstanceState.STOPPING.equals(state);
	}

	public static boolean isTerminated(@NotNull VirtualMachine instance) {
		final InstanceState state = getState(instance);
		switch (state) {
			case SHUTTING_DOWN:
			case TERMINATED:
			case UNKNOWN:
				return true;
			default:
				return false;
		}
	}
	
	public static boolean isRunning(@NotNull VirtualMachine instance) {
		final InstanceState state = getState(instance);
		return InstanceState.RUNNING.equals(state);
	}

	public static String print(VirtualMachine instance) {
		if (instance == null) return "null";
		
		final String tags = printTags(instance.tags(), false);
		return MoreObjects.toStringHelper(instance)
				.add("id", instance.id())
				.add("name", getName(instance))
				.add("state", instance.powerState().toString().toUpperCase())
				.add("type", instance.type())
				//.add("imageId", instance.vmId()) //TODO - cannot find image Id in API
				.add("privateIp", instance.getPrimaryNetworkInterface().primaryPrivateIP())
				.add("publicIp", instance.getPrimaryPublicIPAddressId())
				.add("tags", "[" + tags + "]")
				.toString();
	}
	
	
	
	public static String printTags(@NotNull Map<String, String> tags, boolean includeName) {
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			if (!includeName && AzureConstants.NAME_TAG.equals(tag.getValue())) continue;
			sb.append(String.format("%s=%s; ", tag.getKey(), tag.getValue()));
		}
		return sb.toString().replaceAll("; $", "");
	}

}
