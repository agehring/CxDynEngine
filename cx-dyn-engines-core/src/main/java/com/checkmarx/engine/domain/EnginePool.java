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
package com.checkmarx.engine.domain;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.checkmarx.engine.domain.DynamicEngine.State;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class EnginePool {

	private static final Logger log = LoggerFactory.getLogger(EnginePool.class);
	
	/**
	 * map of all engines by name; key = engine name
	 */
	private final Map<String, DynamicEngine> allNamedEngines = Maps.newConcurrentMap();

	/**
	 * map of all engines by size; key = engine size (string)
	 */
	private final Map<String, Set<DynamicEngine>> allSizedEngines = Maps.newConcurrentMap();
	private final Map<String, Set<DynamicEngine>> activeEngines = Maps.newConcurrentMap();
	private final Map<String, Set<DynamicEngine>> idleEngines = Maps.newConcurrentMap();
	private final Map<String, Set<DynamicEngine>> expiringEngines = Maps.newConcurrentMap();
	private final Map<String, Set<DynamicEngine>> unprovisionedEngines = Maps.newConcurrentMap();

	/**
	 * map of engine maps by State, then by size name
	 * 1st key=engine state, 2nd key = engine size
	 */
	private final Map<State, Map<String, Set<DynamicEngine>>> engineMaps = Maps.newEnumMap(State.class);

	/**
	 * map of engine counts by size; key=ScanSize
	 * immutable after initialization
	 */
	private final Map<EngineSize, AtomicLong> engineSizes = Maps.newLinkedHashMap();
	
	/**
	 * map of scan sizes; key=size name (string)
	 */
	private final Map<String, EngineSize> scanSizes = Maps.newConcurrentMap();
	
	
	/**
	 * map of minimum engines by size; key=size name (string)
	 */
	private final Map<String, Integer> poolMins = Maps.newConcurrentMap();
	
	
	public EnginePool(Set<EnginePoolEntry> entries, Set<DynamicEngine> engines) {
		this(entries);
		engines.forEach(engine->addEngine(engine));
	}
	
	protected EnginePool(Set<EnginePoolEntry> entries) {
		engineMaps.put(DynamicEngine.State.ALL, allSizedEngines);
		engineMaps.put(DynamicEngine.State.SCANNING, activeEngines);
		engineMaps.put(DynamicEngine.State.EXPIRING, expiringEngines);
		engineMaps.put(DynamicEngine.State.IDLE, idleEngines);
		engineMaps.put(DynamicEngine.State.UNPROVISIONED, unprovisionedEngines);
		initSizeMaps(entries);
	}
	
	private void initSizeMaps(Set<EnginePoolEntry> entries) {
		entries.forEach((entry) -> {
			final EngineSize scanSize = entry.getScanSize();
			final String size = scanSize.getName();
			scanSizes.put(scanSize.getName(), scanSize);
			poolMins.put(size, entry.getMinimum());
			engineSizes.put(scanSize, new AtomicLong(0));
			engineMaps.forEach((k, map)->initEngineMaps(size, map));
			log.info("Adding engine size; {}", scanSize); 
		});
	}

	private void initEngineMaps(String size, Map<String, Set<DynamicEngine>> map) {
		if (map.containsKey(size)) return;
		map.put(size, Sets.newTreeSet());
	}
	
	private void addEngine(DynamicEngine engine) {
		final String size = engine.getSize();
		final EngineSize scanSize = scanSizes.get(size);
		final State state = engine.getState();
		
		//initEngineSizes(size);
		engineSizes.get(scanSize).incrementAndGet();
		allSizedEngines.get(size).add(engine);
		engineMaps.get(state).get(size).add(engine);
		allNamedEngines.put(engine.getName(), engine);
		engine.setEnginePool(this);
	}
	
	public int getEngineCount() {
		return allNamedEngines.size();
	}

	public DynamicEngine getEngineByName(String name) {
		return allNamedEngines.get(name);
	}

	ImmutableMap<String, DynamicEngine> getAllEnginesByName() {
		return ImmutableMap.copyOf(allNamedEngines);
	}

	ImmutableMap<String, Set<DynamicEngine>> getAllEnginesBySize() {
		return ImmutableMap.copyOf(allSizedEngines);
	}

	ImmutableMap<String, Set<DynamicEngine>> getActiveEngines() {
		return ImmutableMap.copyOf(activeEngines);
	}

	ImmutableMap<String, Set<DynamicEngine>> getIdleEngines() {
		return ImmutableMap.copyOf(idleEngines);
	}

	ImmutableMap<String, Set<DynamicEngine>> getExpiringEngines() {
		return ImmutableMap.copyOf(expiringEngines);
	}

	ImmutableMap<String, Set<DynamicEngine>> getUnprovisionedEngines() {
		return ImmutableMap.copyOf(unprovisionedEngines);
	}
	
	public IdleEngineMonitor createIdleEngineMonitor(BlockingQueue<DynamicEngine> expiringEngines, int expireBufferMins) {
		return new IdleEngineMonitor(this, expiringEngines, expireBufferMins);
	}

	/**
	 * Replaces an existing engine with the supplied engine.
	 * 
	 * @param newEngine to add
	 * @return the old engine that was replaced
	 */
	public DynamicEngine addExistingEngine(DynamicEngine newEngine) {
		log.trace("addExistingEngine(): {}", newEngine);
		
		final String name = newEngine.getName();
		final String size = newEngine.getSize();
		
		final DynamicEngine curEngine = allNamedEngines.get(name);
		if (curEngine == null) return null;
		
		final State curState = curEngine.getState();

		allSizedEngines.get(size).remove(curEngine);
		engineMaps.get(curState).get(size).remove(curEngine);
		
		addEngine(newEngine);
		
		log.info("action=addExistingEngine; {}", newEngine);
		return curEngine;
	}

	public void changeState(DynamicEngine engine, State fromState, State toState) {
		if (toState.equals(State.ALL)) 
			throw new IllegalArgumentException("Cannot set Engine state to ALL");
		
		String size = engine.getSize();
		
		if (fromState.equals(toState)) return;
		
		engineMaps.get(fromState).get(size).remove(engine);
		engineMaps.get(toState).get(size).add(engine);
	}
	
	void changeState(DynamicEngine engine, State toState) {
		engine.setState(toState);
	}
	
	public EngineSize calcEngineSize(long loc) {
		log.trace("calcEngineSize() : loc={}", loc);
		
		for (EngineSize size : engineSizes.keySet()) {
			if (size.isMatch(loc)) return size;
		}
		return null;
	}
	
	public DynamicEngine allocateEngine(EngineSize scanSize, State fromState) {
		log.trace("allocateEngine() : size={}; state={}", scanSize.getName(), fromState);
		
		final String size = scanSize.getName();
		final Map<String, Set<DynamicEngine>> engineMap = engineMaps.get(fromState);
		if (engineMap == null) return null;
		final Set<DynamicEngine> engineList = engineMap.get(size);
		
		synchronized(this) {
			if (engineList == null || engineList.size() == 0) return null;
			
			final DynamicEngine engine = Iterables.getFirst(engineList, null);
			changeState(engine, State.SCANNING);
			log.debug("Engine allocated: pool={}", this);
			return engine;
		}
	}
	
	public void deallocateEngine(DynamicEngine engine) {
		log.trace("unallocateEngine() : {}", engine);
		synchronized(this) {
			changeState(engine, State.UNPROVISIONED);
			log.debug("Engine unallocated: pool={}", this);
		}
	}
	
	public void idleEngine(DynamicEngine engine) {
		log.trace("idleEngine() : {}", engine);
		synchronized(this) {
			changeState(engine, State.IDLE);
			log.debug("Engine idled: pool={}", this);
		}
	}

	public void expireEngine(DynamicEngine engine) {
		log.trace("expireEngine() : {}", engine);
		synchronized(this) {
			changeState(engine, State.EXPIRING);
			log.debug("Engine expired: pool={}", this);
		}
	}

	public void logEngines()	{
		allSizedEngines.forEach((size,engines)->logEngines(engines));
	}
	
	public void logEngines(DynamicEngine.State state) {
		engineMaps.get(state).forEach((size,engines)->logEngines(engines));
	}
	
	private void logEngines(Set<DynamicEngine> engines) {
		engines.forEach(engine->log.debug("{}", engine));
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		allSizedEngines.forEach((size,engines)->
				engines.forEach(engine->sb.append(String.format("%s; ", engine))));
		final StringBuilder sbSizes = new StringBuilder();
		engineSizes.forEach((size,count)->
				sbSizes.append(String.format("%s:%d, ", size.getName(), count.get())));
		return MoreObjects.toStringHelper(this)
				.add("engineSizes", "[" + sbSizes.toString().replaceAll(", $", "") + "]")
				.add("engines", "[" + sb.toString().replaceAll("; $", "") + "]")
				.toString();
	}
	
	public class IdleEngineMonitor implements Runnable {
		
		private final Logger log = LoggerFactory.getLogger(EnginePool.IdleEngineMonitor.class);
		
		private final BlockingQueue<DynamicEngine> expiredEnginesQueue;
		private final int expireBufferMins;
		private final EnginePool enginePool;

		public IdleEngineMonitor(EnginePool enginePool, BlockingQueue<DynamicEngine> expiredEnginesQueue, int expireBufferMins) {
			this.enginePool = enginePool;
			this.expiredEnginesQueue = expiredEnginesQueue;
			this.expireBufferMins = expireBufferMins;
		}

		@Override
		public void run() {
			log.trace("run()");
			try {

				final AtomicInteger expiredCount = new AtomicInteger(0);
				
				// loop thru IDLE engines looking for expiration
				idleEngines.forEach((engineSize, engines) -> {
					int minEngines = poolMins.get(engineSize);
					log.debug("Idle engines: size={}; count={0}; minimum={}", engineSize, engines.size(), minEngines);
					if (engines.size() > minEngines) {
						engines.forEach((engine) -> checkExpiredEngine(expiredCount, engine));
					}
					log.debug("Expiring engines: size={}; count={}", engineSize, expiredCount.get());
				});
				
			} catch (Throwable t) {
				log.warn("Error occurred while checking expired engines; cause={}; message={}", 
						t, t.getMessage(), t); 
				// swallow for now to avoid killing background thread
			}
			
		}

		private void checkExpiredEngine(AtomicInteger expiredCount, DynamicEngine engine) {
			try {
				final DateTime expireTime = engine.getTimeToExpire();
				log.trace("Checking idle engine: name={}; expireTime={}", 
						engine.getName(), expireTime);

				if (expireTime == null) return;

				if (expireTime.minusMinutes(expireBufferMins).isBeforeNow()) {
					expiredCount.incrementAndGet();
					enginePool.expireEngine(engine);
					//engine.setState(State.EXPIRING);
					expiredEnginesQueue.put(engine);
				}
			} catch (InterruptedException e) {
				log.info("EngineMonitor interrupted");
			}
		}
		
	}
	
	public static class EnginePoolEntry implements Comparable<EnginePoolEntry> {
		
		private EngineSize scanSize;
		private int count;
		private int minimum;
		
		public EnginePoolEntry() {
			// for Spring
		}
		
		public EnginePoolEntry(EngineSize scanSize, int count) {
			this.scanSize = scanSize;
			this.count = count;
		}

		public EngineSize getScanSize() {
			return scanSize;
		}

		public int getCount() {
			return count;
		}
		
		/**
		 * @return minimum # of running/provisioned engines for this pool
		 */
		public int getMinimum() {
			return minimum;
		}

		public void setMinimum(int minimum) {
			this.minimum = minimum;
		}

		public void setScanSize(EngineSize scanSize) {
			this.scanSize = scanSize;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(minimum, count, scanSize);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final EnginePoolEntry other = (EnginePoolEntry) obj;
			return Objects.equal(count, other.count)
				&& Objects.equal(minimum, other.minimum)
				&& Objects.equal(scanSize, other.scanSize);
		}

		@Override
		public int compareTo(EnginePoolEntry other) {
			return scanSize.compareTo(other.scanSize);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("scanSize", scanSize)
					.add("count", count)
					.add("minimum", minimum)
					.toString();
		}

	}

}
