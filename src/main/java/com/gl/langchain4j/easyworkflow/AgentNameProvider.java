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

/**
 * An interface that provides a default method to retrieve the agent's name.
 * Implementations of this interface can use {@link EasyWorkflow#getAgentName(Object)}
 * to automatically derive the agent name based on the class name, or override
 * {@link #getAgentName()} to provide a custom name.
 */
public interface AgentNameProvider {
    /**
     * Retrieves the name of the agent.
     * By default, this method delegates to {@link #getDefaultAgentName()}.
     * Implementations can override this method to provide a custom agent name.
     *
     * @return The name of the agent.
     */
    default String getAgentName() {
        return getDefaultAgentName();
    }

    /**
     * Provides the default agent name, derived from the class name of the implementing object.
     * This method uses {@link EasyWorkflow#getAgentName(Object)} to determine the name.
     *
     * @return The default agent name.
     */
    default String getDefaultAgentName() {
        return EasyWorkflow.getAgentName(this);
    }

}
