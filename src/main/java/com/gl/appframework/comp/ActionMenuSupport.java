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
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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
        setupPopupMenu(popupMenu, actionGroup, new HashMap<>());
    }

    public static void setupPopupMenu(JPopupMenu popupMenu, ActionGroup actionGroup, Map<String, ButtonGroup> buttonGroupMap) {
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
        int i = 0;
        for (Action action : actionGroup.getActions()) {
            if (action == null)
                continue;

            if (action instanceof ActionGroup subGroup) {
                if (subGroup.isPopup()) {
                    JMenu subMenu = new JMenu(subGroup.getValue(Action.NAME).toString());
                    popupMenu.add(subMenu);
                    subMenu.setIcon(subGroup.getValue(Action.SMALL_ICON) instanceof Icon ? (Icon) subGroup.getValue(Action.SMALL_ICON) : null); // Cast to Icon
                    setupSelectedIcon(subGroup, subMenu);
                    setupPopupMenu(subMenu.getPopupMenu(), subGroup, buttonGroupMap); // Pass buttonGroupMap for nested groups
                } else {
                    // If it's an ActionGroup but not a popup, treat its actions as direct menu items
                    for (Action subAction : subGroup.getActions()) {
                        addMenuItem(popupMenu, subAction, buttonGroupMap);
                    }
                    if (i < actionGroup.getActions().size() - 1)
                        popupMenu.addSeparator(); // Separator after a non-popup action group's items
                }
            } else {
                addMenuItem(popupMenu, action, buttonGroupMap);
            }
            i++;
        }
        if (popupMenu.getComponent(popupMenu.getComponentCount() - 1) instanceof JSeparator)
            popupMenu.remove(popupMenu.getComponentCount() - 1);
    }

    private static void addMenuItem(JPopupMenu popupMenu, Action action, Map<String, ButtonGroup> buttonGroupMap) {
        JMenuItem menuItem = null;
        if (action instanceof StateAction stateAction) {
            if (stateAction.getExclusiveGroup() != null) {
                ButtonGroup buttonGroup = buttonGroupMap.computeIfAbsent(stateAction.getExclusiveGroup(), k -> new ButtonGroup());
                JRadioButtonMenuItem rbMenuItem = createRadioButtonMenuItem(stateAction);
                buttonGroup.add(rbMenuItem);
                popupMenu.add(rbMenuItem);
            } else {
                menuItem = createMenuCheckBoxItem(stateAction);
                popupMenu.add(menuItem);
            }
        } else if (action instanceof ActionGroup subGroup && !subGroup.isPopup()) {
            // This case handles non-popup ActionGroups that are not nested within another ActionGroup
            // Their actions are added directly to the current popupMenu
            for (Action subAction : subGroup.getActions()) {
                addMenuItem(popupMenu, subAction, buttonGroupMap);
            }
            popupMenu.addSeparator();
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
        for (Action action : actionGroup.getActions()) {
            if (action == null)
                continue;

            if (action instanceof ActionGroup subGroup) {
                JMenu menu = new JMenu(subGroup.getValue(Action.NAME).toString());
                menuBar.add(menu);
                menu.setIcon(subGroup.getValue(Action.SMALL_ICON) instanceof Icon ? (Icon) subGroup.getValue(Action.SMALL_ICON) : null);
                setupPopupMenu(menu.getPopupMenu(), subGroup, buttonGroupMap);
            } else {
                // Top-level actions in a menu bar are typically JMenus, not direct JMenuItems.
                // If a direct action is encountered here, it's usually an error in the ActionGroup structure
                // for a menu bar, or it implies a single menu item at the top level, which is uncommon.
                // For now, we'll add it as a JMenu with a single item, or you might choose to log an error.
                logger.warn("Direct action '{}' found at top level of JMenuBar setup. Consider wrapping it in an ActionGroup for a JMenu.", action.getValue(Action.NAME));
                JMenu menu = new JMenu(action.getValue(Action.NAME).toString());
                menuBar.add(menu);
                menu.setIcon(action.getValue(Action.SMALL_ICON) instanceof Icon ? (Icon) action.getValue(Action.SMALL_ICON) : null);
                addMenuItem(menu.getPopupMenu(), action, buttonGroupMap);
            }
        }
    }

    /**
     * Creates a {@link JMenuItem} from an {@link Action}
     *
     * @param action The {@link Action} to associate with the menu item.
     * @return A new {@link JMenuItem} instance.
     */
    public static JMenuItem createMenuItem(Action action) {
        JMenuItem result = new JMenuItem(action);
        result.setToolTipText(null);
        setupSelectedIcon(action, result);
        return result;
    }

    private static void setupSelectedIcon(Action action, JMenuItem result) {
        if (UISupport.isMac()) {
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
        JCheckBoxMenuItem result = new JCheckBoxMenuItem(action);
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
        JRadioButtonMenuItem result = new JRadioButtonMenuItem(action);
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

        while (c instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) c;
            Container parent = item.getParent();

            if (!(parent instanceof JPopupMenu))
                return (item instanceof JMenu) ? (JMenu) item : null;

            JPopupMenu parentPopup = (JPopupMenu) parent;
            c = parentPopup.getInvoker();
        }

        return null;
    }
}
