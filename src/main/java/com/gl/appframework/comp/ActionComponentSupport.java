package com.gl.appframework.comp;

import com.gl.appframework.actions.*;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A support class for UI components that display a collection of actions from an {@link ActionGroup}.
 * It handles synchronization between the action group model and the UI component.
 *
 * @param <T> the type of JComponent being supported
 */
public class ActionComponentSupport<T extends JComponent>
        implements ActionGroupListener, PropertyChangeListener {

    private final T component;
    private ActionGroup actionGroup;
    private boolean rebuildLaterScheduled;
    private boolean updateSeparatorsVisibilityScheduled;
    private final Consumer<T> updateSeparatorsVisibilityDelegate;
    private final Consumer<T> rebuildDelegate;

    /**
     * Constructs a new ActionComponentSupport for the specified component.
     *
     * @param component the UI component to be managed
     */
    public ActionComponentSupport(T component, Consumer<T> rebuildDelegate, Consumer<T> updateSeparatorsVisibilityDelegate) {
        this.component = component;
        this.updateSeparatorsVisibilityDelegate = Objects.requireNonNull(updateSeparatorsVisibilityDelegate);
        this.rebuildDelegate = Objects.requireNonNull(rebuildDelegate);
    }

    /**
     * @return the UI component managed by this support class
     */
    public T getComponent() {
        return component;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(BasicAction.VISIBLE)) {
            updateSeparatorsVisibilityLater();
        }
    }

    /**
     * Returns the {@link ActionGroup} associated with this toolbar.
     *
     * @return the current action group
     */
    public ActionGroup getActionGroup() {
        return actionGroup;
    }

    /**
     * Sets the {@link ActionGroup} for this toolbar and refreshes the UI components.
     *
     * @param actionGroup the new action group to display
     */
    public void setActionGroup(ActionGroup actionGroup) {
        if (this.actionGroup != null) {
            this.actionGroup.removeActionGroupListener(this);
            this.actionGroup.removeActionGroupPropertyChangeListener(this);
            component.removeAll();
        }

        this.actionGroup = actionGroup;

        if (actionGroup != null) {
            actionGroup.addActionGroupListener(this);
            actionGroup.addActionGroupPropertyChangeListener(this);
            rebuild();
        }
    }

    @Override
    public void actionAdded(Action action, ActionGroup actionGroup) {
        rebuildLater();
    }

    @Override
    public void actionRemoved(Action action, ActionGroup actionGroup) {
        rebuildLater();
    }

    @Override
    public void actionsAdded(Collection<Action> actions, ActionGroup actionGroup) {
        rebuildLater();
    }

    @Override
    public void actionsRemoved(Collection<Action> actions, ActionGroup actionGroup) {
        rebuildLater();
    }

    private void rebuildLater() {
        if (rebuildLaterScheduled)
            return;

        rebuildLaterScheduled = true;
        SwingUtilities.invokeLater(() -> {
            rebuildLaterScheduled = false;
            rebuild();
        });
    }

    private void updateSeparatorsVisibilityLater() {
        if (updateSeparatorsVisibilityScheduled)
            return;

        updateSeparatorsVisibilityScheduled = true;
        SwingUtilities.invokeLater(() -> {
            updateSeparatorsVisibilityScheduled = false;
            updateSeparatorsVisibility();
        });
    }

    protected void updateSeparatorsVisibility() {
        updateSeparatorsVisibilityDelegate.accept(component);
    }

    protected void rebuild() {
        rebuildDelegate.accept(component);
    }
}
