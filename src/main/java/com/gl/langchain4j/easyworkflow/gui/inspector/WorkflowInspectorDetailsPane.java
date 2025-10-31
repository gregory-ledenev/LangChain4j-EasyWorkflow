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

import com.gl.langchain4j.easyworkflow.gui.Actions;
import com.gl.langchain4j.easyworkflow.gui.HeaderPane;
import com.gl.langchain4j.easyworkflow.gui.UISupport;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.gui.Actions.*;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

/**
 * A panel that displays details of a workflow's execution results. It consists of two main parts: a tree view
 * ({@link ValuesPane}) to navigate through the results and a detail pane ({@link ValueDetailsPane}) to show the
 * selected value.
 */
public class WorkflowInspectorDetailsPane extends JSplitPane {
    private final ValuesPane pnlValues;
    private final ValueDetailsPane pnlValueDetails;
    private Map<String, Object> values;

    /**
     * Constructs a new {@code WorkflowInspectorDetailsPane}. Initializes the two sub-panes and sets up the selection
     * listener for the tree view.
     */
    public WorkflowInspectorDetailsPane() {
        super(VERTICAL_SPLIT);
        setOpaque(false);
        setMinimumSize(new Dimension(400, 500));

        pnlValues = new ValuesPane();
        pnlValueDetails = new ValueDetailsPane();
        pnlValueDetails.setPreferredSize(new Dimension(400, 200));
        pnlValues.getTree().addTreeSelectionListener(e -> {
            ValuesPane.NamedValue namedValue = (ValuesPane.NamedValue) ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            pnlValueDetails.setValue(namedValue.value() != null ? namedValue.value().toString() : "null");
        });
        setTopComponent(pnlValues);
        setBottomComponent(pnlValueDetails);
        setResizeWeight(0.7);
    }

