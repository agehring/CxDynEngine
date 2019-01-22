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
package com.checkmarx.engine.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class EngineSize implements Comparable<EngineSize> {
	
	private String name;
	private long minLOC;
	private long maxLOC;
	
	public EngineSize() {
		// for spring
	}

	public EngineSize(String name, int minLOC, int maxLOC) {
		this.name = name;
		this.minLOC = minLOC;
		this.maxLOC = maxLOC;
	}

	public String getName() {
		return name;
	}

	public long getMinLOC() {
		return minLOC;
	}

	public long getMaxLOC() {
		return maxLOC;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMinLOC(long minLOC) {
		this.minLOC = minLOC;
	}

	public void setMaxLOC(long maxLOC) {
		this.maxLOC = maxLOC;
	}

	public boolean isMatch(long loc) {
		return loc >= minLOC && loc <= maxLOC;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(name, minLOC, maxLOC);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final EngineSize other = (EngineSize) obj;
		return Objects.equal(maxLOC, other.maxLOC)
			&& Objects.equal(minLOC, other.minLOC)
			&& Objects.equal(name, other.name);
	}

	@Override
	public int compareTo(EngineSize other) {
		return Long.compare(maxLOC, other.maxLOC);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", name)
				.add("minLOC", minLOC)
				.add("maxLOC", maxLOC)
				.toString();
	}
}
