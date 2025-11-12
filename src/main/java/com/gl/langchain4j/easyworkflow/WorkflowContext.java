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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.output.ServiceOutputParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the context for a workflow, including input and output guardrails, and the state of the workflow.
 * <p>
 * This class provides mechanisms to:
 * <ul>
 *     <li>Create {@link OutputGuardrail} and {@link InputGuardrail} instances.</li>
 *     <li>Validate inputs and outputs using custom validators.</li>
 *     <li>Store and retrieve workflow states.</li>
 * </ul>
 */
public class WorkflowContext {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowContext.class);
    private final ServiceOutputParser serviceOutputParser = new ServiceOutputParser();
    private StateChangeHandler stateChangeHandler;
    private InputHandler inputHandler;

    /**
     * Creates an {@link OutputGuardrail} instance for a specific agent class and output name. This guardrail can be
     * used to validate AI messages (responses from LLMs).
     *
     * @param agentClass The class of the agent associated with this output.
     * @param outputName The name of the output being guarded. This name can be used to store the output in the
     *                   workflow's state.
     * @return An {@link OutputGuardrail} instance.
     */
    Output output(Class<?> agentClass, String outputName) {
        return new Output(agentClass, outputName);
    }

    /**
     * Creates an {@link InputGuardrail} instance for a specific agent class. This guardrail can be used to validate
     * user messages (inputs to the LLM).
     *
     * @param agentClass The class of the agent associated with this input.
     * @return An {@link InputGuardrail} instance.
     */
    Input input(Class<?> agentClass) {
        return new Input(agentClass);
    }

    /**
     * Validates a {@link UserMessage} using the provided {@link InputGuardrail}.
     *
     * @param inputGuardrail The {@link InputGuardrail} to use for validation.
     * @param userMessage The {@link UserMessage} to validate.
     * @return The result of the input guardrail validation.
     */

    InputGuardrailResult validate(Input inputGuardrail, UserMessage userMessage) {
        InputGuardrailResult result = InputGuardrailResult.success();

        UserMessage message = userMessage;
        if (result.isSuccess() && result.successfulText() != null)
            message = new UserMessage(result.successfulText());

        if (inputHandler != null)
            inputHandler.inputReceived(inputGuardrail.getAgent(), inputGuardrail.getAgentClass(), message);

        return result;
    }

    /**
     * Validates an {@link AiMessage} (response from LLM) using the provided {@link OutputGuardrail}.
     * If validation is successful and an output name is provided, it attempts to parse the output
     * and notify the {@link StateChangeHandler}.
     * @param outputGuardrail The {@link OutputGuardrail} to use for validation.
     * @param responseFromLLM The {@link AiMessage} to validate.
     * @return The result of the output guardrail validation.
     */
    OutputGuardrailResult validate(Output outputGuardrail, AiMessage responseFromLLM) {
        OutputGuardrailResult result;
        result = outputGuardrail.success();

        if (outputGuardrail.getOutputName() != null && result.isSuccess()) {
            Class<?> agentClass = outputGuardrail.getAgentClass();

            if (agentClass != null) {
                for (java.lang.reflect.Method method : agentClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Agent.class)) {
                        try {
                            Object parsedOutput = serviceOutputParser.parse(
                                    ChatResponse.builder().aiMessage(new AiMessage(result.successfulText() != null ? result.successfulText() : responseFromLLM.text())).build(),
                                    method.getGenericReturnType());

                            if (stateChangeHandler != null)
                                stateChangeHandler.stateChanged(outputGuardrail.getAgent(), agentClass, outputGuardrail.getOutputName(), parsedOutput);
                        } catch (Exception e) {
                            logger.error("Failed to parse output for agent " + agentClass.getSimpleName(), e);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the currently set {@link StateChangeHandler}.
     *
     * @return The {@link StateChangeHandler} instance, or {@code null} if none is set.
     */
    @SuppressWarnings("unused")
    public StateChangeHandler getOutputStateChangeHandler() {
        return stateChangeHandler;
    }

    /**
     * Sets a custom {@link StateChangeHandler}. This handler will be invoked whenever a workflow state changes.
     *
     * @param aStateChangeHandler The {@link StateChangeHandler} to be set.
     */
    public void setOutputStateChangeHandler(StateChangeHandler aStateChangeHandler) {
        stateChangeHandler = aStateChangeHandler;
    }

    /**
     * Returns the currently set {@link InputHandler}.
     *
     * @return The {@link InputHandler} instance, or {@code null} if none is set.
     */
    @SuppressWarnings("unused")
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /**
     * Sets a custom {@link InputHandler}. This handler will be invoked whenever an input message is received.
     *
     * @param aInputHandler The {@link InputHandler} to be set.
     */
    public void setInputHandler(InputHandler aInputHandler) {
        inputHandler = aInputHandler;
    }

    /**
     * Functional interface for handling state changes within the workflow.
     */
    @FunctionalInterface
    public interface StateChangeHandler {
        /**
         * Called when a state value changes.
         *
         * @param agent      The agent that caused the state change.
         * @param agentClass The class of the agent that caused the state change.
         * @param stateName  The name of the state that changed.
         * @param stateValue The new value of the state.
         */
        void stateChanged(Object agent, Class<?> agentClass, String stateName, Object stateValue);
    }

    /**
     * Functional interface for handling input messages received by the workflow.
     */
    @FunctionalInterface
    public interface InputHandler {
        /**
         * Called when a user message is received as input.
         *
         * @param agent       The agent that received the input.
         * @param agentClass  The class of the agent that received the input.
         * @param userMessage The {@link UserMessage} that was received.
         */
        void inputReceived(Object agent, Class<?> agentClass, Object userMessage);
    }

    /**
     * An implementation of {@link OutputGuardrail} specific to this workflow context.
     */
    class Output implements OutputGuardrail, TrailingGuardrail {
        private final Class<?> agentClass;
        private Object agent;
        private final String outputName;

        public Output(Class<?> aAgentClass, String aOutputName) {
            this.agentClass = aAgentClass;
            this.outputName = aOutputName;
        }

        @Override
        public OutputGuardrailResult validate(AiMessage responseFromLLM) {
            return WorkflowContext.this.validate(this, responseFromLLM);
        }

        /**
         * Returns the class of the agent associated with this output guardrail.
         *
         * @return The agent's class.
         */
        public Class<?> getAgentClass() {
            return agentClass;
        }

        /**
         * Returns the name of the output being guarded.
         *
         * @return The output name.
         */
        public String getOutputName() {
            return outputName;
        }

        /**
         * Returns the agent instance associated with this output guardrail.
         *
         * @return The agent instance.
         */
        public Object getAgent() {
            return agent;
        }

        /**
         * Sets the agent instance associated with this output guardrail.
         *
         * @param aAgent The agent instance to set.
         */
        public void setAgent(Object aAgent) {
            agent = aAgent;
        }
    }

    /**
     * An implementation of {@link InputGuardrail} specific to this workflow context.
     */
    class Input implements InputGuardrail, TrailingGuardrail {
        private final Class<?> agentClass;
        private Object agent;

        public Input(Class<?> agentClass) {
            this.agentClass = agentClass;
        }

        @Override
        public InputGuardrailResult validate(UserMessage userMessage) {
            return WorkflowContext.this.validate(this, userMessage);
        }

        /**
         * Returns the class of the agent associated with this input guardrail.
         *
         * @return The agent's class.
         */
        public Class<?> getAgentClass() {
            return agentClass;
        }

        /**
         * Returns the agent instance associated with this input guardrail.
         *
         * @return The agent instance.
         */
        public Object getAgent() {
            return agent;
        }

        /**
         * Sets the agent instance associated with this input guardrail.
         *
         * @param aAgent The agent instance to set.
         */
        public void setAgent(Object aAgent) {
            agent = aAgent;
        }
    }
}
