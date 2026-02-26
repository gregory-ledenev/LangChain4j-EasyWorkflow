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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.jthemedetecor.OsThemeDetector;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import static com.gl.saf.ApplicationPreferences.getApplicationPreferences;
import static com.gl.saf.UISupport.isMacOS;

/**
 * Manages the visual appearance and Look and Feel (LaF) of the application.
 * This class handles switching between light and dark themes, supports system theme detection,
 * and notifies listeners of appearance changes.
 */
public class Appearance {

    final static OsThemeDetector osThemeDetector = OsThemeDetector.getDetector();

    private static Boolean darkAppearance;
    private static final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(new Object());

    /**
     * Enum representing the different appearance options for the application.
     */
    public enum Type {
        Light, Dark, Auto
    }

    /**
     * Applies the appearance settings based on the current options. This method will set the look and feel to light,
     * dark, or auto-detect.
     */
    public static void applyAppearance() {
        switch (getApplicationPreferences().getAppearance()) {
            case Light -> setDarkAppearance(false);
            case Dark -> setDarkAppearance(true);
            case Auto -> setDarkAppearance(osThemeDetector.isDark());
        }

        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.revalidate();
            window.repaint();
        }

        firePropertyChange(PROP_APPEARANCE_DARK, !darkAppearance, darkAppearance);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the listener list. The listener is registered for all properties.
     *
     * @param listener The {@link PropertyChangeListener} to be added.
     */
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link PropertyChangeListener} from the listener list.
     *
     * @param listener The {@link PropertyChangeListener} to be removed.
     */
    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private static void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Applies a specific appearance setting and updates the options.
     *
     * @param appearance The desired {@link Appearance.Type} to apply.
     */
    public static void applyAppearance(Appearance.Type appearance) {
        getApplicationPreferences().setAppearance(appearance);
        applyAppearance();
        boolean dark = osThemeDetector.isDark();
        firePropertyChange(PROP_APPEARANCE_DARK, !dark, dark);
    }

    public static final String PROP_APPEARANCE_DARK = "appearanceDark";

    /**
     * Checks if the current appearance is dark.
     *
     * @return true if the dark appearance is active, false otherwise.
     */
    public static boolean isDarkAppearance() {
        return darkAppearance != null ? darkAppearance : false;
    }

    /**
     * Sets the application's look and feel to dark or light.
     *
     * @param isDarkAppearance true for dark appearance, false for light appearance.
     */
    public static void setDarkAppearance(boolean isDarkAppearance) {
        if (darkAppearance == null || darkAppearance != isDarkAppearance) {
            darkAppearance = isDarkAppearance;
            try {
                boolean isMac = isMacOS();
                UIManager.setLookAndFeel(darkAppearance ?
                        (isMac ? new FlatMacDarkLaf() : new FlatDarkLaf()) :
                        (isMac ? new FlatMacLightLaf() : new FlatLightLaf()));
                if (isMac) {
                    UIManager.put("ToggleButton.toolbar.selectedBackground",
                            darkAppearance ? Color.DARK_GRAY : new Color(216, 216, 255));
                }
            } catch (Exception ex) {
                System.err.println("Failed to initialize LaF");
            }
        }
    }

    /**
     * Retrieves the singleton instance of {@link OsThemeDetector}. This detector can be used to determine the operating
     * system's current theme (light or dark) and to listen for theme changes.
     *
     * @return The {@link OsThemeDetector} instance.
     */
    public static OsThemeDetector getAppearanceDetector() {
        return osThemeDetector;
    }
}
