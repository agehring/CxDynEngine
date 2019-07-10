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

import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author randy@checkmarx.com
 *
 */
public class ScriptingUtils {
	
	private static final Logger log = LoggerFactory.getLogger(ScriptingUtils.class);
	
	private ScriptingUtils() {
		// static class
	}
	
	public static void logScriptingEngines() {
		final ScriptEngineManager manager = new ScriptEngineManager();
        final List<ScriptEngineFactory> engines = manager.getEngineFactories();
        if (engines.isEmpty()) {
            log.info("No scripting engines were found");
            return;
        }
        
        log.info("The following {} scripting engines were found: ", engines.size());
        engines.forEach((engine) -> {
        	final StringBuilder names = new StringBuilder();
        	engine.getNames().forEach((name) -> names.append(name + ","));
        	log.info("engineName='{}', version={}, language={}, languageVersion='{}', names=[{}]", 
        			engine.getEngineName(),
        			engine.getEngineVersion(),
        			engine.getLanguageName(),
        			engine.getLanguageVersion(),
        			names.toString().replaceAll(",$", ""));
        });
	}

}
