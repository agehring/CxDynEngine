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
package com.checkmarx.engine.azure;

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.rest.Notification;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class AzureNotificationTest extends AzureSpringTest {

    private static final Logger log = LoggerFactory.getLogger(AzureNotificationTest.class);

    @Autowired
    private Notification notify;

    @Autowired
    private CxConfig config;

    @Before
    public void setUp() throws Exception {
        log.trace("setup()");

        Assume.assumeTrue(runAzureIntegrationTests());

        assertThat(notify, is(notNullValue()));
        assertThat(config, is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void snsTopicConfigTest(){
        assertNotNull(config.getNotificationId());
    }

    @Test()
    public void testSendNotification() {
        notify.sendNotification("DynamicEngines", "DynamicEngines Test", new HttpClientErrorException(HttpStatus.BAD_GATEWAY));
        notify.sendNotification("DynamicEngines", "DynamicEngines Test", new HttpClientErrorException(HttpStatus.BAD_GATEWAY));
        notify.sendNotification("DynamicEngines", "DynamicEngines Test", new HttpClientErrorException(HttpStatus.BAD_GATEWAY));
        notify.sendNotification("DynamicEngines", "DynamicEngines Test", new HttpClientErrorException(HttpStatus.BAD_GATEWAY));
    }
}