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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.jthemedetecor.OsThemeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Provides utility methods and constants for UI-related operations, including icon management, theme handling, and user
 * preferences.
 */
public class UISupport {
    private static final Logger logger = LoggerFactory.getLogger(UISupport.class);

    private static Boolean darkAppearance;
    private static Options options;
    private static final Map<String, Icon> icons = new HashMap<>();

    /**
     * Key for the copy icon.
     */
    public static final String ICON_COPY = "copy";
    /**
     * Key for the paste icon.
     */
    public static final String ICON_PASTE = "paste";
    /**
     * Key for the document icon.
     */
    public static final String ICON_DOCUMENT = "document";
    /**
     * Key for the send icon.
     */
    public static final String ICON_SEND = "send";
    /**
     * Key for the settings icon.
     */
    public static final String ICON_SETTINGS = "settings";
    /**
     * Key for the clear icon.
     */
    public static final String ICON_CLEAR = "clear";
    /**
     * Key for the bulb icon.
     */
    public static final String ICON_BULB = "bulb";
    /**
     * Key for the help icon.
     */
    public static final String ICON_HELP = "help";
    /**
     * Key for the expert icon.
     */
    public static final String ICON_EXPERT = "expert";

    /**
     * An Icon implementation that automatically loads the correct icon based on the current theme (light/dark).
     */
    public static class AutoIcon implements Icon {
        private final String key;

        public AutoIcon(String aKey) {
            key = aKey;
        }

        public Icon getIcon() {
            return UISupport.getIcon(key);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            getIcon().paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return getIcon().getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return getIcon().getIconHeight();
        }
    }

    static {
        icons.put(getIconKey(ICON_COPY, false), loadImageIcon("copy"));
        icons.put(getIconKey(ICON_COPY, true), loadImageIcon("copy-light"));

        icons.put(getIconKey(ICON_PASTE, false), loadImageIcon("paste"));
        icons.put(getIconKey(ICON_PASTE, true), loadImageIcon("paste-light"));

        icons.put(getIconKey(ICON_DOCUMENT, false), loadImageIcon("document"));
        icons.put(getIconKey(ICON_DOCUMENT, true), loadImageIcon("document-light"));

        icons.put(getIconKey(ICON_SEND, false), loadImageIcon("send"));
        icons.put(getIconKey(ICON_SEND, true), loadImageIcon("send-light"));

        icons.put(getIconKey(ICON_SETTINGS, false), loadImageIcon("settings"));
        icons.put(getIconKey(ICON_SETTINGS, true), loadImageIcon("settings-light"));

        icons.put(getIconKey(ICON_CLEAR, false), loadImageIcon("clear"));
        icons.put(getIconKey(ICON_CLEAR, true), loadImageIcon("clear-light"));

        icons.put(getIconKey(ICON_BULB, false), loadImageIcon("bulb"));
        icons.put(getIconKey(ICON_BULB, true), loadImageIcon("bulb-light"));

        icons.put(getIconKey(ICON_HELP, false), loadImageIcon("help"));
        icons.put(getIconKey(ICON_HELP, true), loadImageIcon("help-light"));

        icons.put(getIconKey(ICON_EXPERT, false), loadImageIcon("expert"));
        icons.put(getIconKey(ICON_EXPERT, true), loadImageIcon("expert-light"));
    }

    private static ImageIcon loadImageIcon(String name) {
        return new ImageIcon(new BaseMultiResolutionImage(
                new ImageIcon(Objects.requireNonNull(UISupport.class.getResource(name + ".png"))).getImage(),
                new ImageIcon(Objects.requireNonNull(UISupport.class.getResource(name + "@2x.png"))).getImage()));
    }

    /**
     * Manages user preferences for the application.
     */
    public static class Options {
        static final String PROP_RENDER_MARKDOWN = "renderMarkdown";
        static final String PROP_APPEARANCE = "appearance";
        static final String PROP_FRAME_BOUNDS = "frameBounds";
        static final String PROP_CLEAR_AFTER_SENDING = "clearAfterSending";

        /**
         * Checks if markdown rendering is enabled.
         *
         * @return true if markdown rendering is enabled, false otherwise.
         */
        public boolean isRenderMarkdown() {
            return getPreferences().getBoolean(PROP_RENDER_MARKDOWN, true);
        }

        /**
         * Sets whether markdown rendering should be enabled.
         *
         * @param renderMarkdown true to enable markdown rendering, false to disable.
         */
        public void setRenderMarkdown(boolean renderMarkdown) {
            getPreferences().putBoolean(PROP_RENDER_MARKDOWN, renderMarkdown);
        }

        /**
         * Gets the currently selected appearance (Light, Dark, Auto).
         *
         * @return The current appearance setting.
         */
        public Appearance getAppearance() {
            return Appearance.values()[getPreferences().getInt(PROP_APPEARANCE, Appearance.Auto.ordinal())];
        }

