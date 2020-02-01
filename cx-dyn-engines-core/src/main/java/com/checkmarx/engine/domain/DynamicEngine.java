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

import java.util.Objects;

import org.apache.commons.lang3.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import com.google.common.base.MoreObjects;

/**
 * @author randy@checkmarx.com
 *
 */
public class DynamicEngine implements Comparable<DynamicEngine> {
	
	private static final Logger log = LoggerFactory.getLogger(DynamicEngine.class);

	public enum State {
		ALL,
		LAUNCHING,
		SCANNING,
		EXPIRING,
		IDLE,
		UNPROVISIONED;
	}
	
	private final String name;
	private final String size;
	private State state = State.UNPROVISIONED;
	private EngineStats stats = new EngineStats();
	//private DateTime currentStateTime = DateTime.now();
	private DateTime timeToExpire;
	private Host host;
	//private Map<State, Duration> elapsedTimes = Maps.newConcurrentMap();
	private final int expireDurationSecs;
	//private DateTime launchTime;
	private String scanId;
	private String engineId;
	private EnginePool enginePool;

	public EnginePool getEnginePool() {
		return enginePool;
	}

	public void setEnginePool(EnginePool enginePool) {
		this.enginePool = enginePool;
	}
	
	public DynamicEngine(String name, String size, int expireDurationSecs) {
		this(name, size, expireDurationSecs, null);
	}
	
	public DynamicEngine(String name, String size, int expireDurationSecs, EnginePool enginePool) {
		this.name = name;
		this.size = size;
		this.expireDurationSecs = expireDurationSecs;
		this.enginePool = enginePool;
	}
	
	public static DynamicEngine fromProvisionedInstance(
			String name, String size, int expireDurationSecs,
			DateTime launchTime, DateTime startTime, boolean isRunning, String scanId, String engineId) {
	    
	    log.debug("fromProvisionedInstance(): name={}; size={}; expire={}; launchTime={}; isRunning={}; scanId={}; engineId={}", 
	            name, size, expireDurationSecs, launchTime, isRunning, scanId, engineId);
	    
		final DynamicEngine engine = new DynamicEngine(name, size, expireDurationSecs);
		engine.scanId = scanId;
		engine.engineId = engineId;
		engine.onLaunch(launchTime);
		if (isRunning) {
		    engine.onStart(startTime);
	        /*
		    // TODO-RJG: should we pass state?
			engine.state = State.IDLE;
			engine.timeToExpire = engine.calcExpirationTime();
			*/
		}
		return engine;
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
	
    public DateTime getLaunchTime() {
        return stats.getLaunchTime();
    }
    
	public DateTime getStartTime() {
		return stats.getStartTime();
	}
	
	public DateTime getTimeToExpire() {
		return timeToExpire;
	}
	
	private void setState(State toState) {
		final State curState = this.state; 
		log.debug("setState(): currentState={}; newState={}; {}", curState, toState, this);
		
		if (State.ALL.equals(toState)) throw new IllegalArgumentException("Cannot set engine state to ALL");
		
		//sanity check
		if (curState.equals(toState)) {
			log.warn("Setting DynamicEngine state to current state; state={}", toState);
			return;
		}
		
		this.state = toState;
		if (enginePool != null) enginePool.changeState(this, curState, toState);
	}
	
    public void setHost(Host server) {
        host = server;
        //stats.launchTime = server.getLaunchTime();
        //rjg: ensure calls to setHost start engine
    }
    
    public EngineStats getStats() {
        return stats;
    }
    
	/**
	 * Gets the elapsed time (duration) since the engine was launched.
	 * @return Duration since started
	 */
	public Duration getRunTime() {
	    return stats.getCurrentRunTime();
	}

	public String getScanId() {
		return scanId;
	}

	public void setScanId(String scanId) {
		this.scanId = scanId;
	}
	
	public String getEngineId() {
        return engineId;
    }

    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }
    
    // Engine instance events
    
    public void onLaunch(DateTime launchTime) {
        setState(State.LAUNCHING);
        stats.onLaunch(launchTime);
    }
    
    public void onStart(DateTime startTime) {
        stats.onStart(startTime);
    }
    
    public void onScan() {
        setState(State.SCANNING);
        timeToExpire = null;
        stats.onScan();
    }
    
