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

import com.gl.appframework.actions.ActionGroup;

import javax.swing.*;

/**
 * A JMenuBar implementation that populates itself based on an {@link ActionGroup}.
 */
public class ActionMenuBar extends JMenuBar {
    private final ActionComponentSupport<ActionMenuBar> actionComponentSupport =
            new ActionComponentSupport<>(this,
                    ActionMenuBar::rebuild,
                    ActionMenuBar::updateSeparatorsVisibility);

    /**
     * Constructs an empty ActionMenuBar.
     */
    public ActionMenuBar() {
    }

    /**
     * Constructs an ActionMenuBar with the specified action group.
     *
     * @param actionGroup the action group to populate the menu bar with
     */
    public ActionMenuBar(ActionGroup actionGroup) {
        setActionGroup(actionGroup);
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

    private static void rebuild(ActionMenuBar menuBar) {
        if (menuBar.getActionGroup() == null) return;

        ActionMenuSupport.setupMenuBar(menuBar, menuBar.getActionGroup());
        menuBar.revalidate();
        menuBar.repaint();
    }

    private static void updateSeparatorsVisibility(ActionMenuBar menuBar) {
        ActionMenuSupport.updateSeparatorsVisibility(menuBar);
    }
}
