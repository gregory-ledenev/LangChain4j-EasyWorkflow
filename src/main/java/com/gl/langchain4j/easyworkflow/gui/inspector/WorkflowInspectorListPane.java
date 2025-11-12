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

package com.gl.langchain4j.easyworkflow.gui.inspector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import com.gl.langchain4j.easyworkflow.gui.platform.Actions;
import com.gl.langchain4j.easyworkflow.gui.platform.AppPane;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;
import dev.langchain4j.data.message.UserMessage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.*;
import static com.gl.langchain4j.easyworkflow.gui.Icons.*;
import static javax.swing.BoxLayout.Y_AXIS;

/**
 * A panel that displays a hierarchical view of a workflow, allowing for inspection and debugging.
 */
public abstract class WorkflowInspectorListPane extends AppPane {
    public static final String NODE_AGENTIC_SCOPE = "| Agentic Scope |";
    public static final String NODE_PROGRESSION = "| Progression |";
    public static final String NODE_RESULT = "result";
    public static final String NODE_FAILURE = "failure";

    static final Color BACKGROUND_AGENT = new Color(255, 255, 153);
    static final Color BACKGROUND_AGENT_NONAI = new Color(245, 245, 245);
    static final Color BACKGROUND_STATEMENT = new Color(236, 236, 255);
    static final Color BACKGROUND_DARK_AGENT = new Color(85, 85, 20);
    static final Color BACKGROUND_DARK_AGENT_NONAI = new Color(70, 70, 70);
    static final Color BACKGROUND_DARK_STATEMENT = new Color(50, 50, 90);
    static final ObjectMapper OBJECT_MAPPER = WorkflowDebugger.createObjectMapper();
    static final String TYPE_START = "start";
    static final String TYPE_END = "end";


    protected final JList<WorkflowItem> list;
    protected final DefaultListModel<WorkflowItem> model;
    protected final List<WorkflowItem> listModel = new ArrayList<>();
    private final Map<String, WorkflowItem> workflowItemsByUID = new HashMap<>();
    private final Map<String, WorkflowItem> workflowItemsByType = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    protected boolean painLinkArrows;
    protected WorkflowDebugger workflowDebugger;
    protected AgentWorkflowBuilder<?> workflowBuilder;
    private WorkflowDebugger.Breakpoint breakpointSessionStarted;
    private WorkflowDebugger.Breakpoint breakpointSessionStopped;
    private WorkflowDebugger.Breakpoint breakpointSessionFailed;
    private WorkflowDebugger.Breakpoint breakpointAgentStarted;
    private WorkflowDebugger.Breakpoint breakpointAgentFinished;
    private WorkflowDebugger.AgentInvocationTraceEntryArchive traceEntryArchive;
    private String highlightedVariable;