    public void onIdle() {
        setState(State.IDLE);
        timeToExpire = stats.calcExpirationTime(expireDurationSecs);
        scanId = null;
        engineId = null;
        stats.onIdle();
    }
    
    public void onExpire() {
        log.debug("onExpire()");
        
        setState(State.EXPIRING);
    }

    
    public void onStop() {
        setState(State.UNPROVISIONED);
        host = null;
        timeToExpire = null;
        stats.onStop();
    }
    
    public void onTerminate() {
        onStop();
        stats.onTerminate();
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
                .addValue(stats)
                //.add("currentStateTime", currentStateTime.toString())
                //.add("launchTime", printInstance(launchTime))
				//.add("elapsedTime", getElapsedTime().getStandardSeconds())
				//.add("runTime", getRunTime().getStandardSeconds())
				.add("expireDurationSecs", expireDurationSecs)
				.add("timeToExpire", timeToExpire)
				.add("scanId", scanId)
                .add("engineId", engineId)
				.add("host", host)
				//.add("elapsedTimes", "[" + printElapsedTimes() + "]")
				//.omitNullValues()
				.toString();
	}

//    private Object printInstance(DateTime time) {
//        if (time == null) return null;
//        return time.toInstant();
//    }
    
    public static class EngineStats {

        private static final Logger log = LoggerFactory.getLogger(EngineStats.class);

        private DateTime launchTime;
        private DateTime startTime;
        private DateTime currentStateTime = DateTime.now();
        
        // starts when engine is provisioned (launched), resets when engine is terminated
        private StopWatch provisionedTime = new StopWatch();     

        // starts each time engine is started, resets on stop
        private StopWatch currentRunTime = new StopWatch();
        
        // starts on each scan, resets when idle/stopped
        private StopWatch currentScanTime = new StopWatch();
        
        // starts on idle, resets on scan/stop
        private StopWatch currentIdleTime = new StopWatch();
        
        // starts/resumes on idle, pauses on scanning, resets on stop
        private StopWatch cycleIdleTime = new StopWatch();

        // starts/resumes on scan, pauses on idle, resets on stop
        private StopWatch cycleScanTime = new StopWatch();
        
        // starts/resumes on idle, pauses when scanning, resets on terminate
        private StopWatch totalIdleTime = new StopWatch();

        // starts/resumes on start, pauses on stop, resets on terminate
        private StopWatch totalRunTime = new StopWatch();

        // starts/resumes on scan, pauses when idle/stopped, resets on terminate
        private StopWatch totalScanTime = new StopWatch();
        
        // starts/resumes on stop, pauses when started, resets on terminate
        private StopWatch totalStoppedTime = new StopWatch();

        private long scanCount;
        
        public DateTime getLaunchTime() {
            return launchTime;
        }

        public DateTime getStartTime() {
            return startTime;
        }
        
        public DateTime getCurrentStateTime() {
            return currentStateTime;
        }

        public Duration getProvisionedTime() {
            return new Duration(provisionedTime.getTime());
        }

        public Duration getCurrentRunTime() {
            return new Duration(currentRunTime.getTime());
        }

        public Duration getCurrentScanTime() {
            return new Duration(currentScanTime.getTime());
        }

        public Duration getCurrentIdleTime() {
            return new Duration(currentIdleTime.getTime());
        }

        public Duration getTotalRunTime() {
            return new Duration(totalRunTime.getTime());
        }

        public Duration getCycleIdleTime() {
            return new Duration(cycleIdleTime.getTime());
        }

        public Duration getCycleScanTime() {
            return new Duration(cycleScanTime.getTime());
        }

        public Duration getTotalIdleTime() {
            return new Duration(totalIdleTime.getTime());
        }

        public Duration getTotalScanTime() {
            return new Duration(totalScanTime.getTime());
        }

        public Duration getTotalStoppedTime() {
            return new Duration(totalStoppedTime.getTime());
        }

        public long getScanCount() {
            return scanCount;
        }

        private void startOrResume(StopWatch stopWatch) {
            if (stopWatch.isSuspended()) {
                stopWatch.resume();
            } else if (stopWatch.isStopped()) {
                stopWatch.start();
            }
        }

