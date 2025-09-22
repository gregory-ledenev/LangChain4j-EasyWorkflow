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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An agent that sets states within an {@link AgenticScope}.
 */
public class SetStateAgents {

    public interface SetStateAgent {
        List<String> listStates();
    }

    /**
     * Creates an agent that sets states within an {@link AgenticScope} using the provided map.
     *
     * @param states A map where keys are state names and values are the state objects.
     * @return An agent object that can be used to set states.
     */
    public static Object agentOf(Map<String, Object> states) {
        return new MapAgent(states);
    }

    /**
     * Creates an agent that sets a single state within an {@link AgenticScope}.
     * @param stateKey The key (name) of the state to set.
     * @param stateValue The value of the state to set.
     * @return An agent object that can be used to set a single state.
     */
    public static Object agentOf(String stateKey, Object stateValue) {
        return new MapAgent(stateKey, stateValue);
    }

    /**
     * Creates an agent that sets states within an {@link AgenticScope} using the provided supplier.
     *
     * @param aStateSupplier A supplier that provides a map where keys are state names and values are the state objects.
     * @return An agent object that can be used to set states.
     */
    public static Object agentOf(Supplier<Map<String, Object>> aStateSupplier) {
        return new SupplierAgent(aStateSupplier);
    }

    /**
     * An agent that takes a map and uses it sets states within an {@link AgenticScope}.
     */
    public static class MapAgent implements SetStateAgent {
        private final Map<String, Object> states;

        /**
         * Constructs a new SetStateAgent with the given state map.
         *
         * @param aState The map of states to be set.
         */
        public MapAgent(Map<String, Object> aState) {
            states = aState;
        }

        public MapAgent(String stateKey, Object stateValue) {
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

        @Override
        public List<String> listStates() {
            return states.keySet().stream().sorted().toList();
        }
    }

    /**
     * An agent that takes a {@link Supplier} of a map and uses it to set states within an {@link AgenticScope}.
     */
    public static class SupplierAgent implements SetStateAgent {
        private final Supplier<Map<String, Object>> stateSupplier;

        public SupplierAgent(Supplier<Map<String, Object>> aStateSupplier) {
            stateSupplier = aStateSupplier;
        }

        /**
         * Invokes the agent to write the stored states to the provided AgenticScope.
         *
         * @param agenticScope The AgenticScope to which the states will be written.
         * @return Always returns null.
         */
        @Agent
        public Object invoke(@V("agenticScope") AgenticScope agenticScope) {
            agenticScope.writeStates(stateSupplier.get());
            return null;
        }

        @Override
        public List<String> listStates() {
            return stateSupplier.get().keySet().stream().sorted().toList();
        }
    }
}
