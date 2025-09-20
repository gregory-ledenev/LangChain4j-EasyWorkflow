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
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.internal.A2AClientBuilder;
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.*;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.breakpointActionLog;
import static dev.langchain4j.agentic.internal.AgentUtil.validateAgentClass;

/**
 * EasyWorkflow provides a fluent API for building complex agentic workflows using LangChain4j's Agentic framework. It
 * allows defining sequences of agents, conditional execution, parallel execution, agent grouping, and loops. Use
 * {@code EasyWorkflow.builder(...)} to start.
 */
public class EasyWorkflow {

    /**
     * A shared {@link ExecutorService} used for parallel agent execution. It is initialized on first use and can be
     * explicitly closed.
     */
    private static final AtomicReference<ExecutorService> sharedExecutorService = new AtomicReference<>();
    private static final Logger logger = LoggerFactory.getLogger(EasyWorkflow.class);

    private EasyWorkflow() {
    }

    /**
     * Creates a new {@link AgentWorkflowBuilder} for a given agent class.
     *
     * @param agentClass The class of the main agent for this workflow.
     * @param <T>        The type of the main agent.
     * @return A new {@link AgentWorkflowBuilder} instance.
     */
    public static <T> AgentWorkflowBuilder<T> builder(Class<T> agentClass) {
        return new AgentWorkflowBuilder<>(agentClass);
    }

    /**
     * Retrieves the shared {@link ExecutorService}. If it hasn't been initialized, it creates a new fixed thread pool
     * with 2 threads.
     *
     * @return The shared {@link ExecutorService}.
     */
    static ExecutorService getSharedExecutorService() {
        return sharedExecutorService.updateAndGet(aExecutorService -> {
            if (aExecutorService == null) {
                logger.info("Created shared executor service. Don't forget to close it via closeSharedExecutorService()");
                return Executors.newFixedThreadPool(2);
            }
            return aExecutorService;
        });
    }

    /**
     * Closes the shared {@link ExecutorService} if it has been initialized.
     */
    public static void closeSharedExecutorService() {
        ExecutorService executorService = sharedExecutorService.getAndSet(null);
        if (executorService != null)
            executorService.shutdownNow();
    }

    /**
     * Logs the output of an agent.
     *
     * @param agentClass The class of the agent.
     * @param outputName The name of the output.
     * @param result     The output result.
     */
    public static void logOutput(Class<?> agentClass, String outputName, Object result) {
        logger.info("Agent '{}' output: '{}' -> {}", 
                agentClass.getSimpleName(), 
                outputName, 
                result.toString().replaceAll("\\n", "\\\\n"));
    }

    /**
     * Logs the input to an agent.
     *
     * @param agentClass The class of the agent.
     * @param input      The input to the agent.
     */
    public static void logInput(Class<?> agentClass, Object input) {
        logger.info("Agent '{}' input: {}", agentClass.getSimpleName(), input);
    }

    /**
     * Represents an expression within the workflow, which can create an agent.
     */
    interface Expression {
        Object createAgent();
    }

    /**
     * A builder class for constructing an EasyWorkflow. It allows defining a sequence of agents and control flow
     * statements.
     *
     * @param <T> The type of the main agent for this workflow.
     */
    public static class AgentWorkflowBuilder<T> {

        private final AgentWorkflowBuilder<T> parentBuilder;
        private final Class<T> agentClass;
        private Block block;

        private String outputName;
        private Function<AgenticScope, Object> outputComposer;

        private ChatModel chatModel;
        private ChatMemory chatMemory;
        private Boolean logInput;
        private Boolean logOutput;

        private ExecutorService executor;

        private WorkflowDebugger workflowDebugger;

        AgentWorkflowBuilder(AgentWorkflowBuilder<T> aParentBuilder) {
            Objects.requireNonNull(aParentBuilder, "Parent builder can't be null");

            parentBuilder = aParentBuilder;
            this.agentClass = null;
            this.outputName = null;
        }

        /**
         * Constructs a new {@link AgentWorkflowBuilder} with the specified agent class.
         *
         * @param agentClass The class of the main agent for this workflow.
         */
        public AgentWorkflowBuilder(Class<T> agentClass) {
            Objects.requireNonNull(agentClass, "Agent class can't be null");

            this.parentBuilder = null;
            this.block = new Block();
            this.agentClass = agentClass;
        }

        void setBlock(Block block) {
            Objects.requireNonNull(block, "Block can't be null");
            this.block = block;
        }

        void addExpression(Expression expression) {
            block.addExpression(expression);
        }

