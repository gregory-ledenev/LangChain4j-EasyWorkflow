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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.EasyWorkflow.AgentWorkflowBuilder.HtmlConfiguration;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.gl.langchain4j.easyworkflow.BreakpointActions.log;
import static com.gl.langchain4j.easyworkflow.EasyWorkflow.AgentWorkflowBuilder.FLOW_CHART_NODE_END;
import static com.gl.langchain4j.easyworkflow.EasyWorkflow.AgentWorkflowBuilder.FLOW_CHART_NODE_START;
import static java.lang.System.out;

/**
 * A debugger for the EasyWorkflow framework, allowing users to set breakpoints and inspect the workflow's flow and
 * state.
 */
public class WorkflowDebugger implements WorkflowContext.StateChangeHandler, WorkflowContext.InputHandler {
    public static final String KEY_INPUT = "$input";
    public static final String KEY_OUTPUT = "$output";
    public static final String KEY_AGENT_CLASS = "$agentClass";
    public static final String KEY_AGENT_CLASS_SIMPLE_NAME = "$agentClass.simpleName";
    public static final String KEY_OUTPUT_NAME = "$outputName";

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDebugger.class);
    private final WorkflowContext workflowContext;
    private final List<Breakpoint> breakpoints = Collections.synchronizedList(new ArrayList<>());
    private final List<AgentInvocationTraceEntry> agentInvocationTraceEntries = Collections.synchronizedList(new ArrayList<>() {
        @Override
        public boolean add(AgentInvocationTraceEntry aAgentInvocationTraceEntry) {
            return super.add(aAgentInvocationTraceEntry);
        }
    });
    private final Map<String, Object> workflowInput = new HashMap<>();
    private final ConcurrentHashMap<Object, EasyWorkflow.Expression> agentMetadata = new ConcurrentHashMap<>();
    private boolean breakpointsEnabled = true;
    private AgenticScope agenticScope;
    private Object workflowResult;
    private boolean started = false;
    private EasyWorkflow.AgentWorkflowBuilder<?> agentWorkflowBuilder;
    private Throwable workflowFailure;

    /**
     * Constructs a new {@code WorkflowDebugger}. Initializes a new {@link WorkflowContext} and registers itself as the
     * input and output state change handler.
     */
    public WorkflowDebugger() {
        this.workflowContext = new WorkflowContext();
        this.workflowContext.setInputHandler(this);
        this.workflowContext.setOutputStateChangeHandler(this);
    }

    /**
     * Starts a console chat session with a WorkflowExpert.
     *
     * @param userMessage The initial message from the user.
     * @return The expert's response to the initial message.
     */
    public String consoleChat(String userMessage) {
        return WorkflowExpert.consoleChat(this, userMessage);
    }

    static <T, R> R simulateConsoleProgress(T request, Function<T, R> worker) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                out.print(".");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
        R result = worker.apply(request); // This is the blocking call
        thread.interrupt();

        return result;
    }

    /**
     * Starts a console chat session with an agent specified by the custom chat function.
     *
     * @param userMessage  The initial message from the user.
     * @param chatFunction A function that takes a user message and returns a chat response.
     * @return The last response from the chat function before exiting.
     */
    public static String consoleChat(String userMessage, Function<String, String> chatFunction) {
        String result = null;
        String request = userMessage;

        out.println("Console Chat (type your questions, or 'exit' to quit)\n");
        if (request != null && ! request.isEmpty())
            out.println("> " + request);

        try (Scanner scanner = new Scanner(System.in)) {
            //noinspection ConditionalBreakInInfiniteLoop
            while (true) {
                if (request != null && !request.isEmpty()) {
                    out.print("[thinking");
                    result = simulateConsoleProgress(request, chatFunction);
                    out.println("]");
                    if (result != null && !result.isEmpty())
                        out.println("Answer: " + result);
                }
                out.print("> ");

                request = scanner.nextLine();
                if (request.equalsIgnoreCase("exit"))
                    break;
            }
        }
        return result;
    }

    /**
     * Returns the logger instance for this class.
     *
     * @return The logger instance.
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Replaces newline characters in a given string with their escaped representation ("\\n").
     *
     * @param text The input string.
     * @return The string with newline characters replaced, or {@code null} if the input is {@code null}.
     */
    public static String replaceNewLineCharacters(String text) {
        return text != null ? text.replaceAll("\\n", "\\\\n") : null;
    }

    /**
     * Returns a map containing the input parameters of the workflow.
     *
     * @return A map where keys are parameter names and values are their corresponding objects.
     */
    public Map<String, Object> getWorkflowInput() {
        return workflowInput;
    }

    /**
     * Returns the result of the last workflow execution.
     *
     * @return The workflow result.
     */
    public Object getWorkflowResult() {
        return workflowResult;
    }

    /**
     * Returns the {@link WorkflowContext} associated with this debugger.
     *
     * @return the workflow context
     */
    public WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    /**
     * Called when a new workflow session is started.
     *
     * @param agentClass The class of the agent that initiated the session.
     * @param method     The method that was invoked on the agent.
     * @param args       The arguments passed to the invoked method.
     */
    public void sessionStarted(Class<?> agentClass, Method method, Object[] args) {
        reset();

        started = true;
        saveWorkflowInput(method, args);

        saveBreakpointsState();
        findAndExecuteBreakpoints(Breakpoint.Type.SESSION_STARTED, null, null, null);
    }

    private void saveWorkflowInput(Method method, Object[] args) {
        for (int i = 0; i < method.getParameters().length; i++) {
            if (method.getParameters()[i].isAnnotationPresent(V.class)) {
                V annotation = method.getParameters()[i].getAnnotation(V.class);
                if (annotation == null)
                    continue;
                workflowInput.put(annotation.value(), args[i]);
            }
        }
    }

    /**
     * Called when a workflow session is stopped.
     *
     * @param result result of the workflow
     */
    public void sessionStopped(Object result) {
        workflowResult = result;
        findAndExecuteBreakpoints(Breakpoint.Type.SESSION_STOPPED, null, null, null);
        resetBreakpoints();
        started = false;
    }

    /**
     * Called when a workflow session fails due to an exception.
     *
     * @param failure The {@link Throwable} that caused the workflow to fail.
     */
    public void sessionFailed(Throwable failure) {
        this.workflowFailure = failure;
        updateAgentInvocationTraceEntry(failure);
        findAndExecuteBreakpoints(Breakpoint.Type.SESSION_FAILED, null, null, null);
        resetBreakpoints();
        started = false;
    }

    /**
     * Returns the {@link Throwable} that caused the workflow to fail, if any.
     *
     * @return The {@link Throwable} representing the workflow failure, or {@code null} if the workflow completed successfully.
     */
    public Throwable getWorkflowFailure() {
        return workflowFailure;
    }

    /**
     * Checks if a workflow session is currently started.
     *
     * @return {@code true} if a session is started, {@code false} otherwise.
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Resets the debugger's internal state, clearing all recorded events.
     */
    public void reset() {
        workflowInput.clear();
        workflowResult = null;
        agentInvocationTraceEntries.clear();
    }

    /**
     * Checks if all breakpoints are currently enabled.
     *
     * @return {@code true} if all breakpoints are enabled, {@code false} otherwise.
     */
    public boolean isBreakpointsEnabled() {
        return breakpointsEnabled;
    }

    /**
     * Enables or disables all breakpoints in the debugger.
     *
     * @param allBreakpointsEnabled {@code true} to enable all breakpoints, {@code false} to disable them.
     */
    public void setBreakpointsEnabled(boolean allBreakpointsEnabled) {
        this.breakpointsEnabled = allBreakpointsEnabled;
    }

    void saveBreakpointsState() {
        synchronized (breakpoints) {
            for (Breakpoint breakpoint : breakpoints) {
                breakpoint.saveState();
            }
        }
    }

    void resetBreakpoints() {
        synchronized (breakpoints) {
            for (Breakpoint breakpoint : breakpoints) {
                breakpoint.reset();
            }
        }
    }

    protected void findAndExecuteBreakpoints(Breakpoint.Type type, Class<?> agentClass, String outputName, Object outputValue) {
        if (!breakpointsEnabled)
            return;

        List<Breakpoint> breakpointsCopy;
        synchronized (breakpoints) {
            breakpointsCopy = new ArrayList<>(breakpoints);
        }

        Map<String, Object> agenticScopeState = agenticScope != null ? new HashMap<>(agenticScope.state()) : new HashMap<>();
        if (outputValue != null) {
            agenticScopeState.put(outputName, outputValue);
            agenticScopeState.put(KEY_OUTPUT, outputValue);
        }

        for (Breakpoint breakpoint : breakpointsCopy) {
            if (breakpoint.matches(type, agentClass, outputName)) {
                if (breakpoint.isEnabled() && (breakpoint.getCondition() == null || breakpoint.getCondition().test(agenticScopeState))) {
                    if (agentClass != null) {
                        agenticScopeState.put(KEY_AGENT_CLASS, agentClass);
                        agenticScopeState.put(KEY_AGENT_CLASS_SIMPLE_NAME, agentClass.getSimpleName());
                    }
                    if (outputName != null)
                        agenticScopeState.put(KEY_OUTPUT_NAME, outputName);
                    breakpoint.executeAction(agenticScopeState);
                }
            }
        }
    }

    /**
     * Adds a breakpoint to the debugger.
     *
     * @param breakpoint the breakpoint to add
     */
    public void addBreakpoint(Breakpoint breakpoint) {
        this.breakpoints.add(breakpoint);
        breakpoint.setWorkflowDebugger(this);
    }

    /**
     * Removes a specific breakpoint from the debugger.
     *
     * @param breakpoint the breakpoint to remove
     */
    public void removeBreakpoint(Breakpoint breakpoint) {
        this.breakpoints.remove(breakpoint);
        breakpoint.setWorkflowDebugger(null);
    }

    /**
     * Clears all registered breakpoints from the debugger.
     */
    public void clearBreakpoints() {
        List<Breakpoint> breakpointsCopy;
        synchronized (breakpoints) {
            breakpointsCopy = new ArrayList<>(breakpoints);
        }

        this.breakpoints.clear();

        for (Breakpoint breakpoint : breakpointsCopy)
            breakpoint.setWorkflowDebugger(null);
    }

    void registerAgentMetadata(Object agent, EasyWorkflow.Expression metadata) {
        agentMetadata.put(agent, metadata);
    }

    /**
     * Handles state change events from the {@link WorkflowContext}. This method is called when an agent's output state
     * changes. It then checks for and executes any matching {@link Breakpoint}s of type
     * {@link Breakpoint.Type#AGENT_OUTPUT}.
     *
     * @param agent      the agent whose state changed
     * @param agentClass the class of the agent whose state changed
     * @param stateName  the name of the state that changed
     * @param stateValue the new value of the state
     */
    @Override
    public void stateChanged(Object agent, Class<?> agentClass, String stateName, Object stateValue) {
        updateAgentInvocationTraceEntry(agent, stateName, stateValue);
        findAndExecuteBreakpoints(Breakpoint.Type.AGENT_OUTPUT, agentClass, stateName, stateValue);
    }

    /**
     * Handles input received events from the {@link WorkflowContext}. This method is called when a {@link UserMessage}
     * is received by an agent. It then checks for and executes any matching {@link Breakpoint}s of type
     * {@link Breakpoint.Type#AGENT_INPUT}.
     *
     * @param agent       the agent that received the input
     * @param agentClass  the class of the agent that received the input
     * @param userMessage the user message received
     */
    @Override
    public void inputReceived(Object agent, Class<?> agentClass, UserMessage userMessage) {
        agenticScope.writeState(KEY_INPUT, userMessage);
        try {
            addAgentInvocationTraceEntry(agent, agentClass, userMessage);
            findAndExecuteBreakpoints(Breakpoint.Type.AGENT_INPUT, agentClass, null, null);
        } finally {
            agenticScope.writeState(KEY_INPUT, null);
        }
    }

    /**
     * Returns the {@link AgenticScope} associated with this debugger.
     *
     * @return the {@link AgenticScope} if the workflow is already started or just finished; {@code null} otherwise
     */
    public AgenticScope getAgenticScope() {
        return agenticScope;
    }

    /**
     * Returns a copy of the current state of the {@link AgenticScope}.
     *
     * @return A {@link Map} containing the state variables, or an empty map if the scope is not initialized.
     */
    public Map<String, Object> getAgenticScopeState() {
        return agenticScope != null ? new HashMap<>(agenticScope.state()) : new HashMap<>();
    }

    Object serviceAgent() {
        return new ServiceAgent();
    }

    private void addAgentInvocationTraceEntry(Object agent, Class<?> agentClass, Object input) {
        AgentInvocationTraceEntry traceEntry = new AgentInvocationTraceEntry(agent, agentClass, input);
        agentInvocationTraceEntries.add(traceEntry);
    }

    private void updateAgentInvocationTraceEntry(Throwable failure) {
        ArrayList<AgentInvocationTraceEntry> entries;
        synchronized (agentInvocationTraceEntries) {
            entries = new ArrayList<>(agentInvocationTraceEntries);
        }
        AgentInvocationTraceEntry traceEntry = null;

        for (int i = entries.size() - 1; i >= 0; i--) {
            if (traceEntry == null || entries.get(i).getLastAccessTime() > traceEntry.getLastAccessTime()) {
                traceEntry = entries.get(i);
            }
        }

        if (traceEntry != null)
            traceEntry.setFailure(failure);
    }

    private void updateAgentInvocationTraceEntry(Object agent, String outputName, Object output) {
        ArrayList<AgentInvocationTraceEntry> entries;
        synchronized (agentInvocationTraceEntries) {
            entries = new ArrayList<>(agentInvocationTraceEntries);
        }
        AgentInvocationTraceEntry traceEntry = null;

        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getAgent() == agent) { // == not equals, as we're comparing same objects and because proxies don't support equals()
                traceEntry = entries.get(i);
                if (traceEntry != null && traceEntry.getOutputName() == null && traceEntry.getOutput() == null)
                    break;
            }
        }

        if (traceEntry != null)
            traceEntry.addOutput(outputName, output);
        else
            logger.warn("Unable to update trace entry for agent {}", agent);
    }

    /**
     * Returns a list of all agent invocation trace entries recorded during the workflow execution.
     *
     * @return an unmodifiable list of {@link AgentInvocationTraceEntry} objects
     */
    public List<AgentInvocationTraceEntry> getAgentInvocationTraceEntries() {
        synchronized (agentInvocationTraceEntries) {
            return new ArrayList<>(agentInvocationTraceEntries);
        }
    }

    public EasyWorkflow.AgentWorkflowBuilder<?> getAgentWorkflowBuilder() {
        return agentWorkflowBuilder;
    }

    public void setAgentWorkflowBuilder(EasyWorkflow.AgentWorkflowBuilder<?> agentWorkflowBuilder) {
        this.agentWorkflowBuilder = agentWorkflowBuilder;
    }

    /**
     * Returns a string representation of the workflow debugger's state, optionally including detailed information about
     * workflow input, agent invocations, and the final result.
     *
     * @param isDetailed {@code true} to include detailed information, {@code false} for a brief representation.
     * @return a string representation of the object.
     */
    public String toString(boolean isDetailed) {
        if (!isDetailed)
            return super.toString();

        StringBuilder result = new StringBuilder();

        workflowInput.forEach((name, value) -> {
            result.append(MessageFormat.format("↓ IN > \"{0}\": {1}",
                            name,
                            replaceNewLineCharacters(value.toString())))
                    .append("\n");
        });

        int index = 1;
        result.append("-----------------------\n");
        List<AgentInvocationTraceEntry> entries = getAgentInvocationTraceEntries();
        for (WorkflowDebugger.AgentInvocationTraceEntry agentInvocationTraceEntry : entries) {
            result.append(agentInvocationTraceEntry.toString(index)).append("\n");
            result.append("-----------------------\n");
            index++;
        }

        if (isStarted())
            result.append("▶ RUNNING...\n");
        else if (getWorkflowFailure() == null)
            result.append("◼ RESULT: ").append(replaceNewLineCharacters(getWorkflowResult() != null ? getWorkflowResult().toString() : "N/A"));
        else
            result.append("✘ ERROR: ").append(replaceNewLineCharacters(getFailureCauseException(getWorkflowFailure()).toString()));

        result.append("\n");

        return result.toString();

    }

    /**
     * Recursively unwraps nested exceptions to find the root cause of a failure.
     * Specifically unwraps {@link java.lang.reflect.InvocationTargetException} and {@link AgentInvocationException}.
     * @param failure The initial {@link Throwable} to analyze.
     * @return The root cause {@link Throwable}, or the original failure if no further unwrapping is possible.
     */
    public static Throwable getFailureCauseException(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof java.lang.reflect.InvocationTargetException || cause instanceof AgentInvocationException)) {
            cause = cause.getCause();
        }
        return cause != null ? cause : failure;
    }

    private Object convertInValueToJson(ObjectMapper objectMapper, Object value) {
        if (value instanceof UserMessage userMessage) {
            return userMessage.hasSingleText() ? userMessage.singleText() : convertValueToJson(objectMapper, value);
        } else {
            return convertValueToJson(objectMapper, value);
        }
    }

    private Object convertValueToJson(ObjectMapper objectMapper, Object value) {
        if (value == null)
            return null;
        if (value instanceof String || value instanceof Number || value.getClass().isPrimitive() || value.getClass().isEnum())
            return value;

        try {
            if (value.getClass().isArray() || value instanceof Collection) {
                return objectMapper.convertValue(value, List.class);
            } else {
                return objectMapper.convertValue(value, Map.class);
            }
        } catch (Exception e) {
            logger.warn("Unable to convert a value to JSON. Defaulting to toString()", e);
            return value.toString();
        }
    }

    /**
     * Generates an HTML string with a visual representation of the workflow execution. It includes a flow chart diagram
     * and an inspector that allows checking results of agent invocations.
     *
     * @param title            The title for the HTML page.
     * @param subTitle         The subtitle for the HTML page.
     * @param isIncludeSummary Specifies whether to include an AI generated workflow summary
     * @return An HTML string representing the workflow execution.
     */
    public String toHtml(String title, String subTitle, boolean isIncludeSummary) {
        Map<String, Object> workflowResult = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(UserMessage.class, UserMessageMixIn.class);
        objectMapper.addMixIn(TextContent.class, TextContentMixIn.class);

        Set<String> runningAgents = new HashSet<>();
        List<String> completedAgents = new ArrayList<>();
        completedAgents.add(FLOW_CHART_NODE_START);

        Map<String, Object> in = new HashMap<>();
        workflowResult.put(FLOW_CHART_NODE_START, in);
        in.putAll(workflowInput);

        Set<String> failedAgents = new HashSet<>();
        Map<String, String> agentNames = new HashMap<>(); // uid -> name
        agentNames.put(FLOW_CHART_NODE_START, "Start");
        agentNames.put(FLOW_CHART_NODE_END, "End");

        List<AgentInvocationTraceEntry> entries = getAgentInvocationTraceEntries();
        for (WorkflowDebugger.AgentInvocationTraceEntry traceEntry : entries) {
            Map<String, Object> agentResult = new HashMap<>();
            String id = agentMetadata.get(traceEntry.getAgent()).getId();
            workflowResult.put(id, agentResult);
            agentResult.put("in", convertInValueToJson(objectMapper, traceEntry.input));
            if (traceEntry.output != null)
                agentResult.put("out", convertValueToJson(objectMapper, traceEntry.output));
            else
                runningAgents.add(id);
            if (traceEntry.getFailure() != null) {
                agentResult.put("failure", convertValueToJson(objectMapper, traceEntry.getFailure()));
                failedAgents.add(id);
            }
            agentNames.put(id, traceEntry.getAgentName());
            if (! completedAgents.contains(id))
                completedAgents.add(id);
        }

        if (getWorkflowFailure() != null) {
            Map<String, Object> result = new HashMap<>();
            workflowResult.put(FLOW_CHART_NODE_END, result);
            result.put("failure", convertValueToJson(objectMapper, getFailureCauseException(getWorkflowFailure())));
            completedAgents.add(FLOW_CHART_NODE_END);
        } else if (getWorkflowResult() != null) {
            Map<String, Object> result = new HashMap<>();
            workflowResult.put(FLOW_CHART_NODE_END, result);
            result.put("result", convertValueToJson(objectMapper, getWorkflowResult()));
            completedAgents.add(FLOW_CHART_NODE_END);
        }

        return agentWorkflowBuilder.toHtml(new HtmlConfiguration(
                title,
                subTitle,
                isIncludeSummary,
                workflowResult,
                completedAgents,
                failedAgents,
                runningAgents,
                agentNames));
    }

    /**
     * Generates an HTML string with a visual representation of the workflow execution. The title is "Workflow Diagram"
     * and the subtitle includes the current timestamp.
     *
     * @param isIncludeSummary Specifies whether to include an AI generated workflow summary
     * @return An HTML string representing the workflow execution.
     */
    public String toHtml(boolean isIncludeSummary) {
        return toHtml("Workflow Diagram",
                "A visual representation of the workflow execution (" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")",
                isIncludeSummary);
    }

    /**
     * Generates an HTML string with a visual representation of the workflow execution. The title is "Workflow Diagram"
     * and the subtitle includes the current timestamp.
     *
     * @return An HTML string representing the workflow execution.
     */
    public String toHtml() {
        return toHtml("Workflow Diagram",
                "A visual representation of the workflow execution (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")", false);
    }

    /**
     * Generates an HTML with a visual representation of the workflow execution. It contains a flow chart diagram and an
     * inspector that allows checking results of agent invocations.
     *
     * @param filePath The path to the file where the HTML should be written.
     * @throws IOException If an I/O error occurs.
     */
    public void toHtmlFile(String filePath) throws IOException {
        toHtmlFile(filePath, false);
    }

    /**
     * Generates an HTML with a visual representation of the workflow execution. It contains a flow chart diagram and an
     * inspector that allows checking results of agent invocations.
     *
     * @param filePath The path to the file where the HTML should be written.
     * @param isIncludeSummary Specifies whether to include an AI generated workflow summary.
     * @throws IOException If an I/O error occurs.
     */
    public void toHtmlFile(String filePath, boolean isIncludeSummary) throws IOException {
        Files.write(Paths.get(filePath), toHtml(isIncludeSummary).getBytes());
    }

    public abstract static class UserMessageMixIn {
        @JsonProperty("name")
        abstract String name();

        @JsonProperty("contents")
        abstract int contents();
    }

    public abstract static class TextContentMixIn {
        @JsonProperty("text")
        abstract String text();

        @JsonProperty("contents")
        abstract String type();
    }

    /**
     * Represents a breakpoint that can be set in the workflow.
     */
    public static class Breakpoint {
        private final BiConsumer<Breakpoint, Map<String, Object>> action;
        private final Breakpoint.Type type;
        private final Predicate<Map<String, Object>> condition;
        private volatile boolean enabled;
        private volatile boolean savedEnabled;
        private WorkflowDebugger workflowDebugger;

        /**
         * Constructs a new {@code Breakpoint}.
         *
         * @param action    The action to perform when the breakpoint is hit.
         * @param type      The type of the breakpoint.
         * @param condition The condition that must be met for the breakpoint to be triggered.
         * @param enabled   {@code true} if the breakpoint is enabled, {@code false} otherwise.
         */
        public Breakpoint(BiConsumer<Breakpoint, Map<String, Object>> action, Breakpoint.Type type, Predicate<Map<String, Object>> condition, boolean enabled) {

            this.action = action;
            this.type = type;
            this.condition = condition;
            this.enabled = enabled;
        }

        /**
         * Returns the {@link WorkflowDebugger} instance associated with this breakpoint.
         *
         * @return The associated {@link WorkflowDebugger}.
         */
        public WorkflowDebugger getWorkflowDebugger() {
            return workflowDebugger;
        }

        void setWorkflowDebugger(WorkflowDebugger aWorkflowDebugger) {
            workflowDebugger = aWorkflowDebugger;
        }

        /**
         * Creates a new {@link BreakpointBuilder} to construct a {@link AgentBreakpoint}.
         *
         * @param type   the type of breakpoint
         * @param action the action to perform when the breakpoint is hit
         * @return a new {@link BreakpointBuilder}
         */
        public static BreakpointBuilder builder(Breakpoint.Type type, BiConsumer<Breakpoint, Map<String, Object>> action) {
            return new BreakpointBuilder(type, action);
        }

        /**
         * Creates a new {@link BreakpointBuilder} to construct a {@link AgentBreakpoint} with a logging action.
         *
         * @param type     the type of breakpoint
         * @param template the template string for the log message, which can use workflow context variables using
         *                 {@code {{variable}}} notion
         * @return a new {@link BreakpointBuilder}
         */
        public static BreakpointBuilder builder(Breakpoint.Type type, String template) {
            return new BreakpointBuilder(type, log(template));
        }

        /**
         * Saves the current enabled state of the breakpoint. This is used internally to restore the state after a
         * session.
         */
        public void saveState() {
            savedEnabled = isEnabled();
        }

        /**
         * Resets the breakpoint's enabled state to its previously saved state.
         */
        public void reset() {
            setEnabled(savedEnabled);
        }

        /**
         * Returns the action associated with this breakpoint.
         *
         * @return the action to perform when the breakpoint is hit
         */
        public BiConsumer<Breakpoint, Map<String, Object>> getAction() {
            return action;
        }

        /**
         * Returns the type of this breakpoint.
         *
         * @return the breakpoint type
         */
        public Breakpoint.Type getType() {
            return type;
        }

        /**
         * Returns whether this breakpoint is currently enabled.
         *
         * @return {@code true} if the breakpoint is enabled, {@code false} otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether this breakpoint is enabled.
         *
         * @param aEnabled {@code true} to enable the breakpoint, {@code false} to disable it.
         */
        public void setEnabled(boolean aEnabled) {
            enabled = aEnabled;
        }

        /**
         * Returns the condition that must be met for the breakpoint to be triggered.
         *
         * @return the predicate condition
         */
        public Predicate<Map<String, Object>> getCondition() {
            return condition;
        }

        /**
         * Checks if this breakpoint matches the given event criteria.
         *
         * @param type       the type of event
         * @param agentClass the class of the agent involved in the event
         * @param outputName the name of the output state that changed (only relevant for AGENT_OUTPUT type)
         * @return {@code true} if the breakpoint matches, {@code false} otherwise
         */
        protected boolean matches(Type type, Class<?> agentClass, String outputName) {
            return getType() == type;
        }

        /**
         * Executes the action associated with this breakpoint.
         *
         * @param agenticScope The {@link AgenticScope} at the time the breakpoint is hit.
         */
        public void executeAction(Map<String, Object> agenticScope) {
            action.accept(this, agenticScope);
        }

        /**
         * Represents the type of event a breakpoint can trigger on.
         */
        public enum Type {
            /**
             * Breakpoint triggers when an agent receives an input.
             */
            AGENT_INPUT,
            /**
             * Breakpoint triggers when an agent produces output.
             */
            AGENT_OUTPUT,
            /**
             * Breakpoint triggers when a workflow session starts.
             */
            SESSION_STARTED,
            /**
             * Breakpoint triggers when a workflow session stops.
             */
            SESSION_STOPPED,
            /**
             * Breakpoint triggers when a workflow session fails.
             */
            SESSION_FAILED,
            /**
             * Breakpoint triggers when reached
             */
            LINE
        }
    }

    /**
     * Represents a breakpoint that can be inserted directly into the workflow code. When the workflow execution reaches
     * the point where this breakpoint is inserted, its action will be executed if its condition is not present or is
     * met.
     */
    public static class LineBreakpoint extends Breakpoint {
        /**
         * Constructs a new {@code LineBreakpoint}.
         *
         * @param action    the action to perform when the breakpoint is hit
         * @param condition the condition that must be met for the breakpoint to be triggered
         * @param enabled   {@code true} if the breakpoint is enabled, {@code false} otherwise
         */
        public LineBreakpoint(BiConsumer<Breakpoint, Map<String, Object>> action,
                              Predicate<Map<String, Object>> condition,
                              boolean enabled) {
            super(action, Type.LINE, condition, enabled);
        }

        /**
         * Creates an agent that can be invoked to trigger this line breakpoint.
         *
         * @param workflowDebugger the debugger instance to associate with the agent
         * @return an agent instance that, when invoked, will execute this breakpoint's action
         */
        public Object createAgent(WorkflowDebugger workflowDebugger) {
            return new LineBreakpointAgent(this, workflowDebugger);
        }
    }

    /**
     * An agent that, when invoked, executes the action of a {@link LineBreakpoint}. This class is used internally by
     * {@link LineBreakpoint#createAgent(WorkflowDebugger)}.
     */
    public static class LineBreakpointAgent {
        private final LineBreakpoint lineBreakpoint;
        private final WorkflowDebugger workflowDebugger;

        /**
         * Constructs a new {@code LineBreakpointAgent}.
         *
         * @param lineBreakpoint   the {@link LineBreakpoint} to associate with this agent
         * @param workflowDebugger the {@link WorkflowDebugger} instance to use for accessing the workflow context
         */
        public LineBreakpointAgent(LineBreakpoint lineBreakpoint, WorkflowDebugger workflowDebugger) {
            this.workflowDebugger = workflowDebugger;
            this.lineBreakpoint = lineBreakpoint;
        }

        @Agent
        public Object invoke(@V("agenticScope") AgenticScope agenticScope) {
            Map<String, Object> agenticScopeState = workflowDebugger.getAgenticScopeState();
            if (lineBreakpoint.getCondition() == null || lineBreakpoint.getCondition().test(agenticScopeState))
                lineBreakpoint.executeAction(agenticScopeState);
            return null;
        }
    }

    /**
     * Represents a breakpoint that triggers on events related to agents, such as input received or output produced.
     */
    public static class AgentBreakpoint extends Breakpoint {

        private final Class<?>[] agentClasses;
        private final String[] outputNames;
        private final String[] outputNamePatterns;

        /**
         * Constructs a new {@code AgentBreakpoint}.
         *
         * @param action       the action to perform when the breakpoint is hit
         * @param type         the type of breakpoint (e.g., AGENT_INPUT, AGENT_OUTPUT)
         * @param agentClasses an array of agent classes for which this breakpoint is active. If null or empty, it
         *                     applies to all agents.
         * @param outputNames  an array of output names or patterns for which this breakpoint is active. Wildcards are
         *                     supported.
         * @param condition    the condition that must be met for the breakpoint to be triggered
         * @param enabled      {@code true} if the breakpoint is enabled, {@code false} otherwise
         */
        public AgentBreakpoint(BiConsumer<Breakpoint, Map<String, Object>> action,
                               Type type,
                               Class<?>[] agentClasses,
                               String[] outputNames,
                               Predicate<Map<String, Object>> condition,
                               boolean enabled) {
            super(action, type, condition, enabled);

            this.agentClasses = agentClasses;
            this.outputNames = outputNames;
            this.outputNamePatterns = outputNames != null ?
                    Arrays.stream(outputNames).map(AgentBreakpoint::wildcardsToRegex).toArray(String[]::new) :
                    new String[]{wildcardsToRegex("*")};
        }

        /**
         * Converts a wildcard pattern string into a regular expression string. Supports '*' for any sequence of
         * characters and '?' for any single character.
         *
         * @param pattern The wildcard pattern string.
         * @return The equivalent regular expression string.
         */
        static String wildcardsToRegex(String pattern) {
            StringBuilder sb = new StringBuilder("^");
            for (char c : pattern.toCharArray()) {
                switch (c) {
                    case '*' -> sb.append(".*");
                    case '?' -> sb.append(".");
                    case '.', '(', ')', '[', ']', '{', '}', '|', '\\', '+', '$', '^' -> sb.append('\\').append(c);
                    default -> sb.append(c);
                }
            }
            sb.append("$");
            return sb.toString();
        }

        /**
         * Checks if this breakpoint matches the given event criteria.
         *
         * @param type       the type of event (e.g., AGENT_INPUT, AGENT_OUTPUT, SESSION_STARTED, SESSION_STOPPED)
         * @param agentClass the class of the agent involved in the event
         * @param outputName the name of the output state that changed (only relevant for OUTPUT type)
         * @return {@code true} if the breakpoint matches, {@code false} otherwise
         */
        protected boolean matches(Type type, Class<?> agentClass, String outputName) {
            if (this.getType() != type)
                return false;
            if (getType() == Type.SESSION_STARTED || getType() == Type.SESSION_STOPPED)
                return true;

            // Check if the agentClass matches. If agentClasses is null or empty, it matches all agents.
            // Otherwise, it must be present in the specified agentClasses.
            boolean agentClassMatch = (this.agentClasses == null || this.agentClasses.length == 0)
                    || Arrays.asList(this.agentClasses).contains(agentClass);

            boolean outputNameMatch = (type == Type.AGENT_INPUT)
                    || (this.outputNames == null || this.outputNames.length == 0)
                    || Arrays.stream(this.outputNamePatterns).anyMatch(pattern -> {
                return outputName != null && outputName.matches(pattern);
            });

            return agentClassMatch && outputNameMatch;
        }

        /**
         * Returns the array of agent classes for which this breakpoint is active.
         *
         * @return an array of agent classes
         */
        public Class<?>[] getAgentClasses() {
            return agentClasses;
        }

        /**
         * Returns the array of output names or patterns for which this breakpoint is active.
         *
         * @return an array of output names or patterns
         */
        public String[] getOutputNames() {
            return outputNames;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param obj the reference object with which to compare.
         * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (AgentBreakpoint) obj;
            return Objects.equals(this.getAction(), that.getAction()) &&
                    Objects.equals(this.getType(), that.getType()) &&
                    Arrays.equals(this.agentClasses, that.agentClasses) &&
                    Arrays.equals(this.outputNames, that.outputNames) &&
                    Objects.equals(this.getCondition(), that.getCondition()) &&
                    this.isEnabled() == that.isEnabled();
        }

        /**
         * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as
         * those provided by {@link HashMap}.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(getAction(), getType(), Arrays.hashCode(agentClasses), Arrays.hashCode(outputNames), getCondition(), isEnabled());
        }

        /**
         * Returns a string representation of the object.
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            return "Breakpoint[" +
                    "action=" + getAction() + ", " +
                    "type=" + getType() + ", " +
                    "agentClasses=" + Arrays.toString(agentClasses) + ", " +
                    "outputNames=" + Arrays.toString(outputNames) + ", " +
                    "condition=" + getCondition() + ", " +
                    "enabled=" + isEnabled() + ']';
        }
    }

    /**
     * A builder class for creating {@link AgentBreakpoint} instances.
     */
    public static class BreakpointBuilder {
        private final BiConsumer<Breakpoint, Map<String, Object>> action;
        private Breakpoint.Type type;
        private Class<?>[] agentClasses;
        private String[] outputNames = {"*"};
        private Predicate<Map<String, Object>> condition;
        private boolean enabled = true;

        /**
         * Constructs a new {@code BreakpointBuilder} with the specified action.
         *
         * @param action the action to perform when the breakpoint is hit
         */
        public BreakpointBuilder(Breakpoint.Type type, BiConsumer<Breakpoint, Map<String, Object>> action) {
            this.type = type;
            this.action = action;
        }

        /**
         * Sets the agent classes for which this breakpoint should be active. If not set, the breakpoint applies to all
         * agents.
         *
         * @param agentClasses an array of agent classes
         * @return this builder instance
         */
        public BreakpointBuilder agentClasses(Class<?>... agentClasses) {
            this.agentClasses = agentClasses;
            return this;
        }

        /**
         * Sets the output names for which this breakpoint should be active. This is only relevant for
         * {@link Breakpoint.Type#AGENT_OUTPUT} breakpoints. Wildcards ('*' for any sequence, '?' for any single
         * character) are supported. If not set, or set to {@code "*"}, the breakpoint applies to all outputs.
         *
         * @param outputNames an array of output names or patterns
         * @return this builder instance
         */
        public BreakpointBuilder outputNames(String... outputNames) {
            this.outputNames = outputNames;
            return this;
        }

        /**
         * Sets a condition that must be met for the breakpoint to be triggered. The condition is a {@link Predicate}
         * that takes the current {@link WorkflowContext}.
         *
         * @param condition the predicate condition
         * @return this builder instance
         */
        public BreakpointBuilder condition(Predicate<Map<String, Object>> condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Builds a new {@link Breakpoint} instance.
         *
         * @return a new {@link Breakpoint}
         */
        public Breakpoint build() {
            return switch (type) {
                case AGENT_INPUT, AGENT_OUTPUT ->
                        new AgentBreakpoint(action, type, this.agentClasses, this.outputNames, this.condition, this.enabled);
                case SESSION_STARTED, SESSION_STOPPED, SESSION_FAILED ->
                        new Breakpoint(action, type, this.condition, this.enabled);
                case LINE ->
                        new LineBreakpoint(action, this.condition, this.enabled);
            };
        }

        /**
         * Sets whether this breakpoint is enabled. Defaults to {@code true}.
         *
         * @param enabled {@code true} to enable the breakpoint, {@code false} to disable it
         * @return this builder instance
         */
        public BreakpointBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
    }

    /**
     * Represents a single entry in the agent invocation trace, capturing details about an agent's input and output.
     */
    public static class AgentInvocationTraceEntry {
        private final Object agent;
        private final Class<?> agentClass;
        private final Object input;
        private String outputName;
        private Object output;
        private Throwable failure;
        private long lastAccessTime;

        /**
         * Constructs a new {@code AgentInvocationTraceEntry}.
         *
         * @param agent      The agent object that was invoked.
         * @param agentClass The class of the agent that was invoked.
         * @param input      The input provided to the agent.
         */
        public AgentInvocationTraceEntry(Object agent, Class<?> agentClass, Object input) {
            Objects.requireNonNull(agent);
            Objects.requireNonNull(agentClass);

            this.agent = agent;
            this.agentClass = agentClass;
            this.input = input;

            updateLastAccessTime();
        }

        /**
         * Returns the last access time of this trace entry in milliseconds since the epoch.
         *
         * @return The last access time.
         */
        public long getLastAccessTime() {
            return lastAccessTime;
        }

        private void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        /**
         * Returns the name of the agent. If the agent implements {@link AgentNameProvider}, its provided name is used.
         * Otherwise, the name provided by {@code EasyWorkflow.getAgentName(agentClass)} is returned.
         *
         * @return The name of the agent.
         */
        public String getAgentName() {
            return agent instanceof AgentNameProvider agentNameProvider ?
                    agentNameProvider.getAgentName() :
                    EasyWorkflow.getAgentName(agentClass);
        }

        /**
         * Adds the output details to this trace entry.
         *
         * @param outputName The name of the output.
         * @param output     The output value.
         */
        public void addOutput(String outputName, Object output) {
            this.outputName = outputName;
            this.output = output;
            updateLastAccessTime();
        }

        /**
         * Returns the agent object.
         *
         * @return The agent object.
         */
        public Object getAgent() {
            return agent;
        }

        /**
         * Returns the class of the agent.
         *
         * @return The agent's class.
         */
        public Class<?> getAgentClass() {
            return agentClass;
        }

        /**
         * Returns the input provided to the agent.
         *
         * @return The agent's input.
         */
        public Object getInput() {
            return input;
        }

        /**
         * Returns the name of the output produced by the agent.
         *
         * @return The output name.
         */
        public String getOutputName() {
            return outputName;
        }

        /**
         * Returns the output value produced by the agent.
         *
         * @return The output value.
         */
        public Object getOutput() {
            return output;
        }

        /**
         * Returns the {@link Throwable} that caused the agent invocation to fail, if any.
         *
         * @return The {@link Throwable} representing the agent invocation failure, or {@code null} if the invocation completed successfully.
         */
        public Throwable getFailure() {
            return failure;
        }

        /**
         * Sets the {@link Throwable} that caused the agent invocation to fail.
         *
         * @param aFailure The {@link Throwable} representing the agent invocation failure.
         */
        public void setFailure(Throwable aFailure) {
            failure = aFailure;
        }

        @Override
        public boolean equals(Object aO) {
            if (aO == null || getClass() != aO.getClass()) return false;

            AgentInvocationTraceEntry that = (AgentInvocationTraceEntry) aO;
            return agent.equals(that.agent);
        }

        @Override
        public int hashCode() {
            return agent.hashCode();
        }

        @Override
        public String toString() {
            return toString(-1);
        }

        /**
         * Returns a string representation of the object, optionally including an index.
         *
         * @param index The index to include in the string representation, or -1 if no index should be included.
         * @return a string representation of the object.
         */
        public String toString(int index) {
            String result = MessageFormat.format("""
                                                       ↓ IN: {0}
                                                 {4}▷︎ {1}
                                                       ↓ OUT > "{2}": {3}""",
                    replaceNewLineCharacters(input != null ? input.toString() : ""),
                    agentClass.getSimpleName(),
                    outputName != null ? outputName : "N/A",
                    output != null ? replaceNewLineCharacters(output.toString()) : "N/A",
                    index > 0 ? MessageFormat.format("{0}. ", index) : "");

            if (failure != null)
                result = MessageFormat.format("{0}\n      ↯ FAIL: {1}",
                        result,
                        replaceNewLineCharacters(getFailureCauseException(failure).toString()));
            return result;
        }
    }

    /**
     * An internal service agent used to capture the {@link AgenticScope} when the workflow starts.
     */
    public class ServiceAgent {
        @Agent
        public Object invoke(@V("agenticScope") AgenticScope agenticScope) {
            if (WorkflowDebugger.this.agenticScope == null)
                WorkflowDebugger.this.agenticScope = agenticScope;
            return null;
        }
    }
}