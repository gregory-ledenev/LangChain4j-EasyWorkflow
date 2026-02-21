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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * A class that represents a group of actions.
 */
@SuppressWarnings("unused")
public class ActionGroup extends BasicAction implements ActionGroupListener, PropertyChangeListener {
    /**
     * Property name for the default action of the group.
     */
    public static final String PROPERTY_DEFAULT_ACTION = "actionGroupDefaultAction";
    /**
     * Property name for the popup state of the group.
     */
    public static final String PROPERTY_POPUP = "actionGroupPopup";

    private final List<Action> actions;
    private final boolean popup;
    private transient List<ActionGroupListener> actionGroupListeners;
    private transient List<PropertyChangeListener> actionGroupPropertyChangeListeners;
    private final transient PropertyChangeListener actionGroupPropertyChangeListener = this::fireActionGroupPropertyChange;

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

    /**
     * Constructs an empty ActionGroup.
     */
    public ActionGroup() {
        super(null, null, null);
        this.actions = new ArrayList<>();
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
     * Returns the default action for this group.
     *
     * @return The default {@link Action}, or {@code null} if none is set.
     */
    public Action getDefaultAction() {
        return defaultAction;
    }

    private Action defaultAction;

    /**
     * Sets the default action for this group.
     *
     * @param aDefaultAction The action to be set as default.
     */
    public void setDefaultAction(Action aDefaultAction) {
        Action oldValue = defaultAction;
        defaultAction = aDefaultAction;
        if (oldValue != defaultAction)
            firePropertyChange(PROPERTY_DEFAULT_ACTION, oldValue, defaultAction);
    }

    /**
     * Adds an action to this group.
     *
     * @param action The action to add.
     */
    public void addAction(Action action) {
        addAction(action, false);
    }

    /**
     * Adds an action to this group and optionally sets it as the default action.
     *
     * @param action    The action to add.
     * @param isDefault True if this action should become the default action.
     */
    public void addAction(Action action, boolean isDefault) {
        Objects.requireNonNull(action);

        actions.add(action);
        setParent(action, this, BasicAction.getParentActionGroup(action));

        fireActionAdded(action, this);

        if (isDefault)
            setDefaultAction(action);
    }

    /**
     * Inserts an action at the specified position in this group.
     *
     * @param index  The index at which the action is to be inserted.
     * @param action The action to add.
     */
    public void addAction(int index, Action action) {
        addAction(index, action, false);
    }

    /**
     * Inserts an action at the specified position and optionally sets it as the default action.
     *
     * @param index     The index at which the action is to be inserted.
     * @param action    The action to add.
     * @param isDefault True if this action should become the default action.
     */
    public void addAction(int index, Action action, boolean isDefault) {
        Objects.requireNonNull(action);

        actions.add(index, action);
        setParent(action, this, BasicAction.getParentActionGroup(action));

        fireActionAdded(action, this);

        if (isDefault)
            setDefaultAction(action);
    }

    /**
     * Removes an action from this group.
     *
     * @param action The action to remove.
     * @return {@code true} if the action was removed, {@code false} otherwise.
     */
    public boolean removeAction(Action action) {
        Objects.requireNonNull(action);

        ActionGroup oldParent = BasicAction.getParentActionGroup(action);
        boolean result = actions.remove(action);

        if (result) {
            setParent(action, null, oldParent);
            fireActionRemoved(action, this);
        }

        return result;
    }

    /**
     * Adds all actions from the specified collection to this group.
     *
     * @param actions The collection of actions to add.
     */
    public void addAllActions(Collection<Action> actions) {
        Objects.requireNonNull(actions);

        for (Action action : actions) {
            this.actions.add(action);
            setParent(action, this, null);
        }

        fireActionsAdded(actions, this);
    }

    /**
     * Removes all actions from this group.
     */
    public void removeAllActions() {
        List<Action> all = new ArrayList<>(actions);
        actions.clear();
        for (Action action : all)
            setParent(action, null, BasicAction.getParentActionGroup(action));
        fireActionsRemoved(all, this);
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

    private void setParent(Action action, ActionGroup newParent, ActionGroup oldParent) {
        Objects.requireNonNull(action);

        if (action.equals(newParent)) {
            throw new IllegalArgumentException(
                    "It is impossible to set parent of action to itself. The action (" + action + ") and new parent (" + newParent + ") are the same object.");
        }
        if (oldParent != null) {
            if (action instanceof ActionGroup actionGroup) {
                actionGroup.removeActionGroupListener(oldParent);
                actionGroup.removeActionGroupPropertyChangeListener(oldParent);
            } else {
                action.removePropertyChangeListener(oldParent.actionGroupPropertyChangeListener);
            }
        }

        if (newParent != null) {
            if (action instanceof ActionGroup actionGroup) {
                actionGroup.addActionGroupListener(newParent);
                actionGroup.addActionGroupPropertyChangeListener(newParent);
            } else {
                action.addPropertyChangeListener(newParent.actionGroupPropertyChangeListener);
            }
        }

        BasicAction.setParentActionGroup(action, newParent);
    }

    /**
     * Removes an {@link ActionGroupListener} from this group.
     *
     * @param l the listener to be removed
     */
    public synchronized void removeActionGroupListener(ActionGroupListener l) {
        if (actionGroupListeners != null && actionGroupListeners.contains(l)) {
            List<ActionGroupListener> v = new ArrayList<>(actionGroupListeners);
            v.remove(l);
            actionGroupListeners = v;
        }
    }

    /**
     * Adds an {@link ActionGroupListener} to this group.
     *
     * @param l the listener to be added
     */
    public synchronized void addActionGroupListener(ActionGroupListener l) {
        List<ActionGroupListener> v = actionGroupListeners == null ? new ArrayList<>(2) : new ArrayList<>(actionGroupListeners);
        if (!v.contains(l)) {
            v.add(l);
            actionGroupListeners = v;
        }
    }

    protected void fireActionAdded(Action anAction, ActionGroup aSource) {
        if (actionGroupListeners != null)
            for (ActionGroupListener listener : actionGroupListeners)
                listener.actionAdded(anAction, aSource);
    }

    protected void fireActionRemoved(Action anAction, ActionGroup aSource) {
        if (actionGroupListeners != null)
            for (ActionGroupListener listener : actionGroupListeners)
                listener.actionRemoved(anAction, aSource);
    }

    protected void fireActionsAdded(Collection<Action> anActions, ActionGroup aSource) {
        if (actionGroupListeners != null)
            for (ActionGroupListener listener : actionGroupListeners)
                listener.actionsAdded(anActions, aSource);
    }

    protected void fireActionsRemoved(Collection<Action> anActions, ActionGroup aSource) {
        if (actionGroupListeners != null)
            for (ActionGroupListener listener : actionGroupListeners)
                listener.actionsRemoved(anActions, aSource);
    }

    @Override
    public void actionAdded(Action anAction, ActionGroup aSource) {
        fireActionAdded(anAction, aSource);
    }

    @Override
    public void actionRemoved(Action anAction, ActionGroup aSource) {
        fireActionRemoved(anAction, aSource);
    }

    @Override
    public void actionsAdded(Collection<Action> anActions, ActionGroup aSource) {
        fireActionsAdded(anActions, aSource);
    }

    @Override
    public void actionsRemoved(Collection<Action> anActions, ActionGroup aSource) {
        fireActionsRemoved(anActions, aSource);
    }

    /**
     * Forwards property change events from child actions to group listeners.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        fireActionGroupPropertyChange(evt);
    }

    /**
     * Fires a property change event to all registered action group property change listeners.
     *
     * @param e the property change event to fire
     */
    protected void fireActionGroupPropertyChange(PropertyChangeEvent e) {
        if (actionGroupPropertyChangeListeners != null)
            for (PropertyChangeListener l : actionGroupPropertyChangeListeners)
                l.propertyChange(e);
    }

    /**
     * Removes a {@link PropertyChangeListener} from this group.
     *
     * @param l the listener to be removed
     */
    public synchronized void removeActionGroupPropertyChangeListener(PropertyChangeListener l) {
        if (actionGroupPropertyChangeListeners != null && actionGroupPropertyChangeListeners.contains(l)) {
            List<PropertyChangeListener> v = new ArrayList<>(actionGroupPropertyChangeListeners);
            v.remove(l);
            actionGroupPropertyChangeListeners = v;
        }
    }

    /**
     * Adds a {@link PropertyChangeListener} to this group.
     *
     * @param l the listener to be added
     */
    public synchronized void addActionGroupPropertyChangeListener(PropertyChangeListener l) {
        List<PropertyChangeListener> v = actionGroupPropertyChangeListeners == null ? new ArrayList<>(2) : new ArrayList<>(actionGroupPropertyChangeListeners);
        if (!v.contains(l)) {
            v.add(l);
            actionGroupPropertyChangeListeners = v;
        }
    }

    /**
     * Returns the index of the first occurrence of the specified action in this group.
     *
     * @param action The action to search for.
     * @return The index of the action, or -1 if not found.
     */
    public int indexOf(Action action) {
        Objects.requireNonNull(action);

        return actions.indexOf(action);
    }

    /**
     * An iterator that allows traversing actions within an ActionGroup, including nested groups.
     */
    public static class ActionGroupIterator implements java.util.Iterator<Action>, Iterable<Action> {
        protected final ActionGroup actionGroup;
        protected final Stack<ActionGroup> actionGroupStack = new Stack<>();
        protected final Stack<Integer> indexStack = new Stack<>();
        protected Stack<ActionGroup> rootPath;
        protected int currentIndex = 0;
        protected final boolean includeActionGroups;
        protected ActionGroup groupToEnter;

        protected Action fNext;

        /**
         * Creates an iterator for the specified ActionGroup.
         *
         * @param anActionGroup The group to iterate over.
         */
        public ActionGroupIterator(ActionGroup anActionGroup) {
            this(anActionGroup, true);
        }

        /**
         * Creates an iterator for the specified ActionGroup.
         *
         * @param anActionGroup        The group to iterate over.
         * @param anIncludeActionGroups Whether to include ActionGroup objects themselves in the iteration.
         */
        public ActionGroupIterator(ActionGroup anActionGroup, boolean anIncludeActionGroups) {
            this.actionGroup = anActionGroup;
            includeActionGroups = anIncludeActionGroups;
            actionGroupStack.push(this.actionGroup);
            findNext();
        }

        /**
         * Returns the current ActionGroup being traversed.
         *
         * @return The current {@link ActionGroup}.
         */
        public ActionGroup getCurrentActionGroup() {
            return getRootPath().peek();
        }

        /**
         * Returns the current nesting depth of the iteration.
         *
         * @return The indentation level.
         */
        public int getIndent() {
            return getRootPath().size() - 1;
        }

        @Override
        public ActionGroupIterator iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return fNext != null;
        }

        /**
         * Returns the stack representing the path from the root group to the current action.
         *
         * @return A stack of {@link ActionGroup} objects.
         */
        public Stack<ActionGroup> getRootPath() {
            return rootPath;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Action next() {
            Action result = fNext;
            rootPath = (Stack<ActionGroup>) actionGroupStack.clone();
            findNext();
            return result;
        }

        protected Action findNext() {
            fNext = null;

            if (groupToEnter != null) {
                indexStack.push(currentIndex);
                currentIndex = 0;
                actionGroupStack.push(groupToEnter);
                groupToEnter = null;
            }

            while (fNext == null) {
                if (actionGroupStack.isEmpty()) {
                    return null;
                }

                ActionGroup parent = actionGroupStack.peek();
                List<Action> actions = parent.getActions();

                if (currentIndex >= actions.size()) {
                    actionGroupStack.pop();
                    if (actionGroupStack.isEmpty()) {
                        return null;
                    }
                    currentIndex = indexStack.pop();
                    continue;
                }

                Action current = actions.get(currentIndex);

                if (current instanceof ActionGroup) {
                    if (includeActionGroups) {
                        groupToEnter = (ActionGroup) current;
                        currentIndex++;
                        fNext = current;
                        return fNext;
                    } else {
                        currentIndex++;
                        indexStack.push(currentIndex);
                        currentIndex = 0;
                        actionGroupStack.push((ActionGroup) current);
                        continue;
                    }
                }

                currentIndex++;
                fNext = current;
                return fNext;
            }

            return fNext;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }
    }
}
