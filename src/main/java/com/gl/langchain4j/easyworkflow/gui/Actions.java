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

package com.gl.langchain4j.easyworkflow.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class Actions {
    /**
     * An abstract base class for actions that perform a specific task.
     */
    public static class BasicAction extends AbstractAction {
        private final Consumer<ActionEvent> actionListener;

        /**
         * Constructs a new BasicAction.
         *
         * @param name           The name of the action, used for display purposes.
         * @param icon           The icon to be displayed with the action.
         * @param actionListener The consumer that will be invoked when the action is performed.
         */
        public BasicAction(String name, Icon icon, Consumer<ActionEvent> actionListener) {
            super(name, icon);
            this.actionListener = actionListener;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (actionListener != null)
                actionListener.accept(e);
        }

        /**
         * Returns the short description (tooltip) for the action.
         *
         * @return The short description text.
         */
        public String getShortDescription() {
            return (String) getValue(Action.SHORT_DESCRIPTION);
        }

        /**
         * Sets the short description (tooltip) for the action.
         *
         * @param text The short description text.
         */
        public void setShortDescription(String text) {
            putValue(Action.SHORT_DESCRIPTION, text);
        }

        static final String COPY_NAME = "copyName";

        public boolean isCopyName() {
            return Boolean.TRUE.equals(getValue(COPY_NAME));
        }

        public void setCopyName(boolean isCopyName) {
            putValue(COPY_NAME, isCopyName);
        }
    }

    /**
     * An abstract base class for actions that represent a state (e.g., toggle actions).
     */
    public static class StateAction extends BasicAction {
        private String exclusiveGroup;

        /**
         * Constructs a new StateAction.
         *
         * @param name           The name of the action, used for display purposes.
         * @param icon           The icon to be displayed with the action.
         * @param actionListener The consumer that will be invoked when the action is performed.
         */
        public StateAction(String name, Icon icon, Consumer<ActionEvent> actionListener) {
            super(name, icon, actionListener);
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

    /**
     * A class that represents a group of actions.
     */
    public static class ActionGroup extends BasicAction {
        private final Action[] actions;
        private final boolean popup;

        /**
         * Constructs a new ActionGroup.
         *
         * @param actions An array of actions contained within this group.
         */
        public ActionGroup(Action... actions) {
            this(null, null, false, actions);
        }

        /**
         * Constructs a new ActionGroup.
         *
         * @param name    The name of the action group.
         * @param icon    The icon for the action group.
         * @param popup   True if this action group should be displayed as a popup menu, false otherwise.
         * @param actions An array of actions contained within this group.
         */
        public ActionGroup(String name, Icon icon, boolean popup, Action... actions) {
            super(name, icon, null);
            this.actions = actions;
            this.popup = popup;
        }

        /**
         * Returns the array of actions in this group.
         *
         * @return An array of {@link Action} objects.
         */
        public Action[] getActions() {
            return actions;
        }

        /**
         * Checks if this action group is configured to be displayed as a popup menu.
         *
         * @return {@code true} if the group is a popup, {@code false} otherwise.
         */
        public boolean isPopup() {
            return popup;
        }
    }
}
