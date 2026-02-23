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

package com.gl.appframework.comp;

import com.gl.appframework.LoggerFactory;
import com.gl.appframework.UISupport;
import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.actions.BasicAction;
import com.gl.appframework.actions.StateAction;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for populating Swing menu components (JPopupMenu, JMenuBar) from {@link ActionGroup} definitions.
 */
public class ActionMenuSupport {
    private static final Logger logger = LoggerFactory.getLogger(ActionMenuSupport.class);

    /**
     * Sets up a {@link JPopupMenu} with actions from an {@link ActionGroup}.
     *
     * @param popupMenu   The {@link JPopupMenu} to set up.
     * @param actionGroup The {@link ActionGroup} containing the actions to add to the popup menu.
     */
    public static void setupPopupMenu(JPopupMenu popupMenu, ActionGroup actionGroup) {
        popupMenu.removeAll();
        setupPopupMenuListener(popupMenu, actionGroup);
        setupPopupMenu(popupMenu, actionGroup, new HashMap<>());
    }

    /**
     * Sets up a {@link JPopupMenu} with actions from an {@link ActionGroup}, using a provided map to manage button groups.
     * This method recursively processes {@link ActionGroup}s to build the menu structure.
     *
     * @param popupMenu      The {@link JPopupMenu} to set up.
     * @param actionGroup    The {@link ActionGroup} containing the actions.
     * @param buttonGroupMap A map of group names to {@link ButtonGroup} instances for exclusive selection actions.
     */
    public static void setupPopupMenu(JPopupMenu popupMenu, ActionGroup actionGroup, Map<String, ButtonGroup> buttonGroupMap) {
        for (int i = 0; i < actionGroup.getActions().size(); i++) {
            Action action = actionGroup.getActions().get(i);
            if (action == null)
                continue;

            if (action instanceof ActionGroup subGroup) {
                if (subGroup.isPopup()) {
                    createSubMenu(popupMenu, buttonGroupMap, action, subGroup);
                } else {
                    // Non-popup group: recursively add its items.
                    setupPopupMenu(popupMenu, subGroup, buttonGroupMap);
                    // Add a separator after the group, if it's not the last action.
                    if (i < actionGroup.getActions().size() - 1) {
                        if (popupMenu.getComponentCount() > 0 && !(popupMenu.getComponent(popupMenu.getComponentCount() - 1) instanceof JSeparator)) {
                            popupMenu.addSeparator();
                        }
                    }
                }
            } else {
                addMenuItem(popupMenu, action, buttonGroupMap);
            }
        }
        // Clean up trailing separators.
        if (popupMenu.getComponentCount() > 0 && popupMenu.getComponent(popupMenu.getComponentCount() - 1) instanceof JSeparator) {
            popupMenu.remove(popupMenu.getComponentCount() - 1);
        }
    }

    private static void createSubMenu(JPopupMenu popupMenu, Map<String, ButtonGroup> buttonGroupMap, Action action, ActionGroup subGroup) {
        JMenu subMenu = createMenu(action);
        popupMenu.add(subMenu);
        subMenu.setIcon(subGroup.getValue(Action.SMALL_ICON) instanceof Icon ? (Icon) subGroup.getValue(Action.SMALL_ICON) : null); // Cast to Icon
        setupSelectedIcon(subGroup, subMenu);
        setupPopupMenuListener(subMenu.getPopupMenu(), subGroup);
        setupPopupMenu(subMenu.getPopupMenu(), subGroup, buttonGroupMap); // Pass buttonGroupMap for nested groups
        subMenu.setEnabled(checkEnabled(subMenu));
    }