        /**
         * Sets the application's appearance.
         *
         * @param appearance The desired appearance setting.
         */
        public void setAppearance(Appearance appearance) {
            getPreferences().putInt(PROP_APPEARANCE, appearance.ordinal());
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
            getPreferences().put(PROP_FRAME_BOUNDS, "%s, %s, %s, %s".formatted(frameBounds.x, frameBounds.y, frameBounds.width, frameBounds.height));
        }

        /**
         * Checks if the "clear after sending" option is enabled.
         *
         * @return true if the user message form should be cleared after sending, false otherwise.
         */
        public boolean isClearAfterSending() {
            return getPreferences().getBoolean(PROP_CLEAR_AFTER_SENDING, true);
        }

        /**
         * Sets whether the user message form should be cleared after sending.
         *
         * @param isCleaAfterSending true to enable clearing after sending, false to disable.
         */
        public void setClearAfterSending(boolean isCleaAfterSending) {
            getPreferences().putBoolean(PROP_CLEAR_AFTER_SENDING, isCleaAfterSending);
        }
    }

    /**
     * Creates an {@link Action} with a title, icon, and an action listener.
     *
     * @param title The title of the action.
     * @param icon The icon for the action.
     * @param actionListener The consumer to be called when the action is performed.
     * @return A new {@link Action} instance.
     */
    public static Action createAction(String title, Icon icon,
                                      Consumer<ActionEvent> actionListener) {
        return new AbstractAction(title, icon) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionListener != null)
                    actionListener.accept(e);
            }
        };

    }

    /**
     * Retrieves the user preferences node for the application.
     *
     * @return The {@link Preferences} object for the application.
     */
    public static Preferences getPreferences() {
        return Preferences.userRoot().node(ChatPane.class.getName().replace(".", "/"));
    }

    /**
     * Gets the singleton instance of {@link Options}.
     *
     * @return The {@link Options} instance.
     */
    public static Options getOptions() {
        if (options == null)
            options = new Options();
        return options;
    }

    /**
     * Applies the appearance settings based on the current options.
     * This method will set the look and feel to light, dark, or auto-detect.
     */
    public static void applyAppearance() {
        switch (getOptions().getAppearance()) {
            case Light -> setDarkAppearance(false);
            case Dark -> setDarkAppearance(true);
            case Auto -> setDarkAppearance(osThemeDetector.isDark());
        }
    }

    /**
     * Enum representing the different appearance options for the application.
     */
    public enum Appearance {
        Light, Dark, Auto
    }

    /**
     * Applies a specific appearance setting and updates the options.
     *
     * @param darkAppearance The desired {@link Appearance} to apply.
     */
    public static void applyAppearance(Appearance darkAppearance) {
        getOptions().setAppearance(darkAppearance);
        applyAppearance();
    }

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
                boolean isMac = isMac();
                UIManager.setLookAndFeel(darkAppearance ?
                        (isMac ? new FlatMacDarkLaf() : new FlatDarkLaf()) :
                        (isMac ? new FlatMacLightLaf() : new FlatLightLaf()));
            } catch (Exception ex) {
                System.err.println("Failed to initialize LaF");
            }
        }
    }

    /**
     * Checks if the current operating system is macOS.
     *
     * @return true if the OS is macOS, false otherwise.
     */
    public static boolean isMac() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") || osName.contains("darwin");
    }

    /**
     * Retrieves an icon based on its key and the current appearance.
     *
     * @param iconKey The base key of the icon (e.g., "copy").
     * @return The appropriate {@link Icon} for the given key and current theme.
     */
    public static Icon getIcon(String iconKey) {
        Icon result = icons.get(getIconKey(iconKey));
        if (result == null)
            result = icons.get(getIconKey(iconKey, ! isDarkAppearance()));
        return result;
    }

    private static String getIconKey(String iconKey) {
        return getIconKey(iconKey, isDarkAppearance());
    }

    private static String getIconKey(String iconKey, boolean isDarkAppearance) {
        return iconKey + (! isDarkAppearance ? "" : "-light");
    }

    /**
     * Functional interface for providing an "About" dialog or information.
     * Implementations of this interface can be used to display application-specific
     * information when an "About" action is triggered.
     *
     */
    public interface AboutProvider {

        /**
         * Displays the "About" dialog or information.
         *
         * @param parent The parent component to which the dialog should be relative.
         */
        void showAbout(JComponent parent);

        /**
         * Opens the application's website or relevant URL in a web browser.
         * This method is typically invoked when a "Visit Website" action is triggered
         * from an "About" dialog or similar UI element.
         */
        void visitSite();
    }

    final static OsThemeDetector osThemeDetector = OsThemeDetector.getDetector();

    /**
     * Retrieves the singleton instance of {@link OsThemeDetector}.
     * This detector can be used to determine the operating system's current theme (light or dark)
     * and to listen for theme changes.
     * @return The {@link OsThemeDetector} instance.
     */
    public static OsThemeDetector getDetector() {
        return osThemeDetector;
    }
}
