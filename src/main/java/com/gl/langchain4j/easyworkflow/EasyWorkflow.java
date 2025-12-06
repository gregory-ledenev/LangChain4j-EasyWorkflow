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
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gl.langchain4j.easyworkflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.internal.A2AClientBuilder;
import dev.langchain4j.agentic.internal.AgentSpecification;
import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;

import static com.gl.langchain4j.easyworkflow.BreakpointActions.log;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.Breakpoint;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.LineBreakpoint;
import static dev.langchain4j.agentic.internal.AgentUtil.validateAgentClass;

/**
 * EasyWorkflow provides a fluent API for building complex agentic workflows using LangChain4j's Agentic framework. It
 * allows defining sequences of agents, conditional execution, parallel execution, agent grouping, and loops. Use
 * {@code EasyWorkflow.builder(...)} to start.
 */
@SuppressWarnings("ALL")
public class EasyWorkflow {
    public static final String USER_HOME_FOLDER = ".EasyWorkflow";

    public static final String JSON_TYPE_AGENT = "agent";
    public static final String JSON_TYPE_NON_AI_AGENT = "nonAiAgent";
    public static final String JSON_TYPE_REPEAT = "repeat";
    public static final String JSON_TYPE_IF_THEN = "ifThen";
    public static final String JSON_TYPE_DO_WHEN = "doWhen";
    public static final String JSON_TYPE_MATCH = "match";
    public static final String JSON_TYPE_GROUP = "group";
    public static final String JSON_TYPE_PLANNER_GROUP = "plannerGroup";
    public static final String JSON_TYPE_DO_PARALLEL = "doParallel";
    public static final String JSON_TYPE_BREAKPOINT = "breakpoint";
    public static final String JSON_KEY_UID = "uid";
    public static final String JSON_KEY_AGENT_CLASS_NAME = "className";
    public static final String JSON_KEY_OUTPUT_NAME = "outputName";
    public static final String JSON_KEY_OUTPUT_TYPE = "outputType";
    public static final String JSON_KEY_DESCRIPTION = "description";
    public static final String JSON_KEY_USER_MESSAGE = "userMessage";
    public static final String JSON_KEY_SYSTEM_MESSAGE = "systemMessage";
    public static final String JSON_KEY_PARAMETERS = "parameters";
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_TYPE = "type";
    public static final String JSON_KEY_LABEL = "label";
    public static final String JSON_KEY_EDITOR_TYPE = "editorType";
    public static final String JSON_KEY_EDITOR_CHOICES = "editorChoices";
    public static final String JSON_KEY_CONDITION = "condition";
    public static final String JSON_KEY_EXPRESSION = "expression";
    public static final String JSON_KEY_MAX_ITERATIONS = "maxIterations";
    public static final String JSON_KEY_VALUE = "value";
    public static final String JSON_KEY_PLANNER = "planner";
    /**
     * A shared {@link ExecutorService} used for parallel agent execution. It is initialized on first use and can be
     * explicitly closed.
     */
    private static final AtomicReference<ExecutorService> sharedExecutorService = new AtomicReference<>();
    private static LoggerAspect loggerAspect = null;
    private static final Logger logger = getLogger(EasyWorkflow.class);

    private EasyWorkflow() {
    }

    /**
     * Creates a {@link Predicate} that can be used in conditional workflow statements (e.g., {@code ifThen}) and
     * provides a textual representation for visualization in diagrams.
     *
     * @param condition       The actual predicate logic.
     * @param conditionString A string representation of the condition, used for debugging and diagrams.
     * @return A {@link Predicate} with an overridden {@code toString()} method.
     */
    public static Predicate<AgenticScope> condition(Predicate<AgenticScope> condition, String conditionString) {
        return new Predicate<>() {
            @Override
            public String toString() {
                return conditionString;
            }

            @Override
            public boolean test(AgenticScope agenticScope) {
                return condition.test(agenticScope);
            }
        };
    }

    /**
     * Creates a {@link Function} that can be used in workflow statements (e.g., {@code doWhen}) and provides a textual
     * representation for visualization in diagrams.
     *
     * @param expression       The actual function logic.
     * @param expressionString A string representation of the expression, used for debugging and diagrams.
     * @return A {@link Function} with an overridden {@code toString()} method.
     */
    public static Function<AgenticScope, Object> expression(Function<AgenticScope, Object> expression, String expressionString) {
        return lambdaWithDescription(expression, expressionString);
    }

