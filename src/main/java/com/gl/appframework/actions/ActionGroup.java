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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class that represents a group of actions.
 */
@SuppressWarnings("unused")
public class ActionGroup extends BasicAction {
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
