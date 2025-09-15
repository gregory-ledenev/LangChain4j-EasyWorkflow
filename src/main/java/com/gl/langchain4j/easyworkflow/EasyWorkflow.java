/*
 *
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
 * /
 */

package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.internal.A2AClientBuilder;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * EasyWorkflow provides a fluent API for building complex agentic workflows using LangChain4j's Agentic framework.
 * It allows defining sequences of agents, conditional execution, parallel execution, agent grouping, and loops.
 * Use {@code EasyWorkflow.builder(...)} to start.
 */
public class EasyWorkflow {

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
     * A builder class for constructing an EasyWorkflow. It allows defining a sequence of agents and control flow
     * statements.
     *
     * @param <T> The type of the main agent for this workflow.
     */
    public static class AgentWorkflowBuilder<T> {

        private final AgentWorkflowBuilder<T> parentBuilder;
        private final Class<T> agentClass;
        private final Block root;
        private String outputName;
        private ChatModel chatModel;
        private ChatMemory chatMemory;
        private boolean logInput;
        private boolean logOutput;

        AgentWorkflowBuilder(AgentWorkflowBuilder<T> aParentBuilder, Block block) {
            Objects.requireNonNull(aParentBuilder, "Parent builder can't be null");
            Objects.requireNonNull(block, "Block can't be null");

            parentBuilder = aParentBuilder;
            this.root = block;
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
            this.root = new Block();
            this.agentClass = agentClass;
        }

        void addExpression(Expression expression) {
            root.addExpression(expression);
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
         * Adds an agent to the workflow using its class.
         *
         * @param agentClass The class of the agent to add.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Class<?> agentClass) {
            return agent(agentClass, null, null);
        }

        /**
         * Adds an agent to the workflow using its class and specifies an output name.
         *
         * @param agentClass The class of the agent to add.
         * @param outputName The output name for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Class<?> agentClass, String outputName) {
            return agent(agentClass, outputName, null);
        }

        /**
         * Adds an agent to the workflow using its class and a configurator for its builder.
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
            addExpression(new AgentExpression(agentClass, outputName, configurator));
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
         * Adds an existing agent instance to the workflow and specifies an output name.
         *
         * @param agent      The agent instance to add.
         * @param outputName The output name for this agent.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> agent(Object agent, String outputName) {
            return agent(agent, outputName, null);
        }

        /**
         * Adds an existing agent instance to the workflow and a configurator for its builder.
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
            addExpression(new AgentExpression(agent, outputName, configurator));
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
            addExpression(new RemoteAgentExpression(url, agentClass, outputName));
            return this;
        }

        /**
         * Starts an "if-then" conditional block. The agents added after this call and before the matching {@code end()}
         * will only execute if the provided condition is true.
         *
         * @param condition The condition to evaluate. Use state variables from {@code AgenticScope} com compose a condition.
         * @return A new builder instance representing the "then" block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> ifThen(Predicate<AgenticScope> condition) {
            IfThenStatement ifThenStatement = new IfThenStatement(condition);
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this, ifThenStatement.getBlocks().get(0));
            this.addExpression(ifThenStatement);
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
         * {@code AgenticScope} com compose the output.
         * @return A new builder instance representing the parallel block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doParallel(Function<AgenticScope, Object> outputComposer) {
            return doParallel("response", outputComposer);
        }

        /**
         * Starts a "do parallel" block with a specified output name. Agents within this block will execute in parallel.
         * The output of the parallel execution will be composed by the provided function.
         *
         * @param outputName     The name to assign to the output of this parallel block.
         * @param outputComposer A function to compose the output from the parallel agents. Use state variables from
         * {@code AgenticScope} com compose the output.
         * @return A new builder instance representing the parallel block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doParallel(String outputName, Function<AgenticScope, Object> outputComposer) {
            ParallelAgentsStatement parallelAgentsStatement = new ParallelAgentsStatement(outputName, outputComposer);
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this, parallelAgentsStatement.getBlocks().get(0));
            this.addExpression(parallelAgentsStatement);

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
            return doAsGroup("response");
        }

        /**
         * Starts a "group" block. Agents within this group will represent pure agentic AI; they will be supervised, and
         * their responses summarized. The default output name for the group's response is "response".
         *
         * @param outputName The name to assign to the output of this group.
         * @return A new builder instance representing the group block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doAsGroup(String outputName) {
            GroupStatement groupStatement = new GroupStatement(outputName);
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this, groupStatement.getBlocks().get(0));
            this.addExpression(groupStatement);
            return result;
        }

        /**
         * Starts a "repeat" block. Agents within this block will execute repeatedly until the condition is met, or a
         * maximum of 5 iterations is reached.
         *
         * @param condition The condition that, when true, will cause the loop to exit.
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
            RepeatStatement repeatStatement = new RepeatStatement(maxIterations, condition);
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this, repeatStatement.getBlocks().get(0));
            this.addExpression(repeatStatement);
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
            return AgenticServices
                    .sequenceBuilder(agentClass)
                    .subAgents(root.createAgents(chatModel, chatMemory, logInput, logOutput).toArray())
                    .outputName(outputName != null && !outputName.isEmpty() ? outputName : AgentExpression.getOutputName(agentClass))
                    .build();
        }
    }

    /**
     * Represents an expression within the workflow, which can create an agent.
     */
    interface Expression {
        Object createAgent(ChatModel chatModel, ChatMemory aChatMemory, boolean logInput, boolean logOutput);
    }