    /**
     * Constructs a new WorkflowInspectorListPane.
     */
    public WorkflowInspectorListPane() {

        setMinimumSize(new Dimension(400, 300));
        model = new DefaultListModel<>();
        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                setPlaceHolderVisible(model.isEmpty());
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                setPlaceHolderVisible(model.isEmpty());
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                setPlaceHolderVisible(model.isEmpty());
            }
        });
        list = new JList<>(model) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        list.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new WorkflowItemRenderer());
        JScrollPane scrollPane = UISupport.createScrollPane(list, true, false, true, false, true);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setContent(scrollPane);

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "copy");
        list.getActionMap().put("copy", new Actions.BasicAction("Copy", null, e -> copy()));
    }

    private static String getSimpleClassName(String className) {
        int lastIndex = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$'));

        return lastIndex == -1 ? className : className.substring(lastIndex + 1);
    }

    private static String getAgentTitle(Map<String, Object> node) {
        return ((String) node.get(JSON_KEY_NAME)).replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    @Override
    public void setComponentPopupMenu(JPopupMenu popup) {
        super.setComponentPopupMenu(popup);
        list.setComponentPopupMenu(popup);
    }

    @Override
    public void requestFocus() {
        list.requestFocus();
    }

    public abstract void copy();

    protected boolean isPainLinkArrows() {
        return painLinkArrows;
    }

    /**
     * Returns the JList component displaying the workflow items.
     */
    public JList<WorkflowItem> getListView() {
        return list;
    }

    /**
     * Sets the workflow to be displayed in the inspector.
     *
     * @param builder The EasyWorkflow.AgentWorkflowBuilder representing the workflow.
     */
    public void setWorkflow(EasyWorkflow.AgentWorkflowBuilder<?> builder) {
        this.workflowBuilder = builder;
        model.clear();
        try {
            populateListModel(listModel, builder);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            model.addElement(new WorkflowItem(null, null, null, null, "Error parsing workflow", e.getMessage(), 0));
        }
    }

    @SuppressWarnings("unchecked")
    private void populateListModel(List<WorkflowItem> listModel, AgentWorkflowBuilder<?> builder) throws JsonProcessingException {
        String jsonString = builder.toJson();
        List<Map<String, Object>> workflowData = objectMapper.readValue(jsonString, List.class);
        listModel.add(new WorkflowItem(null, TYPE_START, null, ICON_PLAY, "Start",
                formatParametersForAgentClass(builder.getAgentClass()),
                0));
        populateListModel(listModel, workflowData, 0);
        String outputName = builder.getComputedOutputName();
        listModel.add(new WorkflowItem(null, TYPE_END, outputName, ICON_STOP, "End",
                "(%s %s)".formatted(EasyWorkflow.getAgentMethod(builder.getAgentClass()).getReturnType().getSimpleName(), outputName), 0));

        for (WorkflowItem workflowItem : listModel) {
            workflowItemsByUID.put(workflowItem.getUid(), workflowItem);
            workflowItemsByType.put(workflowItem.getType(), workflowItem);
        }
    }

    private static String formatParametersForAgentClass(Class<?> agentClass) {
        Method agentMethod = getAgentMethod(agentClass);
        Objects.requireNonNull(agentMethod);

        EasyWorkflow.getAgentMethodParameterNames(agentMethod);
        return "(%s)".formatted(Arrays.stream(agentMethod.getParameters())
                .map(parameter -> parameter.getType().getSimpleName() + " " + parameter.getName())
                .collect(Collectors.joining(", ")));
    }

    @SuppressWarnings("unchecked")
    private void populateListModel(List<WorkflowItem> listModel, List<Map<String, Object>> nodes, int indentation) {
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get(EasyWorkflow.JSON_KEY_TYPE);

            WorkflowItem element = createWorkflowItem(indentation, node, type, type);

            listModel.add(element);

            if (node.containsKey("block")) {
                populateListModel(listModel, (List<Map<String, Object>>) node.get("block"), indentation + 1);
            }
            if (node.containsKey("elseBlock")) {
                listModel.add(new WorkflowItem((String) node.get(JSON_KEY_UID), (String) node.get(JSON_KEY_TYPE), null, null, "else", "", indentation));
                populateListModel(listModel, (List<Map<String, Object>>) node.get("elseBlock"), indentation + 1);
            }
        }
    }

    private WorkflowItem createWorkflowItem(int indentation, Map<String, Object> node, String type, String title) {
        String subtitle = null;
        String iconKey = null;
        String outputName = null;
        switch (type) {
            case JSON_TYPE_AGENT:
            case JSON_TYPE_NON_AI_AGENT:
                title = getAgentTitle(node);
                subtitle = getAgentSubtitle(node);
                outputName = (String) node.get(JSON_KEY_OUTPUT_NAME);
                iconKey = ICON_EXPERT;
                break;
            case "setState":
                title = "Set State";
                subtitle = (String) node.get("details");
                break;
            case JSON_TYPE_IF_THEN:
                title = "if (%s)".formatted(node.getOrDefault(JSON_KEY_CONDITION, "..."));
                iconKey = ICON_SIGNPOST;
                break;
            case JSON_TYPE_REPEAT:
                title = "repeat (max: %s, until: %s)".formatted(
                        node.get(JSON_KEY_MAX_ITERATIONS),
                        node.getOrDefault(JSON_KEY_CONDITION, "..."));
                iconKey = ICON_REFRESH;
                break;
            case "doWhen":
                title = "when (%s)".formatted(node.getOrDefault(JSON_KEY_EXPRESSION, "..."));
                iconKey = ICON_SIGNPOST;
                break;
            case "match":
                title = "match (%s)".formatted(node.getOrDefault(JSON_KEY_VALUE, "..."));
                iconKey = ICON_TARGET;
                break;
            case "doParallel":
                title = "Do Parallel";
                iconKey = ICON_STACK;
                break;
            case "group":
                title = "Group";
                iconKey = ICON_BOX;
                break;
            case "breakpoint":
                title = "Breakpoint";
                iconKey = ICON_BREAKPOINT;
                break;
        }

        return new WorkflowItem((String) node.get(JSON_KEY_UID), (String) node.get(JSON_KEY_TYPE), outputName, iconKey, title, subtitle, indentation);
    }

    protected String[] getSubTitles(WorkflowItem workflowItem, int index) {
        return new String[]{workflowItem.getSubtitle()};
    }

    private String getAgentSubtitle(Map<String, Object> node) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) node.get(EasyWorkflow.JSON_KEY_PARAMETERS);
        String parametersStr = null;
        if (parameters != null && !parameters.isEmpty()) {
            parametersStr = parameters.stream()
                    .map(param -> {
                        String name = (String) param.get(JSON_KEY_NAME);
                        String type = param.get(JSON_KEY_TYPE).toString();
                        return getSimpleClassName(type) + " " + name;
                    })
                    .collect(Collectors.joining(", "));
        }
        return "(%s) → (%s %s)".formatted(parametersStr != null ? parametersStr : "",
                getSimpleClassName((String) node.getOrDefault(JSON_KEY_OUTPUT_TYPE, "N/A")),
                node.getOrDefault(JSON_KEY_OUTPUT_NAME, "response"));
    }

    /**
     * Retrieves data associated with the currently selected workflow item.
     *
     * @return A map containing the data of the selected item, or an empty map if no item is selected or data is
     * unavailable.
     */
    public Map<String, Object> getSelectedData() {
        Map<String, Object> result = new HashMap<>();

        WorkflowItem selectedValue = list.getSelectedValue();
        if (selectedValue != null) {
            switch (selectedValue.type) {
                case TYPE_START -> {
                    getWorkflowInput().forEach((key, value) -> result.put(key, convertValue(value)));
                }
                case TYPE_END -> {
                    if (convertValue(getWorkflowResult()) != null)
                        result.put(NODE_RESULT, convertValue(getWorkflowResult()));
                    else if (getWorkflowFailure() != null)
                        result.put(NODE_FAILURE, convertValue(WorkflowDebugger.getFailureCauseException(getWorkflowFailure())));

                    Map<String, Object> state = getAgenticScopeState();
                    if (!state.isEmpty())
                        result.put(NODE_AGENTIC_SCOPE, convertValue(state));

                    Map<String, Object> progression = getProgression();
                    if (!progression.isEmpty())
                        result.put(NODE_PROGRESSION, convertValue(progression));
                }
                case JSON_TYPE_AGENT, JSON_TYPE_NON_AI_AGENT, JSON_TYPE_BREAKPOINT -> {
                    List<WorkflowDebugger.AgentInvocationTraceEntry> entries = getTraceEntries(selectedValue);

                    Map<String, Object> passResult = result;
                    if (entries != null) {
                        for (WorkflowDebugger.AgentInvocationTraceEntry entry : entries) {
                            if (entries.size() > 1)
                                result.put("pass [%s]".formatted(entries.indexOf(entry)), passResult = new HashMap<>());
                            passResult.put("input", convertValue(entry.getInput()));

                            if (selectedValue.getOutputName() != null)
                                passResult.put("output (%s)".formatted(selectedValue.getOutputName()), convertValue(entry.getOutput()));
                            else
                                passResult.put("output", convertValue(entry.getOutput()));

                            if (entry.getFailure() != null) {
                                passResult.put("failure", convertValue(WorkflowDebugger.getFailureCauseException(entry.getFailure())));
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }

        return result;
    }

    private Map<String, Object> getProgression() {
        Map<String, Object> result = new HashMap<>();
        List<WorkflowDebugger.AgentInvocationTraceEntry> traceEntries = getTraceEntries();

        Map<String, List<Object>> progressionMap = new LinkedHashMap<>();
        for (WorkflowDebugger.AgentInvocationTraceEntry entry : traceEntries) {
            String outputName = entry.getOutputName();
            if (outputName == null)
                continue;
            Object output = entry.getOutput();

            progressionMap.computeIfAbsent(outputName, k -> new ArrayList<>())
                    .add(convertValue(output));
        }

        Object workflowResult = getWorkflowResult();
        if (getWorkflowFailure() == null && workflowResult != null) {
            progressionMap.computeIfAbsent(listModel.get(listModel.size() - 1).getOutputName(), k -> new ArrayList<>())
                    .add(convertValue(workflowResult));
        }

        progressionMap.forEach((outputName, values) -> result.put(outputName, values));


        return result;
    }

    protected List<WorkflowDebugger.AgentInvocationTraceEntry> getTraceEntries(WorkflowItem selectedValue) {
        return selectedValue.getTraceEntries(list.getSelectedIndex());
    }

    protected List<WorkflowDebugger.AgentInvocationTraceEntry> getTraceEntries() {
        return traceEntryArchive != null ?
                traceEntryArchive.agentInvocationTraceEntries() :
                workflowDebugger.getAgentInvocationTraceEntries();
    }

    protected Map<String, Object> getAgenticScopeState() {
        return traceEntryArchive == null ?
                workflowDebugger.getAgenticScope() != null ? workflowDebugger.getAgenticScope().state() : Collections.emptyMap() :
                traceEntryArchive.agenticScope();
    }

    protected Throwable getWorkflowFailure() {
        return traceEntryArchive == null ?
                workflowDebugger.getWorkflowFailure() :
                traceEntryArchive.workflowFailure();
    }

    protected Object getWorkflowResult() {
        return traceEntryArchive == null ?
                workflowDebugger.getWorkflowResult() :
                traceEntryArchive.workflowResult();
    }

    protected Map<String, Object> getWorkflowInput() {
        return traceEntryArchive == null ?
                workflowDebugger.getWorkflowInput() :
                traceEntryArchive.workflowInput();
    }

    public WorkflowDebugger.AgentInvocationTraceEntryArchive getTraceEntryArchive() {
        return traceEntryArchive;
    }

    public void setTraceEntryArchive(WorkflowDebugger.AgentInvocationTraceEntryArchive traceEntryArchive, CompletableFuture<Void> completion) {
        this.traceEntryArchive = traceEntryArchive;
        if (traceEntryArchive != null)
            processTraceEntries(traceEntryArchive.workflowInput(),
                    traceEntryArchive.workflowResult(),
                    traceEntryArchive.workflowFailure(),
                    traceEntryArchive.agentInvocationTraceEntries(),
                    traceEntryArchive.agenticScope(),
                    completion);
        else
            processTraceEntries(workflowDebugger.getWorkflowInput(),
                    workflowDebugger.getWorkflowResult(),
                    workflowDebugger.getWorkflowFailure(),
                    workflowDebugger.getAgentInvocationTraceEntries(),
                    workflowDebugger.getAgenticScope().state(),
                    completion);
    }

    private void processTraceEntries(Map<String, Object> workflowInput,
                                     Object workflowResult,
                                     Throwable workflowFailure,
                                     List<WorkflowDebugger.AgentInvocationTraceEntry> traceEntries,
                                     Map<String, Object> agenticScope,
                                     CompletableFuture<Void> completion) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            long delay = 100;
            long currentDelay = 0;

            scheduler.schedule(() -> workflowDebuggerSessionStarted(workflowInput), currentDelay, TimeUnit.MILLISECONDS);
            currentDelay += delay;

            for (WorkflowDebugger.AgentInvocationTraceEntry traceEntry : traceEntries) {
                Map<String, Object> states = Map.of(WorkflowDebugger.KEY_TRACE_ENTRY, traceEntry);

                scheduler.schedule(() -> workflowDebuggerAgentStarted(states), currentDelay, TimeUnit.MILLISECONDS);
                currentDelay += delay;

                scheduler.schedule(() -> workflowDebuggerAgentFinished(states), currentDelay, TimeUnit.MILLISECONDS);
                currentDelay += delay;
            }

            if (workflowFailure == null) {
                scheduler.schedule(() -> {
                    workflowDebuggerSessionStopped(agenticScope);
                    if (completion != null)
                        completion.complete(null);
                }, currentDelay, TimeUnit.MILLISECONDS);
            } else {
                scheduler.schedule(() -> {
                    workflowDebuggerSessionFailed(agenticScope);
                    if (completion != null)
                        completion.complete(null);
                }, currentDelay, TimeUnit.MILLISECONDS);
            }
        } finally {
            scheduler.shutdown();
        }
    }

    private Object convertValue(Object value) {
        if (value == null)
            return null;

        if (value.getClass().isPrimitive() || value instanceof String || value instanceof Number || value.getClass().isEnum())
            return value;
        else if (value.getClass().isArray() || value instanceof List)
            return OBJECT_MAPPER.convertValue(value, List.class);
        else
            return OBJECT_MAPPER.convertValue(value, Map.class);
    }

    private void setupWorkflowDebugger() {
        breakpointSessionStarted = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.SESSION_STARTED,
                        (aBreakpoint, states) -> {
                            workflowDebuggerSessionStarted(states);
                            return null;
                        })
                .build();
        workflowDebugger.addBreakpoint(breakpointSessionStarted);

        breakpointSessionStopped = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.SESSION_STOPPED,
                        (aBreakpoint, states) -> {
                            workflowDebuggerSessionStopped(states);
                            return null;
                        })
                .build();
        workflowDebugger.addBreakpoint(breakpointSessionStopped);

        breakpointSessionFailed = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.SESSION_FAILED,
                        (aBreakpoint, states) -> {
                            workflowDebuggerSessionFailed(states);
                            return null;
                        })
                .build();
        workflowDebugger.addBreakpoint(breakpointSessionFailed);

        breakpointAgentStarted = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.AGENT_INPUT,
                        (aBreakpoint, states) -> {
                            workflowDebuggerAgentStarted(states);
                            return null;
                        })
                .build();
        workflowDebugger.addBreakpoint(breakpointAgentStarted);

        breakpointAgentFinished = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.AGENT_OUTPUT,
                        (aBreakpoint, states) -> {
                            workflowDebuggerAgentFinished(states);
                            return null;
                        })
                .build();
        workflowDebugger.addBreakpoint(breakpointAgentFinished);
    }

    private void cleanupWorkflowDebugger() {
        workflowDebugger.removeBreakpoint(breakpointSessionStarted);
        workflowDebugger.removeBreakpoint(breakpointSessionStopped);
        workflowDebugger.removeBreakpoint(breakpointSessionFailed);
        workflowDebugger.removeBreakpoint(breakpointAgentStarted);
        workflowDebugger.removeBreakpoint(breakpointAgentFinished);
    }

    /**
     * Callback method invoked when a workflow session starts. Updates the UI to reflect the session start.
     *
     * @param states A map containing the current state of the workflow.
     */
    public void workflowDebuggerSessionStarted(Map<String, Object> states) {
        SwingUtilities.invokeLater(() -> {
            clearItems();
            findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Running));
            list.repaint();
            updateSelection();
        });
    }

    protected void updateSelection() {
        int selectedIndex = list.getSelectedIndex();
        list.clearSelection();
        if (selectedIndex == -1 && list.getModel().getSize() > 0)
            selectedIndex = 0;
        if (selectedIndex > -1)
            list.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }

    protected void clearItems() {
        for (WorkflowItem aWorkflowItem : listModel) {
            aWorkflowItem.clear();
        }
    }

    /**
     * Callback method invoked when a workflow session stops successfully. Updates the UI to reflect the session end.
     *
     * @param states A map containing the current state of the workflow.
     */
    public void workflowDebuggerSessionStopped(Map<String, Object> states) {
        SwingUtilities.invokeLater(() -> {
            findItemByType(TYPE_END).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
            findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
            list.repaint();
            updateSelection();
        });
    }

    /**
     * Callback method invoked when a workflow session fails. Updates the UI to reflect the session failure and marks
     * incomplete items as failed.
     *
     * @param states A map containing the current state of the workflow.
     */
    public void workflowDebuggerSessionFailed(Map<String, Object> states) {
        SwingUtilities.invokeLater(() -> {
            findItemByType(TYPE_END).ifPresent(item -> item.setState(WorkflowItem.State.Failed));
            findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
            markIncompleteItemsAsFailed();
            list.repaint();
            updateSelection();
        });
    }

    protected void markIncompleteItemsAsFailed() {
        for (WorkflowDebugger.AgentInvocationTraceEntry entry : workflowDebugger.getAgentInvocationTraceEntries()) {
            if (entry.getFailure() != null) {
                String uid = workflowDebugger.getAgentMetadata(entry.getAgent()).getId();
                findItemByUid(uid).ifPresent(item -> item.setState(WorkflowItem.State.Failed));
            }
        }
    }

    /**
     * Callback method invoked when an agent starts its execution. Updates the UI to mark the agent as running and
     * previous items as finished if they were skipped.
     *
     * @param states A map containing the current state of the workflow.
     */
    public void workflowDebuggerAgentStarted(Map<String, Object> states) {
        WorkflowDebugger.AgentInvocationTraceEntry traceEntry = (WorkflowDebugger.AgentInvocationTraceEntry) states.get(WorkflowDebugger.KEY_TRACE_ENTRY);
        String uid = workflowDebugger.getAgentMetadata(traceEntry.getAgent()).getId();
        SwingUtilities.invokeLater(() -> {
            findItemByUid(uid).ifPresent(currentItem -> {
                currentItem.setState(WorkflowItem.State.Running);
                currentItem.setPassCount(currentItem.getPassCount() + 1);
                currentItem.setTraceEntry(listModel.indexOf(currentItem),
                        traceEntry);
                markMissedItemsAsFinished(currentItem);
            });
            findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
            list.repaint();
            updateSelection();
        });
    }

    protected void markMissedItemsAsFinished(WorkflowItem currentItem) {
        Set<Integer> processedMatches = new HashSet<>();
        int index = model.indexOf(currentItem);
        for (int i = index - 1; i >= 0; i--) {
            WorkflowItem item = model.get(i);
            if (item.getState() == WorkflowItem.State.Unknown && item.getIndentation() <= currentItem.getIndentation()) {
                switch (item.type) {
                    case JSON_TYPE_IF_THEN, JSON_TYPE_REPEAT, JSON_TYPE_DO_WHEN,
                         JSON_TYPE_GROUP, JSON_TYPE_DO_PARALLEL:
                        item.setState(WorkflowItem.State.Finished);
                        break;
                    case JSON_TYPE_MATCH:
                        if (!processedMatches.contains(item.getIndentation())) {
                            processedMatches.add(item.getIndentation());
                            item.setState(WorkflowItem.State.Finished);
                        }
                        break;
                    default:
                }
            }
        }
    }

    /**
     * Callback method invoked when an agent finishes its execution. Updates the UI to mark the agent as finished.
     *
     * @param states A map containing the current state of the workflow.
     */
    public void workflowDebuggerAgentFinished(Map<String, Object> states) {
        WorkflowDebugger.AgentInvocationTraceEntry traceEntry = (WorkflowDebugger.AgentInvocationTraceEntry) states.get(WorkflowDebugger.KEY_TRACE_ENTRY);
        String uid = workflowDebugger.getAgentMetadata(traceEntry.getAgent()).getId();
        SwingUtilities.invokeLater(() -> {
            findItemByUid(uid).ifPresent(item -> {
                item.setState(WorkflowItem.State.Finished);
                list.repaint();
                updateSelection();
            });
        });
    }

    /**
     * Returns the currently set WorkflowDebugger instance.
     */
    public WorkflowDebugger getWorkflowDebugger() {
        return workflowDebugger;
    }

    /**
     * Sets the WorkflowDebugger instance for this inspector. This will register/unregister breakpoints with the
     * debugger.
     *
     * @param workflowDebugger The WorkflowDebugger instance to use.
     */
    public void setWorkflowDebugger(WorkflowDebugger workflowDebugger) {
        if (this.workflowDebugger != workflowDebugger) {
            if (this.workflowDebugger != null)
                cleanupWorkflowDebugger();

            this.workflowDebugger = workflowDebugger;

            if (this.workflowDebugger != null)
                setupWorkflowDebugger();
        }
    }

    /**
     * Finds a {@link WorkflowItem} by its unique identifier (UID).
     *
     * @param uid The unique identifier of the workflow item to find.
     * @return An {@link Optional} containing the {@link WorkflowItem} if found, otherwise an empty {@link Optional}.
     */
    protected Optional<WorkflowItem> findItemByUid(String uid) {
        return Optional.ofNullable(workflowItemsByUID.get(uid));
    }

    /**
     * Finds a {@link WorkflowItem} by its type.
     *
     * @param type The type of the workflow item to find.
     * @return An {@link Optional} containing the {@link WorkflowItem} if found, otherwise an empty {@link Optional}.
     */
    protected Optional<WorkflowItem> findItemByType(String type) {
        return Optional.ofNullable(workflowItemsByType.get(type));
    }

    /**
     * Checks if the list model contains any elements.
     *
     * @return {@code true} if the list model has one or more elements, {@code false} otherwise.
     */
    public boolean hasContent() {
        return model.getSize() > 0;
    }

    protected static String getStateIndicator(WorkflowItem.State state, String type, int passCount) {
        return switch (state) {
            case Unknown -> "";
            case Finished -> type.equals(TYPE_END) ?
                    "✓" :
                    passCount <= 1 ? "•" :
                            passCount == Integer.MAX_VALUE ?
                                    "⟳" :
                                    String.valueOf(passCount);
            case Failed -> "✘";
            case Running -> "▶";
        };
    }

    /**
     * Represents a single item in the workflow list, holding its properties and state.
     */
    public static class WorkflowItem implements Cloneable {

        private final String iconKey;
        private final String title;
        private final String subtitle;
        private final String uid;
        private final String type;
        private final String outputName;
        private final Map<Integer, List<WorkflowDebugger.AgentInvocationTraceEntry>> traceEntriesByIndex = new HashMap<>();
        private int indentation;
        private State state = State.Unknown;
        private int passCount;

        public WorkflowItem(String uid, String type, String outputName, String iconKey, String title, String subtitle, int indentation) {
            this.iconKey = iconKey;
            this.title = title;
            this.subtitle = subtitle;
            this.indentation = indentation;
            this.uid = uid;
            this.type = type;
            this.outputName = outputName;
        }

        public Map<Integer, List<WorkflowDebugger.AgentInvocationTraceEntry>> getTraceEntriesByIndex() {
            return traceEntriesByIndex;
        }

        /**
         * Returns the output name of the workflow item.
         *
         * @return The output name.
         */
        public String getOutputName() {
            return outputName;
        }

        /**
         * Retrieves the agent invocation trace entries for a specific pass index.
         *
         * @param index The index of the pass.
         * @return The {@link WorkflowDebugger.AgentInvocationTraceEntry} for the given index, or null if not found.
         */
        public List<WorkflowDebugger.AgentInvocationTraceEntry> getTraceEntries(int index) {
            return traceEntriesByIndex.get(index);
        }

        /**
         * Sets the agent invocation trace entry for a specific pass index.
         *
         * @param index      The index of the pass.
         * @param traceEntry The {@link WorkflowDebugger.AgentInvocationTraceEntry} to set.
         */
        public void setTraceEntry(int index, WorkflowDebugger.AgentInvocationTraceEntry traceEntry) {
            traceEntriesByIndex.computeIfAbsent(index, k -> new ArrayList<>()).add(traceEntry);
        }

        /**
         * Returns the unique identifier of the workflow item.
         */
        public String getUid() {
            return uid;
        }

        /**
         * Returns the type of the workflow item (e.g., agent, if-then, start, end).
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the key for the icon associated with this workflow item.
         */
        public String getIconKey() {
            return iconKey;
        }

        /**
         * Returns the main title of the workflow item.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the subtitle or detailed description of the workflow item.
         */
        public String getSubtitle() {
            return subtitle;
        }

        /**
         * Returns the indentation level of the workflow item, indicating its nesting.
         */
        public int getIndentation() {
            return indentation;
        }

        /**
         * Sets the indentation level of the workflow item.
         */
        public void setIndentation(int aIndentation) {
            indentation = aIndentation;
        }

        /**
         * Returns the current state of the workflow item (Unknown, Running, Finished, Failed).
         */
        public State getState() {
            return state;
        }

        /**
         * Sets the state of the workflow item.
         *
         * @param state The new state to set.
         */
        public void setState(State state) {
            this.state = state;
            if (state == State.Unknown)
                passCount = 0;
        }

        /**
         * Returns a string indicator representing the current state of the workflow item.
         *
         * @return A string indicating the state.
         */
        public String getStateIndicator() {
            return WorkflowInspectorListPane.getStateIndicator(state, type, passCount);
        }

        public int getPassCount() {
            return passCount;
        }

        public void setPassCount(int aPassCount) {
            passCount = aPassCount;
        }

        /**
         * Returns the background color for the workflow item based on its type and the current UI appearance.
         *
         * @return A Color object for the background.
         */
        public Color getColor() {
            return switch (type) {
                case JSON_TYPE_AGENT -> UISupport.isDarkAppearance() ? BACKGROUND_DARK_AGENT : BACKGROUND_AGENT;
                case JSON_TYPE_NON_AI_AGENT ->
                        UISupport.isDarkAppearance() ? BACKGROUND_DARK_AGENT_NONAI : BACKGROUND_AGENT_NONAI;
                case TYPE_END,
                     TYPE_START,
                     JSON_TYPE_IF_THEN,
                     JSON_TYPE_REPEAT,
                     JSON_TYPE_DO_WHEN,
                     JSON_TYPE_MATCH,
                     JSON_TYPE_GROUP,
                     JSON_TYPE_DO_PARALLEL ->
                        UISupport.isDarkAppearance() ? BACKGROUND_DARK_STATEMENT : BACKGROUND_STATEMENT;
                default -> null;
            };
        }

        @Override
        public WorkflowItem clone() {
            try {
                return (WorkflowItem) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        public void clear() {
            setState(WorkflowItem.State.Unknown);
            setPassCount(0);
            traceEntriesByIndex.clear();
        }

        public enum State {
            Unknown,
            Running,
            Finished,
            Failed
        }
    }

    private static class WorkflowItemRenderer extends JPanel implements ListCellRenderer<WorkflowItem> {
        public static final Color BORDER_COLOR = Color.DARK_GRAY;
        static final Border INDICATOR_LINE_BORDER = BorderFactory.createCompoundBorder(
                UISupport.createRoundRectBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(1, 8, 1, 8));
        static final Border INDICATOR_BORDER = BorderFactory.createEmptyBorder(1, 5, 1, 5);
        public static final Color HIGHLIGHT_COLOR = new Color(0, 128, 0);
        private final JLabel lblIcon = new JLabel();
        private final JLabel lblTitle = new JLabel();
        private final JLabel lblSubTitle = new JLabel() {
        };
        private final JLabel lblSubTitle2 = new JLabel() {
        };
        private final JLabel lblStateIndicator = new JLabel();
        private final JPanel pnlContent;
        private final Box pnlStateIndicator;
        private LinkType linkType;
        private String type;
        private boolean paintLinkArrows;
        private boolean isHighlighted;

        public WorkflowItemRenderer() {
            super(new BorderLayout());
            setOpaque(true);

            // State indicator
            pnlStateIndicator = new Box(Y_AXIS);
            pnlStateIndicator.setOpaque(false);
            pnlStateIndicator.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
            pnlStateIndicator.add(Box.createVerticalGlue());
            pnlStateIndicator.add(lblStateIndicator);
            pnlStateIndicator.add(Box.createVerticalGlue());
            add(pnlStateIndicator, BorderLayout.WEST);

            // This is the new dedicated panel for all content
            pnlContent = new JPanel(new BorderLayout(5, 5));
            pnlContent.setOpaque(false);
            add(pnlContent, BorderLayout.CENTER);

            // Icon on the left
            lblIcon.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 5));
            pnlContent.add(lblIcon, BorderLayout.WEST);

            // Text panel for title and subtitle
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, Y_AXIS));
            textPanel.setOpaque(false);
            pnlContent.add(textPanel, BorderLayout.CENTER);

            // Center text vertically
            textPanel.add(Box.createVerticalGlue());
            textPanel.add(Box.createVerticalStrut(5));

            lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
            textPanel.add(lblTitle);

            textPanel.add(lblSubTitle);
            textPanel.add(lblSubTitle2);

            textPanel.add(Box.createVerticalStrut(5));
            textPanel.add(Box.createVerticalGlue());
            textPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        }

        private void paintStartEndShape(Graphics2D graphics, Rectangle rect, boolean isHighlighted) {
            graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, rect.height, rect.height);
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, rect.height, rect.height);

            if (isHighlighted)
                paintHighlight(graphics, new Rectangle(rect.x + rect.width - HIGHLIGHT_BUDGE_SIZE - HIGHLIGHT_BUDGE_GAP,
                        (rect.y + rect.height) / 2,
                        HIGHLIGHT_BUDGE_SIZE, HIGHLIGHT_BUDGE_SIZE));
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setStroke(new BasicStroke(1f));

            Rectangle rect = pnlContent.getBounds().getBounds();
            Insets insets = pnlContent.getInsets();
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= (insets.left + insets.right);
            rect.height -= (insets.top + insets.bottom);

            graphics.setColor(getBackground());
            switch (type) {
                case TYPE_START, TYPE_END -> paintStartEndShape(graphics, rect, isHighlighted);
                case JSON_TYPE_IF_THEN, JSON_TYPE_DO_WHEN, JSON_TYPE_REPEAT, "doParallel", "group" ->
                        paintControlFlowShape(rect, graphics, isHighlighted);
                case JSON_TYPE_MATCH -> paintMatchShape(rect, graphics, isHighlighted);
                default -> paintAgentShape(graphics, rect, isHighlighted);
            }

            if (paintLinkArrows)
                paintLinkArrows(graphics, rect, insets);
            else
                paintLinks(graphics, rect, insets);


            graphics.dispose();
        }

        private void paintHighlight(Graphics2D graphics, Rectangle rect) {
            graphics.setColor(UISupport.isDarkAppearance() ? Color.GREEN : HIGHLIGHT_COLOR);
            graphics.fillOval(rect.x, rect.y, rect.width, rect.height);
        }

        private static final int HIGHLIGHT_BUDGE_SIZE = 7;
        private static final int HIGHLIGHT_BUDGE_GAP = 7;

        private void paintAgentShape(Graphics2D graphics, Rectangle rect, boolean isHighlighted) {
            graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);
            graphics.setColor(BORDER_COLOR);
            graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);

            if (isHighlighted)
                paintHighlight(graphics, new Rectangle(rect.x + rect.width - HIGHLIGHT_BUDGE_SIZE - HIGHLIGHT_BUDGE_GAP,
                        rect.y + HIGHLIGHT_BUDGE_GAP,
                        HIGHLIGHT_BUDGE_SIZE, HIGHLIGHT_BUDGE_SIZE));
        }

        private void paintMatchShape(Rectangle rect, Graphics2D graphics, boolean isHighlighted) {
            int cornerSize = rect.height / 2;
            int arc = 3; // Radius for rounded corners

            // Create a GeneralPath to draw the rounded pentagon
            java.awt.geom.GeneralPath pentagon = new java.awt.geom.GeneralPath();

            // Start at the top-left corner (flat vertical side)
            pentagon.moveTo(rect.x, rect.y + arc);
            pentagon.lineTo(rect.x, rect.y + rect.height - arc);
            pentagon.quadTo(rect.x, rect.y + rect.height, rect.x + arc, rect.y + rect.height);
            pentagon.lineTo(rect.x + rect.width - cornerSize - arc, rect.y + rect.height);
            pentagon.quadTo(rect.x + rect.width - cornerSize, rect.y + rect.height, rect.x + rect.width - cornerSize + arc, rect.y + rect.height - arc);
            pentagon.lineTo(rect.x + rect.width - arc, rect.y + rect.height / 2 + arc);
            pentagon.quadTo(rect.x + rect.width, rect.y + rect.height / 2, rect.x + rect.width - arc, rect.y + rect.height / 2 - arc);
            pentagon.lineTo(rect.x + rect.width - cornerSize + arc, rect.y + arc);
            pentagon.quadTo(rect.x + rect.width - cornerSize, rect.y, rect.x + rect.width - cornerSize - arc, rect.y);
            pentagon.lineTo(rect.x + arc, rect.y);
            pentagon.quadTo(rect.x, rect.y, rect.x, rect.y + arc);
            pentagon.closePath();

            graphics.fill(pentagon);
            graphics.setColor(BORDER_COLOR);
            graphics.draw(pentagon);

            if (isHighlighted)
                paintHighlight(graphics, new Rectangle(rect.x + rect.width - HIGHLIGHT_BUDGE_SIZE - HIGHLIGHT_BUDGE_GAP,
                        (rect.y + rect.height) / 2,
                        HIGHLIGHT_BUDGE_SIZE, HIGHLIGHT_BUDGE_SIZE));
        }

        private void paintControlFlowShape(Rectangle rect, Graphics2D graphics, boolean isHighlighted) {
            int cornerSize = rect.height / 2;
            int arc = 3; // Radius for rounded corners

            // Create a GeneralPath to draw the rounded hexagon
            java.awt.geom.GeneralPath hexagon = new java.awt.geom.GeneralPath();

            // Start at the top-left rounded corner
            hexagon.moveTo(rect.x + cornerSize + arc, rect.y);
            hexagon.lineTo(rect.x + rect.width - cornerSize - arc, rect.y);
            hexagon.quadTo(rect.x + rect.width - cornerSize, rect.y, rect.x + rect.width - cornerSize + arc, rect.y + arc);
            hexagon.lineTo(rect.x + rect.width - arc, rect.y + rect.height / 2 - arc);
            hexagon.quadTo(rect.x + rect.width, rect.y + rect.height / 2, rect.x + rect.width - arc, rect.y + rect.height / 2 + arc);
            hexagon.lineTo(rect.x + rect.width - cornerSize + arc, rect.y + rect.height - arc);
            hexagon.quadTo(rect.x + rect.width - cornerSize, rect.y + rect.height, rect.x + rect.width - cornerSize - arc, rect.y + rect.height);
            hexagon.lineTo(rect.x + cornerSize + arc, rect.y + rect.height);
            hexagon.quadTo(rect.x + cornerSize, rect.y + rect.height, rect.x + cornerSize - arc, rect.y + rect.height - arc);
            hexagon.lineTo(rect.x + arc, rect.y + rect.height / 2 + arc);
            hexagon.quadTo(rect.x, rect.y + rect.height / 2, rect.x + arc, rect.y + rect.height / 2 - arc);
            hexagon.lineTo(rect.x + cornerSize - arc, rect.y + arc);
            hexagon.quadTo(rect.x + cornerSize, rect.y, rect.x + cornerSize + arc, rect.y);
            hexagon.closePath();

            graphics.fill(hexagon);
            graphics.setColor(BORDER_COLOR);
            graphics.draw(hexagon);

            if (isHighlighted)
                paintHighlight(graphics, new Rectangle(rect.x + rect.width - HIGHLIGHT_BUDGE_SIZE - HIGHLIGHT_BUDGE_GAP,
                        (rect.y + rect.height) / 2,
                        HIGHLIGHT_BUDGE_SIZE, HIGHLIGHT_BUDGE_SIZE));
        }

        private void paintLinkArrows(Graphics2D graphics, Rectangle rect, Insets insets) {
            graphics.setStroke(new BasicStroke(0.5f));
            graphics.setColor(new Color(0, 128, 0));
            int linkX = rect.x + (rect.width + insets.left) / 2 - insets.left;
            int[] xPoints = {linkX - 3, linkX + 3, linkX};
            int[] yPoints = {0, 0, 5};
            switch (linkType) {
                case Top:
                    graphics.fillPolygon(xPoints, yPoints, 3); // Arrow pointing down
                    break;
                case Bottom:
                    graphics.draw(new Line2D.Double(linkX - 0.5, getHeight() - 5, linkX - 0.5, getHeight()));
                    graphics.draw(new Line2D.Double(linkX, getHeight() - 5, linkX, getHeight()));
                    break;
                case Both:
                    graphics.draw(new Line2D.Double(linkX - 0.5, getHeight() - 5, linkX - 0.5, getHeight()));
                    graphics.draw(new Line2D.Double(linkX, getHeight() - 5, linkX, getHeight()));
                    graphics.fillPolygon(xPoints, yPoints, 3); // Arrow pointing down
                    break;
                default:
                    // do nothing
            }
        }

        private void paintLinks(Graphics2D graphics, Rectangle rect, Insets insets) {
            int linkX = rect.x + (rect.width + insets.left) / 2 - insets.left;
            switch (linkType) {
                case Top:
                    graphics.drawLine(linkX, 0, linkX, 5);
                    break;
                case Bottom:
                    graphics.drawLine(linkX, getHeight() - 5, linkX, getHeight());
                    break;
                case Both:
                    graphics.drawLine(linkX, 0, linkX, 5);
                    graphics.drawLine(linkX, getHeight() - 5, linkX, getHeight());
                    break;
                default:
                    // do nothing
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends WorkflowItem> list, WorkflowItem value, int index, boolean isSelected, boolean cellHasFocus) {
            WorkflowInspectorListPane listPane = (WorkflowInspectorListPane) SwingUtilities.getAncestorOfClass(WorkflowInspectorListPane.class, list);
            paintLinkArrows = listPane.isPainLinkArrows();

            this.type = value.getType();
            int size = list.getModel().getSize();
            if (size == 1)
                linkType = LinkType.None;
            else if (index == 0 && size > 1) {
                linkType = LinkType.Bottom;
            } else if (index == size - 1 && size > 1) {
                linkType = LinkType.Top;
            } else {
                linkType = LinkType.Both;
            }

            lblStateIndicator.setText(listPane.getStateIndicator(value, index));
            lblStateIndicator.setBorder(value.getState() == WorkflowItem.State.Failed ||
                    (value.getState() == WorkflowItem.State.Finished &&
                            ((value.getPassCount() > 1 && value.getPassCount() != Integer.MAX_VALUE) ||
                                    value.getType().equals(TYPE_END))) ? INDICATOR_LINE_BORDER : INDICATOR_BORDER);
            pnlStateIndicator.setPreferredSize(new Dimension(50, 0));

            lblIcon.setIcon(value.getIconKey() != null ? UISupport.getIcon(value.getIconKey(), UISupport.isDarkAppearance() || (isSelected && cellHasFocus)) : null);
            lblTitle.setText(value.getTitle());

            String[] subTitles = listPane.getSubTitles(value, index);
            if (subTitles.length > 0) {
                lblSubTitle.setVisible(true);
                lblSubTitle.setText(subTitles[0]);
                if (subTitles.length > 1) {
                    lblSubTitle2.setVisible(true);
                    lblSubTitle2.setText(subTitles[1]);
                } else {
                    lblSubTitle2.setVisible(false);
                    lblSubTitle2.setText(null);
                }
            } else {
                lblSubTitle.setVisible(false);
                lblSubTitle2.setVisible(false);
            }

            int leftIndent = value.getIndentation() * 20;
            pnlContent.setBorder(BorderFactory.createEmptyBorder(5, leftIndent, 5, 10));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                lblTitle.setForeground(list.getSelectionForeground());
                lblSubTitle.setForeground(list.getSelectionForeground());
                lblSubTitle2.setForeground(list.getSelectionForeground());
            } else {
                setBackground(value.getColor() != null ? value.getColor() : list.getBackground());
                setForeground(list.getForeground());
                lblTitle.setForeground(list.getForeground());
                Color subTitleForeground = UISupport.isDarkAppearance() ? Color.LIGHT_GRAY : Color.GRAY;
                lblSubTitle.setForeground(subTitleForeground);
                lblSubTitle2.setForeground(subTitleForeground);
            }

            isHighlighted = listPane.isElementHighlighted(index);

            return this;
        }

        private enum LinkType {
            None, Top, Bottom, Both
        }
    }

    protected boolean isElementHighlighted(int index) {
        if (highlightedVariable == null)
            return false;

        if (index == 0) {
            return getWorkflowInput().containsKey(highlightedVariable);
        } else {
            WorkflowItem selectedValue = model.getElementAt(index);

            return selectedValue != null &&
                    selectedValue.getOutputName() != null &&
                    selectedValue.getOutputName().equals(highlightedVariable);
        }
    }

    protected String getStateIndicator(WorkflowItem aValue, int aIndex) {
        return aValue.getStateIndicator();
    }

    public void highlightUsage(String variable) {
        this.highlightedVariable = variable;
        repaint();
    }

    public static class Structure extends WorkflowInspectorListPane {
        @Override
        public void setWorkflow(EasyWorkflow.AgentWorkflowBuilder<?> builder) {
            super.setWorkflow(builder);
            for (WorkflowItem workflowItem : listModel)
                model.addElement(workflowItem);
        }

        @Override
        public void copy() {
            if (workflowBuilder != null) {
                String json = workflowBuilder.toJson();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(json), null);
            }
        }
    }

    public static class Execution extends WorkflowInspectorListPane {
        public Execution() {
            painLinkArrows = true;
        }

        @Override
        public void copy() {
            if (workflowDebugger != null) {
                WorkflowDebugger.AgentInvocationTraceEntryArchive archive = getTraceEntryArchive();
                String result = archive == null ?
                        workflowDebugger.toString(true) :
                        workflowDebugger.toString(archive.workflowInput(),
                                archive.agentInvocationTraceEntries(),
                                archive.workflowResult(),
                                archive.workflowFailure());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(result), null);
            }
        }

        @Override
        public void workflowDebuggerSessionStarted(Map<String, Object> states) {
            SwingUtilities.invokeLater(() -> {
                model.clear();
                clearItems();
                findItemByType(TYPE_START).ifPresent(item -> {
                    model.addElement(item);
                    item.setState(WorkflowItem.State.Finished);
                });
                list.repaint();
                updateSelection();
            });
        }

        @Override
        public void workflowDebuggerSessionStopped(Map<String, Object> states) {
            SwingUtilities.invokeLater(() -> {
                findItemByType(TYPE_END).ifPresent(item -> {
                    model.addElement(item);
                    item.setState(WorkflowItem.State.Finished);
                });
                findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
                list.repaint();
                updateSelection();
            });
        }

        @Override
        public void workflowDebuggerSessionFailed(Map<String, Object> states) {
            SwingUtilities.invokeLater(() -> {
                findItemByType(TYPE_END).ifPresent(item -> {
                    model.addElement(item);
                    item.setState(WorkflowItem.State.Failed);
                });
                findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
                markIncompleteItemsAsFailed();
                list.repaint();
                updateSelection();
            });
        }

        @Override
        public void workflowDebuggerAgentStarted(Map<String, Object> states) {
            WorkflowDebugger.AgentInvocationTraceEntry traceEntry = (WorkflowDebugger.AgentInvocationTraceEntry) states.get(WorkflowDebugger.KEY_TRACE_ENTRY);
            String uid = workflowDebugger.getAgentMetadata(traceEntry.getAgent()).getId();
            SwingUtilities.invokeLater(() -> {
                findItemByUid(uid).ifPresent(currentItem -> {
                    currentItem.setIndentation(0);
                    model.addElement(currentItem);
                    currentItem.setState(WorkflowItem.State.Running);
                    currentItem.setPassCount(currentItem.getPassCount() == 0 ? 1 : Integer.MAX_VALUE);
                    currentItem.setTraceEntry(model.size() - 1, traceEntry);
                    markMissedItemsAsFinished(currentItem);
                });
                findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
                list.repaint();
                updateSelection();
            });
        }

        @Override
        protected String[] getSubTitles(WorkflowItem workflowItem, int index) {
            if (!(workflowItem.getType().equals(JSON_TYPE_AGENT) ||
                    workflowItem.getType().equals(JSON_TYPE_NON_AI_AGENT) ||
                    workflowItem.getType().equals(JSON_TYPE_BREAKPOINT) ||
                    workflowItem.getType().equals(TYPE_START) ||
                    workflowItem.getType().equals(TYPE_END)))
                return new String[0];

            List<WorkflowDebugger.AgentInvocationTraceEntry> traceEntries = workflowItem.getTraceEntries(index);
            if (workflowItem.getType().equals(TYPE_START)) {
                return new String[] {"→ " + mapToSubTitle(getWorkflowInput())};
            } else if (workflowItem.getType().equals(TYPE_END)) {
                String result = "";
                if (getWorkflowFailure() != null)
                    result = WorkflowDebugger.getFailureCauseException(getWorkflowFailure()).getMessage();
                else
                    result = getWorkflowResult() != null ? getWorkflowResult().toString() : "";

                return new String[] {"← " + result};
            } else if (traceEntries != null && !traceEntries.isEmpty()) {
                String inputStr = "→ ";
                String outputStr = "← ";

                WorkflowDebugger.AgentInvocationTraceEntry traceEntry = traceEntries.get(0);
                if (traceEntry.getInput() instanceof UserMessage userMessage) {
                    inputStr += userMessage.singleText();
                } else if (traceEntry.getInput() != null) {
                    inputStr += traceEntry.getInput().toString();
                }

                String outputStr1 = traceEntry.getOutput() instanceof Map<?, ?> outputMap ?
                        mapToSubTitle(outputMap) :
                        traceEntry.getOutput() != null ? traceEntry.getOutput().toString() : "";

                outputStr += outputStr1;
                return new String[]{inputStr, outputStr};
            }
            return new String[0];
        }

        private String mapToSubTitle(Map<?, ?> map) {
            if (map.isEmpty())
                return "";
            if (map.size() == 1)
                return map.entrySet().iterator().next().getValue().toString();

            return getAgentMethodParameterNames(getAgentMethod(workflowBuilder.getAgentClass())).stream()
                    .map(name -> "%s=%s".formatted(name, map.get(name)))
                    .collect(Collectors.joining(", "));
        }

        @Override
        protected String getStateIndicator(WorkflowItem value, int index) {
            if (value.state == WorkflowItem.State.Running && index < model.size() - 1)
                return getStateIndicator(WorkflowItem.State.Finished, value.type, value.passCount);
            else
                return value.getStateIndicator();
        }
    }
}