    /**
     * Wraps a lambda expression with a proxy that overrides its {@code toString()} method to return a custom description.
     * This is useful for providing meaningful names to lambdas used in workflow definitions, which can then be
     * displayed in diagrams or logs.
     * @param lamnda The lambda expression to wrap.
     * @param description The description to associate with the lambda.
     * @param <T> The type of the lambda expression.
     * @return A proxied lambda expression with the custom description.
     */
    public static <T> T lambdaWithDescription(T lamnda, String description) {
        return (T) Proxy.newProxyInstance(
                lamnda.getClass().getClassLoader(),
                lamnda.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("toString")) {
                            return description;
                        }
                        return method.invoke(lamnda, args);
                    }
                }
        );
    }
    /**
     * Retrieves the textual representation of a lambda, if available.
     *
     * @param lambda The lambda.
     * @return The string representation of the lambda, or {@code null} if it's a lambda expression without a custom
     * {@code toString()}.
     */
    public static String getDescription(Object lambda) {
        String result = lambda.toString();
        return result.contains("$$Lambda/") ? null : result;
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
                result.toString().replaceAll("\n", "\\n"));
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
     * Expands a template using the provided map of states.
     *
     * @param template The template string to expand.
     * @param states   The map containing the state variables.
     * @return An expanded text.
     */
    public static String expandTemplate(String template, Map<String, Object> states) {
        return PromptTemplate.from(template).apply(states).text();
    }

    /**
     * Retrieves the name of an agent.
     *
     * @param agent The agent instance.
     * @return The name of the agent.
     */
    public static String getAgentName(Object agent) {
        return getAgentName(agent.getClass());
    }

    /**
     * Retrieves the name of an agent by its class. If the agent class has a method annotated with {@link Agent} and its
     * {@code name} attribute is not blank, that name is returned. Otherwise, the simple name of the agent's class is
     * returned.
     *
     * @param agentClass The class of the agent.
     * @return The name of the agent.
     */
    public static String getAgentName(Class<?> agentClass) {
        String result = agentClass.getSimpleName().replaceAll("(?<=[a-z])(?=[A-Z])", " ");

        for (Method method : agentClass.getDeclaredMethods()) {
            Agent annotation = method.getAnnotation(Agent.class);
            if (annotation != null) {
                if (!annotation.name().isBlank())
                    result = annotation.name();
                break;
            }
        }

        return result;
    }

    /**
     * Retrieves the method annotated with {@link Agent} from the given class.
     *
     * @param clazz The class to inspect.
     * @return The method annotated with {@link Agent}, or {@code null} if no such method is found.
     */
    public static Method getAgentMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            Agent annotation = method.getAnnotation(Agent.class);
            if (annotation != null)
                return method;
        }
        return null;
    }

    /**
     * Retrieves the names of the parameters of the given agent method.
     *
     * @param agentMethod The agent method to inspect.
     * @return A list of parameter names.
     */
    public static List<String> getAgentMethodParameterNames(Method agentMethod) {
        Objects.requireNonNull(agentMethod);

        return Arrays.stream(agentMethod.getParameters())
                .map(p -> {
                    V v = p.getAnnotation(V.class);
                    return v != null ? v.value() : p.getName();
                })
                .collect(Collectors.toList());
    }

    /**
     * Expands the {@link UserMessage} template of an agent method with the provided states.
     *
     * @param clazz  The class of the agent.
     * @param states The map of states to use for template expansion.
     * @return The expanded user message, or {@code null} if no {@link UserMessage} annotation is found or an error
     * occurs.
     */
    public static String expandUserMessage(Class<?> clazz, Map<String, Object> states) {
        String template = getUserMessageTemplate(clazz);
        return template != null ? EasyWorkflow.expandTemplate(template, states) : null;
    }

    /**
     * Retrieves the {@link UserMessage} template from the method annotated with {@link Agent} in the given class.
     *
     * @param clazz The class of the agent.
     * @return The user message template string, or {@code null} if no {@link UserMessage} annotation is found or an
     * error occurs.
     */
    public static String getUserMessageTemplate(Class<?> clazz) {
        Method agentMethod = getAgentMethod(clazz);
        if (agentMethod != null) {
            UserMessage annotation = agentMethod.getAnnotation(UserMessage.class);
            if (annotation != null)
                return getUserMessageTemplate(clazz, annotation.value(), annotation.delimiter(), annotation.fromResource());
        }
        return null;
    }

    /**
     * Retrieves the {@link SystemMessage} template from the method annotated with {@link Agent} in the given class.
     *
     * @param clazz The class of the agent.
     * @return The system message template string, or {@code null} if no {@link SystemMessage} annotation is found or an
     * error occurs.
     */
    public static String getSystemMessageTemplate(Class<?> clazz) {
        Method agentMethod = getAgentMethod(clazz);
        if (agentMethod != null) {
            SystemMessage annotation = agentMethod.getAnnotation(SystemMessage.class);
            if (annotation != null)
                return getUserMessageTemplate(clazz, annotation.value(), annotation.delimiter(), annotation.fromResource());
        }
        return null;
    }

    private static String getUserMessageTemplate(Class<?> clazz, String[] value, String delimiter, String fromResource) {
        String template = null;
        if (!fromResource.isEmpty()) {
            try (InputStream is = clazz.getResourceAsStream("/" + fromResource)) {
                if (is != null)
                    template = new String(is.readAllBytes());
            } catch (IOException e) {
                logger.warn("Failed to load User Message from resource: {}", fromResource, e);
            }
        } else {
            template = String.join(delimiter, value);
        }
        return template;
    }

    /**
     * Retrieves the currently set {@link LoggerAspect}.
     *
     * @return The {@link LoggerAspect} instance, or {@code null} if none is set.
     */
    public static LoggerAspect getLoggerAspect() {
        return loggerAspect;
    }

    /**
     * Sets the {@link LoggerAspect} to be used for intercepting logger calls.
     *
     * @param loggerAspect The {@link LoggerAspect} instance to set.
     */
    public static void setLoggerAspect(LoggerAspect loggerAspect) {
        EasyWorkflow.loggerAspect = loggerAspect;
    }

    /**
     * Retrieves a proxied {@link Logger} instance for the given class. If a {@link LoggerAspect} is set, it will
     * intercept calls to the logger methods.
     *
     * @param clazz The class for which to get the logger.
     * @return A proxied {@link Logger} instance.
     */
    public static Logger getLogger(Class<?> clazz) {
        Logger originalLogger = LoggerFactory.getLogger(clazz);
        return (Logger) Proxy.newProxyInstance(
                Logger.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        LoggerAspect aspect = getLoggerAspect();
                        return aspect != null ?
                                aspect.invoke(originalLogger, method, args) :
                                method.invoke(originalLogger, args);
                    }
                }
        );

    }

    /**
     * Represents an expression within the workflow, which can create an agent.
     */
    public interface Expression {
        Object createAgent();

        String getId();

        Map<String, Object> toJson();

        String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes);
    }

    /**
     * An interface for defining an aspect that can intercept logger calls.
     */
    public static interface LoggerAspect {
        /**
         * Intercepts a logger method invocation.
         *
         * @param logger The original {@link Logger} instance.
         * @param method The method being invoked on the logger.
         * @param args   The arguments passed to the logger method.
         * @return The result of the invocation.
         * @throws Throwable If an error occurs during invocation.
         */
        Object invoke(Logger logger, Method method, Object[] args) throws Throwable;
    }

    /**
     * A builder class for constructing an EasyWorkflow. It allows defining a sequence of agents and control flow
     * statements.
     *
     * @param <T> The type of the main agent for this workflow.
     */
    public static class AgentWorkflowBuilder<T> {

        public static final String FLOW_CHART_NODE_START = "startNode";
        public static final String FLOW_CHART_NODE_END = "endNode";
        public static final String FLOW_CHART_GRAPH_DEFINITION = "graphDefinition";
        public static final String FLOW_CHART_NODE_DATA = "nodeData";
        public static final String FLOW_CHART_NODE_NAMES = "nodeNames";
        public static final String FLOW_CHART_COMPLETED_NODES = "completedNodes";
        public static final String FLOW_CHART_TITLE = "title";
        public static final String FLOW_CHART_SUB_TITLE = "subTitle";
        public static final String FLOW_CHART_WORKFLOW_SUMMARY_MARKDOWN = "workflowSummaryMarkdown";
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

        private static String mermaidInspectorLink(String node) {
            return "click %s call showInspector(\"%s\")\n".formatted(node, node);
        }

        private static String getOutputName(Class<?> agentClass) {
            String result = null;

            if (agentClass != null) {
                Method[] declaredMethods = agentClass.getDeclaredMethods();
                if (declaredMethods.length > 0) {
                    Agent annotation = declaredMethods[0].getAnnotation(Agent.class);
                    if (annotation != null && !annotation.outputKey().isEmpty())
                        result = annotation.outputKey();
                }
            }

            return result;
        }

        public Class<T> getAgentClass() {
            return agentClass;
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
            Objects.requireNonNull(workflowDebugger, "Workflow debugger can't be null");

            this.workflowDebugger = workflowDebugger;
            this.workflowDebugger.setAgentWorkflowBuilder(rootBuilder());
            return this;
        }

        WorkflowDebugger getWorkflowDebugger() {
            return workflowDebugger == null && parentBuilder != null ? parentBuilder.getWorkflowDebugger() : workflowDebugger;
        }

        AgentWorkflowBuilder<T> rootBuilder() {
            if (parentBuilder == null)
                return this;
            return parentBuilder.rootBuilder();
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
            addExpression(new NonAIAgentExpression(this, agent, null, null));
            return this;
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
            addExpression(new NonAIAgentExpression(this, agent, outputName, configurator));
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
            return setStates(SetStateAgents.agentOf(stateKey, stateValue));
        }

        /**
         * Sets multiple state variables in the workflow's {@link AgenticScope}. This can be used to pass data between
         * agents or to control conditional logic.
         *
         * @param states A map of state keys to state values.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> setStates(Map<String, Object> states) {
            return setStates(SetStateAgents.agentOf(states));
        }

        /**
         * Sets multiple state variables in the workflow's {@link AgenticScope} using a supplier. This can be used to
         * pass data between agents or to control conditional logic.
         *
         * @param stateSupplier A supplier that provides a map of state keys to state values.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> setStates(Supplier<Map<String, Object>> stateSupplier) {
            return setStates(SetStateAgents.agentOf(stateSupplier));
        }

        AgentWorkflowBuilder<T> setStates(Object agent) {
            addExpression(new SetStateAgentExpression(this, agent));
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
         * Adds a breakpoint to the workflow that logs a message when hit. The message is a template that can use
         * workflow context variables with the `{{variable}}` notation.
         *
         * @param template The message template to log.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(String template) {
            return breakpoint(log(template), null);
        }

        /**
         * Adds a conditional breakpoint to the workflow that logs a message when hit and the condition is met. The
         * message is a template that can use workflow context variables with the `{{variable}}` notation. *
         *
         * @param template  The message template to log.
         * @param condition The condition that must be true for the breakpoint to trigger.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(String template, Predicate<Map<String, Object>> condition) {
            return breakpoint(log(template), condition);
        }

        /**
         * Adds a breakpoint to the workflow that executes a custom action when hit.
         *
         * @param action The action to execute when the breakpoint is hit.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(BiFunction<Breakpoint, Map<String, Object>, Object> action) {
            return breakpoint(action, null);
        }

        /**
         * Adds a conditional breakpoint to the workflow that executes a custom action when hit and the condition is
         * met.
         *
         * @param action    The action to execute when the breakpoint is hit.
         * @param condition The condition that must be true for the breakpoint to trigger.
         * @return This builder instance.
         */
        public AgentWorkflowBuilder<T> breakpoint(BiFunction<Breakpoint, Map<String, Object>, Object> action,
                                                  Predicate<Map<String, Object>> condition) {
            addExpression(new BreakpointExpression(this, action, condition));
            return this;
        }

        /**
         * Starts an "if-then" conditional block. The agents added after this call and before the matching {@code end()}
         * will only execute if the provided condition is true. It is not possible to extract a condition expression
         * from a lambda function so to see conditions in documentation or diagrams use the {@code condition(...)}
         * method to associate a predicate with a condition expression.
         *
         * @param condition The condition to evaluate. Use state variables from {@code AgenticScope} com compose a
         *                  condition.
         * @return A new builder instance representing the "then" block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> ifThen(Predicate<AgenticScope> condition) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            IfThenStatement ifThenStatement = new IfThenStatement(result, condition, getDescription(condition));
            this.addExpression(ifThenStatement);
            result.setBlock(ifThenStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts an "else" block, which must follow an {@code ifThen} statement. The agents added after this call and
         * before the matching {@code end()} will execute if the condition of the preceding {@code ifThen} statement is
         * false.
         *
         * @return A new builder instance representing the "else" block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> elseIf() {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);

            Predicate<AgenticScope> condition = null;
            for (int i = block.getExpressions().size() - 1; i >= 0; i--) {
                Expression expression = block.getExpressions().get(i);
                if (expression instanceof IfThenStatement ifThenStatement) {
                    condition = ifThenStatement.condition;
                    break;
                }
            }

            if (condition == null)
                throw new IllegalStateException("Syntax error: misplaced 'elseIf' - no 'ifThen' statement found'");

            ElseIfStatement ifThenStatement = new ElseIfStatement(result, condition.negate());
            this.addExpression(ifThenStatement);
            result.setBlock(ifThenStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts a "do when" or a switch conditional block. This block allows for multiple "match" statements, where
         * each match statement's agents will execute if its condition (based on the function's result) is met.
         *
         * @param function A function that provides a value to be matched against in subsequent {@code match}
         *                 statements.
         * @return A new builder instance representing the "do when" block. Call {@code end()} to close the "doWhen"
         * return to the parent builder.
         * @see #match(Object)
         */
        public AgentWorkflowBuilder<T> doWhen(Function<AgenticScope, Object> function) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            DoWhenStatement doWhenStatement = new DoWhenStatement(result, function, getDescription(function));
            this.addExpression(doWhenStatement);
            result.setBlock(doWhenStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Starts a "do when" or a switch conditional block. This block allows for multiple "match" statements, where
         * each match statement's agents will execute if its condition (based on the state variable's value) is met.
         *
         * @param stateName    The name of the state variable to read from the {@link AgenticScope}.
         * @param defaultValue The default value to use if the state variable is not found.
         * @return A new builder instance representing the "do when" block. Call {@code end()} to close the "doWhen"
         * return to the parent builder.
         */
        public AgentWorkflowBuilder<T> doWhen(String stateName, Object defaultValue) {
            return doWhen(expression(agenticScope -> agenticScope.readState(stateName, defaultValue), stateName));
        }

        /**
         * Adds a "match" statement to a {@code doWhen} block. The agents within this match statement will execute if
         * the value provided here matches the value returned by the {@code doWhen} function.
         *
         * @param value The value to match against.
         * @return A new builder instance representing the "match" block. Call {@code end()} to close the "match" and
         * return to the parent builder.
         */
        public AgentWorkflowBuilder<T> match(Object value) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            MatchStatement matchStatement = new MatchStatement(result, value);
            this.addExpression(matchStatement);
            result.setBlock(matchStatement.getBlocks().get(0));
            return result;
        }

        /**
         * Adds a "match" statement to a {@code doWhen} block. The agents within this match statement will execute if
         * the value provided by the supplier here matches the value returned by the {@code doWhen} function.
         *
         * @param supplier A supplier that provides the value to match against.
         * @return A new builder instance representing the "match" block. Call {@code end()} to close the "match" and
         * return to the parent builder.
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
         * Starts a supervised "group" block. Agents within this group will represent pure agentic AI; they will be
         * supervised, and their responses summarized. The default output name for the group's response is "response".
         *
         * @return A new builder instance representing the group block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doAsGroup() {
            return doAsGroup(null);
        }

        /**
         * Starts a supervised "group" block. Agents within this group will represent pure agentic AI; they will be
         * supervised, and their responses summarized. The default output name for the group's response is "response".
         *
         * @param outputName The output name for the group's response.
         * @return A new builder instance representing the group block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doAsGroup(String outputName) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            GroupStatement groupStatement = new GroupStatement(result);
            this.addExpression(groupStatement);
            result.outputName = outputName != null ? outputName : "response";
            result.setBlock(groupStatement.getBlocks().get(0));
            return result;
        }


        /**
         * Starts a "group" with a specific planner. Agents within this group will ne called according to a strategy
         * defined by the planner. The default output name for the group's response is "response".
         *
         * @param plannerSupplier A supplier for the {@link Planner} instance to be used by the group.
         * @return A new builder instance representing the group. Call {@code end()} to return to the parent builder.
         */
        public AgentWorkflowBuilder<T> doAsPlannerGroup(Supplier<Planner> plannerSupplier) {
            return doAsPlannerGroup(plannerSupplier, null);
        }

        /**
         * Starts a "group" with a specific planner. Agents within this group will ne called according to a strategy
         * defined by the planner. The default output name for the group's response is "response".
         *
         * @param plannerSupplier A supplier for the {@link Planner} instance to be used by the group.
         * @param outputName      The output name for the group's response.
         * @return A new builder instance representing the group. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> doAsPlannerGroup(Supplier<Planner> plannerSupplier, String outputName) {
            Objects.requireNonNull(plannerSupplier, "Planner supplier can't be null");

            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            PlannerGroupStatement groupStatement = new PlannerGroupStatement(result, plannerSupplier);
            this.addExpression(groupStatement);
            result.outputName = outputName != null ? outputName : "response";
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
         * repeatedly until the condition is met or the maximum iterations are reached.  It is not possible to extract a
         * condition expression from a lambda function so to see conditions in documentation or diagrams use the
         * {@code condition(...)} method to associate a predicate with a condition expression.
         *
         * @param maxIterations The maximum number of times to repeat the block.
         * @param condition     The condition that, when true, will cause the loop to exit.
         * @return A new builder instance representing the repeat block. Call {@code end()} to return to the parent
         * builder.
         */
        public AgentWorkflowBuilder<T> repeat(int maxIterations, Predicate<AgenticScope> condition) {
            AgentWorkflowBuilder<T> result = new AgentWorkflowBuilder<>(this);
            RepeatStatement repeatStatement = new RepeatStatement(result, maxIterations, condition.negate(),
                    getDescription(condition));
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
                    .outputKey(outputName != null && !outputName.isEmpty() ? outputName : getOutputName(agentClass));
            if (outputComposer != null)
                builder.output(outputComposer);

            return proxy(builder.build());
        }

        /**
         * Generates a JSON representation of the workflow for documentation and illustration purposes.
         *
         * @return A pretty-printed JSON string representing the workflow structure.
         */
        public String toJson() {
            if (parentBuilder != null || agentClass == null)
                throw new IllegalStateException("toJson() can only be called on the root builder after the workflow is defined.");

            List<Map<String, Object>> workflowData = this.block.toJson();
            try {
                return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(workflowData);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * Generates a summary of the workflow using an AI agent. First, it obtains a Json representation of a workflow
         * using {@code toJson()} method then it builds an {@code WorkflowSummaryProvider} AI agent, and finally it uses
         * its {@code getSummary(...)} to get a summary.
         * <p>
         * Note: a properly configured {@code chatModel} must be specified for a workflow, and it may require local
         * network or the Internet connection, active subscription and/or available credits.
         *
         * @return A string containing the AI-generated summary of the workflow.
         */
        public String generateAISummary() {
            Objects.requireNonNull(getChatModel());
            String json = toJson();

            WorkflowSummaryProvider summaryProvider = AgenticServices.agentBuilder(WorkflowSummaryProvider.class)
                    .chatModel(getChatModel())
                    .build();

            return summaryProvider.getSummary(json);
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

                        try {
                            Object invocationResult = isAgentMethod ? method.invoke(agent, args) : null;

                            if (isAgentMethod && workflowDebugger != null)
                                workflowDebugger.sessionStopped(invocationResult);

                            return invocationResult;
                        } catch (Throwable failure) {
                            //noinspection ConstantValue
                            if (isAgentMethod && workflowDebugger != null)
                                workflowDebugger.sessionFailed(failure);
                            throw failure;
                        }
                    });
        }

        /**
         * Retrieves the {@link ChatModel} for this workflow. If not explicitly set for this builder, it delegates to
         * the parent builder.
         *
         * @return The {@link ChatModel} to be used by agents.
         */
        public ChatModel getChatModel() {
            return chatModel == null && parentBuilder != null ? parentBuilder.getChatModel() : chatModel;
        }

        ChatMemory getChatMemory() {
            return chatMemory == null && parentBuilder != null ? parentBuilder.getChatMemory() : chatMemory;
        }

        String getOutputName() {
            return outputName;
        }

        /**
         * Computes the effective output name for the main agent of this workflow. If an output name is explicitly set,
         * it is used; otherwise, the default output name derived from the agent class is returned.
         *
         * @return The computed output name.
         */
        public String getComputedOutputName() {
            return outputName != null ? outputName : getOutputName(agentClass);
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

        /**
         * Generates an HTML representation of the workflow diagram using Mermaid.js and writes it to the specified
         * file.
         *
         * @param filePath The path to the file where the HTML should be written.
         * @throws IOException If an I/O error occurs.
         */
        public void toHtmlFile(String filePath) throws IOException {
            Files.write(Paths.get(filePath), toHtml().getBytes());
        }

        /**
         * Generates an HTML representation of the workflow diagram using Mermaid.js
         *
         * @return A string containing the HTML page with a visual representation of the workflow executions.
         */
        public String toHtml() {
            return toHtml(new HtmlConfiguration());
        }

        String toHtml(HtmlConfiguration htmlConfiguration) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return expandTemplate(getHtmlTemplate(), Map.of(
                        FLOW_CHART_GRAPH_DEFINITION, toMermaid(htmlConfiguration.completedAgents(), htmlConfiguration.failedAgents(), htmlConfiguration.runningAgents()),
                        FLOW_CHART_NODE_DATA, objectMapper.writeValueAsString(htmlConfiguration.workflowResults()),
                        FLOW_CHART_NODE_NAMES, objectMapper.writeValueAsString(htmlConfiguration.agentNames()),
                        FLOW_CHART_COMPLETED_NODES, objectMapper.writeValueAsString(htmlConfiguration.completedAgents()),
                        FLOW_CHART_TITLE, htmlConfiguration.title() != null ? htmlConfiguration.title() : "Workflow Diagram",
                        FLOW_CHART_SUB_TITLE, htmlConfiguration.subTitle() != null ? htmlConfiguration.subTitle() : "",
                        FLOW_CHART_WORKFLOW_SUMMARY_MARKDOWN, generateAISummary(htmlConfiguration)
                ));
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }

        private String generateAISummary(HtmlConfiguration htmlConfiguration) {
            String markdownSummary = "";
            if (htmlConfiguration.isIncludeSummary()) {
                try {
                    markdownSummary = generateAISummary()
                            .replace("`", "\\`")
                            .replace("```", "\\`\\`\\`");
                } catch (Exception ex) {
                    logger.warn("Failed to generate AI summary", ex);
                }
            }
            return markdownSummary;
        }

        private String getHtmlTemplate() {
            String htmlTemplate;
            final String errorMessage = "Could not load 'flow-chart.html' resource";
            try (InputStream is = getClass().getResourceAsStream("flow-chart.html")) {
                if (is == null)
                    throw new IllegalStateException(errorMessage);
                htmlTemplate = new String(is.readAllBytes());
            } catch (IOException e) {
                throw new IllegalStateException(errorMessage, e);
            }
            return htmlTemplate;
        }

        private String toMermaid(List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            StringBuilder mermaid = new StringBuilder();
            mermaid.append("graph TD\n");
            AtomicInteger counter = new AtomicInteger(0);
            String startId = FLOW_CHART_NODE_START;
            mermaid.append(String.format("    %s([\"Start\"])\n", startId));
            mermaid.append(String.format("    style %s stroke-width:%spx\n",
                    startId,
                    completedNodes.contains(startId) ? 3 : 1));
            mermaid.append(mermaidInspectorLink(startId));

            String lastId = this.block.toMermaid(mermaid, counter, startId, null, completedNodes, failedNodes, runningNodes);

            String endId = FLOW_CHART_NODE_END;
            mermaid.append(String.format("    %s([\"End\"])\n", endId));
            mermaid.append(String.format("    style %s stroke-width:%spx%s\n",
                    endId,
                    completedNodes.contains(endId) ? 3 : 1,
                    failedNodes.isEmpty() ? "" : ",stroke:#ff0000"));
            mermaid.append(mermaidInspectorLink(endId));
            mermaid.append(String.format("    %s --> %s\n", lastId, endId));

            return mermaid.toString();
        }

        /**
         * An AI agent interface for generating a summary of a workflow based on its JSON representation.
         */
        public interface WorkflowSummaryProvider {

            /**
             * Generates a summary of the workflow based on its JSON representation.
             *
             * @param jsonRepresentation The JSON string representing the workflow.
             * @return A string containing the summary of the workflow.
             */
            @UserMessage("""
                         Prepare a summary for a workflow according to its JSON representation.
                         Keep it readable and user friendly.
                         Don't include anything is not related to the workflow (offer to continue conversation etc.).
                         Don't mention:
                         - It's generated based on JSON
                         - UID's
                         The JSON representation is: '{{jsonRepresentation}}'.
                         """)
            @Agent(value = "prepares summary for a workflow", outputKey = "summary")
            public String getSummary(String jsonRepresentation);

        }

        public record HtmlConfiguration(String title, String subTitle,
                                        boolean isIncludeSummary, Map<String, Object> workflowResults,
                                        List<String> completedAgents,
                                        Set<String> failedAgents,
                                        Set<String> runningAgents,
                                        Map<String, String> agentNames) {


            public HtmlConfiguration(String aTitle, String aSubTitle) {
                this(aTitle, aSubTitle, false, Map.of(), List.of(), Set.of(), Set.of(), Map.of());
            }

            public HtmlConfiguration() {
                this(null, null);
            }
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

        public List<Map<String, Object>> toJson() {
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < expressions.size(); i++) {
                Expression expr = expressions.get(i);

                if (expr instanceof IfThenStatement ifStmt && !(expr instanceof ElseIfStatement)) {
                    Expression nextExpr = (i + 1 < expressions.size()) ? expressions.get(i + 1) : null;

                    if (nextExpr instanceof ElseIfStatement elseStmt) {
                        Map<String, Object> ifJson = ifStmt.toJson();
                        ifJson.put("elseBlock", elseStmt.getBlocks().get(0).toJson());
                        result.add(ifJson);
                        i++; // Skip next expression (the else part)
                        continue;
                    }
                }

                Map<String, Object> exprJson = expr.toJson();
                if (exprJson != null) {
                    result.add(exprJson);
                }
            }
            return result;
        }

        public String toMermaid(StringBuilder mermaid, AtomicInteger counter,
                                String entryNodeId, String edgeLabel,
                                List<String> completedNodes,
                                Set<String> failedNodes, Set<String> runningNodes) {
            String currentNodeId = entryNodeId;
            boolean first = true;
            for (int i = 0; i < expressions.size(); i++) {
                Expression expr = expressions.get(i);
                String currentEdgeLabel = first ? edgeLabel : null;

                if (expr instanceof IfThenStatement ifStmt && !(expr instanceof ElseIfStatement)) {
                    Expression nextExpr = (i + 1 < expressions.size()) ? expressions.get(i + 1) : null;

                    if (nextExpr instanceof ElseIfStatement elseStmt) {
                        String ifNodeId = ifStmt.getId();
                        mermaid.append(String.format("    %s{{\"If (%s)\"}}\n", ifNodeId, ifStmt.conditionExpression != null ? ifStmt.conditionExpression : "..."));

                        String edge = currentEdgeLabel != null && !currentEdgeLabel.isEmpty() ?
                                String.format("    %s-- %s -->%s\n", currentNodeId, currentEdgeLabel, ifNodeId) :
                                String.format("    %s --> %s\n", currentNodeId, ifNodeId);
                        mermaid.append(edge);

                        String endIfNodeId = "node" + counter.getAndIncrement();
                        mermaid.append(String.format("    %s((\"end if\"))\n", endIfNodeId));

                        // then branch
                        String thenExitNodeId = ifStmt.getBlocks().get(0).toMermaid(mermaid, counter, ifNodeId, "then", completedNodes, failedNodes, runningNodes);
                        mermaid.append(String.format("    %s --> %s\n", thenExitNodeId, endIfNodeId));

                        // else branch
                        String elseExitNodeId = elseStmt.getBlocks().get(0).toMermaid(mermaid, counter, ifNodeId, "else", completedNodes, failedNodes, runningNodes);
                        mermaid.append(String.format("    %s --> %s\n", elseExitNodeId, endIfNodeId));

                        currentNodeId = endIfNodeId;
                        i++; // Skip next expression
                        first = false;
                        continue;
                    }
                }

                currentNodeId = expr.toMermaid(mermaid, counter, currentNodeId, currentEdgeLabel, completedNodes, failedNodes, runningNodes);
                first = false;
            }
            return currentNodeId;
        }
    }

    static abstract class Statement implements Expression {
        final AgentWorkflowBuilder<?> agentWorkflowBuilder;
        private final String id = UUID.randomUUID().toString();
        private final List<Block> blocks;

        public Statement(AgentWorkflowBuilder<?> agentWorkflowBuilder) {
            this.agentWorkflowBuilder = agentWorkflowBuilder;
            this.blocks = new ArrayList<>();
            this.blocks.add(new Block());
        }

        public String getId() {
            return id;
        }

        public List<Block> getBlocks() {
            return Collections.unmodifiableList(blocks);
        }

        @Override
        public Object createAgent() {
            return null;
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("type", "unknown");
            json.put("uid", getId());
            List<Map<String, Object>> blockJson = getBlocks().get(0).toJson();
            if (!blockJson.isEmpty()) json.put("block", blockJson);
            return json;
        }
    }

    static class RepeatStatement extends Statement {
        private final int maxIterations;
        private final Predicate<AgenticScope> condition;
        private final String conditionExpression;

        public RepeatStatement(AgentWorkflowBuilder<?> builder, int maxIterations, Predicate<AgenticScope> condition,
                               String conditionExpression) {
            super(builder);

            if (maxIterations < 1 || maxIterations > 100)
                throw new IllegalArgumentException("Max iterations must be between 1 and 100");
            Objects.requireNonNull(condition, "Condition can't be null");

            this.maxIterations = maxIterations;
            this.condition = condition;
            this.conditionExpression = conditionExpression;
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

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put("type", "repeat");
            json.put("maxIterations", maxIterations);
            if (conditionExpression != null) {
                json.put("condition", conditionExpression);
            }
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String repeatNodeId = getId();
            mermaid.append(String.format("    %s{{\"Repeat (%s, max: %d)\"}}\n",
                    repeatNodeId,
                    conditionExpression != null ? conditionExpression : "...",
                    maxIterations));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, repeatNodeId) :
                    String.format("    %s --> %s\n", entryNodeId, repeatNodeId);
            mermaid.append(edge);

            String loopBodyExitId = getBlocks().get(0).toMermaid(mermaid, counter, repeatNodeId, null, completedNodes, failedNodes, runningNodes);
            mermaid.append(String.format("    %s -- loop --> %s\n", loopBodyExitId, repeatNodeId));

            String exitNodeId = "node" + counter.getAndIncrement();
            mermaid.append(String.format("    %s((\"end repeat\"))\n", exitNodeId));
            mermaid.append(String.format("    %s -- exit --> %s\n", repeatNodeId, exitNodeId));

            return exitNodeId;
        }
    }

    static class IfThenStatement extends Statement {
        private final Predicate<AgenticScope> condition;
        private final String conditionExpression;

        public IfThenStatement(AgentWorkflowBuilder<?> builder, Predicate<AgenticScope> condition) {
            this(builder, condition, null);
        }

        public IfThenStatement(AgentWorkflowBuilder<?> builder, Predicate<AgenticScope> condition, String conditionExpression) {
            super(builder);

            Objects.requireNonNull(condition, "Condition can't be null");

            this.condition = condition;
            this.conditionExpression = conditionExpression;
        }

        public String getConditionExpression() {
            return conditionExpression;
        }

        @Override
        public Object createAgent() {
            Object[] subAgents = getBlocks().get(0).createAgents().toArray();
            return AgenticServices.conditionalBuilder()
                    .subAgents(condition, subAgents)
                    .build();
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put(JSON_KEY_TYPE, JSON_TYPE_IF_THEN);
            if (conditionExpression != null) {
                json.put(JSON_KEY_CONDITION, conditionExpression);
            }
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String ifNodeId = getId();
            mermaid.append(String.format("    %s{{\"If (%s)\"}}\n", ifNodeId, getConditionExpression() != null ? getConditionExpression() : "..."));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, ifNodeId) :
                    String.format("    %s --> %s\n", entryNodeId, ifNodeId);
            mermaid.append(edge);

            String endIfNodeId = "node" + counter.getAndIncrement();
            mermaid.append(String.format("    %s((\"end if\"))\n", endIfNodeId));

            // then branch
            String thenExitNodeId = getBlocks().get(0).toMermaid(mermaid, counter, ifNodeId, "then", completedNodes, failedNodes, runningNodes);
            mermaid.append(String.format("    %s --> %s\n", thenExitNodeId, endIfNodeId));

            // else branch
            mermaid.append(String.format("    %s -- else --> %s\n", ifNodeId, endIfNodeId));

            return endIfNodeId;
        }
    }

    static class ElseIfStatement extends IfThenStatement {
        public ElseIfStatement(AgentWorkflowBuilder<?> builder, Predicate<AgenticScope> condition) {
            super(builder, condition);
        }
    }

    static class DoWhenStatement extends Statement {
        private final String whenExpression;
        private final Function<AgenticScope, Object> function;

        public DoWhenStatement(AgentWorkflowBuilder<?> builder, Function<AgenticScope, Object> function, String whenExpression) {
            super(builder);

            Objects.requireNonNull(function, "Function can't be null");

            this.function = function;
            this.whenExpression = whenExpression;
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

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put("type", "doWhen");
            if (whenExpression != null) {
                json.put(JSON_KEY_EXPRESSION, whenExpression);
            }
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String switchNodeId = getId();
            mermaid.append(String.format("    %s{{\"doWhen (%s)\"}}\n",
                    switchNodeId,
                    whenExpression != null && !whenExpression.isEmpty() ? whenExpression : "..."));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, switchNodeId) :
                    String.format("    %s --> %s\n", entryNodeId, switchNodeId);
            mermaid.append(edge);

            String endSwitchNodeId = "node" + counter.getAndIncrement();
            mermaid.append(String.format("    %s((\"end\"))\n", endSwitchNodeId));

            for (Expression expr : getBlocks().get(0).getExpressions()) {
                String matchExitId = expr.toMermaid(mermaid, counter, switchNodeId, null, completedNodes, failedNodes, runningNodes);
                mermaid.append(String.format("    %s --> %s\n", matchExitId, endSwitchNodeId));
            }

            return endSwitchNodeId;
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
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put("type", "match");
            Object val = getValue();
            if (val != null) {
                json.put(JSON_KEY_VALUE, val.toString());
            }
            return json;
        }

        @Override
        public Object createAgent() {
            return AgenticServices.sequenceBuilder()
                    .subAgents(getBlocks().get(0).createAgents().toArray())
                    .build();
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            Object val = getValue();
            String label = "match: " + (val != null ? val.toString() : "supplier");
            return getBlocks().get(0).toMermaid(mermaid, counter, entryNodeId, label, completedNodes, failedNodes, runningNodes);
        }
    }

    static class GroupStatement extends Statement {
        public GroupStatement(AgentWorkflowBuilder<?> builder) {
            super(builder);
        }

        @Override
        public Object createAgent() {
            return AgenticServices.supervisorBuilder(SupervisorAgent.class)
                    .outputKey(agentWorkflowBuilder.outputName)
                    .chatModel(agentWorkflowBuilder.getChatModel())
                    .subAgents(getBlocks().get(0).createAgents().toArray())
                    .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                    .build();
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put(JSON_KEY_TYPE, JSON_TYPE_GROUP);
            json.put(JSON_KEY_DESCRIPTION, "A supervised group provided with a set of subagents that can autonomously generate a plan, deciding which agent to invoke next or if the assigned task has been completed.");
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String groupSubgraphId = "subgraph_group_" + counter.getAndIncrement();
            mermaid.append(String.format("    subgraph %s [Group]\n", groupSubgraphId));

            String groupEntryId = "node" + counter.getAndIncrement();
            mermaid.append(String.format("        %s(\"start group\")\n", groupEntryId));

            Block groupBlock = getBlocks().get(0);
            List<String> agentNodeIds = new ArrayList<>();
            List<AgentExpression> agentExpressions = new ArrayList<>();

            for (Expression expr : groupBlock.getExpressions()) {
                if (expr instanceof AgentExpression) {
                    agentExpressions.add((AgentExpression) expr);
                }
            }

            for (AgentExpression agentExpr : agentExpressions) {
                String nodeId = agentExpr.getId();
                String agentName = agentExpr.getMermaidNodeLabel();
                mermaid.append(String.format("        %s[\"%s\"]\n", nodeId, agentName));
                mermaid.append(String.format("        style %s fill:%s\n", nodeId, agentExpr.getMermaidNodeColor()));
                agentNodeIds.add(nodeId);
            }

            String groupExitId = "node" + counter.getAndIncrement();
            mermaid.append(String.format("        %s(\"end group\")\n", groupExitId));

            for (String agentNodeId : agentNodeIds) {
                mermaid.append(String.format("        %s <--> %s <--> %s\n", groupEntryId, agentNodeId, groupExitId));
            }

            mermaid.append("    end\n");

            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, groupEntryId) :
                    String.format("    %s --> %s\n", entryNodeId, groupEntryId);
            mermaid.append(edge);

            return groupExitId;
        }
    }

    static class PlannerGroupStatement extends GroupStatement {
        private final Supplier<Planner> plannerSupplier;

        public PlannerGroupStatement(AgentWorkflowBuilder<?> builder, Supplier<Planner> plannerSupplier) {
            super(builder);
            this.plannerSupplier = plannerSupplier;
        }

        @Override
        public Object createAgent() {
            return AgenticServices.plannerBuilder()
                    .outputKey(agentWorkflowBuilder.outputName)
                    .subAgents(getBlocks().get(0).createAgents().toArray())
                    .planner(plannerSupplier)
                    .build();
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put(JSON_KEY_TYPE, JSON_TYPE_PLANNER_GROUP);
            String s = plannerSupplier.toString();
            if (s.contains("$Lambda"))
                s = "Unnamed";
            json.put(JSON_KEY_PLANNER, s);
            json.put(JSON_KEY_DESCRIPTION, "A group provided with a planner and a set of subagents that can generate a plan, deciding which agent to invoke next or if the assigned task has been completed.");
            return json;
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
                    .outputKey(agentWorkflowBuilder.getOutputName());

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

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put("type", "doParallel");
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String parallelNodeId = getId();
            mermaid.append(String.format("    %s{\"Parallel\"}\n", parallelNodeId));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, parallelNodeId) :
                    String.format("    %s --> %s\n", entryNodeId, parallelNodeId);
            mermaid.append(edge);

            String endParallelNodeId = "node" + counter.getAndIncrement();
            mermaid.append(String.format("    %s((\"end parallel\"))\n", endParallelNodeId));

            Block parallelBlock = getBlocks().get(0);
            for (Expression expr : parallelBlock.getExpressions()) {
                String branchExitId = expr.toMermaid(mermaid, counter, parallelNodeId, null, completedNodes, failedNodes, runningNodes);
                mermaid.append(String.format("    %s --> %s\n", branchExitId, endParallelNodeId));
            }

            return endParallelNodeId;
        }
    }

    public static class AgentExpression implements Expression {
        private final String id = UUID.randomUUID().toString();
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

        public String getId() {
            return id;
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
            WorkflowDebugger workflowDebugger = agentWorkflowBuilder.getWorkflowDebugger();

            if (result == null) {
                String outName = getOutputName();
                AgentBuilder<?> agentBuilder = createAgentBuilder(id, workflowDebugger)
                        .chatModel(agentWorkflowBuilder.getChatModel());
                if (outName != null && !outName.isEmpty())
                    agentBuilder.outputKey(outName);
                ChatMemory chatMemory = agentWorkflowBuilder.getChatMemory();
                if (chatMemory != null)
                    agentBuilder.chatMemoryProvider(memoryId -> chatMemory);

                WorkflowContextConfig workflowContextConfig = setupWorkflowDebugger(agentBuilder, workflowDebugger, outName);
                setupLogging(agentBuilder, agentWorkflowBuilder.isLogInput(), agentWorkflowBuilder.isLogOutput(), outName);

                invokeAnnotatedConfigurator(agentBuilder);

                if (configurator != null)
                    configurator.accept(agentBuilder);

                result = agentBuilder.build();

                if (workflowContextConfig != null) {
                    workflowContextConfig.input.setAgent(result);
                    workflowContextConfig.output.setAgent(result);
                }
            } else {
                if (result instanceof WorkflowDebuggerSupport workflowDebuggerSupport)
                    workflowDebuggerSupport.setWorkflowDebugger(workflowDebugger);
            }

            if (workflowDebugger != null)
                workflowDebugger.registerAgentMetadata(result, this);

            return result;
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put(JSON_KEY_TYPE, JSON_TYPE_AGENT);
            json.put(JSON_KEY_UID, getId());

            Class<?> clazz = (agentClass != null) ? agentClass : agent.getClass();
            json.put(JSON_KEY_AGENT_CLASS_NAME, clazz.getName());
            json.put(JSON_KEY_NAME, clazz.getSimpleName());

            for (Method method : clazz.getDeclaredMethods()) {
                Agent agentAnnotation = method.getAnnotation(Agent.class);
                if (agentAnnotation != null) {
                    if (!agentAnnotation.value().isBlank())
                        json.put(JSON_KEY_DESCRIPTION, agentAnnotation.value());
                    if (!agentAnnotation.outputKey().isBlank())
                        json.put(JSON_KEY_OUTPUT_NAME, agentAnnotation.outputKey());

                    json.put(JSON_KEY_OUTPUT_TYPE, method.getReturnType().getName());

                    UserMessage userAnn = method.getAnnotation(UserMessage.class);
                    if (userAnn != null) {
                        String userMessage = getUserMessageTemplate(clazz, userAnn.value(), userAnn.delimiter(), userAnn.fromResource());
                        if (userMessage != null && !userMessage.isEmpty())
                            json.put(JSON_KEY_USER_MESSAGE, userMessage);
                    }

                    SystemMessage sysAnn = method.getAnnotation(SystemMessage.class);
                    if (sysAnn != null) {
                        String systemMessage = getUserMessageTemplate(clazz, sysAnn.value(), sysAnn.delimiter(), sysAnn.fromResource());
                        if (systemMessage != null && !systemMessage.isEmpty())
                            json.put(JSON_KEY_SYSTEM_MESSAGE, systemMessage);
                    }

                    Parameter[] parameters = method.getParameters();
                    if (parameters.length > 0) {
                        List<Map<String, Object>> params = new ArrayList<>();
                        for (Parameter parameter : parameters) {
                            Map<String, Object> paramJson = new LinkedHashMap<>();
                            paramJson.put(JSON_KEY_NAME, parameter.getName());
                            paramJson.put(JSON_KEY_TYPE, parameter.getType().getName());

                            V vAnnotation = parameter.getAnnotation(V.class);
                            if (vAnnotation != null)
                                paramJson.put(JSON_KEY_NAME, vAnnotation.value());

                            PlaygroundParam playgroundParamAnnotation = parameter.getAnnotation(PlaygroundParam.class);
                            if (playgroundParamAnnotation != null) {
                                paramJson.put(JSON_KEY_LABEL, playgroundParamAnnotation.label());
                                paramJson.put(JSON_KEY_DESCRIPTION, playgroundParamAnnotation.description());
                                paramJson.put(JSON_KEY_EDITOR_TYPE, playgroundParamAnnotation.editorType());
                                paramJson.put(JSON_KEY_EDITOR_CHOICES, playgroundParamAnnotation.editorChoices());
                            }
                            params.add(paramJson);
                        }
                        json.put(JSON_KEY_PARAMETERS, params);
                    }

                    break;
                }
            }

            if (outputName != null) {
                json.put(JSON_KEY_OUTPUT_NAME, outputName);
            }

            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String nodeId = getId();
            String agentName = getMermaidNodeLabel();
            if (runningNodes.contains(nodeId))
                agentName = '▶' + " " + agentName;

            mermaid.append(String.format("    %s[\"%s\"]\n", nodeId, agentName));
            mermaid.append(AgentWorkflowBuilder.mermaidInspectorLink(nodeId));

            mermaid.append(String.format("    style %s fill:%s,stroke-width:%spx%s\n",
                    nodeId,
                    getMermaidNodeColor(),
                    completedNodes.contains(nodeId) ? 3 : 1,
                    !failedNodes.contains(nodeId) ? "" : ",stroke:#ff0000"));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, nodeId) :
                    String.format("    %s --> %s\n", entryNodeId, nodeId);
            mermaid.append(edge);
            return nodeId;
        }

        protected String getMermaidNodeLabel() {
            if (getAgentClass() != null)
                return getAgentName(getAgentClass());
            else
                return getAgentName(getAgent());
        }

        protected String getMermaidNodeColor() {
            return "#ffff99";
        }

        private WorkflowContextConfig setupWorkflowDebugger(AgentBuilder<?> agentBuilder, WorkflowDebugger workflowDebugger, String outputName) {
            if (workflowDebugger == null)
                return null;

            WorkflowContext.Input input = workflowDebugger.getWorkflowContext().input(agentClass);
            agentBuilder.inputGuardrails(workflowDebugger.createAlterInputGuardrail(agentClass), input);
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
        protected AgentBuilder<?> createAgentBuilder(String agentId, WorkflowDebugger aWorkflowDebugger) {
            return new AgentBuilder(agentClass, validateAgentClass(agentClass)) {
                private InputGuardrail[] inputGuardrailsLocal;
                private OutputGuardrail[] outputGuardrailsLocal;
                private Class[] inputGuardrailClassesLocal;
                private Class[] outputGuardrailClassesLocal;
                private final String id = agentId;
                private final WorkflowDebugger workflowDebugger = aWorkflowDebugger;

                @Override
                public AgentBuilder tools(Object... objectsWithTools) {
                    return super.toolProvider(getToolProvider(objectsWithTools));
                }

                private ToolProvider getToolProvider(Object[] tools) {
                    return new ToolProvider() {
                        @Override
                        public ToolProviderResult provideTools(ToolProviderRequest request) {
                            ToolProviderResult.Builder builder = ToolProviderResult.builder();
                            for (Object tool : tools) {
                                Arrays.stream(tool.getClass().getDeclaredMethods())
                                        .filter(method -> method.isAnnotationPresent(Tool.class))
                                        .forEach(method -> {
                                            BoundToolExecutor executor = new BoundToolExecutor(id, tool, method, workflowDebugger);
                                            builder.add(ToolSpecifications.toolSpecificationFrom(method), executor);
                                        });
                            }
                            return builder.build();
                        }
                    };
                }

                private InputGuardrail[] mergeInputGuardrails(InputGuardrail[] existing, InputGuardrail[] newGuardrails) {
                    return mergeGuardrails(existing, newGuardrails, InputGuardrail.class);
                }

                @Override
                public AgentBuilder<?> inputGuardrails(InputGuardrail... inputGuardrails) {
                    inputGuardrailsLocal = inputGuardrailsLocal == null ?
                            inputGuardrails : mergeInputGuardrails(inputGuardrailsLocal, inputGuardrails);

                    return super.inputGuardrails(inputGuardrailsLocal);
                }

                private OutputGuardrail[] mergeOutputGuardrails(OutputGuardrail[] existing, OutputGuardrail[] newGuardrails) {
                    return mergeGuardrails(existing, newGuardrails, OutputGuardrail.class);
                }

                @Override
                public AgentBuilder<?> outputGuardrails(OutputGuardrail... outputGuardrails) {
                    outputGuardrailsLocal = outputGuardrailsLocal == null ?
                            outputGuardrails : mergeOutputGuardrails(outputGuardrailsLocal, outputGuardrails);

                    return super.outputGuardrails(outputGuardrailsLocal);
                }

                private Class[] mergeInputGuardrailClasses(Class[] existing, Class[] newGuardrailClasses) {
                    return mergeGuardrailClasses(existing, newGuardrailClasses);
                }

                @Override
                public AgentBuilder<?> inputGuardrailClasses(Class... inputGuardrailClasses) {
                    inputGuardrailClassesLocal = inputGuardrailClassesLocal == null ?
                            inputGuardrailClasses : mergeInputGuardrailClasses(inputGuardrailClassesLocal, inputGuardrailClasses);

                    return super.inputGuardrailClasses(inputGuardrailClassesLocal);
                }

                private Class[] mergeOutputGuardrailClasses(Class[] existing, Class[] newGuardrailClasses) {
                    return mergeGuardrailClasses(existing, newGuardrailClasses);
                }

                @Override
                public AgentBuilder<?> outputGuardrailClasses(Class... outputGuardrailClasses) {
                    outputGuardrailClassesLocal = outputGuardrailClassesLocal == null ?
                            outputGuardrailClasses : mergeOutputGuardrailClasses(outputGuardrailClassesLocal, outputGuardrailClasses);
                    return super.outputGuardrailClasses(outputGuardrailClassesLocal);
                }

                private <G> G[] mergeGuardrails(G[] existing, G[] newGuardrails, Class<G> guardrailClass) {
                    List<G> mergedList = new ArrayList<>();
                    List<G> trailingGuardrails = new ArrayList<>();
                    List<G> leadingGuardrails = new ArrayList<>();

                    if (existing != null) {
                        for (G guardrail : existing) {
                            if (guardrail instanceof TrailingGuardrail) {
                                trailingGuardrails.add(guardrail);
                            } else if (guardrail instanceof LeadingGuardrail) {
                                leadingGuardrails.add(guardrail);
                            } else {
                                mergedList.add(guardrail);
                            }
                        }
                    }

                    if (newGuardrails != null) {
                        for (G guardrail : newGuardrails) {
                            if (guardrail instanceof TrailingGuardrail) {
                                trailingGuardrails.add(guardrail);
                            } else if (guardrail instanceof LeadingGuardrail) {
                                leadingGuardrails.add(guardrail);
                            } else {
                                mergedList.add(guardrail);
                            }
                        }
                    }
                    leadingGuardrails.addAll(mergedList);
                    leadingGuardrails.addAll(trailingGuardrails);
                    return leadingGuardrails.toArray((G[]) Array.newInstance(guardrailClass, 0));
                }

                private Class[] mergeGuardrailClasses(Class[] existing, Class[] newGuardrailClasses) {
                    List<Class> mergedList = new ArrayList<>();
                    List<Class> trailingGuardrailClasses = new ArrayList<>();
                    List<Class> leadingGuardrailClasses = new ArrayList<>();

                    if (existing != null) {
                        for (Class<?> guardrailClass : existing) {
                            if (TrailingGuardrail.class.isAssignableFrom(guardrailClass)) {
                                trailingGuardrailClasses.add(guardrailClass);
                            } else if (LeadingGuardrail.class.isAssignableFrom(guardrailClass)) {
                                leadingGuardrailClasses.add(guardrailClass);
                            } else {
                                mergedList.add(guardrailClass);
                            }
                        }
                    }

                    if (newGuardrailClasses != null) {
                        for (Class<?> guardrailClass : newGuardrailClasses) {
                            if (TrailingGuardrail.class.isAssignableFrom(guardrailClass)) {
                                trailingGuardrailClasses.add(guardrailClass);
                            } else if (LeadingGuardrail.class.isAssignableFrom(guardrailClass)) {
                                leadingGuardrailClasses.add(guardrailClass);
                            } else {
                                mergedList.add(guardrailClass);
                            }
                        }
                    }
                    leadingGuardrailClasses.addAll(mergedList);
                    leadingGuardrailClasses.addAll(trailingGuardrailClasses);
                    return leadingGuardrailClasses.toArray(new Class[0]);
                }
            };
        }

        public String getOutputName() {
            return outputName != null ? outputName : AgentWorkflowBuilder.getOutputName(agentClass);
        }

        record WorkflowContextConfig(WorkflowContext.Input input, WorkflowContext.Output output) {
        }
    }

    static class NonAIAgentExpression extends AgentExpression {
        public NonAIAgentExpression(AgentWorkflowBuilder<?> agentWorkflowBuilder, Object agent, String outputName, Consumer<AgentBuilder<?>> configurator) {
            super(agentWorkflowBuilder, agent, outputName, configurator);
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put(JSON_KEY_TYPE, JSON_TYPE_NON_AI_AGENT);
            return json;
        }

        @Override
        protected String getMermaidNodeColor() {
            return "#f5f5f5";
        }
    }

    static class SetStateAgentExpression extends NonAIAgentExpression {
        public SetStateAgentExpression(AgentWorkflowBuilder<?> agentWorkflowBuilder, Object agent) {
            super(agentWorkflowBuilder, agent, null, null);
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put(JSON_KEY_OUTPUT_NAME, String.join(", ", ((SetStateAgents.SetStateAgent) getAgent()).listStates()));
            return json;
        }

        @Override
        protected String getMermaidNodeLabel() {
            return ((SetStateAgents.SetStateAgent) getAgent()).getAgentName();
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
                    agentBuilder.outputKey(outName);
                result = agentBuilder.build();
            }

            return result;
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = super.toJson();
            json.put("type", "remoteAgent");
            json.put("url", url);
            if (getAgentClass() != null) {
                json.put("className", getAgentClass().getSimpleName());
            }
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String nodeId = getId();
            String remoteAgentName = getAgentClass() != null ? getAgentClass().getSimpleName() : "UntypedAgent";
            String agentName = String.format("Remote Agent: %s at %s", remoteAgentName, url);

            mermaid.append(String.format("    %s[\"%s\"]\n", nodeId, agentName));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, nodeId) :
                    String.format("    %s --> %s\n", entryNodeId, nodeId);
            mermaid.append(edge);
            return nodeId;
        }
    }

    static class BreakpointExpression implements Expression {
        private final String id = UUID.randomUUID().toString();
        private final BiFunction<Breakpoint, Map<String, Object>, Object> action;
        private final Predicate<Map<String, Object>> condition;
        private final AgentWorkflowBuilder<?> builder;

        public BreakpointExpression(AgentWorkflowBuilder<?> builder,
                                    BiFunction<Breakpoint, Map<String, Object>, Object> action,
                                    Predicate<Map<String, Object>> condition) {
            this.builder = builder;
            this.action = action;
            this.condition = condition;
        }

        public String getId() {
            return id;
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
            Object agent = breakpoint.createAgent(workflowDebugger);

            workflowDebugger.registerAgentMetadata(agent, this);

            return agent;
        }

        @Override
        public Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put(JSON_KEY_TYPE, JSON_TYPE_BREAKPOINT);
            json.put("uid", getId());
            return json;
        }

        @Override
        public String toMermaid(StringBuilder mermaid, AtomicInteger counter, String entryNodeId, String edgeLabel, List<String> completedNodes, Set<String> failedNodes, Set<String> runningNodes) {
            String nodeId = getId();
            mermaid.append(String.format("    %s@{ shape: dbl-circ, label: \" \" }\n", nodeId));
            mermaid.append(String.format("    style %s stroke:#f66\n", nodeId));
            String edge = edgeLabel != null && !edgeLabel.isEmpty() ?
                    String.format("    %s-- %s -->%s\n", entryNodeId, edgeLabel, nodeId) :
                    String.format("    %s --> %s\n", entryNodeId, nodeId);
            mermaid.append(edge);
            return nodeId;
        }
    }

    private static class BoundToolExecutor implements ToolExecutor {
        private final Method toolMethod;
        private final ToolExecutor defaultToolExecutor;
        private final String agentId;

        private final ToolExecutionListener toolExecutionListener;

        public BoundToolExecutor(String agentId, Object tool, Method toolMethod,
                                 ToolExecutionListener toolExecutionListener) {
            this.agentId = agentId;
            this.toolMethod = toolMethod;
            defaultToolExecutor = new DefaultToolExecutor(tool, toolMethod);
            this.toolExecutionListener = toolExecutionListener;
        }

        @Override
        public String execute(ToolExecutionRequest request, Object memoryId) {
            if (toolExecutionListener != null)
                toolExecutionListener.beforeExecuteTool(agentId, request);

            RuntimeException exception = null;
            String result = null;
            try {
                result = defaultToolExecutor.execute(request, memoryId);
            } catch (RuntimeException ex) {
                exception = ex;
            }

            if (toolExecutionListener != null)
                toolExecutionListener.afterExecuteTool(agentId, request, result, exception);

            if (exception != null)
                throw exception;

            return result;
        }
    }

    public static interface ToolExecutionListener {
        void beforeExecuteTool(String agentId, ToolExecutionRequest toolExecutionRequest);
        void afterExecuteTool(String agentId, ToolExecutionRequest toolExecutionRequest, String result, Throwable exception);
    }
}
