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
package com.checkmarx.engine.azure;

import com.checkmarx.engine.servers.CxEngines;
import com.checkmarx.engine.servers.CxEngines.CxServerRole;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.azure.management.compute.VirtualMachine;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

public class AzureClientTests extends AzureSpringTest {
	
	private static final Logger log = LoggerFactory.getLogger(AzureClientTests.class);
	
	@Autowired
	private AzureComputeClient azureClient;
	
	@Autowired
	private AzureEngineConfig config;

	private final List<String> instances = Lists.newArrayList();
		
	@Before
	public void setUp() throws Exception {
		log.trace("setup()");
	
		Assume.assumeTrue(runAzureIntegrationTests());

		assertThat(azureClient, is(notNullValue()));
		assertThat(config, is(notNullValue()));
	}
	
	@After
	public void tearDown() throws Exception {
		for (String instance : instances) {
			azureClient.terminate(instance);
		}
	}

	@Test
	public void testLaunch() throws Exception {
		log.trace("testLaunch()");
		
		final String name = "cx-test1";
		final String instanceType = "t2.small";
		//final String instanceType = "m4.large";
		final String version = "8.9.0-HF1";
		final CxServerRole role = CxServerRole.ENGINE;
		
		final Map<String, String> tags = AzureEngines.createCxTags(role, version);
		
		final VirtualMachine instance = azureClient.launch(name, instanceType, tags);
		instances.add(instance.id());
		
		assertNotNull(instance);
		assertThat(VM.getName(instance), is(name));
		assertThat(VM.getTag(instance, CxEngines.CX_ROLE_TAG), is(role.toString()));
		assertThat(VM.getTag(instance, CxEngines.CX_VERSION_TAG), is(version));
		assertEquals(instanceType, instance.size());
		//assertEquals(config.getKeyName(), instance.getKeyName()); TODO what is key
		//assertEquals(config.getImageId(), instance.getImageId());
		assertEquals(config.getSubnetId(), instance.getPrimaryNetworkInterface().primaryIPConfiguration().subnetName());
		assertEquals(config.getSecurityGroup(), instance.getPrimaryNetworkInterface().getNetworkSecurityGroup().id()); //TODO validate
	}
	
	@Test
	@Ignore
	public void testTerminate() {
		log.trace("testTerminate()");
		
		final String instanceId = "i-05bb6ec6c7aba753c";
		azureClient.terminate(instanceId);
	}

	@Test(expected = RuntimeException.class)
	public void testDescribe_Retry() {
		log.trace("testDescribe_Retry()");

		// instance doesn't exist
		final String instanceId = "i-05bb6ec6c7aba753c";
		
		final VirtualMachine instance = azureClient.describe(instanceId);
		log.debug("{}", instance);
		fail("RuntimeException expected");
	}
	
	@Test
	@Ignore
	public void testDescribe() {
		log.trace("testDescribe()");

		// engine image instance for RJG
		final String instanceId = "i-0882bd5b694fc761a";
		
		final VirtualMachine instance = azureClient.describe(instanceId);
		assertThat(instance, notNullValue());
		log.debug("{}", VM.print(instance));
		assertThat(instance.id(), is(instanceId));
		assertThat(VM.getName(instance), is(notNullValue()));
	}
	
	@Test
	public void testListAllInstances() {
		log.trace("testListAllInstances()");

		Map<String, String> tags = Maps.newHashMap();
		List<VirtualMachine> instances = azureClient.find(tags);
		assertThat(instances, is(notNullValue()));
		assertThat(instances.isEmpty(), is(false));
		
		int i = 0;
		for(VirtualMachine instance : instances) {
			log.debug("{} : name={}; {}", ++i, VM.getName(instance), VM.print(instance));
		}
	}

	@Test
	public void testListInstances() {
		log.trace("testListInstances()");

		Map<String, String> tags = Maps.newHashMap();
		tags.put("cx-role", "MANAGER");
		List<VirtualMachine> instances = azureClient.find(tags);
		assertThat(instances, is(notNullValue()));
		assertThat(instances.isEmpty(), is(false));
		
		int i = 0;
		for(VirtualMachine instance : instances) {
			log.debug("{} : name={}; {}", i++, VM.getName(instance), VM.print(instance));
		}
	}

}
