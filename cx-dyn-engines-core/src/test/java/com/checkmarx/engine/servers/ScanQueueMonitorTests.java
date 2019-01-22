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
import static org.junit.Assert.assertThat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.checkmarx.engine.CoreSpringTest;
import com.checkmarx.engine.rest.CxEngineApi;
import com.checkmarx.engine.servers.ScanQueueMonitor;

public class ScanQueueMonitorTests extends CoreSpringTest {
	
	private static final Logger log = LoggerFactory.getLogger(ScanQueueMonitorTests.class);

	@Autowired
	private ScanQueueMonitor monitor;
	
	@Autowired
	private CxEngineApi cxClient;
	
	@BeforeClass
	public static void before() {
		Assume.assumeTrue(runCxIntegrationTests());
	}
	
	@Before
	public void setUp() throws Exception {
		log.trace("setup()");

		assertThat(monitor, is(notNullValue()));

		cxClient.login();
		cxClient.blockEngine(1);
	}
	
	@After
	public void tearDown() {
		log.trace("tearDown()");

		cxClient.unblockEngine(1);
	}
	
	@Test
	public void test() throws Exception {
		log.trace("test()");
		
		final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(monitor, 0L, 5, TimeUnit.SECONDS);
		
		TimeUnit.MINUTES.sleep(1);
		
		service.shutdownNow();
	}
	
}
