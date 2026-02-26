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
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * An action that delegates its behavior to another action found in the ActionMap of the focused
 * component or one of its ancestors. This is useful for global actions (like Cut, Copy, Paste)
 * that should behave differently
 * depending on which component currently has focus.
 */
public class DelegateAction extends BasicAction {

    /**
     * Creates a new DelegateAction.
     * @param name The name of the action, used to look up the delegate in the ActionMap.
     * @param icon The icon to display for this action.
     */
    public DelegateAction(String id, String name, Icon icon) {
        super(name, icon, null);

        setId(id);
    }

    @Override
    public void update() {
        DelegateActionResult delegateAction = getDelegateAction();
        if (delegateAction != null && delegateAction.action() instanceof BasicAction basicAction)
            basicAction.update();
        setEnabled(delegateAction != null && delegateAction.action().isEnabled());
    }

    @Override
    protected void defaultActionPerformed(ActionEvent e) {
        DelegateActionResult delegateAction = getDelegateAction();
        if (delegateAction != null)
            delegateAction.action().actionPerformed(new ActionEvent(delegateAction.component, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers()));
    }

    record DelegateActionResult(Action action, JComponent component) {}

    private DelegateActionResult getDelegateAction() {
        Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        while (component != null) {
            if (component instanceof JComponent jc) {
                Action action = jc.getActionMap().get(getId());
                if (action != null)
                    return new DelegateActionResult(action, jc);
            }
            component = component.getParent();
        }
        return null;
    }
}