        /**
         * Sets the output name for the main agent of this workflow.
         *
         * @param outputName The desired output name.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> outputName(String outputName) {
            this.outputName = outputName;
            return this;
        }

        /**
         * Sets a custom output composer for the main agent of this workflow.
         *
         * @param outputComposer A function to compose the final output of the workflow from the {@link AgenticScope}.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> output(Function<AgenticScope, Object> outputComposer) {
            this.outputComposer = outputComposer;
            return this;
        }

        /**
         * Sets the {@link ChatModel} to be used by all agents in this workflow.
         *
         * @param chatModel The {@link ChatModel} instance.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> chatModel(ChatModel chatModel) {
            Objects.requireNonNull(chatModel, "Chat model can't be null");
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Sets the {@link ChatMemory} to be used by all agents in this workflow.
         *
         * @param chatMemory The {@link ChatMemory} instance.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> chatMemory(ChatMemory chatMemory) {
            Objects.requireNonNull(chatMemory, "Chat memory can't be null");
            this.chatMemory = chatMemory;
            return this;
        }

        /**
         * Configures logging of input for the workflow.
         *
         * @param logInput If true, logs the input to each agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> logInput(boolean logInput) {
            this.logInput = logInput;
            return this;
        }

        /**
         * Configures logging of output for the workflow.
         *
         * @param logOutput If true, logs the output from each agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> logOutput(boolean logOutput) {
            this.logOutput = logOutput;
            return this;
        }

        /**
         * Sets the {@link ExecutorService} to be used for parallel execution within this workflow. If not set, a shared
         * default executor service will be used.
         *
         * @param executor The {@link ExecutorService} instance.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets the {@link WorkflowDebugger} for this workflow.
         *
         * @param workflowDebugger The {@link WorkflowDebugger} instance.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> workflowDebugger(WorkflowDebugger workflowDebugger) {
            this.workflowDebugger = workflowDebugger;
            return this;
        }

        WorkflowDebugger getWorkflowDebugger() {
            return workflowDebugger == null && parentBuilder != null ? parentBuilder.getWorkflowDebugger() : workflowDebugger;
        }

        /**
         * Retrieves the {@link ExecutorService} for this workflow. If not explicitly set for this builder, it delegates
         * to the parent builder.
         *
         * @return The {@link ExecutorService} to be used for parallel execution.
         */
        ExecutorService getExecutor() {
            return executor == null && parentBuilder != null ? parentBuilder.getExecutor() : executor;
        }

