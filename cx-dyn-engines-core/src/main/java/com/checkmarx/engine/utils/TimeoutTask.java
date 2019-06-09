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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses an single threaded ExecutorService to perform a task with a timeout.
 * NOTE: This blocks the calling thread.  Do not use for asynchronous tasks.
 * 
 * @author randy@checkmarx.com
 *
 * @param <T> the task return type
 */
public class TimeoutTask<T> {
	
	private static final Logger log = LoggerFactory.getLogger(TimeoutTask.class);

	private final String taskName;
	private final int timeout;
	private final TimeUnit timeUnit;
	private final TaskManager taskManager;
	private final ExecutorService executor;
	
	public TimeoutTask(String taskName, int timeout, TimeUnit timeUnit, TaskManager taskManager) {
		this.taskName = taskName;
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		this.taskManager = taskManager;
		this.executor = taskManager.getTimeoutExecutor();
	}
	
	public T execute(Callable<T> task) throws InterruptedException, ExecutionException, TimeoutException {
		log.trace("execute(): task={}; timeout={}; timeUnit={}", taskName, timeout, timeUnit);
		final Future<T> future = executor.submit(task);
		taskManager.addTask(taskName, future);
		try {
			return future.get(timeout, timeUnit);
        } catch (InterruptedException e) {
            log.warn("InterruptedException during TimeoutTask; task={}", taskName);
            future.cancel(true);
            throw e;
		} catch (TimeoutException e) {
			log.warn("TimeoutException during TimeoutTask; task={}", taskName);
			future.cancel(true);
			throw e;
		} catch (ExecutionException e) {
			final Throwable t = e.getCause();
			log.warn("ExecutionException during TimeoutTask; task={}; cause={}; message={}", 
					taskName, t, t.getMessage());
            future.cancel(true);
			throw e;
		} finally {
			taskManager.removeTask(taskName, future);
		}
	}

}
