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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import com.gl.langchain4j.easyworkflow.gui.HeaderPane;
import com.gl.langchain4j.easyworkflow.gui.UISupport;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.*;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;
import static javax.swing.BoxLayout.Y_AXIS;

/**
 * A panel that displays a hierarchical view of a workflow, allowing for inspection and debugging.
 */
public class WorkflowInspectorListPane extends JPanel {
    public static final Color BACKGROUND_AGENT = new Color(255, 255, 153);
    public static final Color BACKGROUND_AGENT_NONAI = new Color(245, 245, 245);
    public static final Color BACKGROUND_STATEMENT = new Color(236, 236, 255);
    public static final Color BACKGROUND_DARK_AGENT = new Color(85, 85, 20);
    public static final Color BACKGROUND_DARK_AGENT_NONAI = new Color(70, 70, 70);
    public static final Color BACKGROUND_DARK_STATEMENT = new Color(50, 50, 90);
    public static final ObjectMapper OBJECT_MAPPER = WorkflowDebugger.createObjectMapper();
    static final String TYPE_START = "start";
    static final String TYPE_END = "end";
    private final JList<WorkflowItem> list;
    private final DefaultListModel<WorkflowItem> model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WorkflowDebugger workflowDebugger;
    private WorkflowDebugger.Breakpoint breakpointSessionStarted;
    private WorkflowDebugger.Breakpoint breakpointSessionStopped;
    private WorkflowDebugger.Breakpoint breakpointSessionFailed;
    private WorkflowDebugger.Breakpoint breakpointAgentStarted;
    private WorkflowDebugger.Breakpoint breakpointAgentFinished;

