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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.actions.BasicAction;
import com.gl.appframework.comp.ActionPopupMenu;
import com.jthemedetecor.OsThemeDetector;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static com.gl.appframework.IconFactory.*;
import static com.gl.appframework.ToolbarIcons.*;

/**
 * Provides utility methods and constants for UI-related operations, including icon management, theme handling, and user
 * preferences.
 */
@SuppressWarnings("ALL")
public class UISupport {
    final static OsThemeDetector osThemeDetector = OsThemeDetector.getDetector();
    private static final Logger logger = LoggerFactory.getLogger(UISupport.class);
    private static final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(new Object());
    private static Boolean darkAppearance;
    private static Options options;

    /**
     * Sets up a context menu (popup menu) for a given {@link JTextComponent} with standard text editing actions (Cut,
     * Copy, Paste).
     *
     * @param textComponent The {@link JTextComponent} to which the popup menu will be attached.
     */
    public static void setupPopupMenu(JTextComponent textComponent) {
        setupPopupMenu(textComponent, null);
    }

    /**
     * Sets up a context menu (popup menu) for a given {@link JTextComponent} with standard text editing actions (Cut,
     * Copy, Paste) and optionally additional custom actions.
     *
     * @param textComponent     The {@link JTextComponent} to which the popup menu will be attached.
     * @param additionalActions An {@link ActionGroup} containing additional actions to be included in the popup menu.
     *                          Can be {@code null}.
     */
    public static void setupPopupMenu(JTextComponent textComponent, ActionGroup additionalActions) {
        ActionPopupMenu popupMenu = new ActionPopupMenu();

        Action action = textComponent.getActionMap().get("delete");
        if (action == null)
            textComponent.getActionMap().put("delete", new BasicAction("Delete", null,
                    e -> textComponent.replaceSelection("")));
        ActionGroup actionGroup = new ActionGroup(
                new ActionGroup(
                        new BasicAction("Cut", new AutoIcon(ICON_CUT),
                                e -> textComponent.cut(),
                                a -> a.setEnabled(textComponent.isEditable() && textComponent.getSelectedText() != null)),
                        new BasicAction("Copy", new AutoIcon(ICON_COPY),
                                e -> {
                                    int caretPosition = textComponent.getCaretPosition();
                                    boolean noSelection = textComponent.getSelectedText() == null || textComponent.getSelectedText().isEmpty();
                                    if (noSelection)
                                        textComponent.selectAll();
                                    textComponent.copy();
                                    if (noSelection) {
                                        textComponent.select(caretPosition, caretPosition);
                                        textComponent.setCaretPosition(caretPosition);
                                    }
                                },
                                a -> a.setEnabled(textComponent.getText() != null)),
                        new BasicAction("Paste", new AutoIcon(ICON_PASTE),
                                e -> textComponent.paste(),
                                a -> a.setEnabled(textComponent.isEditable())),
                        new BasicAction("Delete", null,
                                e -> {
                                    Action deleteAction = textComponent.getActionMap().get("delete");
                                    if (deleteAction != null)
                                        deleteAction.actionPerformed(new ActionEvent(textComponent, ActionEvent.ACTION_PERFORMED, null));
                                },
                                a -> a.setEnabled(textComponent.isEditable() && textComponent.getSelectedText() != null))
                ),
                new ActionGroup(
                        new BasicAction("Select All", null,
                                e -> textComponent.selectAll(),
                                a -> a.setEnabled(textComponent.isEditable() &&
                                        textComponent.getText() != null &&
                                        !textComponent.getText().isEmpty()))

                ),
                additionalActions
        );
        popupMenu.setActionGroup(actionGroup);
        textComponent.setComponentPopupMenu(popupMenu);
    }

    /**
     * Sets up undo/redo functionality for a given {@link JTextComponent}. This method adds an
     * {@link UndoableEditListener} to the text component's document and binds undo/redo actions to standard keyboard
     * shortcuts (Ctrl+Z/Cmd+Z and Ctrl+Shift+Z/Cmd+Shift+Z).
     *
     * @param textComponent The {@link JTextComponent} for which to set up undo/redo.
     * @return A {@link DefaultUndoableEditListener} instance managing the undo/redo operations.
     */
    public static DefaultUndoableEditListener setupUndomanager(JTextComponent textComponent) {
        DefaultUndoableEditListener result = new DefaultUndoableEditListener();
        textComponent.getDocument().addUndoableEditListener(result);

        bindAction(textComponent, "undo",
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                new BasicAction("Undo", null, e -> {
                    if (result.getUndoManager().canUndo())
                        result.getUndoManager().undo();
                }));

        bindAction(textComponent, "redo",
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                new BasicAction("Redo", null, e -> {
                    if (result.getUndoManager().canRedo()) result.getUndoManager().redo();
                }));

        return result;
    }

