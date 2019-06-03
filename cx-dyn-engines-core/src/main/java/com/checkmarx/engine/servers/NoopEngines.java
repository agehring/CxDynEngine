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
package com.checkmarx.engine.servers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.checkmarx.engine.domain.DynamicEngine;
import com.checkmarx.engine.domain.EngineSize;
import com.google.common.collect.Lists;

/**
 * {@code CxEngines} that does nothing.  
 * Useful for unit testing and spring context building.
 *  
 * @author randy@checkmarx.com
 * @see CxEngines
 *
 */
@Profile("noop")
@Component
public class NoopEngines implements CxEngines {
	
	private static final Logger log = LoggerFactory.getLogger(NoopEngines.class);


	@Override
	public List<DynamicEngine> listEngines() {
		log.debug("listEngines()");
		return Lists.newArrayList();
	}

	@Override
	public void launch(DynamicEngine engine, EngineSize size, boolean waitForSpinup) {
		log.debug("launch() : {}; {}; waitForSpinup={}", engine, size, waitForSpinup);
		// noop
	}

	@Override
	public void stop(DynamicEngine engine) {
		log.debug("stop() : {}", engine);
		// noop
	}

	@Override
	public void stop(DynamicEngine engine, boolean forceTerminate) {
		log.debug("stop() : {}, force={}", engine, forceTerminate);
		// noop
	}

}