    private static void setupPopupMenuListener(JPopupMenu popupMenu, ActionGroup actionGroup) {
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                for (Action action : actionGroup.getActions()) {
                    if (action instanceof BasicAction basicAction)
                        basicAction.update();
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    /**
     * Checks if a menu should be enabled - it's enabled if it contains any visible non-separator components.
     *
     * @param menu The {@link JMenu} to check.
     * @return {@code true} if the menu has visible items, {@code false} otherwise.
     */
    public static boolean checkEnabled(JMenu menu) {
        for (Component c : menu.getPopupMenu().getComponents()) {
            if (! (c instanceof JSeparator) && c.isVisible())
                return true;
        }
        return false;
    }

    /**
     * Adds a menu item to a popup menu for a given action. This method handles creating the appropriate
     * JMenuItem (or subclass) based on the action type.
     *
     * @param popupMenu      The popup menu to add the item to.
     * @param action         The action to create the menu item from.
     * @param buttonGroupMap A map for managing radio button groups.
     */
    private static void addMenuItem(JPopupMenu popupMenu, Action action, Map<String, ButtonGroup> buttonGroupMap) {
        JMenuItem menuItem;
        if (action instanceof StateAction stateAction) {
            if (stateAction.getExclusiveGroup() != null) {
                ButtonGroup buttonGroup = buttonGroupMap.computeIfAbsent(stateAction.getExclusiveGroup(), k -> new ButtonGroup());
                JRadioButtonMenuItem rbMenuItem = createRadioButtonMenuItem(stateAction);
                buttonGroup.add(rbMenuItem);
                popupMenu.add(rbMenuItem);
                menuItem = rbMenuItem;
            } else {
                menuItem = createMenuCheckBoxItem(stateAction);
                popupMenu.add(menuItem);
            }
        } else {
            menuItem = createMenuItem(action);
            popupMenu.add(menuItem);
        }

        if (menuItem != null && belongsToMenuBar(popupMenu)) {
            String name = (String) action.getValue(BasicAction.MENU_BAR_ITEM_NAME);
            if (name != null && !name.isEmpty())
                menuItem.setText(name);
        }
    }

    /**
     * Sets up a {@link JMenuBar} with actions from an {@link ActionGroup}.
     *
     * @param menuBar     The {@link JMenuBar} to set up.
     * @param actionGroup The {@link ActionGroup} containing the actions to add to the menu bar.
     */
    public static void setupMenuBar(JMenuBar menuBar, ActionGroup actionGroup) {
        setupMenuBar(menuBar, actionGroup, new HashMap<>());
    }

    private static void setupMenuBar(JMenuBar menuBar, ActionGroup actionGroup, Map<String, ButtonGroup> buttonGroupMap) {
        menuBar.removeAll();

        for (Action action : actionGroup.getActions()) {
            if (action == null)
                continue;

            if (action instanceof ActionGroup subGroup) {
                JMenu menu = createMenu(subGroup);
                menuBar.add(menu);
                menu.setIcon(subGroup.getValue(Action.SMALL_ICON) instanceof Icon ? (Icon) subGroup.getValue(Action.SMALL_ICON) : null);
                setupPopupMenuListener(menu.getPopupMenu(), subGroup);
                setupPopupMenu(menu.getPopupMenu(), subGroup, buttonGroupMap);
            } else {
                // Top-level actions in a menu bar are typically JMenus, not direct JMenuItems.
                // If a direct action is encountered here, it's usually an error in the ActionGroup structure
                // for a menu bar, or it implies a single menu item at the top level, which is uncommon.
                // For now, we'll add it as a JMenu with a single item, or you might choose to log an error.
                logger.warn("Direct action '{}' found at top level of JMenuBar setup. Consider wrapping it in an ActionGroup for a JMenu.", action.getValue(Action.NAME));
                JMenu menu = createMenu(action);
                menuBar.add(menu);
                menu.setIcon(action.getValue(Action.SMALL_ICON) instanceof Icon ? (Icon) action.getValue(Action.SMALL_ICON) : null);
                addMenuItem(menu.getPopupMenu(), action, buttonGroupMap);
            }
        }
    }

    private static JMenu createMenu(Action action) {
        JMenu result = new JMenu(action) {
            @Override
            protected void actionPropertyChanged(Action action, String propertyName) {
                super.actionPropertyChanged(action, propertyName);
                setToolTipText(null);
                if (propertyName.equals(BasicAction.VISIBLE))
                    setVisible(BasicAction.isVisible(action));
            }
        };
        result.setToolTipText(null);
        return result;
    }

    /**
     * Creates a {@link JMenuItem} from an {@link Action}
     *
     * @param action The {@link Action} to associate with the menu item.
     * @return A new {@link JMenuItem} instance.
     */
    public static JMenuItem createMenuItem(Action action) {
        JMenuItem result = new JMenuItem(action) {
            @Override
            protected void actionPropertyChanged(Action action, String propertyName) {
                super.actionPropertyChanged(action, propertyName);
                if (propertyName.equals(BasicAction.VISIBLE))
                    setVisible(BasicAction.isVisible(action));
            }
        };
        result.setToolTipText(null);
        setupSelectedIcon(action, result);
        return result;
    }

    private static void setupSelectedIcon(Action action, JMenuItem result) {
        if (UISupport.isMacOS()) {
            UISupport.AutoIcon icon = action.getValue(Action.SMALL_ICON) instanceof UISupport.AutoIcon ? (UISupport.AutoIcon) action.getValue(Action.SMALL_ICON) : null;
            if (icon != null)
                result.setSelectedIcon(UISupport.getIcon(icon.getKey(), true));
        }
    }

    /**
     * Creates a {@link JCheckBoxMenuItem} from an {@link Action}.
     *
     * @param action The {@link Action} to associate with the checkbox menu item.
     * @return A new {@link JCheckBoxMenuItem} instance.
     */
    public static JCheckBoxMenuItem createMenuCheckBoxItem(Action action) {
        JCheckBoxMenuItem result = new JCheckBoxMenuItem(action) {
            @Override
            protected void actionPropertyChanged(Action action, String propertyName) {
                super.actionPropertyChanged(action, propertyName);
                if (propertyName.equals(BasicAction.VISIBLE))
                    setVisible(BasicAction.isVisible(action));
            }
        };
        result.setToolTipText(null);
        setupSelectedIcon(action, result);
        return result;
    }

    /**
     * Creates a {@link JRadioButtonMenuItem} from an {@link Action}.
     *
     * @param action The {@link Action} to associate with the radio button menu item.
     * @return A new {@link JRadioButtonMenuItem} instance.
     */
    public static JRadioButtonMenuItem createRadioButtonMenuItem(Action action) {
        JRadioButtonMenuItem result = new JRadioButtonMenuItem(action) {
            @Override
            protected void actionPropertyChanged(Action action, String propertyName) {
                super.actionPropertyChanged(action, propertyName);
                if (propertyName.equals(BasicAction.VISIBLE))
                    setVisible(BasicAction.isVisible(action));
            }
        };
        result.setToolTipText(null);
        setupSelectedIcon(action, result);
        return result;
    }

    private static boolean belongsToMenuBar(JPopupMenu popup) {
        JMenu top = findTopMenu(popup);
        return top != null && top.getParent() instanceof JMenuBar;
    }

    private static JMenu findTopMenu(JPopupMenu popup) {
        if (popup == null)
            return null;

        Component c = popup.getInvoker();

        while (c instanceof JMenuItem item) {
            Container parent = item.getParent();

            if (!(parent instanceof JPopupMenu parentPopup))
                return (item instanceof JMenu) ? (JMenu) item : null;

            c = parentPopup.getInvoker();
        }

        return null;
    }

    /**
     * Recursively updates the visibility of separators in all menus within a {@link JMenuBar}.
     *
     * @param menuBar The {@link JMenuBar} to process.
     */
    public static void updateSeparatorsVisibility(JMenuBar menuBar) {
        for (Component c : menuBar.getComponents()) {
            if (c instanceof JMenu menu)
                updateSeparatorsVisibility(menu.getPopupMenu());
        }
    }

    /**
     * Updates the visibility of separators within a {@link JPopupMenu} based on the visibility
     * of surrounding menu items to avoid double separators or separators at the edges.
     *
     * @param popupMenu The {@link JPopupMenu} to process.
     */
    public static void updateSeparatorsVisibility(JPopupMenu popupMenu) {
        boolean lastComponentWasVisible = false;
        for (int i = 0; i < popupMenu.getComponentCount(); i++) {
            JComponent component = (JComponent) popupMenu.getComponent(i);

            if (component instanceof JSeparator separator) {
                // A separator should be visible only if the previous visible component was not a separator
                // and there is a visible component after it.
                boolean currentSeparatorCanBeVisible = lastComponentWasVisible;

                // Check if there's a visible component after this separator
                boolean hasVisibleComponentAfter = false;
                for (int j = i + 1; j < popupMenu.getComponentCount(); j++) {
                    if ( ! (popupMenu.getComponent(j) instanceof JSeparator) &&
                            popupMenu.getComponent(j).isVisible()) {
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

            if (component instanceof JMenu menu)
                updateSeparatorsVisibility(menu.getPopupMenu());
        }
    }
}