        /**
         * Adds an agent to the workflow using its class.
         *
         * @param agentClass The class of the agent to add.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Class<?> agentClass) {
            return agent(agentClass, null, null);
        }

        /**
         * Adds an agent to the workflow using its class, an output name, and a configurator.
         *
         * @param agentClass The class of the agent to add.
         * @param outputName The output name for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Class<?> agentClass, String outputName) {
            return agent(agentClass, outputName, null);
        }

        /**
         * Adds an agent to the workflow using its class, an output name, and a configurator.
         *
         * @param agentClass   The class of the agent to add.
         * @param configurator A consumer to configure the {@link AgentBuilder} for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Class<?> agentClass, Consumer<AgentBuilder<?>> configurator) {
            return agent(agentClass, null, configurator);
        }

        /**
         * Adds an agent to the workflow using its class, an output name, and a configurator.
         *
         * @param agentClass   The class of the agent to add.
         * @param outputName   The output name for this agent.
         * @param configurator A consumer to configure the {@link AgentBuilder} for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Class<?> agentClass, String outputName, Consumer<AgentBuilder<?>> configurator) {
            addExpression(new AgentExpression(this, agentClass, outputName, configurator));
            return this;
        }

        /**
         * Adds an existing agent instance to the workflow.
         *
         * @param agent The agent instance to add.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Object agent) {
            return agent(agent, null, null);
        }

        /**
         * Adds an existing agent instance to the workflow, an output name, and a configurator.
         *
         * @param agent      The agent instance to add.
         * @param outputName The output name for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Object agent, String outputName) {
            return agent(agent, outputName, null);
        }

        /**
         * Adds an existing agent instance to the workflow, an output name, and a configurator.
         *
         * @param agent        The agent instance to add.
         * @param configurator A consumer to configure the {@link AgentBuilder} for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Object agent, Consumer<AgentBuilder<?>> configurator) {
            return agent(agent, null, configurator);
        }

        /**
         * Adds an existing agent instance to the workflow, an output name, and a configurator.
         *
         * @param agent        The agent instance to add.
         * @param outputName   The output name for this agent.
         * @param configurator A consumer to configure the {@link AgentBuilder} for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Object agent, String outputName, Consumer<AgentBuilder<?>> configurator) {
            addExpression(new AgentExpression(this, agent, outputName, configurator));
            return this;
        }

        /**
         * Sets a state variable in the workflow's {@link AgenticScope}. This can be used to pass data between agents or
         * to control conditional logic.
         *
         * @param stateKey   The key for the state variable.
         * @param stateValue The value for the state variable.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> setState(String stateKey, Object stateValue) {
            agent(new SetStatesAgent(stateKey, stateValue));
            return this;
        }

        /**
         * Sets multiple state variables in the workflow's {@link AgenticScope}. This can be used to pass data between
         * agents or to control conditional logic.
         *
         * @param states A map of state keys to state values.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> setStates(Map<String, Object> states) {
            agent(new SetStatesAgent(states));
            return this;
        }

        /**
         * Adds a remote A2A agent to the workflow using its class and specifies an output name.
         *
         * @param url The URL of the remote A2A agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> remoteAgent(String url) {
            return remoteAgent(url, UntypedAgent.class);
        }

        /**
         * Adds a remote A2A agent to the workflow using its class and specifies an output name.
         *
         * @param url        The URL of the remote A2A agent.
         * @param agentClass The class of the agent to add.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> remoteAgent(String url, Class<?> agentClass) {
            return remoteAgent(url, agentClass, null);
        }

        /**
         * Adds a remote A2A agent to the workflow using its class and specifies an output name.
         *
         * @param url        The URL of the remote A2A agent.
         * @param outputName The output name for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> remoteAgent(String url, String outputName) {
            return remoteAgent(url, null, outputName);
        }

        /**
         * Adds a remote A2A agent to the workflow using its class and specifies an output name.
         *
         * @param url        The URL of the remote A2A agent.
         * @param agentClass The class of the agent to add.
         * @param outputName The output name for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> remoteAgent(String url, Class<?> agentClass, String outputName) {
            addExpression(new RemoteAgentExpression(this, url, agentClass, outputName));
            return this;
        }

        /**
         * Adds a breakpoint to the workflow that logs a message when hit. The message is a template that
         * can use workflow context variables with the `{{variable}}` notation.
         *
         * @param template The message template to log.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(String template) {
            return breakpoint(breakpointActionLog(template), null);
        }

        /**
         * Adds a conditional breakpoint to the workflow that logs a message when hit and the condition is met.
         * The message is a template that can use workflow context variables with the `{{variable}}` notation.
         *          *
         * @param template The message template to log.
         * @param condition The condition that must be true for the breakpoint to trigger.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(String template, Predicate<AgenticScope> condition) {
            return breakpoint(breakpointActionLog(template), condition);
        }

        /**
         * Adds a breakpoint to the workflow that executes a custom action when hit.
         *
         * @param action The action to execute when the breakpoint is hit.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(BiConsumer<Breakpoint, AgenticScope> action) {
            return breakpoint(action, null);
        }

        /**
         * Adds a conditional breakpoint to the workflow that executes a custom action when hit and the condition is met.
         *
         * @param action The action to execute when the breakpoint is hit.
         * @param condition The condition that must be true for the breakpoint to trigger.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(BiConsumer<Breakpoint, AgenticScope> action,
                                                  Predicate<AgenticScope> condition) {
            addExpression(new BreakpointExpression(this, action, condition));
            return this;
        }

        /**
        * Starts an "if-then" conditional block. The agents added after this call and before the matching {@code end()}
         * will only execute if the provided condition is true.
        *
        * <p>Example usage:</p>
        * <pre>{@code
        * EasyWorkflow.builder(MyAgent.class)
        * .ifThen(scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL)
        *   .agent(AnotherAgent.class)
        * .end()
        * }</pre>
         *
         * @param condition The condition to evaluate. Use state variables from {@code AgenticScope} com compose a
         *                  condition.
         * @return A new builder instance representing the "then" block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> ifThen(Predicate<AgenticScope> condition) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            IfThenStatement ifThenStatement = new IfThenStatement(result, condition);
            this.addExpression(ifThenStatement);
            result.setBlock(ifThenStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts a "do when" or a switch conditional block. This block allows for multiple "match" statements,
         * where each match statement's agents will execute if its condition (based on the function's
         * result) is met.
         *
         * @param function A function that provides a value to be matched against in subsequent
         *                 {@code match} statements.
         * @return A new builder instance representing the "do when" block. Call {@code end()} to close the "doWhen"
         *         return to the parent builder.
         * @see #match(Object)
         */
        public AgentWorkflowBuilder<T> doWhen(Function<AgenticScope, Object> function) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            DoWhenStatement doWhenStatement = new DoWhenStatement(result, function);
            this.addExpression(doWhenStatement);
            result.setBlock(doWhenStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Adds a "match" statement to a {@code doWhen} block. The agents within this match statement will execute
         * if the value provided here matches the value returned by the {@code doWhen} function.
         *
         * @param value The value to match against.
         * @return A new builder instance representing the "match" block. Call {@code end()} to close the "match"
         *         and return to the parent builder.
         */
        public AgentWorkflowBuilder<T> match(Object value) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            MatchStatement matchStatement = new MatchStatement(result, value);
            this.addExpression(matchStatement);
            result.setBlock(matchStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Adds a "match" statement to a {@code doWhen} block. The agents within this match statement will execute
         * if the value provided by the supplier here matches the value returned by the {@code doWhen} function.
         *
         * @param supplier A supplier that provides the value to match against.
         * @return A new builder instance representing the "match" block. Call {@code end()} to close the "match"
         *         and return to the parent builder.
         */
        public AgentWorkflowBuilder<T> match(Supplier<Object> supplier) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            MatchStatement matchStatement = new MatchStatement(result, supplier);
            this.addExpression(matchStatement);
            result.setBlock(matchStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts a "do parallel" block. Agents within this block will execute in parallel. The output of the parallel
         * execution will be composed by combining the output from the parallel agents to a {@code Map<String, Object>}
         * using a function obtained via {@code composeResultAsMap}.
         *
         * @return A new builder instance representing the parallel block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doParallel() {
            return doParallel(null);
        }

        /**
         * Starts a "do parallel" block. Agents within this block will execute in parallel. The output of the parallel
         * execution will be composed by the provided function.
         *
         * @param outputComposer A function to compose the output from the parallel agents. Use state variables from
         *                       {@code AgenticScope} com compose the output.
         * @return A new builder instance representing the parallel block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doParallel(Function<AgenticScope, Object> outputComposer) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            ParallelAgentsStatement parallelAgentsStatement = new ParallelAgentsStatement(result, outputComposer);
            this.addExpression(parallelAgentsStatement);
            result.outputName = "response";
            result.setBlock(parallelAgentsStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts a "group" block. Agents within this group will represent pure agentic AI; they will be supervised, and
         * their responses summarized. The default output name for the group's response is "response".
         *
         * @return A new builder instance representing the group block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doAsGroup() {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            GroupStatement groupStatement = new GroupStatement(result);
            this.addExpression(groupStatement);
            result.outputName = "response";
            result.setBlock(groupStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts a "repeat" block. Agents within this block will execute repeatedly till the condition evaluates to
         * {@code true}, or a maximum of 5 iterations is reached.
         *
         * @param condition The condition that, when {@code false}, will cause the loop to exit.
         * @return A new builder instance representing the repeat block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> repeat(Predicate<AgenticScope> condition) {
            return repeat(5, condition);
        }

        /**
         * Starts a "repeat" block with a specified maximum number of iterations. Agents within this block will execute
         * repeatedly until the condition is met or the maximum iterations are reached.
         *
         * @param maxIterations The maximum number of times to repeat the block.
         * @param condition     The condition that, when true, will cause the loop to exit.
         * @return A new builder instance representing the repeat block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> repeat(int maxIterations, Predicate<AgenticScope> condition) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            RepeatStatement repeatStatement = new RepeatStatement(result, maxIterations, condition.negate());
            this.addExpression(repeatStatement);
            result.setBlock(repeatStatement.getBlocks().get(0));

            return result;
        }

        /**
         * Ends the current nested statement (e.g., ifThen, doParallel, group, repeat) and returns to the parent
         * builder.
         *
         * @return The parent {@link AgentWorkflowBuilder} instance.
         * @throws IllegalStateException If there is no matching statement to end.
         */
        public AgentWorkflowBuilder<T> end() {
            if (parentBuilder == null)
                throw new IllegalStateException("Syntax error, e.g. 'end' without matching 'repeat'");
            return parentBuilder;
        }

        /**
         * Terminal operation that builds the EasyWorkflow and returns the main agent instance.
         *
         * @return The built main agent instance.
         * @throws IllegalStateException If there is a syntax error (e.g., an unclosed statement) or if the chat model
         *                               is not specified.
         */
        public T build() {
            if (parentBuilder != null || agentClass == null)
                throw new IllegalStateException("Syntax error, e.g. 'repeat' without matching 'end'");
            Objects.requireNonNull(chatModel, "Chat model is not specified");
            List<Object> agents = new ArrayList<>();
            if (workflowDebugger != null)
                agents.add(workflowDebugger.serviceAgent());
            agents.addAll(block.createAgents());

            SequentialAgentService<T> builder = AgenticServices.sequenceBuilder(agentClass)
                    .subAgents(agents.toArray())
                    .outputName(outputName != null && !outputName.isEmpty() ? outputName : AgentExpression.getOutputName(agentClass));
            if (outputComposer != null)
                builder.output(outputComposer);

            return proxy(builder.build());
        }

        @SuppressWarnings("unchecked")
        private T proxy(T agent) {
            assert agentClass != null;
            return (T) Proxy.newProxyInstance(
                    agentClass.getClassLoader(),
                    new Class<?>[]{agentClass, AgentSpecification.class, AgenticScopeOwner.class},
                    (proxy, method, args) -> {
                        boolean isAgentMethod = method.getAnnotation(Agent.class) != null;

                        if (isAgentMethod && workflowDebugger != null)
                            workflowDebugger.sessionStarted(agentClass, method, args);

                        Object invocationReult = method.invoke(agent, args);

                        if (isAgentMethod && workflowDebugger != null)
                            workflowDebugger.sessionStopped(invocationReult);
                        return invocationReult;
                    });
        }

        ChatModel getChatModel() {
            return chatModel == null && parentBuilder != null ? parentBuilder.getChatModel() : chatModel;
        }

        ChatMemory getChatMemory() {
            return chatMemory == null && parentBuilder != null ? parentBuilder.getChatMemory() : chatMemory;
        }

        String getOutputName() {
            return outputName;
        }

        boolean isLogInput() {
            if (logInput != null)
                return logInput;
            return parentBuilder != null && parentBuilder.isLogInput();
        }

        boolean isLogOutput() {
            if (logOutput != null)
                return logOutput;
            return parentBuilder != null && parentBuilder.isLogOutput();
        }
    }

    static class Block {
        private final List<Expression> expressions = new ArrayList<>();

        public List<Expression> getExpressions() {
            return Collections.unmodifiableList(expressions);
        }

        public void addExpression(Expression expression) {
            expressions.add(expression);
        }

        public List<Object> createAgents() {
            return expressions.stream()
                    .map(Expression::createAgent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    static class Statement implements Expression {
        final AgentWorkflowBuilder<?> agentWorkflowBuilder;
        private final List<Block> blocks;

        public Statement(AgentWorkflowBuilder<?> agentWorkflowBuilder, List<Block> aBlocks) {
            this.agentWorkflowBuilder = agentWorkflowBuilder;
            this.blocks = aBlocks;
        }

        public Statement(AgentWorkflowBuilder<?> agentWorkflowBuilder) {
            this.agentWorkflowBuilder = agentWorkflowBuilder;
            this.blocks = new ArrayList<>();
            this.blocks.add(new Block());
        }

        public List<Block> getBlocks() {
            return Collections.unmodifiableList(blocks);
        }

        @Override
        public Object createAgent() {
            return null;
        }
    }

    static class RepeatStatement extends Statement {
        private final int maxIterations;
        private final Predicate<AgenticScope> condition;

        public RepeatStatement(AgentWorkflowBuilder<?> builder, Predicate<AgenticScope> condition) {
            this(builder, 5, condition);
        }

        public RepeatStatement(AgentWorkflowBuilder<?> builder, int maxIterations, Predicate<AgenticScope> condition) {
            super(builder);

            if (maxIterations < 1 || maxIterations > 100)
                throw new IllegalArgumentException("Max iterations must be between 1 and 100");
            Objects.requireNonNull(condition, "Condition can't be null");

            this.maxIterations = maxIterations;
            this.condition = condition;
        }

        @Override
        public Object createAgent() {
            Object[] subAgents = getBlocks().get(0).createAgents().toArray();
            return AgenticServices
                    .loopBuilder()
                    .subAgents(subAgents)
                    .maxIterations(maxIterations)
                    .exitCondition(condition)
                    .build();
        }
    }

    static class IfThenStatement extends Statement {
        private final Predicate<AgenticScope> condition;

        public IfThenStatement(AgentWorkflowBuilder<?> builder, Predicate<AgenticScope> condition) {
            super(builder);

            Objects.requireNonNull(condition, "Condition can't be null");

            this.condition = condition;
        }

        @Override
        public Object createAgent() {
            return AgenticServices.conditionalBuilder()
                    .subAgents(condition, getBlocks().get(0).createAgents().toArray())
                    .build();
        }
    }

    static class DoWhenStatement extends Statement {
        private final Function<AgenticScope, Object> function;

        public DoWhenStatement(AgentWorkflowBuilder<?> builder, Function<AgenticScope, Object> function) {
            super(builder);

            Objects.requireNonNull(function, "Function can't be null");

            this.function = function;
        }

        @Override
        public Object createAgent() {
            ConditionalAgentService<UntypedAgent> builder = AgenticServices.conditionalBuilder();

            for (Expression expression : getBlocks().get(0).getExpressions()) {
                if (expression instanceof MatchStatement matchStatement) {
                    builder.subAgents(ctx -> {
                                Object state = function.apply(ctx);
                                Object valueToCompare = matchStatement.getValue();
                                return state == valueToCompare ||
                                        (state != null && state.equals(valueToCompare)) ||
                                        (valueToCompare != null && valueToCompare.equals(state));
                            },
                            matchStatement.getBlocks().get(0).createAgents().toArray());
                } else {
                    throw new IllegalStateException("'Syntax error: doWhen' statement may contain only 'match' statements");
                }
            }
            return builder.build();
        }
    }


    static class MatchStatement extends Statement {
        private final Object value;
        private final Supplier<Object> supplier;

        public MatchStatement(AgentWorkflowBuilder<?> builder, Object value) {
            super(builder);

            this.value = value;
            this.supplier = null;
        }

        public MatchStatement(AgentWorkflowBuilder<?> builder, Supplier<Object> supplier) {
            super(builder);

            Objects.requireNonNull(supplier);

            this.supplier = supplier;
            this.value = null;
        }

        public Object getValue() {
            return value != null ?
                    value :
                    (supplier != null ? supplier.get() : null);
        }

        @Override
        public Object createAgent() {
            return AgenticServices.sequenceBuilder()
                    .subAgents(getBlocks().get(0).createAgents().toArray())
                    .build();
        }
    }

    static class GroupStatement extends Statement {
        public GroupStatement(AgentWorkflowBuilder<?> builder) {
            super(builder);
        }

        @Override
        public Object createAgent() {
            return AgenticServices.supervisorBuilder()
                    .outputName(agentWorkflowBuilder.outputName)
                    .chatModel(agentWorkflowBuilder.getChatModel())
                    .subAgents(getBlocks().get(0).createAgents().toArray())
                    .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                    .build();
        }
    }

    static class ParallelAgentsStatement extends Statement {
        private final Function<AgenticScope, Object> outputComposer;

        public ParallelAgentsStatement(AgentWorkflowBuilder<?> builder, Function<AgenticScope, Object> aOutputComposer) {
            super(builder);

            outputComposer = aOutputComposer;
        }

        @Override
        public Object createAgent() {
            Function<AgenticScope, Object> composer = outputComposer;
            if (composer == null && agentWorkflowBuilder.getOutputName() != null && !agentWorkflowBuilder.getOutputName().isEmpty()) {
                List<String> outputNames = new ArrayList<>();
                for (Expression expression : getBlocks().get(0).getExpressions()) {
                    if (expression instanceof AgentExpression agentExpression)
                        outputNames.add(agentExpression.getOutputName());
                }
                composer = OutputComposers.asMap(outputNames.toArray(new String[0]));
            }

            ExecutorService executor = agentWorkflowBuilder.getExecutor();

            ParallelAgentService<UntypedAgent> builder = AgenticServices
                    .parallelBuilder()
                    .subAgents(getBlocks().get(0).createAgents().toArray())
                    .executor(executor != null ? executor : getSharedExecutorService())
                    .outputName(agentWorkflowBuilder.getOutputName());

            if (agentWorkflowBuilder.isLogOutput() && composer != null) {
                builder.output(composer.andThen(result -> {
                    logOutput(ParallelAgentService.class, agentWorkflowBuilder.outputName, result);
                    return result;
                }));
            } else {
                builder.output(composer);
            }

            return builder.build();
        }
    }

    static class AgentExpression implements Expression {
        private final AgentWorkflowBuilder<?> agentWorkflowBuilder;
        private final String outputName;
        private final Consumer<AgentBuilder<?>> configurator;
        private Class<?> agentClass;
        private Object agent;

        public AgentExpression(AgentWorkflowBuilder<?> agentWorkflowBuilder, Object agent, String outputName, Consumer<AgentBuilder<?>> configurator) {
            Objects.requireNonNull(agent, "Agent can't be null");
            this.agentWorkflowBuilder = agentWorkflowBuilder;
            this.agent = agent;
            this.outputName = outputName;
            this.configurator = configurator;
        }

        public AgentExpression(AgentWorkflowBuilder<?> agentWorkflowBuilder, Class<?> agentClass, String outputName, Consumer<AgentBuilder<?>> configurator) {
            Objects.requireNonNull(agentClass, "Agent class can't be null");
            this.agentWorkflowBuilder = agentWorkflowBuilder;
            this.agentClass = agentClass;
            this.outputName = outputName;
            this.configurator = configurator;
        }

        protected static String getOutputName(Class<?> agentClass) {
            String result = "response";
            Method[] declaredMethods = agentClass.getDeclaredMethods();
            if (declaredMethods.length > 0) {
                Agent annotation = declaredMethods[0].getAnnotation(Agent.class);
                if (annotation != null && !annotation.outputName().isEmpty())
                    result = annotation.outputName();
            }

            return result;
        }

        public Class<?> getAgentClass() {
            return agentClass;
        }

        public Object getAgent() {
            return agent;
        }

        @Override
        public Object createAgent() {
            Object result = agent;

            if (result == null) {
                String outName = getOutputName();
                AgentBuilder<?> agentBuilder = createAgentBuilder()
                        .chatModel(agentWorkflowBuilder.getChatModel());
                if (outName != null && !outName.isEmpty())
                    agentBuilder.outputName(outName);
                ChatMemory chatMemory = agentWorkflowBuilder.getChatMemory();
                if (chatMemory != null)
                    agentBuilder.chatMemoryProvider(memoryId -> chatMemory);

                WorkflowContextConfig workflowContextConfig = setupWorkflowDebugger(agentBuilder, agentWorkflowBuilder.getWorkflowDebugger(), outName);
                setupLogging(agentBuilder, agentWorkflowBuilder.isLogInput(), agentWorkflowBuilder.isLogOutput(), outName);

                invokeAnnotatedConfigurator(agentBuilder);

                if (configurator != null)
                    configurator.accept(agentBuilder);

                result = agentBuilder.build();

                if (workflowContextConfig != null) {
                    workflowContextConfig.input.setAgent(result);
                    workflowContextConfig.output.setAgent(result);
                }
            }

            return result;
        }

        record WorkflowContextConfig(WorkflowContext.Input input, WorkflowContext.Output output) {}

        private WorkflowContextConfig setupWorkflowDebugger(AgentBuilder<?> agentBuilder, WorkflowDebugger workflowDebugger, String outputName) {
            if (workflowDebugger == null)
                return null;

            WorkflowContext.Input input = workflowDebugger.getWorkflowContext().input(agentClass);
            agentBuilder.inputGuardrails(input);
            WorkflowContext.Output output = workflowDebugger.getWorkflowContext().output(agentClass, outputName);
            agentBuilder.outputGuardrails(output);

            return new WorkflowContextConfig(input, output);
        }

        private void setupLogging(AgentBuilder<?> agentBuilder, boolean logInput, boolean logOutput, String outName) {
            if (logInput)
                agentBuilder.inputGuardrails(new LoggingGuardrails.Input(agentClass));
            if (logOutput)
                agentBuilder.outputGuardrails(new LoggingGuardrails.Output(agentClass, outName));
        }

        private void invokeAnnotatedConfigurator(AgentBuilder<?> agentBuilder) {
            for (Method method : agentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AgentBuilderConfigurator.class)
                        && Modifier.isStatic(method.getModifiers())
                        && Modifier.isPublic(method.getModifiers())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(agentBuilder.getClass())) {
                    try {
                        method.invoke(null, agentBuilder);
                    } catch (Exception e) {
                        logger.warn("Failed to invoke AgentBuilderConfigurator method {}.{}", agentClass.getName(), method.getName(), e);
                    }
                }
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        protected AgentBuilder<?> createAgentBuilder() {
            return new AgentBuilder(agentClass, validateAgentClass(agentClass)) {
                private InputGuardrail[] inputGuardrailsLocal;
                private OutputGuardrail[] outputGuardrailsLocal;
                private Class[] inputGuardrailClassesLocal;
                private Class[] outputGuardrailClassesLocal;

                private InputGuardrail[] mergeInputGuardrails(InputGuardrail[] existing, InputGuardrail[] newGuardrails) {
                    return mergeGuardrails(existing, newGuardrails, InputGuardrail.class);
                }

                @Override
                public AgentBuilder<?> inputGuardrails(InputGuardrail[] inputGuardrails) {
                    inputGuardrailsLocal = inputGuardrailsLocal == null ?
                            inputGuardrails : mergeInputGuardrails(inputGuardrailsLocal, inputGuardrails);

                    return super.inputGuardrails(inputGuardrailsLocal);
                }

                private OutputGuardrail[] inputGuardrails(OutputGuardrail[] existing, OutputGuardrail[] newGuardrails) {
                    return mergeGuardrails(existing, newGuardrails, OutputGuardrail.class);
                }

                @Override
                public AgentBuilder<?> outputGuardrails(OutputGuardrail[] outputGuardrails) {
                    outputGuardrailsLocal = outputGuardrailsLocal == null ?
                            outputGuardrails : inputGuardrails(outputGuardrailsLocal, outputGuardrails);

                    return super.outputGuardrails(outputGuardrailsLocal);
                }

                private Class[] mergeInputGuardrailClasses(Class[] existing, Class[] newGuardrailClasses) {
                    return mergeGuardrailClasses(existing, newGuardrailClasses);
                }


                @Override
                public AgentBuilder<?> inputGuardrailClasses(Class[] inputGuardrailClasses) {
                    inputGuardrailClassesLocal = inputGuardrailClassesLocal == null ?
                            inputGuardrailClasses : mergeInputGuardrailClasses(inputGuardrailClassesLocal, inputGuardrailClasses);

                    return super.inputGuardrailClasses(inputGuardrailClassesLocal);
                }

                private Class[] mergeOutputGuardrailClasses(Class[] existing, Class[] newGuardrailClasses) {
                    return mergeGuardrailClasses(existing, newGuardrailClasses);
                }

                @Override
                public AgentBuilder<?> outputGuardrailClasses(Class[] outputGuardrailClasses) {
                    outputGuardrailClassesLocal = outputGuardrailClassesLocal == null ?
                            outputGuardrailClasses : mergeOutputGuardrailClasses(outputGuardrailClassesLocal, outputGuardrailClasses);
                    return super.outputGuardrailClasses(outputGuardrailClassesLocal);
                }

                private <G> G[] mergeGuardrails(G[] existing, G[] newGuardrails, Class<G> guardrailClass) {
                    List<G> mergedList = new ArrayList<>();
                    List<G> trailingGuardrails = new ArrayList<>();

                    if (existing != null) {
                        for (G guardrail : existing) {
                            if (guardrail instanceof TrailingGuardrail) {
                                trailingGuardrails.add(guardrail);
                            } else {
                                mergedList.add(guardrail);
                            }
                        }
                    }

                    if (newGuardrails != null) {
                        for (G guardrail : newGuardrails) {
                            if (guardrail instanceof TrailingGuardrail) {
                                trailingGuardrails.add(guardrail);
                            } else {
                                mergedList.add(guardrail);
                            }
                        }
                    }
                    mergedList.addAll(trailingGuardrails);
                    return mergedList.toArray((G[]) java.lang.reflect.Array.newInstance(guardrailClass, 0));
                }

                private Class[] mergeGuardrailClasses(Class[] existing, Class[] newGuardrailClasses) {
                    List<Class> mergedList = new ArrayList<>();
                    List<Class> trailingGuardrailClasses = new ArrayList<>();

                    if (existing != null) {
                        for (Class<?> guardrailClass : existing) {
                            if (TrailingGuardrail.class.isAssignableFrom(guardrailClass)) {
                                trailingGuardrailClasses.add(guardrailClass);
                            } else {
                                mergedList.add(guardrailClass);
                            }
                        }
                    }

                    if (newGuardrailClasses != null) {
                        for (Class<?> guardrailClass : newGuardrailClasses) {
                            if (TrailingGuardrail.class.isAssignableFrom(guardrailClass)) {
                                trailingGuardrailClasses.add(guardrailClass);
                            } else {
                                mergedList.add(guardrailClass);
                            }
                        }
                    }
                    mergedList.addAll(trailingGuardrailClasses);
                    return mergedList.toArray(new Class[0]);
                }
            };
        }

        public String getOutputName() {
            return outputName != null ? outputName : getOutputName(agentClass);
        }
    }

    static class RemoteAgentExpression extends AgentExpression {
        private final String url;

        public RemoteAgentExpression(AgentWorkflowBuilder<?> agentWorkflowBuilder, String url, Class<?> agentClass, String outputName) {
            super(agentWorkflowBuilder, agentClass, outputName, null);
            this.url = url;
        }

        @Override
        public Object createAgent() {
            Object result = getAgent();
            if (result == null) {
                String outName = getOutputName();
                A2AClientBuilder<?> agentBuilder = AgenticServices.a2aBuilder(url, getAgentClass());
                if (outName != null && !outName.isEmpty())
                    agentBuilder.outputName(outName);
                result = agentBuilder.build();
            }

            return result;
        }
    }

    static class BreakpointExpression implements Expression {
        private final BiConsumer<Breakpoint, AgenticScope> action;
        private final Predicate<AgenticScope> condition;
        private final AgentWorkflowBuilder<?> builder;

        public BreakpointExpression(AgentWorkflowBuilder<?> builder,
                                    BiConsumer<Breakpoint, AgenticScope> action,
                                    Predicate<AgenticScope> condition) {
            this.builder = builder;
            this.action = action;
            this.condition = condition;
        }

        @Override
        public Object createAgent() {
            WorkflowDebugger workflowDebugger = builder.getWorkflowDebugger();
            if (workflowDebugger == null)
                throw new IllegalStateException("Unable to create a breakpoint - no debugger specified");

            LineBreakpoint breakpoint = (LineBreakpoint) Breakpoint.builder(Breakpoint.Type.LINE, action)
                    .condition(condition)
                    .build();
            workflowDebugger.addBreakpoint(breakpoint);
            return breakpoint.createAgent(workflowDebugger);
        }
    }
}