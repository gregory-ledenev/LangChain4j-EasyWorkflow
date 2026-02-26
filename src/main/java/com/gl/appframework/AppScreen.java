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

package com.gl.appframework;

import com.gl.appframework.actions.BasicAction;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * Represents a screen or view within the application framework.
 * An AppScreen is a specialized module that can be activated, passivated,
 * and displayed to the user.
 */
public interface AppScreen<T extends AppFrame> extends AppModule<T> {

    /**
     * Returns the visual component associated with this screen.
     * @return the JComponent to be displayed in the UI.
     */
    JComponent getComponent();

    /**
     * @return A short, human-readable description of the screen (e.g., for tooltips).
     */
    String getShortDescription();

    /**
     * @return A detailed description of the screen's purpose or functionality.
     */
    String getLongDescription();

    /**
     * @return A message explaining why the screen is currently disabled, or null if enabled.
     */
    String getDisableReason();

    /**
     * @return The standard icon representing this screen.
     */
    Icon getIcon();

    /**
     * @return The selected standard icon representing this screen.
     */
    Icon getSelectedIcon();

    /**
     * @return The icon to display when the mouse hovers over the screen's trigger.
     */
    Icon getRolloverIcon();

    /**
     * @return A high-resolution icon for large displays or prominent UI elements.
     */
    Icon getLargeIcon();

    /**
     * @return A high-resolution selected icon for large displays or prominent UI elements.
     */
    Icon getLargeSelectedIcon();

    /**
     * @return The large icon to display during mouse rollover.
     */
    Icon getLargeRolloverIcon();

    /**
     * Checks if the screen can currently be activated.
     * @return true if activation is allowed.
     */
    boolean canActivate();

    /**
     * Performs the logic required to make this screen the active view.
     */
    void activate();

    /**
     * Checks if the screen can currently be passivated (deactivated).
     * @return true if the screen can be safely hidden or backgrounded.
     */
    boolean canPassivate();

    /**
     * Performs the logic required when the screen is no longer the active view.
     */
    void passivate();

    /**
     * @return true if the screen is interactive and available for selection.
     */
    boolean isEnabled();

    /**
     * Sets whether the screen is enabled.
     * @param anEnabled true to enable, false to disable.
     */
    void setEnabled(boolean anEnabled);

    /**
     * Adds a listener to track changes to screen properties (e.g., enabled state).
     * @param l the listener to add.
     */
    void addPropertyChangeListener(PropertyChangeListener l);

    /**
     * Removes a previously registered property change listener.
     * @param l the listener to remove.
     */
    void removePropertyChangeListener(PropertyChangeListener l);

    /**
     * Returns an Action that, when triggered, will activate this screen via the provided manager.
     *
     * @return an Action object for UI integration (e.g., buttons, menu items).
     */
    BasicAction getActivationAction();
}