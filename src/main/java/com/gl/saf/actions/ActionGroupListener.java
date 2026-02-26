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
package com.gl.saf.actions;

import javax.swing.*;
import java.util.Collection;
import java.util.EventListener;

/**
 * Listener interface for receiving notifications when actions are added to or removed from an {@link ActionGroup}.
 */
public interface ActionGroupListener extends EventListener {
    /**
     * Invoked when a single action has been added to the action group.
     *
     * @param action      the action that was added
     * @param actionGroup the group to which the action was added
     */
    void actionAdded(Action action, ActionGroup actionGroup);

    /**
     * Invoked when a single action has been removed from the action group.
     *
     * @param action      the action that was removed
     * @param actionGroup the group from which the action was removed
     */
    void actionRemoved(Action action, ActionGroup actionGroup);

    /**
     * Invoked when multiple actions have been added to the action group.
     *
     * @param actions     the collection of actions that were added
     * @param actionGroup the group to which the actions were added
     */
    void actionsAdded(Collection<Action> actions, ActionGroup actionGroup);

    /**
     * Invoked when multiple actions have been removed from the action group.
     *
     * @param actions     the collection of actions that were removed
     * @param actionGroup the group from which the actions were removed
     */
    void actionsRemoved(Collection<Action> actions, ActionGroup actionGroup);
}