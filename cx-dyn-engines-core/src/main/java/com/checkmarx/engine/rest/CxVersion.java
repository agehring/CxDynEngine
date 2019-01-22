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
package com.checkmarx.engine.rest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CxVersion {
	
	private static final Pattern VERSION_REGEX = Pattern.compile("^([0-9]+\\.[0-9]+)");
	
	public static boolean isVersion86(String version) {
		return getCxVersion(version) == 8.6;
	}
	
	public static boolean isMinVersion86(String version) {
		return getCxVersion(version) >= 8.6;
	}

	private static double getCxVersion(String cxVersion) {
		return convertVersion(cxVersion);
	}

	static double convertVersion(String cxVersion) {
		
		//TODO-rjg: fix regex to work without substring	
		final Matcher matcher = VERSION_REGEX.matcher(cxVersion.substring(0,3));
		if (!matcher.matches()) return 0.0;
		final String version = matcher.group(1);
		try {
			return Double.valueOf(version);
		} catch (NumberFormatException ex) {
			return 0.0;
		}
	}
	

}
