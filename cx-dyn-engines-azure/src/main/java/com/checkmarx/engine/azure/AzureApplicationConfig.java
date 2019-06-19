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
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Boot configuration
 * 
 * @author randy@checkmarx.com
 */
@Configuration
@Import(CoreApplicationConfig.class)
@EnableRetry
public class AzureApplicationConfig {
	
	private static final Logger log = LoggerFactory.getLogger(AzureApplicationConfig.class);
	
	public AzureApplicationConfig() {
		log.info("ctor()");
	}

	@Bean
	public Azure getAzure(){
		//https://github.com/Azure/azure-libraries-for-java/blob/master/AUTH.md
		ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
				"b293215c-dd52-4546-b84d-c3dc8f57f648", "6677be72-cda1-47e8-ae4a-320b4692c7d7",
				"6p8JQeNCPs7=Agv?4UiPnEGVlgT.HBD+", AzureEnvironment.AZURE);
		Azure azure = Azure.authenticate(credentials).withSubscription("c56687e8-b5b1-46f5-8dd3-84957bd57135");
		//Or cert
		/*ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
				client, tenant, pfxCertificatePath, password, AzureEnvironment.AZURE);
		Azure azure = Azure.authenticate(credentials).withSubscription(subscriptionId);*/
		//Or credential file
		// try {
		//az login
		//az login --service-principal --username b293215c-dd52-4546-b84d-c3dc8f57f648 --password 6p8JQeNCPs7=Agv?4UiPnEGVlgT.HBD+ --tenant 6677be72-cda1-47e8-ae4a-320b4692c7d7
		//az account set --subscription <subscription Id>
		//azure = Azure.authenticate(AzureCliCredentials.create()).withDefaultSubscription();
		// }catch (IOException e){

		//}
		return azure;
	}
}
