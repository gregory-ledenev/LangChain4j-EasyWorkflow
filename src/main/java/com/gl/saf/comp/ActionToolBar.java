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

package com.gl.saf.comp;

import com.gl.saf.actions.*;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.gl.saf.actions.BasicAction.COMPONENT_ACTION_KEY;
import static com.gl.saf.actions.BasicAction.RETAIN_NAME_KEY;

/**
 * A specialized {@link JToolBar} that populates itself based on an {@link ActionGroup}.
 */
public class ActionToolBar extends JToolBar {

    /**
     * Creates a new ActionToolBar and initializes it with the provided {@link ActionGroup}.
     *
     * @param actionGroup the group of actions to display in the toolbar
     */
    public ActionToolBar(ActionGroup actionGroup) {
        this();
        setActionGroup(actionGroup);
    }    private final ActionComponentSupport<ActionToolBar> actionComponentSupport =
            new ActionComponentSupport<>(this,
                    ActionToolBar::rebuild,
                    ActionToolBar::updateSeparatorsVisibility);

    /**
     * Creates a new ActionToolBar with default horizontal orientation.
     */
    public ActionToolBar() {
        this(JToolBar.HORIZONTAL);
    }

    /**
     * Creates a new ActionToolBar with the specified orientation.
     *
     * @param orientation the orientation of the toolbar (either {@link SwingConstants#HORIZONTAL}
     *                    or {@link SwingConstants#VERTICAL})
     */
    public ActionToolBar(int orientation) {
        super(orientation);

        setLayout(new DefaultToolBarLayout(this));
    }

    /**
     * Creates a new ActionToolBar with the specified orientation.
     *
     * @param orientation the orientation of the toolbar (either {@link SwingConstants#HORIZONTAL}
     *                    or {@link SwingConstants#VERTICAL})
     * @param actionGroup the group of actions to display in the toolbar
     */
    public ActionToolBar(int orientation, ActionGroup actionGroup) {
        this(orientation);
        setActionGroup(actionGroup);
    }

    private static void rebuild(ActionToolBar actionToolBar) {
        if (actionToolBar.getActionGroup() == null) return;

        setupToolbar(actionToolBar, actionToolBar.getActionGroup());
        actionToolBar.revalidate();
        actionToolBar.repaint();
    }

    /**
     * Sets up a {@link JToolBar} with actions from an {@link ActionGroup}.
     *
     * @param toolbar     The {@link JToolBar} to set up.
     * @param actionGroup The {@link ActionGroup} containing the actions to add to the toolbar.
     */
    public static void setupToolbar(JToolBar toolbar, ActionGroup actionGroup) {
        toolbar.removeAll();
        setupToolbar(toolbar, actionGroup, true, new HashMap<>());
        updateSeparatorsVisibility(toolbar);
    }

    private static void updateSeparatorsVisibility(JToolBar toolbar) {
        boolean lastComponentWasVisible = false;
        for (int i = 0; i < toolbar.getComponentCount(); i++) {
            JComponent component = (JComponent) toolbar.getComponent(i);

            if (component instanceof JSeparator separator) {
                // A separator should be visible only if the previous visible component was not a separator
                // and there is a visible component after it.
                boolean currentSeparatorCanBeVisible = lastComponentWasVisible;

                // Check if there's a visible component after this separator
                boolean hasVisibleComponentAfter = false;
                for (int j = i + 1; j < toolbar.getComponentCount(); j++) {
                    if (!(toolbar.getComponent(j) instanceof JSeparator) &&
                            toolbar.getComponent(j).isVisible()) {
                        hasVisibleComponentAfter = true;
                        break;
                    }
                }
                separator.setVisible(currentSeparatorCanBeVisible && hasVisibleComponentAfter);
                lastComponentWasVisible = false; // Reset for the next component
            } else {
                // For non-separator components, if they are visible, they enable the next separator.
                if (component.isVisible()) {
                    lastComponentWasVisible = true;
                }
            }
        }
    }

