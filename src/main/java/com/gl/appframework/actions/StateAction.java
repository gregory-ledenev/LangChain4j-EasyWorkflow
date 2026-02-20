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

package com.gl.appframework.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * An abstract base class for actions that represent a state (e.g., toggle actions).
 */
@SuppressWarnings("unused")
public class StateAction extends BasicAction {
    private String exclusiveGroup;

    /**
     * Constructs a new StateAction.
     *
     * @param name           The name of the action, used for display purposes.
     * @param icon           The icon to be displayed with the action.
     * @param exclusiveGroup The name of the exclusive group this action belongs to.
     * @param actionListener The consumer that will be invoked when the action is performed.
     */
    public StateAction(String name, Icon icon, String exclusiveGroup, Consumer<ActionEvent> actionListener) {
        this(name, icon, exclusiveGroup, actionListener, null);
    }

    /**
     * Constructs a new StateAction.
     *
     * @param name           The name of the action, used for display purposes.
     * @param icon           The icon to be displayed with the action.
     * @param exclusiveGroup The name of the exclusive group this action belongs to.
     * @param actionListener The consumer that will be invoked when the action is performed.
     * @param actionUpdater  The consumer that will be invoked when the action needs to be updated.
     */
    public StateAction(String name, Icon icon, String exclusiveGroup, Consumer<ActionEvent> actionListener, Consumer<StateAction> actionUpdater) {
        super(name, icon, actionListener, actionUpdater);
        this.exclusiveGroup = exclusiveGroup;
    }

    /**
     * Checks if the action is currently selected.
     *
     * @return true if the action is selected, false otherwise.
     */
    public boolean isSelected() {
        Object value = getValue(SELECTED_KEY);
        return (value instanceof Boolean) ? (Boolean) value : false;
    }

    /**
     * Sets the selected state of the action.
     *
     * @param newValue true to select the action, false to deselect it.
     */
    public void setSelected(boolean newValue) {
        if (newValue != isSelected()) {
            putValue(SELECTED_KEY, newValue);
        }
    }

    /**
     * Returns the name of the exclusive group this action belongs to.
     *
     * @return The exclusive group name, or {@code null} if not part of an exclusive group.
     */
    public String getExclusiveGroup() {
        return exclusiveGroup;
    }

    /**
     * Sets the name of the exclusive group this action belongs to.
     *
     * @param aExclusiveGroup The name of the exclusive group.
     */
    public void setExclusiveGroup(String aExclusiveGroup) {
        exclusiveGroup = aExclusiveGroup;
    }
}
