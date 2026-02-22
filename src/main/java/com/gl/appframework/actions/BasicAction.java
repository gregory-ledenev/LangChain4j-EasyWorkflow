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

import com.gl.appframework.UISupport;
import com.gl.appframework.Updatable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * An abstract base class for actions that perform a specific task.
 */
@SuppressWarnings("unused")
public class BasicAction extends AbstractAction implements Updatable {
    public static final String MENU_BAR_ITEM_NAME = "menuBarItemName";
    public static final String ID = "id";
    public static final String PARENT_ACTION_GROUP = "parentActionGroup";
    public static final String COPY_NAME = "copyName";
    public static final String DISABLE_REASON = "disableReason";
    public static final String VISIBLE = "actionVisible";
    public static final String COMPONENT_ACTION = "componentAction";
    private final Consumer<ActionEvent> actionListener;
    private final Consumer<? extends BasicAction> actionUpdater;
    private long when;

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
        setShortDescription(name);
    }

    private boolean isMenuBarSource(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem menuItem) {
            Component parent = menuItem.getParent();
            while (parent != null) {
                if (parent instanceof JMenuBar) return true;
                if (parent instanceof JPopupMenu popupMenu)
                    parent = popupMenu.getInvoker();
                parent = parent.getParent();
            }
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isMenuBarSource(e) && UISupport.isMac())
            macMenuBarActionPerformed(e);
        else
            defaultActionPerformed(e);
    }

    private void macMenuBarActionPerformed(ActionEvent e) {
        if (e.getWhen() > when) {
            SwingUtilities.invokeLater(() -> defaultActionPerformed(e));
        }
        when = e.getWhen();
    }

    protected void defaultActionPerformed(ActionEvent e) {
        if (actionListener == null)
            return;

        update();
        if (isEnabled()) {
            actionListener.accept(e);
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
     * Returns the name of the action.
     *
     * @return The name of the action.
     */
    public String getName() {
        return (String) getValue(Action.NAME);
    }

    /**
     * Sets the name of the action.
     *
     * @param name The name of the action.
     */
    public void setName(String name) {
        putValue(Action.NAME, name);
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

    /**
     * Returns the long description for the action.
     *
     * @return The long description text.
     */
    public String getLongDescription() {
        return (String) getValue(Action.LONG_DESCRIPTION);
    }

    /**
     * Sets the long description for the action.
     *
     * @param text The long description text.
     */
    public void setLongDescription(String text) {
        putValue(Action.LONG_DESCRIPTION, text);
    }

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

    /**
     * Returns the reason why this action is disabled.
     *
     * @return The disable reason text, or {@code null} if not set.
     */
    public String getDisableReason() {
        return (String) getValue(DISABLE_REASON);
    }

    /**
     * Sets the reason why this action is disabled.
     *
     * @param disableReason The text explaining why the action is disabled.
     */
    public void setDisableReason(String disableReason) {
        putValue(DISABLE_REASON, disableReason);
    }

    /**
     * Returns the ID of the action.
     *
     * @return The ID of the action.
     */
    public String getId() {
        String result = (String) getValue(ID);
        return result != null ? result : getName();
    }

    /**
     * Sets the ID of the action.
     *
     * @param id The ID of the action.
     */
    public void setId(String id) {
        putValue(ID, id);
    }

    /**
     * Returns the parent action group of this action.
     *
     * @return The {@link ActionGroup} that contains this action.
     */
    public ActionGroup getParentActionGroup() {
        return (ActionGroup) getValue(PARENT_ACTION_GROUP);
    }

    /**
     * Sets the parent action group for this action.
     *
     * @param parent The {@link ActionGroup} that contains this action.
     */
    public void setParentActionGroup(ActionGroup parent) {
        putValue(PARENT_ACTION_GROUP, parent);
    }

    /**
     * Returns the parent action group of the specified action.
     *
     * @param action The action to query.
     * @return The {@link ActionGroup} associated with the action.
     */
    public static ActionGroup getParentActionGroup(Action action) {
        return (ActionGroup) Objects.requireNonNull(action).getValue(PARENT_ACTION_GROUP);
    }

    /**
     * Sets the parent action group for the specified action.
     *
     * @param action The action to update.
     * @param parent The {@link ActionGroup} to associate with the action.
     */
    public static void setParentActionGroup(Action action, ActionGroup parent) {
        Objects.requireNonNull(action).putValue(PARENT_ACTION_GROUP, parent);
    }

    /**
     * Returns whether the action is visible.
     *
     * @return {@code true} if the action is visible, {@code false} otherwise.
     */
    public boolean isVisible() {
        return isVisible(this);
    }

    /**
     * Sets the visibility of the action.
     *
     * @param visible {@code true} to make the action visible, {@code false} to hide it.
     */
    public void setVisible(boolean visible) {
        setVisible(this, visible);
    }

    /**
     * Returns whether the specified action is visible.
     *
     * @param anAction The action to query.
     * @return {@code true} if the action is visible, {@code false} otherwise.
     */
    public static boolean isVisible(javax.swing.Action anAction) {
        Object value = anAction.getValue(VISIBLE);
        return value == null || Boolean.TRUE.equals(value);
    }

    /**
     * Sets the visibility of the specified action.
     *
     * @param anAction The action to update.
     * @param visible  {@code true} to make the action visible, {@code false} to hide it.
     */
    public static void setVisible(javax.swing.Action anAction, boolean visible) {
        if (isVisible(anAction) != visible)
            anAction.putValue(VISIBLE, visible ? null : Boolean.FALSE);
    }

    /**
     * Retrieves the {@link Action} associated with a given {@link JComponent}.
     *
     * @param c The component to check.
     * @return The associated action, or {@code null} if none is found.
     */
    public static Action getActionForComponent(JComponent c) {
        if (c instanceof AbstractButton)
            return ((AbstractButton) c).getAction();
        else
            return (Action) c.getClientProperty(COMPONENT_ACTION);
    }
}
