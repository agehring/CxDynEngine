/*******************************************************************************
 * Copyright (c) 2017-2020 Checkmarx
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

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.rest.Notification;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("vmware")
public class VmwareNotification implements Notification {
    private static final Logger log = LoggerFactory.getLogger(VmwareNotification.class);

    private final CxConfig cxConfig;

    public VmwareNotification(CxConfig cxConfig) {
        this.cxConfig = cxConfig;
    }

    @Override
    public void sendNotification(String subject, String message, Throwable throwable) {
        log.info("Subject: {}, Message: {}, Exception: {}", subject, message, ExceptionUtils.getRootCauseMessage(throwable));
    }

}
