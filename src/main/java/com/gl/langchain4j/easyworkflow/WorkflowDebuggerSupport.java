/*
 * Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.data.message.UserMessage;

import java.lang.reflect.Method;
import java.util.Map;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.getAgentMethod;

/**
 * This interface provides support for debugging workflows. Non-AI agents should implement it and call its
 * {@code inputReceived()} and {@code outputProduced()} methods to notify the debugger about agent's invocation.
 * <p>
 * It allows setting and getting a {@link WorkflowDebugger} instance, and provides default methods to notify the
 * debugger about various workflow events.
 */
public interface WorkflowDebuggerSupport {

    /**
     * Returns the current {@link WorkflowDebugger} instance.
     *
     * @return The {@link WorkflowDebugger} instance.
     */
    WorkflowDebugger getWorkflowDebugger();

    /**
     * Sets the {@link WorkflowDebugger} instance to be used for debugging.
     *
     * @param workflowDebugger The {@link WorkflowDebugger} instance.
     */
    void setWorkflowDebugger(WorkflowDebugger workflowDebugger);

    /**
     * Notifies the debugger that a {@link UserMessage} has been received as input.
     *
     * @param userMessage The received {@link UserMessage}.
     */
    default void inputReceived(UserMessage userMessage) {
        if (getWorkflowDebugger() != null)
            getWorkflowDebugger().inputReceived(this, getClass(), userMessage);
    }

    /**
     * Notifies the debugger that a text input has been received. This method converts the text into a
     * {@link UserMessage}.
     *
     * @param text The received text input.
     */
    default void inputReceived(String text) {
        inputReceived(UserMessage.userMessage(text));
    }

    /**
     * Notifies the debugger that an output has been produced. It attempts to find an {@link Agent} annotation with an
     * {@code outputName} to identify the state change.
     *
     * @param output The produced output object.
     */
    default void outputProduced(Object output) {
        if (getWorkflowDebugger() == null)
            return;

        for (Method method : getClass().getDeclaredMethods()) {
            Agent annotation = method.getAnnotation(Agent.class);
            if (annotation != null) {
                if (annotation.outputName() != null && !annotation.outputName().isEmpty())
                    getWorkflowDebugger().stateChanged(this, getClass(), annotation.outputName(), output);
                break;
            }
        }
    }

    /**
     * Notifies the debugger that an output has been produced within an agentic scope.
     *
     * @param output The produced output object.
     */
    default void agenticScopeOutputProduced(Object output) {
        if (getWorkflowDebugger() != null)
            getWorkflowDebugger().stateChanged(this, getClass(), "agenticScope", output);
    }

    /**
     * An abstract implementation of {@link WorkflowDebuggerSupport} that provides basic functionality for setting and
     * getting a {@link WorkflowDebugger} instance. Non-AI agents can extend this class to easily integrate with the
     * workflow debugger.
     */
    public static abstract class Impl implements WorkflowDebuggerSupport {
        private WorkflowDebugger workflowDebugger;

        @Override
        public WorkflowDebugger getWorkflowDebugger() {
            return workflowDebugger;
        }

        @Override
        public void setWorkflowDebugger(WorkflowDebugger workflowDebugger) {
            this.workflowDebugger = workflowDebugger;
        }

        public String expandUserMessage(Map<String, Object> states) {
            return EasyWorkflow.expandUserMessage(getClass(), states);
        }
    }
}
