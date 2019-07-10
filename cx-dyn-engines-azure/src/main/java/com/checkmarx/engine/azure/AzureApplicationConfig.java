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
package com.checkmarx.engine.azure;

import com.checkmarx.engine.spring.CoreApplicationConfig;
import com.google.common.base.Strings;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

import java.io.IOException;

/**
 * Spring Boot configuration
 * 
 * @author randy@checkmarx.com
 */
@Configuration
@Import(CoreApplicationConfig.class)
@EnableRetry
@Profile("azure")
public class AzureApplicationConfig {
	
	private static final Logger log = LoggerFactory.getLogger(AzureApplicationConfig.class);
	private final AzureEngineConfig config;

	public AzureApplicationConfig(AzureEngineConfig config) {
		this.config = config;
		log.info("ctor()");
	}

	@Bean
	public Azure getAzure(){
		//https://github.com/Azure/azure-libraries-for-java/blob/master/AUTH.md

		ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
				config.getClientId(), config.getTenantId(),
				config.getSecret(), AzureEnvironment.AZURE);
		if(Strings.isNullOrEmpty(config.getSubscriptionId())) {
			try {
				return Azure.authenticate(credentials).withDefaultSubscription();
			}catch (IOException e){
				throw new RuntimeException("Error creating Azure Bean");
			}
		}
		return Azure.authenticate(credentials).withSubscription(config.getSubscriptionId());
		//TODO - Or cert
		/*ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
				client, tenant, pfxCertificatePath, password, AzureEnvironment.AZURE);
		Azure azure = Azure.authenticate(credentials).withSubscription(subscriptionId);*/
		//Or credential file
	}
}
