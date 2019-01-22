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
package com.checkmarx.engine.utils;

import javax.validation.constraints.NotNull;

public class ManifestUtils {
	
	public static String getVersion(@NotNull Object object) {
		return getVersion(object.getClass());
	}
	
	public static String getVersion(@NotNull Object object, String defaultVersion) {
		return getVersion(object.getClass(), defaultVersion);
	}
	
	public static String getVersion(@NotNull Class<?> clazz) {
		return getVersion(clazz, null);
	}

	public static String getVersion(@NotNull Class<?> clazz, String defaultVersion) {
		final String version = clazz.getPackage().getImplementationVersion(); 
		return version == null ? defaultVersion : version;
	}

}
