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
package com.checkmarx.engine.aws;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.rest.Notification;
import com.google.common.base.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.security.cert.CRL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("aws")
public class AwsNotification implements Notification {

    private final AmazonSNS amazonSNS;
    //Active map containing a hash of a particular message event that was sent and the time associated with it
    private Map<String, LocalDateTime> activeNotification = new HashMap<>();
    private final CxConfig cxConfig;
    private static final Logger log = LoggerFactory.getLogger(AwsNotification.class);
    private static final String CRLF = "\r\n";

    public AwsNotification(CxConfig cxConfig) {
        this.cxConfig = cxConfig;
        this.amazonSNS = AmazonSNSClientBuilder.defaultClient();
    }

    @Override
    public void sendNotification(String subject, String message, Throwable throwable) {
        StringBuilder msg = new StringBuilder();
        String hash = DigestUtils.sha256Hex(message);
        msg.append(CRLF).append(message).append(CRLF);
        if(throwable instanceof HttpStatusCodeException){
            HttpStatusCodeException ex = (HttpStatusCodeException) throwable;
            msg.append("HTTP Status Code: ").append(ex.getStatusCode()).append(CRLF);
            msg.append("HTTP Response: ").append(ex.getResponseBodyAsString()).append(CRLF);
            msg.append("HTTP Response Headers: ").append(ex.getResponseHeaders()).append(CRLF);
        }
        msg.append("Exception Message: ").append(ExceptionUtils.getMessage(throwable)).append(CRLF);
        msg.append("Exception Root Cause Message: ").append(ExceptionUtils.getRootCauseMessage(throwable)).append(CRLF);
        msg.append("Exception StackTrace: ").append(ExceptionUtils.getStackTrace(throwable)).append(CRLF);
        LocalDateTime now = LocalDateTime.now();
        if(activeNotification.containsKey(hash)){
            LocalDateTime sent = activeNotification.get(hash);
            if(now.isAfter(sent.plusMinutes(cxConfig.getNotificationTimer()))){
                publish(subject, msg.toString());
                activeNotification.put(hash, now);
            }
            else{
                log.debug("Message not sent: {}", msg.toString());
                log.debug("Last message sent {}", sent);
            }
        }
        else{
            publish(subject, msg.toString());
            activeNotification.put(hash, now);
        }

    }

    private void publish(String subject, String message) {
        String snsTopic = cxConfig.getNotificationId();
        if (Strings.isNullOrEmpty(snsTopic)) {
            log.info("No SNS topic has been specified.  No notification sent");
        } else {
            try {
                PublishRequest publishRequest = new PublishRequest(snsTopic, message, subject);
                log.debug("Publishing event to topic {} - {}", snsTopic, message);
                PublishResult publishResult = this.amazonSNS.publish(publishRequest);
                log.debug("SNS topic {} , response id {}", snsTopic, publishResult.getMessageId());
            } catch (AmazonSNSException e) {
                log.error("Error occurred sending message to ARN {}.  Details: {}", snsTopic, ExceptionUtils.getMessage(e));
            }
        }
    }

}
