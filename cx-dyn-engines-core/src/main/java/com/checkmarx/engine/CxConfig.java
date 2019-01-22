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
package com.checkmarx.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.common.base.MoreObjects;

@Component
@ConfigurationProperties(prefix="cx")
public class CxConfig {
	
	private final String cxEngineUrlPath = "/CxSourceAnalyzerEngineWCF/CxEngineWebServices.svc";
	
	private String userName;
	private String password;
	private int concurrentScanLimit;
	private String cxEnginePrefix = "**";
	private boolean cxEngineUseSSL = false;
	private int expireEngineBufferMins = 1;
	private int idleMonitorSecs = 15;
	private int queueCapacity = 100;
	private int queueIntervalSecs = 20;
	private String queueingEngineName="DynamicEngine";
	private String restUrl;
	private int timeoutSecs = 20;
	private String userAgent = "CxDynamicEngineManager";
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getConcurrentScanLimit() {
		return concurrentScanLimit;
	}

	public void setConcurrentScanLimit(int concurrentScanLimit) {
		this.concurrentScanLimit = concurrentScanLimit;
	}

	/**
	 * @return the prefix to append to the engine name registered with CxManager.
	 * 			Default value is {@code '**'}.  This can be used to distinguish
	 * 			dynamic engines from non-dynamic engines. 
	 */
	public String getCxEnginePrefix() {
		return cxEnginePrefix;
	}

	public void setCxEnginePrefix(String cxEnginePrefix) {
		this.cxEnginePrefix = cxEnginePrefix;
	}

	public boolean isCxEngineUseSSL() {
		return cxEngineUseSSL;
	}

	public void setCxEngineUseSSL(boolean cxEngineUseSSL) {
		this.cxEngineUseSSL = cxEngineUseSSL;
	}

	public int getExpireEngineBufferMins() {
		return expireEngineBufferMins;
	}

	public void setExpireEngineBufferMins(int expireEngineBufferMins) {
		this.expireEngineBufferMins = expireEngineBufferMins;
	}

	public int getIdleMonitorSecs() {
		return idleMonitorSecs;
	}

	public void setIdleMonitorSecs(int idleMonitorSecs) {
		this.idleMonitorSecs = idleMonitorSecs;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public int getQueueIntervalSecs() {
		return queueIntervalSecs;
	}
	
	public void setQueueIntervalSecs(int queueIntervalSecs) {
		this.queueIntervalSecs = queueIntervalSecs;
	}

	public String getQueueingEngineName() {
		return queueingEngineName;
	}

	public void setQueueingEngineName(String queueingEngineName) {
		this.queueingEngineName = queueingEngineName;
	}

	public String getRestUrl() {
		return restUrl;
	}

	public void setRestUrl(String url) {
		this.restUrl = url;
	}

	public int getTimeoutSecs() {
		return timeoutSecs;
	}

	public void setTimeoutSecs(int timeoutSecs) {
		this.timeoutSecs = timeoutSecs;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getCxEngineUrlPath() {
		return cxEngineUrlPath;
	}

	public String getVersion() {
		return getManifestVersion();
	}
	
	private String getManifestVersion() {
	    final Package objPackage = this.getClass().getPackage();
	    return objPackage.getImplementationVersion();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("userName", userName)
				.add("concurrentScanLimit", concurrentScanLimit)
				.add("cxEnginePrefix", cxEnginePrefix)
				.add("cxEngineUseSSL", cxEngineUseSSL)
				.add("cxEngineUrlPath", cxEngineUrlPath)
				.add("expireEngineBufferMins", expireEngineBufferMins)
				.add("idleMonitorSecs", idleMonitorSecs)
				.add("queueCapacity", queueCapacity)
				.add("queueIntervalSecs", queueIntervalSecs)
				.add("queueingEngineName", queueingEngineName)
				.add("restUrl", restUrl)
				.add("timeoutSecs", timeoutSecs)
				.add("userAgent", userAgent)
				.add("version", getVersion())
				.toString();
	}

}
