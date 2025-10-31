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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Actions {
    /**
     * An abstract base class for actions that perform a specific task.
     */
    public static class BasicAction extends AbstractAction {
        private final Consumer<ActionEvent> actionListener;
        private final Consumer<? extends BasicAction> actionUpdater;


        /**
         * Constructs a new BasicAction.
         *
         * @param name           The name of the action, used for display purposes.
         * @param icon           The icon to be displayed with the action.
         * @param actionListener The consumer that will be invoked when the action is performed.
         */
        public BasicAction(String name, Icon icon, Consumer<ActionEvent> actionListener) {
            this(name, icon, actionListener, null);
        }

        /**
         * Constructs a new BasicAction.
         *
         * @param name           The name of the action, used for display purposes.
         * @param icon           The icon to be displayed with the action.
         * @param actionListener The consumer that will be invoked when the action is performed.
         * @param actionUpdater  The consumer that will be invoked when the action needs to be updated.
         */
        public BasicAction(String name, Icon icon, Consumer<ActionEvent> actionListener, Consumer<? extends BasicAction> actionUpdater) {
            super(name, icon);
            this.actionListener = actionListener;
            this.actionUpdater = actionUpdater;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (actionListener != null) {
                actionListener.accept(e);
                update();
            }
        }

        /**
         * Updates the action by invoking its action updater
         */
        public void update() {
            if (actionUpdater != null)
                //noinspection unchecked
                ((Consumer<BasicAction>) actionUpdater).accept(this);
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

        /**
         * Returns the mnemonic (keyboard shortcut) for the action.
         *
         * @return The mnemonic key code.
         */
        public int getMnemonic() {
            Object value = getValue(Action.MNEMONIC_KEY);
            return (value instanceof Integer) ? (Integer) value : 0;
        }

        /**
         * Sets the mnemonic (keyboard shortcut) for the action.
         *
         * @param mnemonic The mnemonic key code.
         */
        public void setMnemonic(int mnemonic) {
            putValue(Action.MNEMONIC_KEY, mnemonic);
        }

        /**
         * Returns the accelerator (keyboard shortcut) for the action.
         *
         * @return The {@link KeyStroke} representing the accelerator.
         */
        public KeyStroke getAccelerator() {
            return (KeyStroke) getValue(Action.ACCELERATOR_KEY);
        }

        /**
         * Sets the accelerator (keyboard shortcut) for the action.
         *
         * @param accelerator The {@link KeyStroke} representing the accelerator.
         */
        public void setAccelerator(KeyStroke accelerator) {
            putValue(Action.ACCELERATOR_KEY, accelerator);
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

    /**
     * A class that represents a group of actions.
     */
    public static class ActionGroup extends BasicAction {
        private final List<Action> actions;
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
            this.actions = new ArrayList<>(Arrays.asList(actions));
            this.popup = popup;
        }

        public ActionGroup() {
            super(null, null, null);
            this.actions = List.of();
            this.popup = false;
        }

        /**
         * Returns the list of actions in this group.
         *
         * @return A list of {@link Action} objects.
         */
        public List<Action> getActions() {
            return Collections.unmodifiableList(actions);
        }

        /**
         * Adds an action to this group.
         *
         * @param action The action to add.
         */
        public void addAction(Action action) {
            actions.add(action);
        }

        /**
         * Removes an action from this group.
         *
         * @param action The action to remove.
         */
        public void removeAction(Action action) {
            actions.remove(action);
        }

        /**
         * Checks if this action group is configured to be displayed as a popup menu.
         *
         * @return {@code true} if the group is a popup, {@code false} otherwise.
         */
        public boolean isPopup() {
            return popup;
        }

        @Override
        public void update() {
            super.update();
            for (Action action : actions)
                if (action instanceof BasicAction basicAction)
                    basicAction.update();
        }
    }
}