    private static void setupToolbar(JToolBar toolbar, ActionGroup actionGroup, boolean addSeparators,
                                     Map<String, ButtonGroup> buttonGroupMap) {
        for (int i = 0; i < actionGroup.getActions().size(); ++i) {
            Action action = actionGroup.getActions().get(i);
            if (action == null || !BasicAction.isVisible(action))
                continue;

            int componentCountBefore = toolbar.getComponentCount();

            if (action instanceof ActionGroup subGroup) {
                if (subGroup.isPopup()) {
                    JButton popupButton = createToolbarButton(toolbar, subGroup);
                    ActionPopupMenu subPopupMenu = new ActionPopupMenu();
                    subPopupMenu.setActionGroup(subGroup);
                    popupButton.addActionListener(e -> subPopupMenu.show(popupButton, 0, popupButton.getHeight()));
                    toolbar.add(popupButton); // Add the popup button to the toolbar
                } else { // Not a popup, so add its actions directly to the current toolbar
                    setupToolbar(toolbar, subGroup, false, buttonGroupMap); // Recursively add actions of the subgroup without separators
                }
            } else {
                addToolbarItem(toolbar, action, buttonGroupMap);
            }

            int componentCountAfter = toolbar.getComponentCount();
            boolean addedComponents = componentCountAfter > componentCountBefore;

            // Add separator only if it's not the last item and the previous item was not a separator
            if (addedComponents && addSeparators && action instanceof ActionGroup && i < actionGroup.getActions().size() - 1 &&
                    toolbar.getComponentCount() > 0 &&
                    !(toolbar.getComponent(toolbar.getComponentCount() - 1) instanceof JSeparator)) {
                toolbar.addSeparator();
            }
        }
        if (toolbar.getComponentCount() > 0 && toolbar.getComponent(toolbar.getComponentCount() - 1) instanceof JSeparator)
            toolbar.remove(toolbar.getComponentCount() - 1);
    }

    private static void addToolbarItem(JToolBar toolbar, Action action, Map<String, ButtonGroup> buttonGroupMap) {
        if (action instanceof ComponentAction componentAction) {
            if (componentAction.getValue(Action.NAME) != null) {
                final JLabel label = new JLabel(componentAction.getValue(Action.NAME).toString());
                label.putClientProperty(COMPONENT_ACTION_KEY, action);
                label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
                toolbar.add(label);
                componentAction.getComponent().addPropertyChangeListener("enabled",
                        evt -> label.setEnabled(componentAction.getComponent().isEnabled()));
            }
            toolbar.add(componentAction.getComponent());
        } else if (action instanceof StateAction) {
            toolbar.add(createToolbarToggleButton(toolbar, action, false, buttonGroupMap));
        } else {
            toolbar.add(createToolbarButton(toolbar, action));
        }
    }

    /**
     * Creates a {@link JButton} for use in a toolbar, optionally preserving its text.
     *
     * @param toolbar The {@link JToolBar} to add the button to.
     * @param action  The {@link Action} to associate with the button.
     * @return A new {@link JButton} instance configured for toolbar use.
     */
    public static JButton createToolbarButton(JToolBar toolbar, Action action) {
        JButton result = new JButton(action) {
            @Override
            public JToolTip createToolTip() {
                return ActionTooltip.FACTORY.createToolTip(this);
            }

            @Override
            protected void actionPropertyChanged(Action action, String propertyName) {
                super.actionPropertyChanged(action, propertyName);
                if (propertyName.equals(BasicAction.VISIBLE_KEY))
                    setVisible(BasicAction.isVisible(action));
                if (propertyName.equals(BasicAction.NAME) && !Boolean.TRUE.equals(action.getValue(RETAIN_NAME_KEY)))
                    setText(null);
            }
        };
        result.setVisible(BasicAction.isVisible(action));
        if (!Boolean.TRUE.equals(action.getValue(RETAIN_NAME_KEY)) && action.getValue(Action.SMALL_ICON) != null)
            result.setText(null);

        boolean hasText = result.getText() != null;
        result.setMargin(new Insets(5, hasText ? 6 : 5, 5, hasText ? 6 : 5));

        if (action instanceof ActionGroup)
            result.setText(hasText ? result.getText() + " ▾" : "▾");

        if (toolbar.getOrientation() == VERTICAL) {
            result.setHorizontalTextPosition(SwingConstants.CENTER);
            result.setVerticalTextPosition(SwingConstants.BOTTOM);
            result.setHorizontalAlignment(SwingConstants.CENTER);
            result.setVerticalAlignment(SwingConstants.CENTER);
        }

        return result;
    }

    /**
     * Returns the selected icon for the specified action, prioritizing the large icon.
     *
     * @param action The action to query.
     * @return The selected icon, or {@code null} if none is set.
     */
    public static Icon getSelectedIcon(Action action) {
        Icon result = (Icon) action.getValue(BasicAction.SELECTED_LARGE_ICON_KEY);
        if (result == null)
            result = (Icon) action.getValue(BasicAction.SELECTED_ICON_KEY);

        return result;
    }

