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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.checkmarx.engine.rest.model.ScanRequest;
import com.google.common.base.MoreObjects;

public class ScanQueue {
	
	private final int capacity;
	private final BlockingQueue<ScanRequest> queue;
	
	public ScanQueue(int capacity) {
		this.capacity = capacity;
		this.queue = new ArrayBlockingQueue<ScanRequest>(capacity);
	}

	public BlockingQueue<ScanRequest> getQueue() {
		return queue;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("capacity", capacity)
				.add("queuedCount", queue.size())
				.toString();
	}

}