    static class Block {
        List<Expression> expressions = new ArrayList<>();

        public List<Expression> getExpressions() {
            return Collections.unmodifiableList(expressions);
        }

        public void addExpression(Expression expression) {
            expressions.add(expression);
        }

        public List<Object> createAgents(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            return expressions.stream()
                    .map(expression -> expression.createAgent(chatModel, chatMemory, logInput, logOutput))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    static class Statement implements Expression {

        List<Block> blocks;

        public Statement(List<Block> aBlocks) {
            blocks = aBlocks;
        }

        public Statement() {
            blocks = new ArrayList<>();
            blocks.add(new Block());
        }

        public List<Block> getBlocks() {
            return Collections.unmodifiableList(blocks);
        }

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory aChatMemory, boolean logInput, boolean logOutput) {
            return null;
        }
    }

    static class RepeatStatement extends Statement {
        int maxIterations;
        Predicate<AgenticScope> condition;

        public RepeatStatement(Predicate<AgenticScope> condition) {
            this(5, condition);
        }

        public RepeatStatement(int maxIterations, Predicate<AgenticScope> condition) {
            if (maxIterations < 1 || maxIterations > 100)
                throw new IllegalArgumentException("Max iterations must be between 1 and 100");
            Objects.requireNonNull(condition, "Condition can't be null");

            this.maxIterations = maxIterations;
            this.condition = condition;
        }

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            Object[] subAgents = blocks.get(0).createAgents(chatModel, chatMemory, logInput, logOutput).toArray();
            return AgenticServices
                    .loopBuilder()
                    .subAgents(subAgents)
                    .maxIterations(maxIterations)
                    .exitCondition(condition)
                    .build();
        }
    }

    static class IfThenStatement extends Statement {
        Predicate<AgenticScope> condition;

        public IfThenStatement(Predicate<AgenticScope> condition) {
            Objects.requireNonNull(condition, "Condition can't be null");

            this.condition = condition;
        }

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            return AgenticServices.conditionalBuilder()
                    .subAgents(condition, blocks.get(0).createAgents(chatModel, chatMemory, logInput, logOutput).toArray())
                    .build();
        }
    }

    static class GroupStatement extends Statement {
        private final String outputName;