    /**
     * Sets up a double-click action for a given {@link JComponent}. When the component is double-clicked with the left
     * mouse button, the provided {@link Action} is performed.
     *
     * @param c      The {@link JComponent} to which the double-click listener will be added.
     * @param action The {@link Action} to be performed on a double-click.
     */
    public static void bindDoubleClickAction(JComponent c, Action action) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    action.actionPerformed(new ActionEvent(c, ActionEvent.ACTION_PERFORMED, null));
                }
            }
        });
    }


    /**
     * Creates an {@link Action} with a title, icon, and an action listener.
     *
     * @param title          The title of the action.
     * @param icon           The icon for the action.
     * @param actionListener The consumer to be called when the action is performed.
     * @return A new {@link Action} instance.
     */
    public static BasicAction createAction(String title, Icon icon,
                                           Consumer<ActionEvent> actionListener) {
        return new BasicAction(title, icon, actionListener);
    }

    /**
     * Retrieves the user preferences node for the application.
     *
     * @return The {@link Preferences} object for the application.
     */
    public static Preferences getPreferences() {
        return Preferences.userRoot().node(Application.getSharedApplication().getId().replace(".", "/"));
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

        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.revalidate();
            window.repaint();
        }

        firePropertyChange(Options.PROP_APPEARANCE_DARK, !darkAppearance, darkAppearance);
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
     * @param appearance The desired {@link Appearance} to apply.
     */
    public static void applyAppearance(Appearance appearance) {
        getOptions().setAppearance(appearance);
        applyAppearance();
        boolean dark = osThemeDetector.isDark();
        firePropertyChange(Options.PROP_APPEARANCE_DARK, !dark, dark);
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
     * Checks if the current operating system is macOS.
     *
     * @return true if the OS is macOS, false otherwise.
     */
    public static boolean isMacOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") || osName.contains("darwin");
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
     * @param view         The component to be displayed in the scroll pane. dark/light appearance.
     * @param retainBorder If true, a line border will be applied to the scroll pane, its color adapting to the current
     * @return A new {@link JScrollPane} instance.
     */
    public static JScrollPane createScrollPane(Component view, boolean retainBorder) {
        return createScrollPane(view, retainBorder, false, false, false, false);
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

    /**
     * Returns the default border color based on the current appearance.
     *
     * @return {@link Color#DARK_GRAY} if the dark appearance is active, otherwise {@link Color#LIGHT_GRAY}.
     */
    public static Color getDefaultBorderColor() {
        return isDarkAppearance() ? Color.GRAY : Color.LIGHT_GRAY;
    }

    /**
     * Creates a custom line border with specified color and sides to paint.
     *
     * @param lineColor   The color of the border.
     * @param paintTop    True to paint the top border, false otherwise.
     * @param paintLeft   True to paint the left border, false otherwise.
     * @param paintBottom True to paint the bottom border, false otherwise.
     * @param paintRight  True to paint the right border, false otherwise.
     * @return A new {@link Border} instance with the specified custom line border.
     */
    public static Border createCustomLineBorder(Color lineColor, boolean paintTop, boolean paintLeft, boolean paintBottom, boolean paintRight) {
        return new CustomLineBorder(lineColor, new Insets(paintTop ? 1 : 0, paintLeft ? 1 : 0, paintBottom ? 1 : 0, paintRight ? 1 : 0),
                paintTop, paintLeft, paintBottom, paintRight);
    }

    /**
     * Converts a given Markdown text into HTML format.
     *
     * @param text The Markdown text to be converted.
     * @return The HTML representation of the Markdown text.
     */
    public static String convertMarkdownToHtml(String text) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(text);
        HtmlRenderer renderer = HtmlRenderer.builder().omitSingleParagraphP(true).build();

        return renderer.render(document);
    }

    /**
     * Copies the selected text from a {@link JTextComponent} to the system clipboard. If no text is selected, the
     * entire content of the text component is copied.
     *
     * @param textComponent The {@link JTextComponent} from which to copy text.
     */
    public static void copy(JTextComponent textComponent) {
        String text = textComponent.getSelectedText();
        if (textComponent instanceof JEditorPane editorPane && "text/html".equals(editorPane.getContentType())) {
            int caretPosition = -1;
            if (text == null || text.isEmpty()) {
                caretPosition = textComponent.getCaretPosition();
                textComponent.selectAll();
            }
            textComponent.copy();
            if (caretPosition > -1)
                textComponent.setCaretPosition(caretPosition);
        } else {
            if (text == null || text.isEmpty())
                text = textComponent.getText();
            if (text != null && !text.isEmpty()) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(text), null);
            }
        }
    }

    /**
     * Binds an {@link Action} to a {@link KeyStroke} for a given {@link JComponent}. This method associates a keystroke
     * with an action key in the component's input map, and then associates the action key with the actual
     * {@link Action} in the component's action map.
     *
     * @param c         The {@link JComponent} to which the action will be bound.
     * @param actionKey A unique string identifier for the action.
     * @param keyStroke The {@link KeyStroke} that will trigger the action.
     * @param action    The {@link Action} to be performed when the key stroke is pressed.
     */
    public static void bindAction(JComponent c, String actionKey, KeyStroke keyStroke, Action action) {
        c.getInputMap().put(keyStroke, actionKey);
        c.getActionMap().put(actionKey, action);
    }

    /**
     * Scrolls a given rectangle within a component to be visible. If the component's parent is a {@link JViewport}, it
     * attempts to align the rectangle vertically within the viewport based on the specified alignment.
     *
     * @param comp              The component containing the rectangle to be scrolled.
     * @param rect              The rectangle to make visible.
     * @param verticalAlignment The vertical alignment for the rectangle within the viewport (e.g.,
     *                          {@link JComponent#TOP_ALIGNMENT}, {@link JComponent#BOTTOM_ALIGNMENT},
     *                          {@link JComponent#CENTER_ALIGNMENT}).
     */
    public static void scrollRectToVisible(JComponent comp, Rectangle rect, float verticalAlignment) {
        if (comp.getParent() instanceof JViewport viewport) {
            Rectangle viewRect = viewport.getViewRect();

            int viewHeight = viewRect.height;
            int newY;

            if (verticalAlignment == JComponent.TOP_ALIGNMENT) {
                newY = rect.y;
            } else if (verticalAlignment == JComponent.BOTTOM_ALIGNMENT) {
                newY = rect.y + rect.height - viewHeight;
            } else if (verticalAlignment == JComponent.CENTER_ALIGNMENT) {
                newY = rect.y + rect.height / 2 - viewHeight / 2;
            } else {
                // Default to TOP if an unknown alignment is provided
                newY = rect.y;
            }

            // Ensure newY is within the valid scroll range
            // The maximum Y position is the component height minus the viewport height
            // The minimum Y position is 0

            newY = Math.max(0, Math.min(newY, comp.getHeight() - viewHeight));

            Rectangle adjusted = new Rectangle(rect);
            adjusted.y = newY;
            adjusted.height = viewHeight;
            comp.scrollRectToVisible(adjusted);
        } else {
            comp.scrollRectToVisible(rect);
        }
    }

    /**
     * Updates the content of a {@link JScrollPane} while attempting to preserve its vertical scroll position. This is
     * useful when the content of the scroll pane changes, but the user's current scroll view should be maintained.
     *
     * @param scrollPane The {@link JScrollPane} whose content is being updated.
     * @param action     A {@link Runnable} containing the code that updates the content of the scroll pane.
     */
    public static void updateAndPreserveScrollPosition(JScrollPane scrollPane, Runnable action) {
        Objects.requireNonNull(scrollPane, "scrollPane cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        double scrollPercentage = (double) verticalScrollBar.getValue() / (verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount());

        action.run();

        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue((int) (scrollPercentage * (verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount()))));
    }

    enum ImageFilter {
        None, Lighter, Inverted
    }

    /**
     * Enum representing the different appearance options for the application.
     */
    public enum Appearance {
        Light, Dark, Auto
    }

    /**
     * An {@link UndoableEditListener} implementation that manages undo/redo operations for a {@link JTextComponent}.
     */
    public static class DefaultUndoableEditListener implements UndoableEditListener {
        private final UndoManager undoManager = new UndoManager();
        private boolean enabled = true;

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            if (isEnabled())
                undoManager.addEdit(e.getEdit());
        }

        /**
         * Returns the {@link UndoManager} associated with this listener.
         *
         * @return The {@link UndoManager} instance.
         */
        public UndoManager getUndoManager() {
            return undoManager;
        }

        /**
         * Checks if undo/redo functionality is currently enabled.
         *
         * @return {@code true} if enabled, {@code false} otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether undo/redo functionality should be enabled.
         *
         * @param aEnabled {@code true} to enable, {@code false} to disable.
         */
        public void setEnabled(boolean aEnabled) {
            enabled = aEnabled;
        }
    }

    static class InvertFilter extends RGBImageFilter {
        public int filterRGB(int x, int y, int rgb) {
            return rgb ^ 0x00FFFFFF; // Preserve transparency
        }
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

    private static class ImageIconUIResource extends ImageIcon implements UIResource {
        ImageIconUIResource(Image image) {
            super(image);
        }
    }

    /**
     * Manages user preferences for the application.
     */
    public static class Options {
        public static final String PROP_RENDER_MARKDOWN = "renderMarkdown";
        public static final String PROP_APPEARANCE = "appearance";
        public static final String PROP_APPEARANCE_DARK = "appearanceDark";
        public static final String PROP_FRAME_BOUNDS = "frameBounds";
        public static final String PROP_CLEAR_AFTER_SENDING = "clearAfterSending";
        public static final String PROP_OPEN_FILE_AFTER_EXPORTING = "openFileAfterSharing";
        private final PropertyChangeSupport propetyChangeSupport = new PropertyChangeSupport(this);

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
            if (isRenderMarkdown() != renderMarkdown) {
                getPreferences().putBoolean(PROP_RENDER_MARKDOWN, renderMarkdown);
                propetyChangeSupport.firePropertyChange(PROP_RENDER_MARKDOWN, !renderMarkdown, renderMarkdown);
            }
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
            if (getAppearance() != appearance) {
                Appearance oldValue = getAppearance();
                getPreferences().putInt(PROP_APPEARANCE, appearance.ordinal());
                propetyChangeSupport.firePropertyChange(PROP_APPEARANCE, oldValue, appearance);
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
                propetyChangeSupport.firePropertyChange(PROP_FRAME_BOUNDS, getFrameBounds(), frameBounds);
            }
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
            if (isClearAfterSending() != isCleaAfterSending) {
                getPreferences().putBoolean(PROP_CLEAR_AFTER_SENDING, isCleaAfterSending);
                propetyChangeSupport.firePropertyChange(PROP_CLEAR_AFTER_SENDING, !isCleaAfterSending, isCleaAfterSending);
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
                propetyChangeSupport.firePropertyChange(PROP_OPEN_FILE_AFTER_EXPORTING, !value, value);
            }
        }

        /**
         * Adds a {@link PropertyChangeListener} to the listener list. The listener is registered for all properties.
         *
         * @param propertyChangeListener The {@link PropertyChangeListener} to be added.
         */
        public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
            propetyChangeSupport.addPropertyChangeListener(propertyChangeListener);
        }

        /**
         * Removes a {@link PropertyChangeListener} from the listener list.
         *
         * @param propertyChangeListener The {@link PropertyChangeListener} to be removed.
         */
        public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
            propetyChangeSupport.removePropertyChangeListener(propertyChangeListener);
        }
    }

    static class RoundRectBorder extends AbstractBorder {
        protected final Color lineColor;
        protected int arcWidth = -1;
        protected int arcHeight = -1;

        public RoundRectBorder(Color lineColor, int arcWidth, int arcHeight) {
            this.lineColor = lineColor;
            this.arcWidth = arcWidth;
            this.arcHeight = arcHeight;
        }

        public RoundRectBorder(Color lineColor) {
            this.lineColor = lineColor;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            super.paintBorder(c, g, x, y, width, height);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(lineColor);
            g2.drawRoundRect(x, y, width - 1, height - 1,
                    arcWidth == -1 ? height : arcWidth,
                    arcHeight == -1 ? height : arcHeight);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1); // Default insets for a thin border
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = 1;
            return insets;
        }
    }

    static class CustomLineBorder extends AbstractBorder {
        protected final Color lineColor;
        protected final Insets insets;
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
            ((Graphics2D) g).setStroke(new BasicStroke(0.5f));
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

    /**
     * Creates a rounded rectangle border with the specified line color.
     *
     * @param lineColor The color of the border.
     * @return A new {@link Border} instance with a rounded rectangle shape.
     */
    public static Border createRoundRectBorder(Color lineColor) {
        return new UISupport.RoundRectBorder(lineColor);
    }
}
