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
 * Copyright (c) 2017 Checkmarx
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
 */
package com.checkmarx.engine.vmware;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.checkmarx.engine.domain.DynamicEngine;
import com.checkmarx.engine.domain.EngineSize;
import com.checkmarx.engine.servers.CxEngines;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * {@code CxEngines} implementation for VMware hosted engines.
 * 
 * @author sergio.pinto@checkmarx.com
 */
@Component
@Profile("vmware")
public class VmwareEngines implements CxEngines {
	
	private static final Logger log = LoggerFactory.getLogger(VmwareEngines.class);
	
	private final VmwareEngineConfig config;

	public VmwareEngines(VmwareEngineConfig config) {
		this.config = config;
		log.info("{}", this);
	}

	@Override
	public List<DynamicEngine> listEngines() {
		log.trace("listEngines()");
		
		final List<DynamicEngine> engines = Lists.newArrayList(); 
		
		// TODO: implement listEngines
		return engines;
	}

	@Override
	public void launch(DynamicEngine engine, EngineSize size, boolean waitForSpinup) {
		log.trace("launch() : {}; size={}; waitForSpinup={}", engine, size, waitForSpinup);

		// TODO: implement launch
	}

	@Override
	public void stop(DynamicEngine engine) {
		log.trace("stop() : {}", engine);

		stop(engine, false);
	}

	@Override
	public void stop(DynamicEngine engine, boolean forceTerminate) {
		log.trace("stop() : {}; forceTerminate={}", engine, forceTerminate);

		// TODO: implement stop
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("config", config)
				.toString();
	}
	
}
