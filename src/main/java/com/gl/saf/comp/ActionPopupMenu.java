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

import com.gl.saf.actions.ActionGroup;

import javax.swing.*;

/**
 * A JPopupMenu that populates itself based on an {@link ActionGroup}.
 */
public class ActionPopupMenu extends JPopupMenu {
    private final ActionComponentSupport<ActionPopupMenu> actionComponentSupport =
            new ActionComponentSupport<>(this,
                    ActionPopupMenu::rebuild,
                    ActionPopupMenu::updateSeparatorsVisibility);

    /**
     * Creates an ActionPopupMenu with the specified ActionGroup.
     *
     * @param actionGroup the group of actions to populate the menu with
     */
    public ActionPopupMenu(ActionGroup actionGroup) {
        setActionGroup(actionGroup);
    }

    /**
     * Creates an empty ActionPopupMenu.
     */
    public ActionPopupMenu() {
    }

    /**
     * Returns the action group associated with this menu bar.
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

    private static void rebuild(ActionPopupMenu popupMenu) {
        if (popupMenu.getActionGroup() == null) return;

        ActionMenuSupport.setupPopupMenu(popupMenu, popupMenu.getActionGroup());
    }

    private static void updateSeparatorsVisibility(ActionPopupMenu popupMenu) {
        ActionMenuSupport.updateSeparatorsVisibility(popupMenu);
    }
}
