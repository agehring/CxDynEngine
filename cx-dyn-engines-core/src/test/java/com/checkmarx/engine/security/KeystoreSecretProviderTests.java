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
package com.checkmarx.engine.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreSecretProviderTests {
	
	private static final Logger log = LoggerFactory.getLogger(KeystoreSecretProviderTests.class);
	
	private static final String KEY = "cx.password";
	private static final String SECRET = "Im@hom3y!!";
	
	private KeystoreSecretProvider ksp; 
	
	@Before
	public void setup() {

		System.setProperty(KeystoreSecretProvider.KEYSTORE_PW_PROP, "changeme");
		ksp = new KeystoreSecretProvider("cxengine.jks");
		assertThat(ksp, notNullValue());
		assertThat(System.getProperty(KeystoreSecretProvider.KEYSTORE_PW_PROP), is(""));
		
		testStore();
	}

	private void testStore() {
		log.trace("testStore()");
		
		ksp.store(KEY, new SecureString(SECRET.toCharArray()));
	}
	
	@Test
	public void testGet() {
		log.trace("testGet()");
		
		final SecureString secret = ksp.get(KEY);
		assertThat(secret, notNullValue());

		final String pw = new String(secret.toBytes()); 
		//log.debug("pw={}", pw);
		assertThat(secret.length(), is(greaterThan(0)));
		assertThat(pw, is(SECRET));
		
	}

}
