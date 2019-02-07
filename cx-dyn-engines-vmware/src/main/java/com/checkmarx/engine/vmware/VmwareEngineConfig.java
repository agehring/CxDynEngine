/**
 * Copyright (c) 2017 Checkmarx
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
 */
package com.checkmarx.engine.vmware;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.checkmarx.engine.domain.EngineSize;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * VMware engine configuration class. 
 * Config properties populated from application-vmware.properties
 * 
 * @author sergio.pinto@checkmarx.com
 *
 */
@Profile("vmware")
@Configuration
@ConfigurationProperties(prefix="cx-vmware-engine")
public class VmwareEngineConfig {

	private String urlStr;
	private String username;
	private String password;
	private Boolean bypassSSLVerification;
	private Boolean usePublicUrlForCx = false;
	private Boolean usePublicUrlForMonitor = false;
	private int monitorPollingIntervalSecs = 10;
	private String datacenterName;
	private String vmPath;
	private String templateVMName;
	
	/**
	 * Maps EngineSize to Vmware instanceType; 
	 * 	key=size (M), 
	 * 	value=vm instance type
	 */
	private final Map<String, String> engineSizeMap = Maps.newHashMap();
	private final Map<String, String> engineMemSizeMap = Maps.newHashMap();
	private final Map<String, String> engineCpuSizeMap = Maps.newHashMap();
	
	private String printEngineSizeMap() {
		final StringBuilder sb = new StringBuilder();
		engineSizeMap.forEach((size,instanceType) ->
			sb.append(String.format("%s->%s, ", size,instanceType)) );
		return sb.toString().replaceAll(", $", "");
	}
	
	private String printEngineMemSizeMap() {
		final StringBuilder sb = new StringBuilder();
		engineMemSizeMap.forEach((size,instanceType) ->
			sb.append(String.format("%s->%s, ", size,instanceType)) );
		return sb.toString().replaceAll(", $", "");
	}
	
	private String printEngineCpuSizeMap() {
		final StringBuilder sb = new StringBuilder();
		engineCpuSizeMap.forEach((size,instanceType) ->
			sb.append(String.format("%s->%s, ", size,instanceType)) );
		return sb.toString().replaceAll(", $", "");
	}
	
	public String getDatacenterName() {
		return this.datacenterName;
	}

	public void setDatacenterName(String datacenterName) {
		this.datacenterName = datacenterName;
	}
	
	public String getVmPath() {
		return this.vmPath;
	}

	public void setVmPath(String vmPath) {
		this.vmPath = vmPath;
	}
	
	public String getTemplateVMName() {
		return this.templateVMName;
	}

	public void setTemplateVMName(String templateVMName) {
		this.templateVMName = templateVMName;
	}
	
	/**
	 * Polling interval for monitoring instance/engine launch/startup 
	 */
	public int getMonitorPollingIntervalSecs() {
		return monitorPollingIntervalSecs;
	}

	public void setMonitorPollingIntervalSecs(int monitorPollingIntervalSecs) {
		this.monitorPollingIntervalSecs = monitorPollingIntervalSecs;
	}
	
	public Boolean isUsePublicUrlForMonitor() {
		return this.usePublicUrlForMonitor;
	}

	public void setUsePublicUrlForMonitor(boolean usePublicUrlForMonitor) {
		this.usePublicUrlForMonitor = usePublicUrlForMonitor;
	}
	
	public String getUrlStr() {
		return urlStr;
	}

	public void setUrlStr(String urlStr) {
		this.urlStr = urlStr;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public Boolean getBypassSSLVerification() {
		return bypassSSLVerification;
	}

	public void setBypassSSLVerification(boolean bypassSSLVerification) {
		this.bypassSSLVerification = bypassSSLVerification;
	}
	
	public Boolean isUsePublicUrlForCx() {
		return this.usePublicUrlForCx;
	}
	
	public void setUsePublicUrlForCx(boolean usePublicUrlForCx) {
		this.usePublicUrlForCx = usePublicUrlForCx;
	}
	
	/**
	 * Map of EngineSize to Vmware instanceType; 
	 * key=size (name), 
	 * value=vm instance type
	 * @see EngineSize
	 */
	public Map<String, String> getEngineSizeMap() {
		return engineSizeMap;
	}
	
	/**
	 * Map of EngineSize to Vmware instanceType; 
	 * key=size (name), 
	 * value=vm instance type
	 * @see EngineSize
	 */
	public Map<String, String> getEngineMemSizeMap() {
		return engineMemSizeMap;
	}
	
	public Map<String, String> getEngineCpuSizeMap() {
		return engineCpuSizeMap;
	}

	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("urlStr", urlStr)
				.add("username", username)
				.add("bypassSSLVerification", bypassSSLVerification.toString())
				.add("usePublicUrlForCx", usePublicUrlForCx.toString())
				.add("usePublicUrlForMonitor", usePublicUrlForMonitor.toString())
				.add("monitorPollingIntervalSecs", Integer.toString(monitorPollingIntervalSecs))
				.add("datacenterName", datacenterName)
				.add("vmPath", vmPath)
				.add("templateVMName", templateVMName)
				.add("engineSizeMap", "[" + printEngineSizeMap() +"]")
				.add("engineMemSizeMap", "[" + printEngineMemSizeMap() +"]")
				.add("engineCpuSizeMap", "[" + printEngineCpuSizeMap() +"]")
				.toString();
	}

}
