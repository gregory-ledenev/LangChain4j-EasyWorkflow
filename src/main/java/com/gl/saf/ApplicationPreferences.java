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

package com.gl.saf;

import org.slf4j.Logger;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Manages user preferences for the application.
 */
public class ApplicationPreferences {
    protected static final Logger logger = LoggerFactory.getLogger(ApplicationPreferences.class);
    public static final String PROP_APPEARANCE = "appearance";
    public static final String PROP_FRAME_BOUNDS = "frameBounds";
    public static final String PROP_OPEN_FILE_AFTER_EXPORTING = "openFileAfterSharing";

    protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    protected Preferences getPreferences() {
        return Application.getUserPreferences();
    }

    /**
     * Gets the singleton instance of {@link ApplicationPreferences}.
     *
     * @return The {@link ApplicationPreferences} instance.
     */
    public static ApplicationPreferences getApplicationPreferences() {
        return Application.getSharedApplication().getApplicationPreferences();
    }

    /**
     * Gets the currently selected appearance (Light, Dark, Auto).
     *
     * @return The current appearance setting.
     */
    public Appearance.Type getAppearance() {
        return Appearance.Type.values()[getPreferences().getInt(PROP_APPEARANCE, Appearance.Type.Auto.ordinal())];
    }

    /**
     * Sets the application's appearance.
     *
     * @param appearance The desired appearance setting.
     */
    public void setAppearance(Appearance.Type appearance) {
        if (getAppearance() != appearance) {
            Appearance.Type oldValue = getAppearance();
            getPreferences().putInt(PROP_APPEARANCE, appearance.ordinal());
            propertyChangeSupport.firePropertyChange(PROP_APPEARANCE, oldValue, appearance);
        }
    }

    /**
     * Retrieves the stored bounds of the main application frame.
     *
     * @return A {@link Rectangle} representing the frame's bounds, or null if not found.
     */
    public Rectangle getFrameBounds() {
        String bounds = getPreferences().get(PROP_FRAME_BOUNDS, null);
        if (bounds != null) {
            String[] wh = bounds.split(", ");
            try {
                return new Rectangle(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]),
                        Integer.parseInt(wh[2]), Integer.parseInt(wh[3]));
            } catch (NumberFormatException e) {
                logger.error("failed to read frame bounds", e);
            }
        }
        return null;

    }

    /**
     * Stores the bounds of the main application frame.
     *
     * @param frameBounds The {@link Rectangle} representing the frame's current bounds.
     */
    public void setFrameBounds(Rectangle frameBounds) {
        if (!Objects.equals(getFrameBounds(), frameBounds)) {
            getPreferences().put(PROP_FRAME_BOUNDS, "%s, %s, %s, %s".formatted(frameBounds.x, frameBounds.y, frameBounds.width, frameBounds.height));
            propertyChangeSupport.firePropertyChange(PROP_FRAME_BOUNDS, getFrameBounds(), frameBounds);
        }
    }

    /**
     * Checks if the "open file after exporting" option is enabled.
     *
     * @return true if the exported file should be opened automatically, false otherwise.
     */
    public boolean isOpenFileAfterSharing() {
        return getPreferences().getBoolean(PROP_OPEN_FILE_AFTER_EXPORTING, false);
    }

    /**
     * Sets whether the exported file should be opened automatically.
     *
     * @param value true to enable opening after exporting, false to disable.
     */
    public void setOpenFileAfterSharing(boolean value) {
        if (isOpenFileAfterSharing() != value) {
            getPreferences().putBoolean(PROP_OPEN_FILE_AFTER_EXPORTING, value);
            propertyChangeSupport.firePropertyChange(PROP_OPEN_FILE_AFTER_EXPORTING, !value, value);
        }
    }

    /**
     * Adds a {@link PropertyChangeListener} to the listener list. The listener is registered for all properties.
     *
     * @param propertyChangeListener The {@link PropertyChangeListener} to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Removes a {@link PropertyChangeListener} from the listener list.
     *
     * @param propertyChangeListener The {@link PropertyChangeListener} to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }
}
