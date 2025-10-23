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
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.jthemedetecor.OsThemeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
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
    public static final String ICON_SIGNPOST = "signpost";
    public static final String ICON_REFRESH = "refresh";
    public static final String ICON_BOX = "box";
    public static final String ICON_STACK = "stack";
    public static final String ICON_TARGET = "target";
    public static final String ICON_BREAKPOINT = "breakpoint";
    public static final String ICON_PLAY = "play";
    public static final String ICON_STOP = "stop";
    final static OsThemeDetector osThemeDetector = OsThemeDetector.getDetector();
    private static final Logger logger = LoggerFactory.getLogger(UISupport.class);
    private static final Map<String, Icon> icons = new HashMap<>();
    private static Boolean darkAppearance;
    private static Options options;

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

        icons.put(getIconKey(ICON_SIGNPOST, false), loadImageIcon("signpost"));
        icons.put(getIconKey(ICON_SIGNPOST, true), loadImageIcon("signpost-light"));

        icons.put(getIconKey(ICON_REFRESH, false), loadImageIcon("refresh"));
        icons.put(getIconKey(ICON_REFRESH, true), loadImageIcon("refresh-light"));

        icons.put(getIconKey(ICON_BOX, false), loadImageIcon("box"));
        icons.put(getIconKey(ICON_BOX, true), loadImageIcon("box-light"));

        icons.put(getIconKey(ICON_TARGET, false), loadImageIcon("target"));
        icons.put(getIconKey(ICON_TARGET, true), loadImageIcon("target-light"));

        icons.put(getIconKey(ICON_BREAKPOINT, false), loadImageIcon("breakpoint"));
        icons.put(getIconKey(ICON_BREAKPOINT, true), loadImageIcon("breakpoint-light"));

        icons.put(getIconKey(ICON_PLAY, false), loadImageIcon("play"));
        icons.put(getIconKey(ICON_PLAY, true), loadImageIcon("play-light"));

        icons.put(getIconKey(ICON_STOP, false), loadImageIcon("stop"));
        icons.put(getIconKey(ICON_STOP, true), loadImageIcon("stop-light"));
    }

    private static ImageIcon loadImageIcon(String name) {
        return new ImageIcon(new BaseMultiResolutionImage(
                new ImageIcon(Objects.requireNonNull(UISupport.class.getResource(name + ".png"))).getImage(),
                new ImageIcon(Objects.requireNonNull(UISupport.class.getResource(name + "@2x.png"))).getImage()));
    }

    /**
     * Creates an {@link Action} with a title, icon, and an action listener.
     *
     * @param title          The title of the action.
     * @param icon           The icon for the action.
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
     * Applies the appearance settings based on the current options. This method will set the look and feel to light,
     * dark, or auto-detect.
     */
    public static void applyAppearance() {
        switch (getOptions().getAppearance()) {
            case Light -> setDarkAppearance(false);
            case Dark -> setDarkAppearance(true);
            case Auto -> setDarkAppearance(osThemeDetector.isDark());
        }
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
            result = icons.get(getIconKey(iconKey, !isDarkAppearance()));
        return result;
    }

    /**
     * Retrieves an icon based on its key and a specified appearance.
     *
     * @param iconKey          The base key of the icon (e.g., "copy").
     * @param isDarkAppearance true to get the dark appearance icon, false for the light appearance icon.
     * @return The appropriate {@link Icon} for the given key and specified theme.
     */
    public static Icon getIcon(String iconKey, boolean isDarkAppearance) {
        return icons.get(getIconKey(iconKey, isDarkAppearance));
    }

    private static String getIconKey(String iconKey) {
        return getIconKey(iconKey, isDarkAppearance());
    }

    private static String getIconKey(String iconKey, boolean isDarkAppearance) {
        return iconKey + (!isDarkAppearance ? "" : "-light");
    }

    /**
     * Retrieves the singleton instance of {@link OsThemeDetector}. This detector can be used to determine the operating
     * system's current theme (light or dark) and to listen for theme changes.
     *
     * @return The {@link OsThemeDetector} instance.
     */
    public static OsThemeDetector getDetector() {
        return osThemeDetector;
    }

    /**
     * Creates a {@link JScrollPane} with optional border retention based on the current theme.
     *
     * @param retainBorder If true, a line border will be applied to the scroll pane, its color adapting to the current
     * @param view         The component to be displayed in the scroll pane. dark/light appearance.
     * @return A new {@link JScrollPane} instance.
     */
    public static JScrollPane createScrollPane(Component view, boolean retainBorder,
                                               boolean topBorder, boolean leftBorder, boolean bottomBorder, boolean rightBorder) {
        return new CustomScrollPane(view, retainBorder, topBorder, leftBorder, bottomBorder, rightBorder);
    }

    static class CustomScrollPane extends JScrollPane {
        private final boolean retainBorder;

        private final boolean topBorder;
        private final boolean leftBorder;
        private final boolean bottomBorder;
        private final boolean rightBorder;

        public CustomScrollPane(Component view, boolean retainBorder,
                                boolean topBorder, boolean leftBorder, boolean bottomBorder, boolean rightBorder) {
            super(view);

            this.retainBorder = retainBorder;

            this.topBorder = topBorder;
            this.leftBorder = leftBorder;
            this.bottomBorder = bottomBorder;
            this.rightBorder = rightBorder;

            updateUI();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (this.retainBorder)
                setBorder(new CustomLineBorder(getDefaultBorderColor(),
                        new Insets(topBorder ? 1 : 0, leftBorder ? 1 : 0, bottomBorder ? 1 : 0, rightBorder ? 1 : 0),
                        topBorder, leftBorder, bottomBorder, rightBorder));
            else
                setBorder(null);
        }
    }

    /**
     * Returns the default border color based on the current appearance.
     *
     * @return {@link Color#DARK_GRAY} if the dark appearance is active, otherwise {@link Color#LIGHT_GRAY}.
     */
    public static Color getDefaultBorderColor() {
        return isDarkAppearance() ? Color.DARK_GRAY : Color.LIGHT_GRAY;
    }

    /**
     * Enum representing the different appearance options for the application.
     */
    public enum Appearance {
        Light, Dark, Auto
    }

    /**
     * Functional interface for providing an "About" dialog or information. Implementations of this interface can be
     * used to display application-specific information when an "About" action is triggered.
     */
    public interface AboutProvider {

        /**
         * Displays the "About" dialog or information.
         *
         * @param parent The parent component to which the dialog should be relative.
         */
        void showAbout(JComponent parent);

        /**
         * Opens the application's website or relevant URL in a web browser. This method is typically invoked when a
         * "Visit Website" action is triggered from an "About" dialog or similar UI element.
         */
        void visitSite();
    }

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
     * Creates a custom line border with specified color and sides to paint.
     *
     * @param lineColor The color of the border.
     * @param paintTop  True to paint the top border, false otherwise.
     * @param paintLeft True to paint the left border, false otherwise.
     * @param paintBottom True to paint the bottom border, false otherwise.
     * @param paintRight True to paint the right border, false otherwise.
     * @return A new {@link Border} instance with the specified custom line border.
     */
    public static Border createCustomLineBorder(Color lineColor, boolean paintTop, boolean paintLeft, boolean paintBottom, boolean paintRight) {
        return new CustomLineBorder(lineColor, new Insets(paintTop ? 1 : 0, paintLeft ? 1 : 0, paintBottom ? 1 : 0, paintRight ? 1 : 0),
                paintTop, paintLeft, paintBottom, paintRight);
    }

    static class CustomLineBorder extends AbstractBorder {
        protected Color lineColor;
        protected Insets insets;
        protected boolean paintTop = true;
        protected boolean paintLeft = true;
        protected boolean paintBottom = true;
        protected boolean paintRight = true;

        public CustomLineBorder(Color lineColor, Insets insets, boolean paintTop, boolean paintLeft, boolean paintBottom, boolean paintRight) {
            this(lineColor, insets);
            this.paintTop = paintTop;
            this.paintLeft = paintLeft;
            this.paintBottom = paintBottom;
            this.paintRight = paintRight;
        }

        public CustomLineBorder(Color lineColor) {
            this(lineColor, new Insets(0, 0, 0, 0));
        }

        public CustomLineBorder(Color lineColor, Insets insets) {
            this.lineColor = lineColor;
            this.insets = insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            super.paintBorder(c, g, x, y, width, height);
            g.setColor(lineColor);
            if (paintTop) {
                g.drawLine(x + insets.left, y, x + width - insets.right, y); // Top
            }
            if (paintBottom) {
                g.drawLine(x + insets.left, y + height - 1, x + width - insets.right, y + height - 1); // Bottom
            }
            if (paintLeft) {
                g.drawLine(x, y + insets.top, x, y + height - insets.bottom); // Left
            }
            if (paintRight) {
                g.drawLine(x + width - 1, y + insets.top, x + width - 1, y + height - insets.bottom); // Right
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return (Insets) insets.clone();
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = this.insets.left;
            insets.top = this.insets.top;
            insets.right = this.insets.right;
            insets.bottom = this.insets.bottom;
            return insets;
        }

    }
}
