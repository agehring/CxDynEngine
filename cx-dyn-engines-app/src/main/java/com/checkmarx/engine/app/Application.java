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
package com.checkmarx.engine.app;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.checkmarx.engine.servers.EngineService;
import com.checkmarx.engine.utils.ScriptingUtils;

@SpringBootApplication(scanBasePackages="com.checkmarx.engine")
public class Application {
	
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	static EngineService service;
	
	public static void main(String[] args) {
		final ConfigurableApplicationContext context = 
				SpringApplication.run(Application.class, args);

		waitForQuit(context);
	}
	
	@Bean
	@Profile("!test")
	CommandLineRunner run(EngineService service) {
		return args -> {
			
			log.info("Application.run()");
			Application.service = service;
			ScriptingUtils.logScriptingEngines();
			
			service.run();
		};
	}
	
	private static void waitForQuit(ConfigurableApplicationContext context) {
		if (System.in == null) {
			log.warn("System.in is null.  {}{}",
				"Add the following to your gradle bootRun configuration:\n\t",
				"standardInput = System.in");
			return;
		}

		final Scanner scanner = new Scanner(System.in);
		String input = "";
		System.out.println("Enter quit to shutdown:\n");
		while (!input.equalsIgnoreCase("quit")) {
			input = scanner.next();
			log.debug("Console user entered: {}", input);
		}
		log.info("Console user entered: {}.  Shutting down....", input);
		service.stop();
		context.close();
		scanner.close();
	}

}
