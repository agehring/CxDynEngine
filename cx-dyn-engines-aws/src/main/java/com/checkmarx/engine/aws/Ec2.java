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

import javax.validation.constraints.NotNull;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class Ec2 {
	
	public enum InstanceState {
		PENDING(0),
		RUNNING(16),
		SHUTTING_DOWN(32),
		TERMINATED(48),
		STOPPING(64),
		STOPPED(80),
		UNKNOWN(9999);
		
		
		private int code;
		public int getCode() {
			return code;
		}

		private InstanceState(int code) {
			this.code = code;
		}
		
		public static InstanceState from(int code) {
			for (InstanceState state : InstanceState.values()) {
				if (code == state.code)
					return state;
			}
			return InstanceState.UNKNOWN;
		}
	}
	
	private Ec2() {
		// static class
	}
	
	public static String getName(@NotNull Instance instance) {
		return getTag(instance, AwsConstants.NAME_TAG);
	}

	public static String getTag(@NotNull Instance instance, String key) {
		final List<Tag> tags = instance.getTags();
		if (tags == null) return null;
		
		final Optional<Tag> theTag = Iterables.tryFind(tags, new Predicate<Tag>() {
		    @Override
		    public boolean apply(final Tag tag) {
		        return tag.getKey().equals(key);
		    }
		});
		return theTag.isPresent() ? theTag.get().getValue() : null;
	}
	
	public static InstanceState getState(Instance instance) {
		return instance == null ? InstanceState.TERMINATED 
				: InstanceState.from(instance.getState().getCode());
	}
	
	public static boolean isProvisioned(@NotNull Instance instance) {
		return !isTerminated(instance);
	}
	
    public static boolean isStopping(Instance instance) {
        final InstanceState state = getState(instance);
        return InstanceState.STOPPING.equals(state);
    }

	public static boolean isTerminated(@NotNull Instance instance) {
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
	
	public static boolean isRunning(@NotNull Instance instance) {
		final InstanceState state = getState(instance);
		return InstanceState.RUNNING.equals(state);
	}

	public static String print(Instance instance) {
		if (instance == null) return "null";
		
		final String tags = printTags(instance.getTags(), false);
		return MoreObjects.toStringHelper(instance)
				.add("id", instance.getInstanceId())
				.add("name", getName(instance))
				.add("state", instance.getState().getName().toUpperCase())
				.add("type", instance.getInstanceType())
				.add("imageId", instance.getImageId())
				.add("privateIp", instance.getPrivateIpAddress())
				.add("publicIp", instance.getPublicIpAddress())
				.add("launchTime", instance.getLaunchTime().toInstant().toString())
				.add("tags", "[" + tags + "]")
				.toString();
	}
	
	
	
	public static String printTags(@NotNull List<Tag> tags, boolean includeName) {
		final StringBuilder sb = new StringBuilder();
		for (Tag tag : tags) {
	    	final String key = tag.getKey();
	    	if (!includeName && AwsConstants.NAME_TAG.equals(key)) continue;
			sb.append(String.format("%s=%s; ", key, tag.getValue()));
		}
		return sb.toString().replaceAll("; $", "");
	}

}