    /**
     * Constructs a new WorkflowInspectorListPane.
     */
    public WorkflowInspectorListPane() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(400, 300));
        model = new DefaultListModel<>();
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
        add(scrollPane, BorderLayout.CENTER);
    }

    private static String getSimpleClassName(String className) {
        int lastIndex = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$'));

        return lastIndex == -1 ? className : className.substring(lastIndex + 1);
    }

    private static String getAgentTitle(Map<String, Object> node) {
        return ((String) node.get(JSON_KEY_NAME)).replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }

    /**
     * Displays the Workflow Inspector in a new JFrame.
     *
     * @param builder The EasyWorkflow.AgentWorkflowBuilder to inspect.
     */
    public static void show(EasyWorkflow.AgentWorkflowBuilder<?> builder) {
        JFrame frame = new JFrame("Workflow Inspector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        WorkflowInspectorListPane pane = new WorkflowInspectorListPane();
        pane.setWorkflow(builder);
        frame.setLocationRelativeTo(null);
        frame.add(pane);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Returns the JList component displaying the workflow items.
     */
    public JList<WorkflowItem> getList() {
        return list;
    }

    /**
     * Sets the workflow to be displayed in the inspector.
     *
     * @param builder The EasyWorkflow.AgentWorkflowBuilder representing the workflow.
     */
    public void setWorkflow(EasyWorkflow.AgentWorkflowBuilder<?> builder) {
        model.clear();
        try {
            String jsonString = builder.toJson();
            List<Map<String, Object>> workflowData = objectMapper.readValue(jsonString, List.class);
            model.addElement(new WorkflowItem(null, TYPE_START, ICON_PLAY, "Start", null, 0));
            populateModelFromJson(workflowData, 0);
            model.addElement(new WorkflowItem(null, TYPE_END, ICON_STOP, "End", null, 0));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            model.addElement(new WorkflowItem(null, null, null, "Error parsing workflow", e.getMessage(), 0));
        }
    }

    private void populateModelFromJson(List<Map<String, Object>> nodes, int indentation) {
        for (Map<String, Object> node : nodes) {
            String type = (String) node.get(EasyWorkflow.JSON_KEY_TYPE);
            String title = type;
            String subtitle = null;
            String iconKey = null;

            switch (type) {
                case JSON_TYPE_AGENT:
                case JSON_TYPE_NON_AI_AGENT:
                    title = getAgentTitle(node);
                    subtitle = getAgentSubtitle(node);
                    iconKey = UISupport.ICON_EXPERT;
                    break;
                case "setState":
                    title = "Set State";
                    subtitle = (String) node.get("details");
                    break;
                case JSON_TYPE_IF_THEN:
                    title = "if (%s)".formatted((String) node.getOrDefault(JSON_KEY_CONDITION, "..."));
                    iconKey = ICON_SIGNPOST;
                    break;
                case JSON_TYPE_REPEAT:
                    title = "repeat (max: %s, until: %s)".formatted(
                            node.get(JSON_KEY_MAX_ITERATIONS),
                            node.getOrDefault(JSON_KEY_CONDITION, "..."));
                    iconKey = ICON_REFRESH;
                    break;
                case "doWhen":
                    title = "when (%s)".formatted((String) node.getOrDefault(JSON_KEY_EXPRESSION, "..."));
                    iconKey = ICON_SIGNPOST;
                    break;
                case "match":
                    title = "match (%s)".formatted((String) node.getOrDefault(JSON_KEY_VALUE, "..."));
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

            model.addElement(new WorkflowItem((String) node.get(JSON_KEY_UID), (String) node.get(JSON_KEY_TYPE), iconKey, title, subtitle, indentation));

            if (node.containsKey("block")) {
                populateModelFromJson((List<Map<String, Object>>) node.get("block"), indentation + 1);
            }
            if (node.containsKey("elseBlock")) {
                model.addElement(new WorkflowItem((String) node.get(JSON_KEY_UID), (String) node.get(JSON_KEY_TYPE), null, "else", "", indentation));
                populateModelFromJson((List<Map<String, Object>>) node.get("elseBlock"), indentation + 1);
            }
        }
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
        return "(%s) → [%s %s]".formatted(parametersStr != null ? parametersStr : "",
                getSimpleClassName((String) node.getOrDefault(JSON_KEY_OUTPUT_TYPE, "N/A")),
                (String) node.getOrDefault(JSON_KEY_OUTPUT_NAME, "response"));
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
                    workflowDebugger.getWorkflowInput().forEach((key, value) -> result.put(key, valueToMap(value)));
                }
                case TYPE_END -> {
                    if (valueToMap(workflowDebugger.getWorkflowResult()) != null)
                        result.put("result", valueToMap(workflowDebugger.getWorkflowResult()));
                    else if (workflowDebugger.getWorkflowFailure() != null)
                        result.put("failure", valueToMap(WorkflowDebugger.getFailureCauseException(workflowDebugger.getWorkflowFailure())));

                    if (workflowDebugger.getAgenticScope() != null) {
                        Map<String, Object> state = workflowDebugger.getAgenticScope().state();
                        if (!state.isEmpty())
                            result.put("| Agentic Scope |", valueToMap(state));
                    }
                }
                case JSON_TYPE_AGENT, JSON_TYPE_NON_AI_AGENT -> {
                    String uid = selectedValue.getUid();
                    List<WorkflowDebugger.AgentInvocationTraceEntry> entries = workflowDebugger.getAgentInvocationTraceEntries().stream()
                            .filter(e -> workflowDebugger.getAgentMetadata(e.getAgent()).getId().equals(uid))
                            .toList();

                    Map<String, Object> passResult = result;
                    for (WorkflowDebugger.AgentInvocationTraceEntry entry : entries) {
                        if (entries.size() > 1)
                            result.put("pass [%s]".formatted(entries.indexOf(entry) + 1), passResult = new HashMap<>());
                        passResult.put("input", valueToMap(entry.getInput()));
                        passResult.put("output", valueToMap(entry.getOutput()));
                        if (entry.getFailure() != null) {
                            passResult.put("failure", valueToMap(WorkflowDebugger.getFailureCauseException(entry.getFailure())));
                        }
                    }
                }
                default -> {
                }
            }
        }

        return result;
    }

    private Object valueToMap(Object value) {
        if (value == null)
            return null;

        if (value.getClass().isPrimitive() || value instanceof String || value instanceof Number)
            return value;
        else if (value.getClass().isArray() || value instanceof List)
            return OBJECT_MAPPER.convertValue(value, List.class);
        else
            return OBJECT_MAPPER.convertValue(value, Map.class);
    }

    private void setupWorkflowDebugger() {
        breakpointSessionStarted = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.SESSION_STARTED,
                        (aBreakpoint, states) -> workflowDebuggerSessionStarted(states))
                .build();
        workflowDebugger.addBreakpoint(breakpointSessionStarted);

        breakpointSessionStopped = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.SESSION_STOPPED,
                        (aBreakpoint, states) -> workflowDebuggerSessionStopped(states))
                .build();
        workflowDebugger.addBreakpoint(breakpointSessionStopped);

        breakpointSessionFailed = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.SESSION_FAILED,
                        (aBreakpoint, states) -> workflowDebuggerSessionFailed(states))
                .build();
        workflowDebugger.addBreakpoint(breakpointSessionFailed);

        breakpointAgentStarted = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.AGENT_INPUT,
                        (aBreakpoint, states) -> workflowDebuggerAgentStarted(states))
                .build();
        workflowDebugger.addBreakpoint(breakpointAgentStarted);

        breakpointAgentFinished = WorkflowDebugger.Breakpoint.builder(WorkflowDebugger.Breakpoint.Type.AGENT_OUTPUT,
                        (aBreakpoint, states) -> workflowDebuggerAgentFinished(states))
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
            for (int i = 0; i < model.getSize(); i++) {
                model.get(i).setState(WorkflowItem.State.Unknown);
            }
            findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Running));
            list.repaint();
            updateSelection();
        });
    }

    private void updateSelection() {
        int selectedIndex = list.getSelectedIndex();
        list.clearSelection();
        if (selectedIndex > -1)
            list.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
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

    private void markIncompleteItemsAsFailed() {
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
        String uid = getAgentMetadata(states).getId();
        SwingUtilities.invokeLater(() -> {
            findItemByUid(uid).ifPresent(currentItem -> {
                currentItem.setState(WorkflowItem.State.Running);
                currentItem.setPassCount(currentItem.getPassCount() + 1);
                markMissedItemsAsFinished(currentItem);
            });
            findItemByType(TYPE_START).ifPresent(item -> item.setState(WorkflowItem.State.Finished));
            list.repaint();
            updateSelection();
        });
    }

    private void markMissedItemsAsFinished(WorkflowItem currentItem) {
        int index = model.indexOf(currentItem);
        for (int i = index - 1; i >= 0; i--) {
            WorkflowItem item = model.get(i);
            if (item.getState() == WorkflowItem.State.Unknown) {
                switch (item.type) {
                    case JSON_TYPE_IF_THEN, JSON_TYPE_REPEAT, JSON_TYPE_DO_WHEN, JSON_TYPE_MATCH,
                         JSON_TYPE_GROUP, JSON_TYPE_DO_PARALLEL:
                        item.setState(WorkflowItem.State.Finished);
                        break;
                    default:
                }
            }
        }
    }

    private Expression getAgentMetadata(Map<String, Object> states) {
        return workflowDebugger.getAgentMetadata(states.get(WorkflowDebugger.KEY_AGENT));
    }

    /**
     * Callback method invoked when an agent finishes its execution. Updates the UI to mark the agent as finished.
     *
     * @param states A map containing the current state of the workflow.
     */
    public void workflowDebuggerAgentFinished(Map<String, Object> states) {
        String uid = getAgentMetadata(states).getId();
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

    private Optional<WorkflowItem> findItemByUid(String uid) {
        for (int i = 0; i < model.getSize(); i++) {
            WorkflowItem item = model.get(i);
            if (uid.equals(item.getUid())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private Optional<WorkflowItem> findItemByType(String type) {
        for (int i = 0; i < model.getSize(); i++) {
            WorkflowItem item = model.get(i);
            if (type.equals(item.getType())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Represents a single item in the workflow list, holding its properties and state.
     */
    public static class WorkflowItem {

        private final String iconKey;
        private final String title;
        private final String subtitle;
        private final int indentation;
        private final String uid;
        private final String type;
        private State state = State.Unknown;
        private int passCount;

        public WorkflowItem(String uid, String type, String iconKey, String title, String subtitle, int indentation) {
            this.iconKey = iconKey;
            this.title = title;
            this.subtitle = subtitle;
            this.indentation = indentation;
            this.uid = uid;
            this.type = type;
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
         * @return A symbol (e.g., "✓", "✘", "▶") indicating the state.
         */
        public String getStateIndicator() {
            return switch (state) {
                case Unknown -> "";
                case Finished -> type.equals(TYPE_END) ? "✓" : passCount <= 1 ? "•" : String.valueOf(passCount);
                case Failed -> "✘";
                case Running -> "▶";
            };
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

        public enum State {
            Unknown,
            Running,
            Finished,
            Failed
        }
    }

    private static class WorkflowItemRenderer extends JPanel implements ListCellRenderer<WorkflowItem> {
        public static final Color BORDER_COLOR = Color.DARK_GRAY;
        private final JLabel iconLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel subtitleLabel = new JLabel();
        private final JLabel lblStateIndicator = new JLabel();
        private final JPanel pnlContent;
        private final Box pnlStateIndicator;
        private LinkType linkType;
        private String type;

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
            iconLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 5));
            pnlContent.add(iconLabel, BorderLayout.WEST);

            // Text panel for title and subtitle
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, Y_AXIS));
            textPanel.setOpaque(false);
            pnlContent.add(textPanel, BorderLayout.CENTER);

            // Center text vertically
            textPanel.add(Box.createVerticalGlue());
            textPanel.add(Box.createVerticalStrut(5));

            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            textPanel.add(titleLabel);

            textPanel.add(subtitleLabel);

            textPanel.add(Box.createVerticalStrut(5));
            textPanel.add(Box.createVerticalGlue());
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


            Rectangle rect = pnlContent.getBounds().getBounds();
            Insets insets = pnlContent.getInsets();
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= (insets.left + insets.right);
            rect.height -= (insets.top + insets.bottom);

            graphics.setColor(getBackground());
            if (type.equals(TYPE_START) || type.equals(TYPE_END)) {
                graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, rect.height, rect.height);
                graphics.setColor(Color.DARK_GRAY);
                graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, rect.height, rect.height);

            } else if (type.equals(JSON_TYPE_IF_THEN) || type.equals(JSON_TYPE_DO_WHEN) || type.equals(JSON_TYPE_REPEAT)
                    || type.equals("doParallel") || type.equals("group")) {
                // A more subtle hexagon shape (closer to a rectangle with clipped corners)
                int cornerSize = rect.height / 2;
                int[] xPoints = {rect.x + cornerSize, rect.x + rect.width - cornerSize, rect.x + rect.width, rect.x + rect.width - cornerSize, rect.x + cornerSize, rect.x};
                int[] yPoints = {rect.y, rect.y, rect.y + rect.height / 2, rect.y + rect.height, rect.y + rect.height, rect.y + rect.height / 2};
                graphics.fillPolygon(xPoints, yPoints, 6);
                graphics.setColor(BORDER_COLOR);
                graphics.drawPolygon(xPoints, yPoints, 6);

            } else if (type.equals(JSON_TYPE_MATCH)) {
                int cornerSize = rect.height / 3;
                int[] xPoints = {rect.x, rect.x + rect.width - cornerSize, rect.x + rect.width, rect.x + rect.width - cornerSize, rect.x};
                int[] yPoints = {rect.y, rect.y, rect.y + rect.height / 2, rect.y + rect.height, rect.y + rect.height};
                graphics.fillPolygon(xPoints, yPoints, 5);
                graphics.setColor(BORDER_COLOR);
                graphics.drawPolygon(xPoints, yPoints, 5);
            } else {
                graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
                graphics.setColor(BORDER_COLOR);
                graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
            switch (linkType) {
                case Top:
                    graphics.drawLine(getWidth() / 2, 0, getWidth() / 2, 5);
                    break;
                case Bottom:
                    graphics.drawLine(getWidth() / 2, getHeight() - 5, getWidth() / 2, getHeight());
                    break;
                case Both:
                    graphics.drawLine(getWidth() / 2, 0, getWidth() / 2, 5);
                    graphics.drawLine(getWidth() / 2, getHeight() - 5, getWidth() / 2, getHeight());
                    break;
                default:
                    // do nothing
            }
            graphics.dispose();
        }

        static final Border INDICATOR_LINE_BORDER = BorderFactory.createCompoundBorder(
                UISupport.createRoundRectBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(1, 8, 1, 8));
        static final Border INDICATOR_BORDER = BorderFactory.createEmptyBorder(1, 5, 1, 5);

        @Override
        public Component getListCellRendererComponent(JList<? extends WorkflowItem> list, WorkflowItem value, int index, boolean isSelected, boolean cellHasFocus) {
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

            lblStateIndicator.setText(value.getStateIndicator());
            lblStateIndicator.setBorder(value.getState() == WorkflowItem.State.Failed ||
                    (value.getState() == WorkflowItem.State.Finished &&
                    (value.getPassCount() > 1 || value.getType().equals(TYPE_END))) ? INDICATOR_LINE_BORDER : INDICATOR_BORDER);
            pnlStateIndicator.setPreferredSize(new Dimension(50, 0));

            iconLabel.setIcon(value.getIconKey() != null ? UISupport.getIcon(value.getIconKey(), UISupport.isDarkAppearance() || (isSelected && cellHasFocus)) : null);
            titleLabel.setText(value.getTitle());

            boolean hasSubtitle = value.getSubtitle() != null && !value.getSubtitle().trim().isEmpty();
            subtitleLabel.setVisible(hasSubtitle);
            if (hasSubtitle) {
                subtitleLabel.setText(value.getSubtitle());
            }

            int leftIndent = value.getIndentation() * 20;
            pnlContent.setBorder(BorderFactory.createEmptyBorder(5, leftIndent, 5, 10));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                titleLabel.setForeground(list.getSelectionForeground());
                subtitleLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(value.getColor() != null ? value.getColor() : list.getBackground());
                titleLabel.setForeground(list.getForeground());
                subtitleLabel.setForeground(Color.GRAY);
            }

            return this;
        }

        private enum LinkType {
            None, Top, Bottom, Both
        }
    }
}
