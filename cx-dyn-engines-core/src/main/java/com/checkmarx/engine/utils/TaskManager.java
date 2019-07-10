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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Responsible for tracking and stopping background tasks & shutting down
 * Executors.
 * 
 * @author randy@checkmarx.com
 *
 */
@Component
public class TaskManager {
    
    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private final ExecutorService timerExecutor = ExecutorServiceUtils.buildPooledExecutorService(20, "timer-%d", false);

    private final Map<String, Future<?>> tasks = Maps.newConcurrentMap();
    private final Map<String, ExecutorService> executors = Maps.newConcurrentMap();
    
    public TaskManager() {
        addExecutor("TimerExecutor", timerExecutor);
    }

    public ExecutorService getTimeoutExecutor() {
        return timerExecutor;
    }
    
    public ExecutorService getExecutor(String name, String format, boolean isDaemon) {
        final ExecutorService executor = ExecutorServiceUtils.buildPooledExecutorService(20, format, isDaemon);
        addExecutor(name, executor);
        return executor;
    }

    public void addTask(String name, Future<?> task) {
        log.debug("addTask(): name={}", name);
        tasks.put(name, task);
    }
    
    public void removeTask(String name, Future<?> task) {
        log.debug("removeTask(): name={}", name);
        tasks.remove(name, task);
    }
    
    public void addExecutor(String name, ExecutorService executor) {
        log.debug("addExecutor(): name={}", name);
        executors.put(name, executor);
    }
    
    public void removeExecutor(String name, ExecutorService executor) {
        log.debug("removeExecutor(): name={}", name);
        executors.remove(name, executor);
    }
    
    public void shutdown() {
        log.debug("shutdown()");
        
        log.info("Shutting down background services and tasks...");
        
        tasks.entrySet().forEach(entry -> cancel(entry.getKey(), entry.getValue()));
        executors.entrySet().forEach(entry -> stop(entry.getKey(), entry.getValue()));
    }
    
    private void cancel(String name, Future<?> task) {
        log.debug("cancel(): name={}", name);

        if (task.isDone()) return;
        log.info("Cancelling task: name={}", name);
        
        try {
            task.cancel(true);
        } catch (Throwable t) {
            // log and swallow
            log.warn("Exception while cancelling task; name={}", name);
        }
    }

    private void stop(String name, ExecutorService executor) {
        log.debug("stop(): name={}", name);
        
        if (executor.isTerminated()) {
            return;
        }
        log.info("Shutting down executor: name={}", name);
        
        try {
            executor.shutdown();
            ExecutorServiceUtils.terminateExecutor(executor, name);
        } catch (Throwable t) {
            // log and swallow
            log.warn("Exception while shutting down executor; name={}", name);
        }
    }

}