        public GroupStatement(String outputName) {
            this.outputName = outputName;
        }

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            return AgenticServices.supervisorBuilder()
                    .outputName(outputName)
                    .chatModel(chatModel)
                    .subAgents(blocks.get(0).createAgents(chatModel, chatMemory, logInput, logOutput).toArray())
                    .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                    .build();
        }
    }

    static class ParallelAgentsStatement extends Statement {
        private final String outputName;
        Function<AgenticScope, Object> outputComposer;

        public ParallelAgentsStatement(String aOutputName, Function<AgenticScope, Object> aOutputComposer) {
            outputName = aOutputName;
            outputComposer = aOutputComposer;
        }

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            Function<AgenticScope, Object> composer = outputComposer;
            if (composer == null && outputName != null && ! outputName.isEmpty()) {
                List<String> outputNames = new ArrayList<>();
                for (Expression expression : blocks.get(0).getExpressions()) {
                    if (expression instanceof AgentExpression agentExpression)
                        outputNames.add(agentExpression.getOutputName());
                }
                composer = ResultComposers.asMap(outputNames.toArray(new String[0]));
            }
            ParallelAgentService<UntypedAgent> builder = AgenticServices
                    .parallelBuilder()
                    .subAgents(blocks.get(0).createAgents(chatModel, chatMemory, logInput, logOutput).toArray())
                    .executor(getSharedExecutorService())
                    .outputName(outputName);

            if (logOutput && composer != null)
                builder.output(composer.andThen(result -> {
                    logOutput(ParallelAgentService.class, outputName, result);
                    return result;
                }));
            else
                builder.output(composer);

            return builder.build();
        }
    }

    static class AgentExpression implements Expression {
        Class<?> agentClass;
        Object agent;
        String outputName;
        Consumer<AgentBuilder<?>> configurator;

        public AgentExpression(Object agent, String outputName, Consumer<AgentBuilder<?>> configurator) {
            Objects.requireNonNull(agent, "Agent can't be null");
            this.agent = agent;
            this.outputName = outputName;
            this.configurator = configurator;
        }

        public AgentExpression(Class<?> agentClass, String outputName, Consumer<AgentBuilder<?>> configurator) {
            Objects.requireNonNull(agentClass, "Agent class can't be null");
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

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            Object result = agent;
            if (result == null) {
                String outName = getOutputName();
                AgentBuilder<?> agentBuilder = createAgentBuilder()
                        .chatModel(chatModel);
                if (outName != null && !outName.isEmpty())
                    agentBuilder.outputName(outName);
                if (chatMemory != null)
                    agentBuilder.chatMemoryProvider(memoryId -> chatMemory);

                if (logInput)
                    agentBuilder.inputGuardrails(new LoggingGuardrails.Input(agentClass));
                if (logOutput)
                    agentBuilder.outputGuardrails(new LoggingGuardrails.Output(agentClass, outName));

                if (configurator != null)
                    configurator.accept(agentBuilder);

                result = agentBuilder.build();
            }

            return result;
        }

        protected AgentBuilder<?> createAgentBuilder() {
            return AgenticServices.agentBuilder(agentClass);
        }

        public String getOutputName() {
            return outputName != null ? outputName : getOutputName(agentClass);
        }
    }

    static class RemoteAgentExpression extends AgentExpression {
        private final String url;

        public RemoteAgentExpression(String url, Class<?> agentClass, String outputName) {
            super(agentClass, outputName, null);
            this.url = url;
        }

        @Override
        public Object createAgent(ChatModel chatModel, ChatMemory chatMemory, boolean logInput, boolean logOutput) {
            Object result = agent;
            if (result == null) {
                String outName = getOutputName();
                A2AClientBuilder<?> agentBuilder = AgenticServices.a2aBuilder(url, agentClass);
                if (outName != null && !outName.isEmpty())
                    agentBuilder.outputName(outName);
                result = agentBuilder.build();
            }

            return result;
        }
    }

    /**
     * A shared {@link ExecutorService} used for parallel agent execution. It is initialized on first use and can be
     * explicitly closed.
     */
    static AtomicReference<ExecutorService> sharedExecutorService = new AtomicReference<>();

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
     * Creates a {@link HumanInTheLoop} agent that interacts with the user via the console.
     *
     * @param outputName The name to assign to the output of this human-in-the-loop agent.
     * @param description A description of the human's role or the prompt to display to the human.
     * @return A new {@link HumanInTheLoop} instance configured for console interaction.
     */
    public static HumanInTheLoop consoleHumanInTheLoopAgent(String outputName, String description) {
        return AgenticServices
                .humanInTheLoopBuilder()
                .description(description)
                .outputName(outputName)
                .requestWriter(request -> {
                    System.out.println(request);
                    System.out.print("> ");
                })
                .responseReader(() -> {
                    try (Scanner scanner = new Scanner(System.in)) {
                        String confirmation = scanner.nextLine();
                        System.out.println("Proceeding...");
                        return confirmation;
                    }
                })
                .build();
    }

    private static final Logger logger = LoggerFactory.getLogger(EasyWorkflow.class);

    /**
     * Logs the output of an agent.
     *
     * @param agentClass The class of the agent.
     * @param outputName The name of the output.
     * @param result The output result.
     */
    public static void logOutput(Class<?> agentClass, String outputName, Object result) {
        logger.info("Agent '{}' output: '{}' -> {}", agentClass.getSimpleName(), outputName, result);
    }

    /**
     * Logs the input to an agent.
     * @param agentClass The class of the agent.
     * @param input The input to the agent.
     */
    public static void logInput(Class<?> agentClass, Object input) {
        logger.info("Agent '{}' input: {}", agentClass.getSimpleName(), input);
    }
}
