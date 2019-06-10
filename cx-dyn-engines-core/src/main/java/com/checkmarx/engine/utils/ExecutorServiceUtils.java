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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ExecutorServiceUtils {
	
    private static final Logger log = LoggerFactory.getLogger(ExecutorServiceUtils.class);

    private ExecutorServiceUtils() {
		// static class
	}
	
	public static ThreadFactory buildThreadFactory(String nameFormat, boolean daemon) {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat(nameFormat)
				.setDaemon(daemon)
				.build();
		return threadFactory;
	}
	
	public static ExecutorService buildPooledExecutorService(int count, String nameFormat, boolean daemon) {
		return Executors.newFixedThreadPool(count, buildThreadFactory(nameFormat, daemon));
	}

	public static ExecutorService buildSingleThreadExecutorService(String nameFormat, boolean daemon) {
		return Executors.newSingleThreadExecutor(buildThreadFactory(nameFormat, daemon));
	}

	public static ScheduledExecutorService buildScheduledExecutorService(String nameFormat, boolean daemon) {
		return Executors.newSingleThreadScheduledExecutor(buildThreadFactory(nameFormat, daemon));
	}
	
    public static void terminateExecutor(ExecutorService executor, String name) {
        log.debug("terminateExecutor(): name={}", name);
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
}