    @Override
    public void requestFocus() {
        pnlValues.requestFocus();
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

    public JComponent getTreeView() {
        return pnlValues.getTree();
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
         * Constructs a new {@code ValueDetailsPane}. Initializes the editor pane and sets up its properties.
         */
        public ValueDetailsPane() {
            edtValue.setEditable(false);
            edtValue.setFont(edtValue.getFont().deriveFont(14f));
            setMinimumSize(new Dimension(200, 100));
            setOpaque(false);
            setLayout(new BorderLayout());
            edtValue.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane scrollPane = UISupport.createScrollPane(edtValue, true, true, true, false, false);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, BorderLayout.CENTER);

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem mniCopy = new JMenuItem(UISupport.createAction("Copy", new UISupport.AutoIcon(UISupport.ICON_COPY), e -> copy()));
            popupMenu.add(mniCopy);
            edtValue.setComponentPopupMenu(popupMenu);
        }

        private void copy() {
            String text = edtValue.getSelectedText();
            if (text == null || text.isEmpty())
                text = edtValue.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
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

    /**
     * A panel that displays a tree view of the workflow execution results. It allows navigating through nested maps and
     * lists.
     */
    class ValuesPane extends JPanel implements TreeSelectionListener {
        private static final String PROP_ALWAYS_EXPAND = "alwaysExpand";
        private final HeaderPane headerPane;
        private final JTree treeValues = new JTree();
        private final Action actionCopy = createAction("Copy",
                null,
                e -> copy(true, true));
        private final Action actionCopyName = createAction("Copy Name",
                null,
                e -> copy(true, false));
        private final Action actionCopyValue = createAction("Copy Value",
                null,
                e -> copy(false, true));
        private final Action actionExpandAll = createAction("Expand All",
                new AutoIcon(ICON_EXPAND),
                e -> expandAllValues(true));
        private final Action actionCollapseAll = createAction("Collapse All",
                new AutoIcon(ICON_COLLAPSE),
                e -> collapseAllValues());
        private Map<String, Object> values;

        /**
         * Constructs a new {@code ValuesPane}. Initializes the tree view and sets up its rendering and selection
         * properties.
         */
        public ValuesPane() {
            setMinimumSize(new Dimension(200, 100));
            setOpaque(false);
            setLayout(new BorderLayout());
            headerPane = new HeaderPane(false);
            add(headerPane, BorderLayout.NORTH);
            JScrollPane scrollPane = UISupport.createScrollPane(treeValues, true, false, true, true, false);
            headerPane.setTitle("Inspector");
//            headerPane.setSubtitle("Execution results for a selected agent");
            add(scrollPane, BorderLayout.CENTER);
            treeValues.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            treeValues.setRootVisible(false);
            treeValues.setShowsRootHandles(true);
            treeValues.setFont(treeValues.getFont().deriveFont(15f));
            treeValues.setCellRenderer(new ValuesPaneRenderer());
            treeValues.addTreeSelectionListener(this);
            setValues(Map.of());

            actionAlwaysExpand.putValue(Action.SELECTED_KEY, isAlwaysExpandValues());
            actionAlwaysExpand.putValue(Action.SHORT_DESCRIPTION, "Always Expand All");

            UISupport.bindAction(treeValues,
                    "copy",
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                    new BasicAction("Copy", null, e -> copy()));

            setupActions();
            updateActions();
        }

        @Override
        public void requestFocus() {
            treeValues.requestFocus();
        }

        @Override
        public void setComponentPopupMenu(JPopupMenu popup) {
            super.setComponentPopupMenu(popup);
        }

        private static Preferences getPreferences() {
            return UISupport.getPreferences().node("Inspector.ValuesPane");
        }

        private static NamedValue createNamedValue(String icon, String name, Object value) {
            int openParen = name.indexOf('(');
            int closeParen = name.indexOf(')');
            if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                String actualName = name.substring(0, openParen);
                String actualSubName = name.substring(openParen + 1, closeParen);
                return new NamedValue(icon, actualName, actualSubName, value);
            }
            return new NamedValue(icon, name, null, value);
        }

        public HeaderPane getHeaderPane() {
            return headerPane;
        }

        private void setupActions() {
            actionExpandAll.putValue(Action.SHORT_DESCRIPTION, "Expand All");
            actionCollapseAll.putValue(Action.SHORT_DESCRIPTION, "Collapse All");

            ActionGroup actionGroup = new ActionGroup(
                    new ActionGroup("Copy", new AutoIcon(ICON_COPY), true,
                            actionCopy,
                            actionCopyName,
                            actionCopyValue
                    ),
                    new ActionGroup(),
                    new ActionGroup(
                            actionExpandAll,
                            actionCollapseAll
                    ),
                    new ActionGroup(
                            actionAlwaysExpand
                    )
            );

            JPopupMenu popupMenu = new JPopupMenu();
            UISupport.setupPopupMenu(popupMenu, actionGroup);
            treeValues.setComponentPopupMenu(popupMenu);
            UISupport.setupToolbar(headerPane.getToolbar(), actionGroup);
        }

        public boolean isAlwaysExpandValues() {
            return getPreferences().getBoolean(PROP_ALWAYS_EXPAND, false);
        }

        public void setAlwaysExpandValues(boolean alwaysExpand) {
            if (isAlwaysExpandValues() == alwaysExpand)
                return;

            getPreferences().putBoolean(PROP_ALWAYS_EXPAND, alwaysExpand);
            actionAlwaysExpand.putValue(Action.SELECTED_KEY, alwaysExpand);
            if (alwaysExpand)
                expandAllValues(false);
        }

        public void collapseAllValues() {
            TreePath path = treeValues.getSelectionPath();
            if (path == null)
                return;

            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            java.util.Enumeration<TreeNode> e = selectedNode.depthFirstEnumeration();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                if (!node.isRoot()) { // Don't collapse the root
                    treeValues.collapsePath(new TreePath(node.getPath()));
                }
            }
        }

        public void expandAllValues(boolean expandSelected) {
            DefaultMutableTreeNode selectedNode;
            if (expandSelected) {
                TreePath path = treeValues.getSelectionPath();
                if (path == null)
                    return;
                selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            } else {
                selectedNode = (DefaultMutableTreeNode) treeValues.getModel().getRoot();
            }

            java.util.Enumeration<TreeNode> e = selectedNode.depthFirstEnumeration();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                treeValues.expandPath(new TreePath(node.getPath()));
            }
        }

        public void copy() {
            copy(true, true);
        }

        private void copy(boolean copyName, boolean copyValue) {
            NamedValue namedValue = getSelectedValue();
            if (namedValue != null) {
                String text;
                String subName = namedValue.subName() != null ? " (%s)".formatted(namedValue.subName()) : "";
                if (copyName && copyValue)
                    text = "%s%s = %s".formatted(namedValue.name(), subName, namedValue.value() != null ? namedValue.value() : "null");
                else if (copyName)
                    text = "%s%s".formatted(namedValue.name(), subName);
                else
                    text = namedValue.value() != null ? namedValue.value().toString() : "null";
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            }
        }

