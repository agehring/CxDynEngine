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
package com.checkmarx.engine.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class EngineServerDTO {

	private String name;
	private String uri;
	private int minLoc;
	private int maxLoc;
	@JsonProperty(value="isBlocked")
	private boolean blocked;

	public EngineServerDTO() {
		// default .ctor for unmarshalling
	}
	
	public EngineServerDTO(String name, String uri, int minLoc, int maxLoc, boolean isBlocked) {
		this.name = name;
		this.uri = uri;
		this.minLoc = minLoc;
		this.maxLoc = maxLoc;
		this.blocked = isBlocked;
	}

	public String getName() {
		return name;
	}

	public String getUri() {
		return uri;
	}

	public int getMinLoc() {
		return minLoc;
	}

	public int getMaxLoc() {
		return maxLoc;
	}

	public boolean isBlocked() {
		return blocked;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", name)
				.add("uri", uri)
				.add("minLOC", minLoc)
				.add("maxLOC", maxLoc)
				.add("isBlocked", blocked)
				.omitNullValues()
				.toString();
	}

}
