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
package com.checkmarx.engine.rest;

/**
 * Checkmarx engine server client wrapper.
 * 
 * @author randy@checkmarx.com
 *
 */
public interface CxEngineClient {

	/**
	 * Pings the specified Cx engine.
	 * 
	 * @param host engine server host or ip, e.g. cx-engine-01 or 123.12.12.12
	 * @return {@code true} if engine is running and engine service is responsive.
	 */
	boolean pingEngine(String host);

	/**
	 * @param host engine server host name or ip
	 * @return the engine server url for the specified host, 
	 * 			e.g. http://127.0.0.1 
	 */
	String buildEngineServerUrl(String host);

	/**
	 * @param host engine server host name or ip
	 * @return the engine service url for the specified host
	 * 			e.g. http://127.0.0.1/CxSourceAnalyzerEngineWCF/CxEngineWebServices.svc 
	 */
	String buildEngineServiceUrl(String host);

}