        public NamedValue getSelectedValue() {
            TreePath selectionPath = treeValues.getSelectionPath();
            if (selectionPath == null)
                return null;

            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            return (NamedValue) selectedNode.getUserObject();
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
         * @param values A {@code Map} containing the values to display.
         */
        public void setValues(Map<String, Object> values) {
            this.values = values;
            if (this.values == null) {
                treeValues.setModel(null);
                return;
            }

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Values");
            buildTree(root, this.values);
            DefaultTreeModel model = new DefaultTreeModel(root);
            treeValues.setModel(model);

            if (isAlwaysExpandValues()) {
                expandAllValues(false);
            } else {
                for (int i = 0; i < root.getChildCount(); i++)
                    treeValues.expandPath(new TreePath(model.getPathToRoot(root.getChildAt(i))));
            }
            treeValues.getSelectionModel().addSelectionPath(treeValues.getPathForRow(0));
        }

        private void buildTree(DefaultMutableTreeNode parent, Object data) {
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) data;
                map.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value instanceof Map || value instanceof List) {
                                DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                                        createNamedValue(value instanceof Map ? "❖" : "≡", key, value));
                                parent.add(node);
                                buildTree(node, value);
                            } else {
                                parent.add(new DefaultMutableTreeNode(createNamedValue("•", key, value)));
                            }
                        });
            } else if (data instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map || item instanceof List) {
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(createNamedValue(item instanceof Map ? "❖" : "≡", "[" + i + "]", item));
                        parent.add(node);
                        buildTree(node, item);
                    } else {
                        parent.add(new DefaultMutableTreeNode(createNamedValue("•", "[" + i + "]", item)));
                    }
                }
            }
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            updateActions();
        }

        private void updateActions() {
            NamedValue namedValue = getSelectedValue();
            boolean enabled = namedValue != null;
            actionCopy.setEnabled(enabled);
            actionCopyName.setEnabled(enabled);
            actionCopyValue.setEnabled(enabled);
            actionExpandAll.setEnabled(enabled);
            actionCollapseAll.setEnabled(enabled);
            actionAlwaysExpand.putValue(Action.SELECTED_KEY, isAlwaysExpandValues());
        }

        /**
         * A record representing a named value in the tree.
         *
         * @param icon  The icon string to display next to the name (e.g., "❖", "≡", "•").
         * @param name  The name of the value.
         * @param value The actual value.
         */
        record NamedValue(String icon, String name, String subName, Object value) {
        }
        private final Action actionAlwaysExpand = new StateAction("Always Expand All",
                new AutoIcon(ICON_ALWAYS_EXPAND),
                null, e -> setAlwaysExpandValues(!isAlwaysExpandValues()));
    }

    /**
     * A custom renderer for the {@code JTree} in {@link ValuesPane}. It formats the display of
     * {@link ValuesPane.NamedValue} objects.
     */
    class ValuesPaneRenderer extends DefaultTreeCellRenderer {
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();

            if (userObject instanceof ValuesPane.NamedValue namedValue) {
                String subName = namedValue.subName != null ? " (%s)".formatted(namedValue.subName) : "";
                if (namedValue.value() instanceof Map || namedValue.value() instanceof List) {
                    if (!expanded) {
                        String color = selected ?
                                String.format("#%06x", ((hasFocus ? getTextSelectionColor() : getTextNonSelectionColor()).getRGB() & 0xFFFFFF)) :
                                "gray";
                        setText("<html>%s <b>%s</b><i>%s</i>: <span style=\"color: %s;\">%s</span></html>".
                                formatted(namedValue.icon(),
                                        namedValue.name(),
                                        subName,
                                        color,
                                        namedValue.value()));
                    } else {
                        setText("<html>%s <b>%s</b><i>%s</i></html>".formatted(
                                namedValue.icon(),
                                namedValue.name(),
                                subName));
                    }
                } else {
                    setText("<html>%s <b>%s</b><i>%s</i>: %s</html>".formatted(
                            namedValue.icon(),
                            namedValue.name(),
                            subName,
                            namedValue.value()));
                }
            }

            return this;
        }
    }
}
