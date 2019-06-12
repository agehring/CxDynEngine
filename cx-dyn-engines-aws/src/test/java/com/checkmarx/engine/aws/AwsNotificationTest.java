package com.checkmarx.engine.aws;

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

public class AwsNotificationTest extends AwsSpringTest {

    private static final Logger log = LoggerFactory.getLogger(AwsNotificationTest.class);

    @Autowired
    private Notification notify;

    @Autowired
    private CxConfig config;

    @Before
    public void setUp() throws Exception {
        log.trace("setup()");

        Assume.assumeFalse(super.runAwsIntegrationTests()); //???TODO validate

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