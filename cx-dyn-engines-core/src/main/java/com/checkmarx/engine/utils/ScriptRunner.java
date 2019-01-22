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
package com.checkmarx.engine.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

/**
 * Loads and runs a script from a specified file.
 *  
 * @author rjgey
 *
 * @param <T> Type of data to be bound and passed to the running script.
 */
public class ScriptRunner<T> implements Runnable {
	
	private static final Logger log = LoggerFactory.getLogger(ScriptRunner.class);
	
	private static final ScriptEngineManager scriptManager = new ScriptEngineManager();

	private String script;
	private String scriptFile;
	private ScriptEngine scriptEngine;

	/**
	 * Reads the supplied script file, and loads the corresponding scripting engine.
	 * Script file can be a standard file or on the classpath.
	 * 
	 * @param scriptFile relative or absolute path to script file
	 * @return <code>true</code> if script file is found, non-empty, 
	 * 			and corresponding ScriptEngine is available
	 */
	public boolean loadScript(String scriptFile) {
		log.trace("loadScript() : ", scriptFile);
		
		this.scriptFile = scriptFile;
		this.script = readScript(scriptFile);
		
		if (Strings.isNullOrEmpty(script)) {
			log.debug("Script file not found, or is empty: {}", scriptFile);
			return false;
		}
		
		final String ext = FilenameUtils.getExtension(scriptFile);
		scriptEngine = scriptManager.getEngineByExtension(ext);
		if (scriptEngine == null) {
			log.warn("Unable to load ScriptEngine for extension: {}", scriptFile);
			return false;
		}
		log.debug("ScriptEngine loaded: {}", scriptEngine);
		
		return true;
	}
	
	public void bindData(String key, T data) {
		if (scriptEngine == null) 
			throw new IllegalStateException("Must loadScript before binding data.");
		
		scriptEngine.put(key, data);
	}
	
	@Override
	public void run() {
		log.debug("run()");

		if (scriptEngine == null) {
			log.debug("Unsupported or empty script");
			return;
		}
			
		final Stopwatch timer = Stopwatch.createStarted();
		try {
			scriptEngine.eval(script);
		} catch (ScriptException e) {
			log.warn("Error executing script file: script={}; cause={}", scriptFile, e.getMessage());
		} finally {
			log.info("Script ran; scriptFile={}; elapsedTime={}ms", scriptFile, timer.elapsed(TimeUnit.MILLISECONDS));
			log.debug("...script: \n{}", script);
		}
		
	}
	
	@SuppressWarnings("deprecation")
	private String readScript(String scriptFile) {
		if (scriptFile == null) return null;
		
		InputStream inputStream = null;
		try {
			final File file = new File(scriptFile);
			if (file.isFile()) {
				log.debug("Script file loading: {}", file.getAbsolutePath());
				inputStream = new FileInputStream(file);
			} else {
				final ClassPathResource resource = new ClassPathResource(scriptFile);
				log.debug("Script file loading: {}", resource.getURL());
				inputStream = resource.getInputStream();
			}
			if (inputStream == null) {
				log.warn("Cannot run engine script, script not found: {}", scriptFile);
				return null;
			}
			return IOUtils.toString(inputStream, Charset.defaultCharset());
		} catch (IOException e) {
			log.warn("Error opening script file: {}", e.getMessage(), e);
			return null;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

}
