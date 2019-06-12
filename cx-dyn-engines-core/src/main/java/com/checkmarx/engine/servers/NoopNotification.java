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

import com.checkmarx.engine.rest.Notification;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * {@code Notification} implementation that does nothing.
 * Useful for unit testing and spring context building.
 *  
 * @author ken.mcdonald@checkmarx.com
 * @see Notification
 *
 */
@Profile("noop")
@Component
public class NoopNotification implements Notification {
	
	private static final Logger log = LoggerFactory.getLogger(NoopNotification.class);

	@Override
	public void sendNotification(String subject, String message, Throwable throwable) {
		log.info("Executing noop notification with message {}, details {}", message, ExceptionUtils.getMessage(throwable));
	}
}
