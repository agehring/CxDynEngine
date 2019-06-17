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
package com.checkmarx.engine.vmware;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.checkmarx.engine.domain.DynamicEngine;
import com.google.common.collect.Lists;

public class VmwareEnginesTest extends VmwareSpringTest {
	
	private static final Logger log = LoggerFactory.getLogger(VmwareEnginesTest.class);

	private final List<DynamicEngine> runningEngines = Lists.newArrayList();
	
	@Autowired
	private VmwareEngines vmwareEngines;
	
    @BeforeClass
    public static void init() {
        Assume.assumeTrue(runIntegrationTests());
    }
    
	@Before
	public void setUp() throws Exception {
		log.trace("setUp()");

		assertThat(vmwareEngines, is(notNullValue()));
	}
	
	@After
	public void tearDown() throws Exception {
		runningEngines.forEach((engine) -> {
			vmwareEngines.stop(engine, true);
			log.debug("Stopped: {}", engine);
		});
		Thread.sleep(2000);
	}
	
	@Test
	public void testListEngines() {
		log.trace("testListEngines()");
		
		final List<DynamicEngine> engines = vmwareEngines.listEngines();
		assertThat(engines, is(notNullValue()));
		
		engines.forEach((engine) -> log.debug("{}", engine));
	}

	@Test
	public void testLaunchAndStop() throws Exception {
		log.trace("testLaunchAndStop()");
		
		Assume.assumeTrue(super.runIntegrationTests());
		
	}

}
