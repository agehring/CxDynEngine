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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.checkmarx.engine.domain.EnginePool.EnginePoolEntry;
import com.google.common.collect.Sets;

public class DefaultEnginePoolBuilder implements EnginePoolBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(DefaultEnginePoolBuilder.class);

	private final Set<EnginePoolEntry> entries = Sets.newTreeSet();
	private final Set<DynamicEngine> engines = Sets.newTreeSet();
	
	private final EnginePoolConfig config;
	
	public DefaultEnginePoolBuilder(EnginePoolConfig config) {
		this.config = config;
	}

	@Override
	public EnginePoolBuilder addEntry(EnginePoolEntry entry) {
		log.trace("addEntry(): {}", entry);
		entries.add(entry);
		return this;
	}
	
	@Override
	public EnginePool build() {
		log.trace("build()");
		engines.clear();
		entries.forEach((entry) -> addEngines(entry.getScanSize(), entry.getCount()));
		return new EnginePool(entries, engines);
	}

	private void addEngines(EngineSize size, int count) {
		log.trace("addEngines(): {}, count={}", size, count);
		for(int i = 1; i <= count; i++) {
			final String name = String.format("%s-%s-%03d", 
					config.getEnginePrefix(), size.getName(), i);
			engines.add(new DynamicEngine(
					name, size.getName(), config.getEngineExpireIntervalSecs()));
			log.info("Adding engine to pool; name={}", name);
		}
	}

}
