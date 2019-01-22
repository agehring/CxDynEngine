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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.checkmarx.engine.CoreSpringTest;
import com.checkmarx.engine.rest.CxEngineApiClient;
import com.checkmarx.engine.servers.EngineManager;

public class EngineManagerTests extends CoreSpringTest {
	
	private static final Logger log = LoggerFactory.getLogger(EngineManagerTests.class);
	
	@Autowired
	private EngineManager engineManager;

	@Autowired
	private CxEngineApiClient cxClient;
	
	@Before
	public void setUp() throws Exception {
		
		Assume.assumeTrue(super.runCxIntegrationTests());
		
		assertThat(engineManager, notNullValue());
		assertThat(cxClient, notNullValue());
		assertThat(cxClient.login(), is(true));
	}
	
	@Test
	public void testShutdown() throws InterruptedException {
		log.trace("testShutdown()");
		
		engineManager.run();
		TimeUnit.SECONDS.sleep(10);
		engineManager.stop();
	}	

}
