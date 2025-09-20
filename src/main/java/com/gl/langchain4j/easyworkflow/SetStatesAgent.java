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
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * An agent that sets states within an {@link AgenticScope}.
 */
public class SetStatesAgent {
    private final Map<String, Object> states;

    /**
     * Constructs a new SetStateAgent with the given state map.
     *
     * @param aState The map of states to be set.
     */
    public SetStatesAgent(Map<String, Object> aState) {
        states = aState;
    }

    public SetStatesAgent(String stateKey, Object stateValue) {
        this(Map.of(stateKey, stateValue));
    }

    /**
     * Invokes the agent to write the stored states to the provided AgenticScope.
     *
     * @param agenticScope The AgenticScope to which the states will be written.
     * @return Always returns null.
     */
    @Agent
    public Object invoke(@V("agenticScope") AgenticScope agenticScope) {
        agenticScope.writeStates(states);
        return null;
    }
}