    /**
     * Creates a {@link JToggleButton} for use in a toolbar, optionally preserving its text.
     *
     * @param toolBar      The {@link JToolBar} to add the toggle button to.
     * @param action       The {@link Action} to associate with the toggle button.
     * @param preserveText If {@code true}, the toggle button's text will be kept; otherwise, it will be set to
     *                     {@code null}.
     * @return A new {@link JToggleButton} instance configured for toolbar use.
     */
    public static JToggleButton createToolbarToggleButton(JToolBar toolBar, Action action, boolean preserveText, Map<String, ButtonGroup> buttonGroupMap) {
        ButtonGroup buttonGroup = null;
        if (action instanceof StateAction stateAction) {
            if (stateAction.getExclusiveGroup() != null)
                buttonGroup = buttonGroupMap.computeIfAbsent(stateAction.getExclusiveGroup(), k -> new ButtonGroup());
        }

        JToggleButton result = new JToggleButton(action) {
            @Override
            public JToolTip createToolTip() {
                return ActionTooltip.FACTORY.createToolTip(this);
            }

            @Override
            protected void configurePropertiesFromAction(Action a) {
                super.configurePropertiesFromAction(a);

                setSelectedIcon(ActionToolBar.getSelectedIcon(a));
            }

            @Override
            protected void actionPropertyChanged(Action action, String propertyName) {
                super.actionPropertyChanged(action, propertyName);
                if (propertyName.equals(BasicAction.VISIBLE_KEY))
                    setVisible(BasicAction.isVisible(action));
                else if (propertyName.equals(BasicAction.SELECTED_LARGE_ICON_KEY) || propertyName.equals(BasicAction.SELECTED_ICON_KEY))
                    setSelectedIcon(ActionToolBar.getSelectedIcon(action));
            }
        };
        result.setVisible(BasicAction.isVisible(action));
        if (buttonGroup != null)
            buttonGroup.add(result);
        if (!Boolean.TRUE.equals(action.getValue(RETAIN_NAME_KEY)) && action.getValue(Action.SMALL_ICON) != null) {
            result.setText(null);
        }
        boolean hasText = result.getText() != null;
        result.setMargin(new Insets(5, hasText ? 6 : 5, 5, hasText ? 6 : 5));

        if (toolBar.getOrientation() == VERTICAL) {
            result.setHorizontalTextPosition(SwingConstants.CENTER);
            result.setVerticalTextPosition(SwingConstants.BOTTOM);
            result.setHorizontalAlignment(SwingConstants.CENTER);
            result.setVerticalAlignment(SwingConstants.CENTER);
        }

        return result;
    }

    /**
     * Returns the {@link ActionGroup} associated with this toolbar.
     *
     * @return the current action group
     */
    public ActionGroup getActionGroup() {
        return actionComponentSupport.getActionGroup();
    }

    /**
     * Sets the {@link ActionGroup} for this toolbar and refreshes the UI components.
     *
     * @param actionGroup the new action group to display
     */
    public void setActionGroup(ActionGroup actionGroup) {
        actionComponentSupport.setActionGroup(actionGroup);
    }

    private static class DefaultToolBarLayout
            implements LayoutManager2, Serializable, PropertyChangeListener, UIResource {

        BoxLayout lm;

        JToolBar toolBar;

        DefaultToolBarLayout(JToolBar toolBar) {
            this.toolBar = toolBar;
            if (toolBar.getOrientation() == JToolBar.VERTICAL) {
                lm = new BoxLayout(toolBar, BoxLayout.PAGE_AXIS);
            } else {
                lm = new BoxLayout(toolBar, BoxLayout.LINE_AXIS);
            }
        }

        public void addLayoutComponent(String name, Component comp) {
            lm.addLayoutComponent(name, comp);
        }

        public void addLayoutComponent(Component comp, Object constraints) {
            lm.addLayoutComponent(comp, constraints);
        }

        public void removeLayoutComponent(Component comp) {
            lm.removeLayoutComponent(comp);
        }

        public Dimension preferredLayoutSize(Container target) {
            return lm.preferredLayoutSize(target);
        }

        public Dimension minimumLayoutSize(Container target) {
            return lm.minimumLayoutSize(target);
        }

        public Dimension maximumLayoutSize(Container target) {
            return lm.maximumLayoutSize(target);
        }

        public void layoutContainer(Container target) {
            lm.layoutContainer(target);
            if (toolBar.getOrientation() == JToolBar.HORIZONTAL)
                return;

            int width = 0;
            for (Component component : toolBar.getComponents()) {
                if (component instanceof AbstractButton)
                    width = Math.max(width, component.getWidth());
            }
            for (Component component : toolBar.getComponents()) {
                if (component instanceof AbstractButton)
                    component.setSize(width, component.getHeight());
            }
        }

        public float getLayoutAlignmentX(Container target) {
            return lm.getLayoutAlignmentX(target);
        }

        public float getLayoutAlignmentY(Container target) {
            return lm.getLayoutAlignmentY(target);
        }

        public void invalidateLayout(Container target) {
            lm.invalidateLayout(target);
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if (name.equals("orientation")) {
                int o = (Integer) e.getNewValue();

                if (o == JToolBar.VERTICAL)
                    lm = new BoxLayout(toolBar, BoxLayout.PAGE_AXIS);
                else {
                    lm = new BoxLayout(toolBar, BoxLayout.LINE_AXIS);
                }
            }
        }
    }


}
