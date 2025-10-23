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

import com.gl.langchain4j.easyworkflow.gui.HeaderPane;
import com.gl.langchain4j.easyworkflow.gui.UISupport;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * A panel that displays details of a workflow's execution results.
 * It consists of two main parts: a tree view ({@link ValuesPane}) to navigate through the results
 * and a detail pane ({@link ValueDetailsPane}) to show the selected value.
 */
public class WorkflowInspectorDetailsPane extends JSplitPane {
    private final ValuesPane pnlValues;
    private final ValueDetailsPane pnlValueDetails;
    private Map<String, Object> values;

    /**
     * Constructs a new {@code WorkflowInspectorDetailsPane}.
     * Initializes the two sub-panes and sets up the selection listener for the tree view.
     */
    public WorkflowInspectorDetailsPane() {
        super(VERTICAL_SPLIT);
        setOpaque(false);
        setMinimumSize(new Dimension(400, 300));

        pnlValues = new ValuesPane();
        pnlValueDetails = new ValueDetailsPane();
        pnlValues.getTree().addTreeSelectionListener(e -> {
            ValuesPane.NamedValue namedValue = (ValuesPane.NamedValue) ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            pnlValueDetails.setValue(namedValue.value() != null ? namedValue.value().toString() : "null");
        });
        setTopComponent(pnlValues);
        setBottomComponent(pnlValueDetails);
        setResizeWeight(0.7);
    }

    /**
     * Returns the currently displayed values.
     *
     * @return A {@code Map} containing the values.
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * Sets the values to be displayed in the inspector.
     *
     * @param values A {@code Map} containing the values to display.
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
        if (pnlValueDetails != null)
            pnlValueDetails.setValue(null);
        if (pnlValues != null)
            pnlValues.setValues(values);
    }

    /**
     * A panel that displays a tree view of the workflow execution results.
     * It allows navigating through nested maps and lists.
     */
    class ValuesPane extends JPanel {
        private Map<String, Object> values;
        private JTree treeValues = new JTree();

        /**
         * Constructs a new {@code ValuesPane}.
         * Initializes the tree view and sets up its rendering and selection properties.
         */
        public ValuesPane() {
            setMinimumSize(new Dimension(200, 100));
            setOpaque(false);
            setLayout(new BorderLayout());
            JScrollPane scrollPane = UISupport.createScrollPane(treeValues, true, false, true, true, false);
            HeaderPane headerPane = new HeaderPane();
            headerPane.setTitle("Inspector");
            headerPane.setSubtitle("Execution results for a selected agent");
            scrollPane.setColumnHeaderView(headerPane);
            add(scrollPane, BorderLayout.CENTER);
            treeValues.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            treeValues.setRootVisible(false);
            treeValues.setShowsRootHandles(true);
            treeValues.setFont(treeValues.getFont().deriveFont(15f));
            treeValues.setCellRenderer(new ValuesPaneRenderer());
            setValues(Map.of());
        }

        /**
         * Returns the {@code JTree} component used to display the values.
         *
         * @return The {@code JTree} instance.
         */
        public JTree getTree() {
            return treeValues;
        }

        /**
         * Returns the currently displayed values in this pane.
         *
         * @return A {@code Map} containing the values.
         */
        public Map<String, Object> getValues() {
            return values;
        }

        /**
         * Sets the values to be displayed in the tree view.
         *
         * @param aValues A {@code Map} containing the values to display.
         */
        public void setValues(Map<String, Object> aValues) {
            values = aValues;
            if (values == null) {
                treeValues.setModel(null);
                return;
            }

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Values");
            buildTree(root, values);
            DefaultTreeModel model = new DefaultTreeModel(root);
            treeValues.setModel(model);

            for (int i = 0; i < root.getChildCount(); i++)
                treeValues.expandPath(new TreePath(model.getPathToRoot(root.getChildAt(i))));
            treeValues.getSelectionModel().addSelectionPath(treeValues.getPathForRow(0));
        }

        /**
         * A record representing a named value in the tree.
         *
         * @param icon The icon string to display next to the name (e.g., "❖", "≡", "•").
         * @param name The name of the value.
         * @param value The actual value.
         */
        record NamedValue(String icon, String name, Object value) {}

        private void buildTree(DefaultMutableTreeNode parent, Object data) {
            if (data instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) data;
                map.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value instanceof Map || value instanceof List) {
                                DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                                        new NamedValue(value instanceof Map ? "❖" : "≡",key, value));
                                parent.add(node);
                                buildTree(node, value);
                            } else {
                                parent.add(new DefaultMutableTreeNode(new NamedValue("•", key, value)));
                            }
                        });
            } else if (data instanceof List) {
                List<?> list = (List<?>) data;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map || item instanceof List) {
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NamedValue(item instanceof Map ? "❖" : "≡",  "[" + i + "]", item));
                        parent.add(node);
                        buildTree(node, item);
                    } else {
                        parent.add(new DefaultMutableTreeNode(new NamedValue("•", "[" + i + "]", item)));
                    }
                }
            }
        }
    }

    /**
     * A custom renderer for the {@code JTree} in {@link ValuesPane}.
     * It formats the display of {@link ValuesPane.NamedValue} objects.
     */
    class ValuesPaneRenderer extends DefaultTreeCellRenderer {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof ValuesPane.NamedValue namedValue) {
                if (namedValue.value() instanceof Map || namedValue.value() instanceof List) {
                    if (! expanded)
                        setText("<html>%s <b>%s</b>: %s</html>".formatted(namedValue.icon(), namedValue.name(), namedValue.value()));
                    else
                        setText("<html>%s <b>%s</b></html>".formatted(namedValue.icon(), namedValue.name()));
                } else {
                    setText("<html>%s <b>%s</b>: %s</html>".formatted(namedValue.icon(), namedValue.name(), namedValue.value()));
                }
            }

            return this;
        }
    }

    /**
     * A panel that displays the detailed value of a selected item from the {@link ValuesPane} tree.
     */
    static class ValueDetailsPane extends JPanel {
        private final JEditorPane edtValue = new JEditorPane() {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        /**
         * Constructs a new {@code ValueDetailsPane}.
         * Initializes the editor pane and sets up its properties.
         */
        public ValueDetailsPane() {
            edtValue.setEditable(false);
            edtValue.setFont(edtValue.getFont().deriveFont(14f));
            setMinimumSize(new Dimension(200, 100));
            setOpaque(false);
            setLayout(new BorderLayout());
            JScrollPane scrollPane = UISupport.createScrollPane(edtValue, true, true, true, false, false);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, BorderLayout.CENTER);
        }

        /**
         * Sets the text value to be displayed in the detail pane.
         *
         * @param value The string value to display.
         */
        public void setValue(String value) {
            edtValue.setText(value);
            edtValue.setCaretPosition(0);
        }
    }
}
