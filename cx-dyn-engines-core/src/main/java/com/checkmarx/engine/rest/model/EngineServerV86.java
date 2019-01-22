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
package com.checkmarx.engine.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EngineServerV86 extends EngineServer {
	
	public enum EngineState {
		
		Unknown(-1),
		Offline(0),
		Blocked(1),
		// what is status id - 2?
		Scanning(3),
		Idle(4);
		
		private long statusId;
		public long getStatusId() {
			return statusId;
		}
		
		private EngineState(int statusId) {
			this.statusId = statusId;
		}
		
		public boolean isBlocked() {
			return statusId == 1;
		}

		public Boolean isAlive() {
			return statusId > 0;
		}

		public static EngineState from(long statusId) {
			for (EngineState state : EngineState.values()) {
				if (statusId == state.statusId)
					return state;
			}
			return EngineState.Unknown;
		}
		
		public static EngineState from(Status state) {
			return state == null ? EngineState.Unknown : from(state.getId());
		}
		
		public static Status to(EngineState status) {
			return new Status(status.statusId, status.name());
		}

	}
	
	private Status status;
	
	public EngineServerV86() {
		super();
		// default .ctor for unmarshalling
	}
	
	public EngineServerV86(String name, String uri, int minLoc, int maxLoc, int maxScans, boolean isBlocked) {
		super(name, uri, minLoc, maxLoc, maxScans, isBlocked);
	}
	
	@Override
	public boolean isBlocked() {
		return status == null ? super.isBlocked() : EngineState.from(status).isBlocked();
	}
	
	@Override
	public Boolean isAlive() {
		return status == null ? super.isAlive() : EngineState.from(status).isAlive();
	}

	@JsonIgnore
	public EngineState getState() {
		return EngineState.from(status);
	}
	
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	protected MoreObjects.ToStringHelper toStringHelper() {
		return super.toStringHelper()
				.add("state", getState())
				.add("status", status);
	}

}
