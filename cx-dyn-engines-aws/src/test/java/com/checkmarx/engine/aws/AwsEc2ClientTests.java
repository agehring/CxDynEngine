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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.ec2.model.Instance;
import com.checkmarx.engine.servers.CxEngines;
import com.checkmarx.engine.servers.CxEngines.CxServerRole;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AwsEc2ClientTests extends AwsSpringTest {
	
	private static final Logger log = LoggerFactory.getLogger(AwsEc2ClientTests.class);
	
	@Autowired
	private AwsComputeClient ec2Client;
	
	@Autowired
	private AwsEngineConfig config;

	private final List<String> instances = Lists.newArrayList();
		
	@Before
	public void setUp() throws Exception {
		log.trace("setup()");
	
		Assume.assumeTrue(super.runAwsIntegrationTests());

		assertThat(ec2Client, is(notNullValue()));
		assertThat(config, is(notNullValue()));
	}
	
	@After
	public void tearDown() throws Exception {
		for (String instance : instances) {
			ec2Client.terminate(instance);
		}
	}

	@Test
	public void testLaunch() {
		log.trace("testLaunch()");
		
		final String name = "cx-test1";
		final String instanceType = "t2.small";
		//final String instanceType = "m4.large";
		final String version = "8.8.0-HF1";
		final CxServerRole role = CxServerRole.ENGINE;
		
		final Map<String, String> tags = AwsEngines.createCxTags(role, version);
		
		final Instance instance = ec2Client.launch(name, instanceType, tags);
		instances.add(instance.getInstanceId());
		
		assertNotNull(instance);
		assertThat(Ec2.getName(instance), is(name));
		assertThat(Ec2.getTag(instance, CxEngines.CX_ROLE_TAG), is(role.toString()));
		assertThat(Ec2.getTag(instance, CxEngines.CX_VERSION_TAG), is(version));
		assertEquals(instanceType, instance.getInstanceType());
		assertEquals(config.getKeyName(), instance.getKeyName());
		assertEquals(config.getImageId(), instance.getImageId());
		assertEquals(config.getSubnetId(), instance.getSubnetId());
		assertEquals(config.getSecurityGroup(), instance.getSecurityGroups().get(0).getGroupId());
	}
	
	@Test
	@Ignore
	public void testTerminate() {
		log.trace("testTerminate()");
		
		final String instanceId = "i-05bb6ec6c7aba753c";
		ec2Client.terminate(instanceId);
	}

	@Test(expected = RuntimeException.class)
	public void testDescribe_Retry() {
		log.trace("testDescribe_Retry()");

		// instance doesn't exist
		final String instanceId = "i-05bb6ec6c7aba753c";
		
		final Instance instance = ec2Client.describe(instanceId);
		log.debug("{}", instance);
		fail("RuntimeException expected");
	}
	
	@Test
	@Ignore
	public void testDescribe() {
		log.trace("testDescribe()");

		// engine image instance for RJG
		final String instanceId = "i-0882bd5b694fc761a";
		
		final Instance instance = ec2Client.describe(instanceId);
		assertThat(instance, notNullValue());
		log.debug("{}", Ec2.print(instance));
		assertThat(instance.getInstanceId(), is(instanceId));
		assertThat(Ec2.getName(instance), is(notNullValue()));
	}
	
	@Test
	public void testListAllInstances() {
		log.trace("testListAllInstances()");

		Map<String, String> tags = Maps.newHashMap();
		List<Instance> instances = ec2Client.find(tags);
		assertThat(instances, is(notNullValue()));
		assertThat(instances.isEmpty(), is(false));
		
		int i = 0;
		for(Instance instance : instances) {
			log.debug("{} : name={}; {}", ++i, Ec2.getName(instance), Ec2.print(instance));
		}
	}

	@Test
	public void testListInstances() {
		log.trace("testListInstances()");

		Map<String, String> tags = Maps.newHashMap();
		tags.put("cx-role", "MANAGER");
		List<Instance> instances = ec2Client.find(tags);
		assertThat(instances, is(notNullValue()));
		assertThat(instances.isEmpty(), is(false));
		
		int i = 0;
		for(Instance instance : instances) {
			log.debug("{} : name={}; {}", i++, Ec2.getName(instance), Ec2.print(instance));
		}
	}

}
