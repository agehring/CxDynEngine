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

import com.checkmarx.engine.rest.model.ScanRequest;
import com.google.common.base.MoreObjects;

public class QueuedScan {

	private final EngineSize size;
	private final ScanRequest scanRequest;
	
	public QueuedScan(EngineSize size, ScanRequest scanJob) {
		this.size = size;
		this.scanRequest = scanJob;
	}

	public EngineSize getSize() {
		return size;
	}

	public ScanRequest getScanJob() {
		return scanRequest;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("size", size.getName())
				.add("scanRequest", scanRequest)
				.toString();
	}
	
	
	
}
