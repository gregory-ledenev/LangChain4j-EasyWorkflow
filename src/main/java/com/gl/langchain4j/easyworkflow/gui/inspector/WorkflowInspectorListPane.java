package com.gl.langchain4j.easyworkflow.gui.inspector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.gui.UISupport;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.*;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

public class WorkflowInspectorListPane extends JPanel {
    private final JList<WorkflowItem> list;
    private final DefaultListModel<WorkflowItem> model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowInspectorListPane() {
        setLayout(new BorderLayout());
        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setCellRenderer(new WorkflowItemRenderer());
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setWorkflow(EasyWorkflow.AgentWorkflowBuilder<?> builder) {
        model.clear();
        try {
            String jsonString = builder.toJson();
            List<Map<String, Object>> workflowData = objectMapper.readValue(jsonString, List.class);
            populateModelFromJson(workflowData, 0);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            model.addElement(new WorkflowItem(null, "Error parsing workflow", e.getMessage(), 0));
        }
    }

    private static String getSimpleClassName(String className) {
        int lastIndex = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$'));

        return lastIndex == -1 ? className : className.substring(lastIndex + 1);
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

            model.addElement(new WorkflowItem(iconKey, title, subtitle, indentation));

            if (node.containsKey("block")) {
                populateModelFromJson((List<Map<String, Object>>) node.get("block"), indentation + 1);
            }
            if (node.containsKey("elseBlock")) {
                model.addElement(new WorkflowItem(null, "else", "", indentation));
                populateModelFromJson((List<Map<String, Object>>) node.get("elseBlock"), indentation + 1);
            }
        }
    }

    private static String getAgentTitle(Map<String, Object> node) {
        return ((String) node.get(JSON_KEY_NAME)).replaceAll("(?<=[a-z])(?=[A-Z])", " ");
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
                (String) node.getOrDefault(JSON_KEY_OUTPUT_NAME, "N/A"));

    }

    private Icon createIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(Color.BLUE);
                g.fillOval(x, y, getIconWidth(), getIconHeight());
            }

            @Override
            public int getIconWidth() {
                return 22;
            }

            @Override
            public int getIconHeight() {
                return 22;
            }
        };
    }

    private static class WorkflowItem {
        private final String iconKey;
        private final String title;
        private final String subtitle;
        private final int indentation;

        public WorkflowItem(String iconKey, String title, String subtitle, int indentation) {
            this.iconKey = iconKey;
            this.title = title;
            this.subtitle = subtitle;
            this.indentation = indentation;
        }

        public String getIconKey() {
            return iconKey;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public int getIndentation() {
            return indentation;
        }
    }

    private static class WorkflowItemRenderer extends JPanel implements ListCellRenderer<WorkflowItem> {
        private final JLabel iconLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel subtitleLabel = new JLabel();

        public WorkflowItemRenderer() {
            setLayout(new BorderLayout(10, 5));
            setOpaque(true);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            textPanel.add(Box.createVerticalGlue());

            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            textPanel.add(titleLabel);

            textPanel.add(subtitleLabel);

            textPanel.add(Box.createVerticalGlue());

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends WorkflowItem> list, WorkflowItem value, int index, boolean isSelected, boolean cellHasFocus) {
            iconLabel.setIcon(value.getIconKey() != null ? UISupport.getIcon(value.getIconKey(), isSelected && cellHasFocus) : null);
            titleLabel.setText(value.getTitle());

            boolean hasSubtitle = value.getSubtitle() != null && !value.getSubtitle().trim().isEmpty();
            subtitleLabel.setVisible(hasSubtitle);
            if (hasSubtitle) {
                subtitleLabel.setText(value.getSubtitle());
            }

            int leftIndent = value.getIndentation() * 20;
            setBorder(BorderFactory.createEmptyBorder(5, 10 + leftIndent, 5, 10));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                titleLabel.setForeground(list.getSelectionForeground());
                subtitleLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                titleLabel.setForeground(list.getForeground());
                subtitleLabel.setForeground(Color.GRAY);
            }

            return this;
        }
    }

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
}
