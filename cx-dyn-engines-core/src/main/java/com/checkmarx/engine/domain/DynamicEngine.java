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
/**
 * 
 */
package com.checkmarx.engine.domain;

import java.util.Map;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * @author rjgeyer
 *
 */
public class DynamicEngine implements Comparable<DynamicEngine> {
	
	private static final Logger log = LoggerFactory.getLogger(DynamicEngine.class);

	public enum State {
		ALL,
		SCANNING,
		EXPIRING,
		IDLE,
		UNPROVISIONED;
	}
	
	private final String name;
	private final String size;
	private State state = State.UNPROVISIONED;
	private DateTime currentStateTime = DateTime.now();
	private DateTime timeToExpire;
	private Host host;
	private Map<State, Duration> elapsedTimes = Maps.newConcurrentMap();
	private final long expireDurationSecs;
	private DateTime launchTime;
	private Long scanId;
	private EnginePool enginePool;

	public EnginePool getEnginePool() {
		return enginePool;
	}

	public void setEnginePool(EnginePool enginePool) {
		this.enginePool = enginePool;
	}
	
	public DynamicEngine(String name, String size, long expireDurationSecs) {
		this(name, size, expireDurationSecs, null);
	}
	
	public DynamicEngine(String name, String size, long expireDurationSecs, EnginePool enginePool) {
		this.name = name;
		this.size = size;
		this.expireDurationSecs = expireDurationSecs;
		this.enginePool = enginePool;
		initElapsedTimes();
	}
	
	public static DynamicEngine fromProvisionedInstance(
			String name, String size, long expireDurationSecs,
			DateTime launchTime, boolean isRunning) {
		final DynamicEngine engine = new DynamicEngine(name, size, expireDurationSecs);
		engine.launchTime = launchTime;
		if (isRunning) {
			engine.state = State.IDLE;
			engine.timeToExpire = engine.calcExpirationTime();
		}
		return engine;
	}
	
	private void initElapsedTimes() {
		final Duration zero = new Duration(0);
		for (State state : State.values()) {
			elapsedTimes.put(state, zero);
		}
	}

	public String getName() {
		return name;
	}

	public String getSize() {
		return size;
	}

	public State getState() {
		return state;
	}

	public Host getHost() {
		return host;
	}
	
	public String getUrl() {
		return host == null ? null : host.getCxManagerUrl();
	}
	
	public DateTime getCurrentStateTime() {
		return currentStateTime;
	}
	
	public DateTime getLaunchTime() {
		return launchTime;
	}
	
	public DateTime getTimeToExpire() {
		return timeToExpire;
	}
	
	public void setState(State toState) {
		final State curState = this.state; 
		log.debug("setState(): currentState={}; newState={}; {}", curState, toState, this);
		
		//sanity check
		if (curState.equals(toState)) {
			log.warn("Setting DynamicEngine state to current state; state={}", toState);
			return;
		}
		
		// before changing state, update current state elapsed time
		final Duration currentDuration = elapsedTimes.get(this.state); 
		elapsedTimes.put(this.state, currentDuration.plus(getElapsedTime()));

		// if current state is UNPROVISIONED, set launch time
		if (curState.equals(State.UNPROVISIONED)) {
			launchTime = DateTime.now();
			if (host != null && host.getLaunchTime() != null) {
				launchTime = host.getLaunchTime();
			}
			//timeToExpire = launchTime.plusSeconds(Math.toIntExact(this.expireDurationSecs));
		}

		// update state
		this.state = toState;
		currentStateTime = DateTime.now();
		
		// if new state is UNPROVISIONED, clear applicable items
		switch (toState) {
			case UNPROVISIONED :
				host = null;
				launchTime = null;
				timeToExpire = null;
				break;
			case IDLE : 
				timeToExpire = calcExpirationTime();
				break;
			case SCANNING :
				timeToExpire = null;
				break;
			default:
				break;
		}
		if (enginePool != null) enginePool.changeState(this, curState, toState);
	}
	
	DateTime calcExpirationTime() {
		final Duration runTime = getRunTime();
		Long factor = Math.floorDiv(runTime.getStandardSeconds(), expireDurationSecs) + 1;
		// set the expiration time 2 minutes before the next interval increment
		return launchTime.plusSeconds(factor.intValue() * Math.toIntExact(expireDurationSecs));
	}

	/**
	 * Gets the elapsed time (duration) in the current state
	 * @return Duration since last state transition
	 */
	public Duration getElapsedTime() {
		return new Duration(currentStateTime, DateTime.now());  
	}
	
	/**
	 * Gets the elapsed time (duration) since the engine was launched.
	 * @return Duration since launched
	 */
	public Duration getRunTime() {
		if (launchTime == null) return Duration.ZERO;
		return new Duration(launchTime, DateTime.now());  
	}

	public void setHost(Host server) {
		this.host = server;
		this.launchTime = server.getLaunchTime();
	}
	
	public String printElapsedTimes() {
		final StringBuilder sb = new StringBuilder();
		elapsedTimes.forEach((state,duration) -> {
			if (state.equals(State.ALL)) return;
			if (state.equals(this.state)) duration = duration.plus(getElapsedTime()); 
			sb.append(String.format("%s:%ss, ", state, duration.getStandardSeconds()));
		});
		return sb.toString().replaceAll(", $", "");
	}

	public Long getScanId() {
		return scanId;
	}

	public void setScanId(Long scanId) {
		this.scanId = scanId;
	}

	// name and size are only immutable properties
	@Override
	public int hashCode() {
		return Objects.hash(name, size);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final DynamicEngine other = (DynamicEngine) obj;
		return Objects.equals(this.name, other.name)
				&& Objects.equals(this.size, other.size);
	}

	@Override
	public int compareTo(DynamicEngine o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", name)
				.add("size", size)
				.add("state", state)
				.add("elapsedTime", getElapsedTime().getStandardSeconds())
				.add("launchTime", launchTime)
				.add("runTime", getRunTime().getStandardSeconds())
				.add("currentStateTime", currentStateTime)
				.add("expireDurationSecs", expireDurationSecs)
				.add("timeToExpire", timeToExpire)
				.add("scanId", scanId)
				.add("host", host)
				.add("elapsedTimes", "[" + printElapsedTimes() + "]")
				//.omitNullValues()
				.toString();
	}

}
