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
package com.checkmarx.engine.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.checkmarx.engine.CoreSpringTest;
import com.checkmarx.engine.rest.model.Login;

@TestPropertySource("/application-test.properties")
public class CxVersionTests extends CoreSpringTest {

	private static final Logger log = LoggerFactory.getLogger(CxVersionTests.class);

	@Autowired
	private CxEngineApiClient cxClient;
	
	@Before
	public void setUp() throws Exception {
		
		Assume.assumeTrue(super.runCxIntegrationTests());
		
		assertThat(cxClient, notNullValue());
		assertThat(login(), is(true));
	}
	
	private boolean login() {
		return cxClient.login(new Login("admin@cx", "Im@hom3y!!"));
	}
	
	@Test
	public void testConvert() {
		log.trace("testConvert()");

		assertThat(CxVersion.convertVersion("Unknown"), is(equalTo(0.0)));
		assertThat(CxVersion.convertVersion("8.5"), is(equalTo(8.5)));
		assertThat(CxVersion.convertVersion("8.5.0.2"), is(equalTo(8.5)));
	}
}