        private void suspendIfStarted(StopWatch stopWatch) {
            if (stopWatch.isStarted() && !stopWatch.isSuspended()) {
                stopWatch.suspend();
            }
        }
        
        private DateTime calcExpirationTime(int expirationSeconds) {
            if (startTime == null) 
                throw new IllegalStateException("Engine has not been started, cannot calculate expiration time");
            
            final Duration runDuration = new Duration(currentRunTime.getTime());
            final Long factor = Math.floorDiv(runDuration.getStandardSeconds(), expirationSeconds) + 1;
            return startTime.plusSeconds(factor.intValue() * expirationSeconds);
        }
        
        public boolean isLaunched() {
            return launchTime != null;
        }
        
        public boolean isRunning() {
            return startTime != null;
        }
        
        // events
        public void onLaunch(@Nullable DateTime launchTime) {
            log.debug("onLaunch()");
            
            if (provisionedTime.isStopped())
                provisionedTime.start();
            this.launchTime = launchTime != null ? launchTime : new DateTime(provisionedTime.getStartTime());
            //onStart(launchTime);
        }
        
        public void onStart(@Nullable DateTime startTime) {
            log.debug("onStart()");

            if (currentRunTime.isStopped()) {
                currentRunTime.start();
                currentStateTime = startTime;
            }
            this.startTime = startTime != null ? startTime : new DateTime(currentRunTime.getStartTime());
            if (launchTime == null) {
                onLaunch(startTime);
            }
            startOrResume(totalRunTime);
            suspendIfStarted(totalStoppedTime);
        }
        
        public void onIdle() {
            log.debug("onIdle()");

            if (currentIdleTime.isStopped()) {
                currentIdleTime.start();
                currentStateTime = new DateTime(currentIdleTime.getStartTime());
            }
            startOrResume(cycleIdleTime);
            startOrResume(totalIdleTime);
            currentScanTime.reset();
            suspendIfStarted(cycleScanTime);
            suspendIfStarted(totalScanTime);
        }
        
        public void onScan() {
            log.debug("onScan()");

            scanCount++;
            currentScanTime.start();
            startOrResume(cycleScanTime);
            startOrResume(totalScanTime);
            currentIdleTime.reset();
            suspendIfStarted(cycleIdleTime);
            suspendIfStarted(totalIdleTime);
            currentStateTime = new DateTime(currentScanTime.getStartTime());
        }
        
        public void onExpiring() {
            log.debug("onExpiring()");
            
            // rjg: for now, not tracking expiring state 
        }

        public void onStop() {
            log.debug("onStop()");

            currentStateTime = DateTime.now();
            startOrResume(totalStoppedTime);
            startTime = null;
            currentRunTime.reset();
            suspendIfStarted(totalRunTime);
            currentIdleTime.reset();
            cycleIdleTime.reset();
            suspendIfStarted(totalIdleTime);
            currentScanTime.reset();
            cycleScanTime.reset();
            suspendIfStarted(totalScanTime);
        }
        
        public void onTerminate() {
            log.debug("onTerminate()");
            
            currentStateTime = DateTime.now();
            launchTime = null;
            startTime = null;
            provisionedTime.reset();
            currentIdleTime.reset();
            currentRunTime.reset();
            currentScanTime.reset();
            cycleIdleTime.reset();
            cycleScanTime.reset();
            totalRunTime.reset();
            totalIdleTime.reset();
            totalScanTime.reset();
            totalStoppedTime.reset();
        }
        
        public void reset() {
            log.debug("reset()");

            scanCount = 0L;
            onTerminate();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("scanCount", scanCount)
                .add("currentStateAt", currentStateTime)
                .add("launchedAt", launchTime)
                .add("startedAt", startTime)
                .add("provisionedTime", provisionedTime)
                .add("currentIdleTime", currentIdleTime)
                .add("currentRunTime", currentRunTime)
                .add("currentScanTime", currentScanTime)
                .add("cycleIdleTime", cycleIdleTime)
                .add("cycleScanTime", cycleScanTime)
                .add("totalRunTime", totalRunTime)
                .add("totalIdleTime", totalIdleTime)
                .add("totalScanTime", totalScanTime)
                .add("totalStoppedTime", totalStoppedTime)
                .toString();
        }
    }

}
