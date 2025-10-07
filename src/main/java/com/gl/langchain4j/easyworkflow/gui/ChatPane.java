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

import com.gl.langchain4j.easyworkflow.gui.UISupport.AutoIcon;
import dev.langchain4j.service.V;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

/**
 * A panel that provides a chat interface, including message input, display, and settings.
 */
public class ChatPane extends JPanel implements PropertyChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatPane.class);
    private final ChatMessagesHostPane chatMessagesHostPane = new ChatMessagesHostPane();
    private final FormPanel edtMessage = new FormPanel();
    private final JButton btnSend = new JButton("➤");
    private final JButton btnSettings;
    private final Consumer<Boolean> appearanceChangeHandler = isDarkMode -> {
        if (UISupport.getOptions().getAppearance() == Appearance.Auto)
            SwingUtilities.invokeLater(() -> applyAppearance(Appearance.Auto, this));
    };
    private ChatEngine chatEngine;
    private boolean waitingForResponse;

    /**
     * Constructs a new ChatPane.
     */
    public ChatPane() {
        super(new BorderLayout());
        setBorder(null);

        add(chatMessagesHostPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(
                new CompoundBorder(new EmptyBorder(2, 10, 10, 10),
                        new CompoundBorder(new InputPaneBorder(),
                                new EmptyBorder(10, 10, 10, 10))));
        add(inputPanel, BorderLayout.SOUTH);

        edtMessage.addPropertyChangeListener(this);
        JScrollPane messageScrollPane = new JScrollPane(edtMessage) {
            @Override
            public void updateUI() {
                super.updateUI();

                setOpaque(false);
                getViewport().setOpaque(false);
                setBorder(null);
            }
        };
        inputPanel.add(messageScrollPane, BorderLayout.CENTER);

        // Send buttons
        Box buttons = new Box(BoxLayout.Y_AXIS);
        buttons.setAlignmentX(RIGHT_ALIGNMENT);

        btnSettings = new JButton(new AutoIcon(ICON_SETTINGS)) {
            @Override
            public void updateUI() {
                super.updateUI();
                setOpaque(false);
                setBackground(null);
                setBorder(new EmptyBorder(2, 2, 2, 2));
            }
        };
        btnSettings.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnSettings.setToolTipText("Settings");
        btnSettings.setFocusable(false);
        btnSettings.addActionListener(e -> showOptions());
        buttons.add(btnSettings);
        buttons.add(Box.createVerticalStrut(10));
        buttons.add(Box.createVerticalGlue());

        btnSend.setFont(new Font("SansSerif", Font.BOLD, 30));
        btnSend.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnSend.addActionListener(e -> sendMessage());
        btnSend.setEnabled(false);
        btnSend.setToolTipText("Send");
        buttons.add(btnSend);

        inputPanel.add(buttons, BorderLayout.EAST);
    }

    /**
     * Retrieves the ChatPane instance that contains the given subComponent.
     *
     * @param subComponent The sub-component to search from.
     * @return The ChatPane instance, or null if not found.
     */
    public static ChatPane getChatPane(JComponent subComponent) {
        Container parent = subComponent.getParent();
        while (parent != null && !(parent instanceof ChatPane)) {
            parent = parent.getParent();
        }

        return (ChatPane) parent;
    }

    /**
     * Applies the specified appearance to the ChatPane and its components.
     *
     * @param darkAppearance The appearance to apply (Light, Dark, or Auto).
     * @param chatPane       The ChatPane instance to apply the appearance to.
     */
    public static void applyAppearance(Appearance darkAppearance, ChatPane chatPane) {
        UISupport.applyAppearance(darkAppearance);
        SwingUtilities.updateComponentTreeUI(chatPane.getParent());
        chatPane.chatMessagesHostPane.getChatMessagesPane().updateRenderers();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(FormPanel.PROPERTY_VALUE_CHANGED))
            updateSendButon();
        else if (evt.getPropertyName().equals(FormPanel.PROPERTY_ENTER_PRESSED))
            sendMessage();
    }

    private void showOptions() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenu darkModeMenu = new JMenu("Appearance");
        darkModeMenu.setIcon(new AutoIcon(ICON_BULB));

        JRadioButtonMenuItem mniLight = new JRadioButtonMenuItem(createAction("Light", null,
                e -> applyAppearance(Appearance.Light, this)));
        mniLight.setSelected(getOptions().getAppearance() == Appearance.Light);
        darkModeMenu.add(mniLight);

        JRadioButtonMenuItem mniDark = new JRadioButtonMenuItem(createAction("Dark", null,
                e -> applyAppearance(Appearance.Dark, this)));
        mniDark.setSelected(getOptions().getAppearance() == Appearance.Dark);
        darkModeMenu.add(mniDark);

        JRadioButtonMenuItem mniAuto = new JRadioButtonMenuItem(createAction("Auto", null,
                e -> applyAppearance(Appearance.Auto, this)));
        mniAuto.setSelected(getOptions().getAppearance() == Appearance.Auto);
        darkModeMenu.add(mniAuto);

        popupMenu.add(darkModeMenu);

        popupMenu.add(new JSeparator());

        JMenuItem mniRenderMarkdown = new JCheckBoxMenuItem(createAction("Render Markdown", new AutoIcon(ICON_DOCUMENT),
                e -> getChatMessagesPane().setRenderMarkdown(!getChatMessagesPane().isRenderMarkdown())));
        mniRenderMarkdown.setSelected(getChatMessagesPane().isRenderMarkdown());
        popupMenu.add(mniRenderMarkdown);

        JMenuItem mniClearAfterSending = new JCheckBoxMenuItem(createAction("Clear After Sending", new AutoIcon(ICON_CLEAR),
                e -> UISupport.getOptions().setClearAfterSending(! UISupport.getOptions().isClearAfterSending())));
        mniClearAfterSending.setSelected(UISupport.getOptions().isClearAfterSending());
        popupMenu.add(mniClearAfterSending);

        final AboutProvider aboutProvider = (AboutProvider) SwingUtilities.getAncestorOfClass(AboutProvider.class, this);
        if (aboutProvider != null) {
            popupMenu.add(new JSeparator());
            popupMenu.add(new JCheckBoxMenuItem(createAction("About", new AutoIcon(ICON_HELP), e -> aboutProvider.showAbout(this))));
        }

        popupMenu.pack();
        Dimension size = popupMenu.getPreferredSize();
        popupMenu.show(btnSettings, btnSend.getWidth() - size.width, -size.height);
    }

    public ChatMessagesPane getChatMessagesPane() {
        return getChatMessagesHostPane().getChatMessagesPane();
    }

    ChatMessagesHostPane getChatMessagesHostPane() {
        return chatMessagesHostPane;
    }

    private void updateSendButon() {
        btnSend.setEnabled(!waitingForResponse && edtMessage.hasRequiredContent());
    }

    /**
     * Retrieves the user's message from the input editor.
     *
     * @return A {@code Map<String, Object>} representing the user's message, where keys are parameter names and values
     * are their corresponding inputs. Returns {@code null} if the message editor's content is invalid.
     */
    public Map<String, Object> getUserMessage() {
        Map<String, Object> result = null;

        if (edtMessage.checkValidity() == null)
            result = edtMessage.getFormValues();

        return result;
    }

    /**
     * Sets the content of the user message editor.
     *
     * @param userMessage A {@code Map<String, Object>} containing the values to set in the form. The keys should
     *                    correspond to the names of the form elements.
     */
    public void setUserMessage(Map<String, Object> userMessage) {
        edtMessage.setFormValues(userMessage);
    }

    /**
     * Sets the content of the user message editor with a single string value. This method attempts to find the first
     * form element of type {@code String.class} and sets its value to the provided {@code userMessage}.
     *
     * @param userMessage The string value to set in the message editor.
     */
    public void setUserMessage(String userMessage) {
        for (FormPanel.FormElement formElement : edtMessage.getFormElements()) {
            if (formElement.type() == String.class) {
                edtMessage.setFormValues(Map.of(formElement.name(), userMessage));
                break;
            }
        }
    }

    private void sendMessage() {
        Map<String, Object> message = getUserMessage();
        if (message != null && !message.isEmpty()) {
            addChatMessage(new ChatMessage(message, message.toString(), null, true));
            if (UISupport.getOptions().isClearAfterSending())
                edtMessage.clearContent();
            edtMessage.requestFocus();

            setWaitingForResponse(true);
            chatMessagesHostPane.addTypingIndicator();
            CompletableFuture.supplyAsync(() -> {
                String response = chatEngine.send(message).toString();
                String responseAsHtml = convertMarkdownToHtml(response);

                return new ChatMessage(null, response, responseAsHtml, false);
            }).whenComplete(this::processChatEngineResponse);
        }
    }

    private String convertMarkdownToHtml(String text) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(text);
        HtmlRenderer renderer = HtmlRenderer.builder().omitSingleParagraphP(true).build();

        return renderer.render(document);
    }

    private void processChatEngineResponse(ChatMessage result, Throwable ex) {
        SwingUtilities.invokeLater(() -> {
            if (ex == null) {
                addChatMessage(result);
            } else {
                final String error = "Failed to get response from chat.";
                logger.error(error, ex);
                addChatMessage(new ChatMessage(null, "🛑 " + error + " " + ex.getMessage(), null, false));
            }
            setWaitingForResponse(false);
            chatMessagesHostPane.removeTypingIndicator();
            updateSendButon();
        });
    }

    private void setWaitingForResponse(boolean isWaitingForResponse) {
        if (waitingForResponse != isWaitingForResponse) {
            waitingForResponse = isWaitingForResponse;
            updateSendButon();
        }
    }

    private void addChatMessage(ChatMessage message) {
        chatMessagesHostPane.addChatMessage(message);
    }

    /**
     * Returns the currently set chat engine.
     *
     * @return The chat engine function.
     */
    public ChatEngine getChatEngine() {
        return chatEngine;
    }

    /**
     * Sets the chat engine function. This function will be called to get responses to user messages.
     *
     * @param chatEngine The function that takes a user message (String) and returns a response (String).
     * @throws NullPointerException if chatEngine is null.
     */
    public void setChatEngine(ChatEngine chatEngine) {
        Objects.requireNonNull(chatEngine);
        this.chatEngine = chatEngine;
        setupMessageEditor(chatEngine);
    }

    private void setupMessageEditor(ChatEngine chatEngine) {
        Objects.requireNonNull(chatEngine);

        List<FormPanel.FormElement> formElements = new ArrayList<>();
        for (Parameter parameter : chatEngine.getMessageParameters()) {
            String parameterName = parameter.getName();
            V v = parameter.getAnnotation(V.class);
            if (v != null)
                parameterName = v.value();
            FormPanel.EditorType editorType = FormPanel.EditorType.Default;
            if (parameter.getType() == String.class)
                editorType = FormPanel.EditorType.Note;

            formElements.add(new FormPanel.FormElement(parameterName, null, parameter.getType(), null, editorType, true));
        }
        edtMessage.setFormElements(formElements);
    }

    /**
     * Returns a list of all chat messages currently displayed.
     *
     * @return A List of ChatMessage objects.
     */
    public List<ChatMessage> getChatMessages() {
        return getChatMessagesPane().getChatMessages();
    }

    @Override
    public void addNotify() {
        super.addNotify();

        edtMessage.requestFocus();
        UISupport.getDetector().registerListener(appearanceChangeHandler);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();

        UISupport.getDetector().removeListener(appearanceChangeHandler);
    }

    /**
     * Interface for defining a chat engine that can send messages and provide parameter information.
     */
    public interface ChatEngine {
        Object send(Map<String, Object> message);

        Parameter[] getMessageParameters();
    }

    static class InputPaneBorder extends AbstractBorder {
        public InputPaneBorder() {
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color oldColor = g2d.getColor();
            g2d.setColor(Color.gray);
            g2d.draw(new RoundRectangle2D.Float(x, y, width, height, 10, 10));

            g2d.setColor(oldColor);
        }
    }
}
