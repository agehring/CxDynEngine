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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactorySupplier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.rest.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import org.springframework.web.client.HttpServerErrorException;

public abstract class BaseHttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(BaseHttpClient.class);
	private static final String MESSAGE_TEMPLATE = "Error occurred during operation: %s\nResponse Code: %s\nDetails: %s";

	protected final int timeoutMillis;
	protected final CxConfig config;
	private final Notification notify;

	protected BaseHttpClient(CxConfig config, Notification notify) {
		this.config = config;
		this.timeoutMillis = config.getTimeoutSecs() * 1000;
		this.notify = notify;
	}

    protected RestTemplateBuilder getRestBuilder(RestTemplateBuilder builder) {
        return builder.requestFactory(getClientHttpRequestFactory());
    }
    
	/**
	 * Creates a custom HttpClient that:
	 *  - disables SSL host verification
	 *  - disables cookie management
	 *  - sets a custom user agent
	 */
	protected ClientHttpRequestFactorySupplier getClientHttpRequestFactory() {
		
		try {
			final TrustStrategy trustStrategy = TrustSelfSignedStrategy.INSTANCE;
			final SSLContextBuilder sslContextBuilder = SSLContextBuilder.create().loadTrustMaterial(trustStrategy);
			final CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLContext(sslContextBuilder.build())
				    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				    .setUserAgent(config.getUserAgent() + " : v" + config.getVersion())
				    .disableCookieManagement()
				    .useSystemProperties()
				    .build();
		    final HttpComponentsClientHttpRequestFactory clientHttpRequestFactory
		    	= new HttpComponentsClientHttpRequestFactory(httpClient);
		    clientHttpRequestFactory.setConnectTimeout(timeoutMillis);
		    clientHttpRequestFactory.setReadTimeout(timeoutMillis);
		    return new ClientHttpRequestFactorySupplier() {
		        @Override
		        public ClientHttpRequestFactory get() {
		            return clientHttpRequestFactory;
		        }
		    };
		} catch (Throwable t) {
			final String msg = "Unable to initialize ClientHttpRequestFactory";
			throw new RuntimeException(msg, t);
		}
		
	}

	protected interface Request<R> {
		public R send();
	}
	
	protected <T,R> R execute(String operation, Request<R> request) {
		final Stopwatch timer = Stopwatch.createStarted();
		boolean success = false;
		try {
			R result = request.send();
			success = true;
			return result;
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			final ErrorResponse error = unmarshallError(e.getResponseBodyAsString());
			log.warn("Cx rest call failed: request={}; status={}; {}", operation, e.getRawStatusCode(), error);

			String errorMsg = String.format(MESSAGE_TEMPLATE, operation, e.getRawStatusCode(), error);
			notify.sendNotification(config.getNotificationSubject(), errorMsg, e);

			throw e;
		} finally {
			log.debug("Cx api call; request={}; success={}; elapsed={}ms", 
					operation, success, timer.elapsed(TimeUnit.MILLISECONDS)); 
		}
	}

	private ErrorResponse unmarshallError(String content) {
		log.trace("unmarshallError(): {}", content);
		
		final ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(content, ErrorResponse.class);
		} catch (IOException e) {
			log.warn("Failed to unmarshall http response: content={}; cause={}", content, e.getMessage());
		}
		return new ErrorResponse(-1, content);
	}

}
