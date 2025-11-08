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

package com.gl.langchain4j.easyworkflow.gui.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.PlaygroundParam;
import com.gl.langchain4j.easyworkflow.gui.platform.FormEditorType;
import com.gl.langchain4j.easyworkflow.gui.platform.FormPanel;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport.AutoIcon;
import dev.langchain4j.service.V;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.KEY_SESSION_UID;
import static com.gl.langchain4j.easyworkflow.gui.Icons.ICON_SEND;
import static com.gl.langchain4j.easyworkflow.gui.Icons.ICON_SPACER;
import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.*;
import static com.gl.langchain4j.easyworkflow.gui.platform.Actions.*;
import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.*;

/**
 * A panel that provides a chat interface, including message input, display, and settings.
 */
public class ChatPane extends JPanel implements PropertyChangeListener {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ChatPane.class);
    private final ChatMessagesHostPane chatMessagesHostPane = new ChatMessagesHostPane();
    private final FormPanel edtMessage = new FormPanel();
    private final JButton btnSend = new JButton(new AutoIcon(ICON_SEND));
    private final Consumer<Boolean> appearanceChangeHandler = isDarkMode -> {
        if (UISupport.getOptions().getAppearance() == Appearance.Auto)
            SwingUtilities.invokeLater(() -> applyAppearance(Appearance.Auto, this));
    };
    private final Box pnlButtons;
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final JPanel waitPanel = new JPanel(new GridBagLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Color color = UIManager.getColor("Panel.background");
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 240));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    };
    private final JLabel lblWaiting;
    private final JToolBar toolsToolbar;
    private ChatEngine chatEngine;
    private boolean waitingForResponse;
    private boolean waitState;
    private Timer waitStateTimer;
    private StateAction renderMarkdownAction;
    private StateAction clearAfterSendingAction;
    private ActionGroup toolsActionGroup;
    private ChatMessage lastUserMessage;
    private ExecutionDetailsProvider executionDetailsProvider;

    /**
     * Constructs a new ChatPane.
     */
    public ChatPane() {
        super(new BorderLayout());
        setBorder(null);
        setMinimumSize(new Dimension(400, 300));

        // Layered pane for wait state
        add(layeredPane, BorderLayout.CENTER);
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                contentPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                waitPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                layeredPane.revalidate();
                layeredPane.repaint();
            }
        });

        // Content panel
        contentPanel.setOpaque(false);
        layeredPane.add(contentPanel, JLayeredPane.DEFAULT_LAYER);
        contentPanel.add(chatMessagesHostPane, BorderLayout.CENTER);

        // Wait panel
        waitPanel.setOpaque(false);
        waitPanel.setVisible(false);
        // consume mouse events
        waitPanel.addMouseListener(new MouseAdapter() {
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lblWaiting = new JLabel("Waiting...");
        waitPanel.add(lblWaiting, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JProgressBar waitIndicator = new JProgressBar();
        waitIndicator.setIndeterminate(true);
        waitPanel.add(waitIndicator, gbc);
        layeredPane.add(waitPanel, JLayeredPane.PALETTE_LAYER);


        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(
                new CompoundBorder(new EmptyBorder(2, 10, 10, 10),
                        new CompoundBorder(new InputPaneBorder(),
                                new EmptyBorder(10, 10, 10, 10))));
        contentPanel.add(inputPanel, BorderLayout.SOUTH);

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

        toolsToolbar = new JToolBar(JToolBar.VERTICAL) {
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
//        toolsToolbar.setBackground(Color.YELLOW);
//        toolsToolbar.setOpaque(true);
        toolsToolbar.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlButtons = new Box(BoxLayout.Y_AXIS);
        pnlButtons.setAlignmentX(RIGHT_ALIGNMENT);
        pnlButtons.add(toolsToolbar);
        pnlButtons.add(Box.createVerticalStrut(10));
        pnlButtons.add(Box.createVerticalGlue());
//        pnlButtons.setBackground(Color.RED);
//        pnlButtons.setOpaque(true);

        btnSend.setMargin(new Insets(5, 5, 5, 5));
        btnSend.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSend.addActionListener(e -> sendMessage());
        btnSend.setEnabled(false);
        btnSend.setToolTipText("Send");
        pnlButtons.add(btnSend);

        inputPanel.add(pnlButtons, BorderLayout.EAST);

        setupActions();
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
    }

    @Override
    public void requestFocus() {
        edtMessage.requestFocus();
    }

    public StateAction getRenderMarkdownAction() {
        return renderMarkdownAction;
    }

    public StateAction getClearAfterSendingAction() {
        return clearAfterSendingAction;
    }

    public void setupToolActions(Action... actions) {
        toolsActionGroup.addAction(new ActionGroup(actions));
        UISupport.setupToolbar(toolsToolbar, toolsActionGroup);
    }

    public void scheduledUpdate() {
        toolsActionGroup.update();
    }

    /**
     * Checks if the chat pane is currently in a waiting state.
     *
     * @return {@code true} if the chat pane is waiting for a response, {@code false} otherwise.
     */
    public boolean isWaitState() {
        return waitState;
    }

    /**
     * Sets the waiting state of the chat pane.
     *
     * @param waitState {@code true} to put the chat pane in a waiting state, {@code false} otherwise.
     */
    public void setWaitState(boolean waitState) {
        setWaitState(waitState, null);
    }

    /**
     * Sets the waiting state of the chat pane.
     *
     * @param waitState {@code true} to put the chat pane in a waiting state, {@code false} otherwise.
     * @param message   The message to display while waiting. If {@code null}, a default "Waiting..." message is used.
     */
    public void setWaitState(boolean waitState, String message) {
        if (this.waitState == waitState)
            return;

        this.waitState = waitState;

        if (this.waitState) {
            lblWaiting.setText(message != null ? message : "Waiting...");
            if (waitStateTimer == null)
                waitStateTimer = new Timer(500, e -> waitPanel.setVisible(true));
            waitStateTimer.start();
        } else {
            if (waitStateTimer != null) {
                waitStateTimer.stop();
                waitStateTimer = null;
            }
            waitPanel.setVisible(false);
        }
        firePropertyChange("waitState", !waitState, waitState);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(FormPanel.PROPERTY_VALUE_CHANGED))
            updateSendButton();
        else if (evt.getPropertyName().equals(FormPanel.PROPERTY_ENTER_PRESSED))
            sendMessage();
    }

    private void setupActions() {
        renderMarkdownAction = new StateAction("Render Markdown", new AutoIcon(ICON_DOCUMENT), null,
                e -> getOptions().setRenderMarkdown(!getOptions().isRenderMarkdown()),
                a -> a.setSelected(getOptions().isRenderMarkdown()));
        clearAfterSendingAction = new StateAction("Clear Prompt After Sending", new AutoIcon(ICON_CHAT), null,
                e -> getOptions().setClearAfterSending(!getOptions().isClearAfterSending()),
                a -> a.setSelected(getOptions().isClearAfterSending()));

        BasicAction resendAction = new BasicAction("Resend", new AutoIcon(ICON_TOOLBAR_SEND), e ->
                resendLast(),
                a -> a.setEnabled(canResendLast()));
        resendAction.setShortDescription("Resend");
        toolsActionGroup = new ActionGroup(
                new ActionGroup(
                        resendAction
                )
        );

        UISupport.setupToolbar(toolsToolbar, toolsActionGroup);
    }

    private boolean canResendLast() {
        return lastUserMessage != null;
    }

    private void resendLast() {
        if (lastUserMessage != null) {
            if (lastUserMessage.rawMessage() instanceof Map<?, ?>)
                //noinspection unchecked
                setUserMessage((Map<String, Object>) lastUserMessage.rawMessage());
            else
                setUserMessage(lastUserMessage.rawMessage().toString());
        }

    }

    /**
     * Returns the ChatMessagesPane instance associated with this ChatPane.
     *
     * @return The ChatMessagesPane instance.
     */
    public ChatMessagesPane getChatMessagesPane() {
        return getChatMessagesHostPane().getChatMessagesPane();
    }

    ChatMessagesHostPane getChatMessagesHostPane() {
        return chatMessagesHostPane;
    }

    private void updateSendButton() {
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
        boolean result = edtMessage.setFormValues(userMessage);
        if (!result && userMessage != null && !userMessage.isEmpty())
            setUserMessage(userMessage.toString());
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
        if (waitingForResponse)
            return;

        Map<String, Object> message = getUserMessage();
        if (message != null && !message.isEmpty()) {
            String uid = UUID.randomUUID().toString();
            message.put(KEY_SESSION_UID, uid);

            if (getChatMessages().isEmpty())
                addSystemChatMessage(message);

            lastUserMessage = chatMessageForMap(uid, message, true);
            addChatMessage(lastUserMessage);
            if (UISupport.getOptions().isClearAfterSending())
                edtMessage.clearContent();
            edtMessage.requestFocus();

            setWaitingForResponse(true);
            chatMessagesHostPane.addTypingIndicator();
            CompletableFuture.supplyAsync(() -> {
                Object response = chatEngine.send(message);
                return chatMessageForResponse(response);
            }).whenComplete(this::processChatEngineResponse);
        }
    }

    private void addSystemChatMessage(Map<String, Object> message) {
        String systemMessageTemplate = chatEngine.getSystemMessageTemplate();
        if (systemMessageTemplate != null && !systemMessageTemplate.isEmpty())
            addChatMessage(new ChatMessage(UUID.randomUUID().toString(), null, EasyWorkflow.expandTemplate(systemMessageTemplate, message), null, ChatMessage.Type.System));
    }

    private ChatMessage chatMessageForResponse(Object response) {
        if (response.getClass().isArray())
            response = Arrays.asList((Object[]) response);

        String uid = UUID.randomUUID().toString();
        if (response instanceof List<?> list) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (result.length() > 0)
                    result.append("\n");
                Object element = list.get(i);
                result.append("%d. ".formatted(i + 1));
                try {
                    Map<?, ?> map = OBJECT_MAPPER.convertValue(element, Map.class);
                    result.append((chatMessageForMap(uid, map, false).message()));
                } catch (IllegalArgumentException ex) {
                    result.append(element.toString());
                    logger.warn("Failed to convert element to map: " + element, ex);
                }
            }
            return new ChatMessage(uid, response, result.toString(), convertMarkdownToHtml(result.toString()), ChatMessage.Type.Agent);
        } else if (response instanceof Map<?, ?> responseMap) {
            return chatMessageForMap(uid, responseMap, false);
        } else if (response instanceof String) {
            String responseString = response.toString();
            String responseAsHtml = convertMarkdownToHtml(responseString);
            return new ChatMessage(uid, response, responseString, responseAsHtml, ChatMessage.Type.Agent);
        } else {
            ChatMessage chatMessage = new ChatMessage(uid, response, response.toString(), null, ChatMessage.Type.Agent);
            if (response instanceof Number || response instanceof Boolean || response.getClass().isEnum()) {
                return chatMessage;
            } else {
                try {
                    return chatMessageForMap(uid, OBJECT_MAPPER.convertValue(response, Map.class), false);
                } catch (Exception e) {
                    return chatMessage;
                }
            }
        }
    }

    private ChatMessage chatMessageForMap(String uid, Map<?, ?> message, boolean isFromUser) {
        String text = null;
        String html = null;

        String userMessage = null;
        if (isFromUser) {
            String template = chatEngine.getUserMessageTemplate();
            if (template != null && !template.isEmpty()) {
                try {
                    //noinspection unchecked
                    userMessage = EasyWorkflow.expandTemplate(template, (Map<String, Object>) message);
                } catch (Exception ex) {
                    logger.error("Failed to expand template: %s".formatted(template), ex);
                }
            }
        }

        boolean isSimplified = false;

        if (message.size() == 1) {
            text = message.values().iterator().next().toString();
            isSimplified = userMessage == null || text.equals(userMessage);
        }

        if (!isSimplified) {
            StringBuilder markdown = new StringBuilder();
            final String format = "**%s**: %s<br>";
            if (userMessage != null && !userMessage.isEmpty())
                markdown.append(String.format(format, "userMessage", userMessage));
            message.forEach((key, value) -> {
                if (!key.equals(KEY_SESSION_UID))
                    markdown.append(String.format(format, key, value));
            });
            text = markdown.toString();
            html = convertMarkdownToHtml(text);
        }

        return new ChatMessage(uid, message, text, html, isFromUser ? ChatMessage.Type.User : ChatMessage.Type.Agent);
    }

    private void processChatEngineResponse(ChatMessage result, Throwable ex) {
        SwingUtilities.invokeLater(() -> {
            if (ex == null) {
                addChatMessage(result);
            } else {
                final String error = "Failed to get response from chat.";
                logger.error(error, ex);
                addChatMessage(new ChatMessage(UUID.randomUUID().toString(), null, "🛑 " + error + " " + ex.getMessage(), null, ChatMessage.Type.Agent));
            }
            setWaitingForResponse(false);
            chatMessagesHostPane.removeTypingIndicator();
            updateSendButton();
        });
    }

    private void setWaitingForResponse(boolean isWaitingForResponse) {
        if (waitingForResponse != isWaitingForResponse) {
            waitingForResponse = isWaitingForResponse;
            updateSendButton();
        }
    }

    private void addChatMessage(ChatMessage message) {
        chatMessagesHostPane.addChatMessage(message);
        if (message.type() == ChatMessage.Type.User)
            chatMessagesHostPane.getChatMessagesPane().selectChatMessage(message);
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
            String label = null;
            String description = null;
            FormEditorType editorType = FormEditorType.Default;
            if (parameter.getType() == String.class)
                editorType = FormEditorType.Note;
            String[] editorChoices = null;

            PlaygroundParam playgroundParam = parameter.getAnnotation(PlaygroundParam.class);
            if (playgroundParam != null) {
                if (!playgroundParam.label().isEmpty())
                    label = playgroundParam.label();
                if (!playgroundParam.description().isEmpty())
                    description = playgroundParam.description();
                editorType = playgroundParam.editorType();
                editorChoices = playgroundParam.editorChoices();
            }
            V v = parameter.getAnnotation(V.class);
            if (v != null)
                parameterName = v.value();

            formElements.add(new FormPanel.FormElement(parameterName,
                    label,
                    description,
                    parameter.getType(),
                    null,
                    editorType,
                    editorChoices, true
            ));
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
        if (waitStateTimer != null) {
            waitStateTimer.stop();
            waitStateTimer = null;
        }
    }

    /**
     * Returns the chat messages as a JSON string.
     *
     * @return A JSON string representation of the chat messages, or {@code null} if there are no messages or an error
     * occurs during serialization.
     */
    public String getChatMessagesAsJson() {
        if (getChatMessages().isEmpty())
            return null;

        try {
            return OBJECT_MAPPER.writeValueAsString(getChatMessages());
        } catch (JsonProcessingException ex) {
            logger.error("Failed to serialize chat messages", ex);
        }
        return null;
    }

    /**
     * Copies the chat messages as a JSON string to the system clipboard.
     */
    public void copy() {
        String content = getChatMessagesAsJson();
        if (content != null) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(content), null);
        }
    }

    /**
     * Returns the current {@link ExecutionDetailsProvider} for this chat pane.
     *
     * @return The {@link ExecutionDetailsProvider} instance.
     */
    public ExecutionDetailsProvider getExecutionDetailsProvider() {
        return executionDetailsProvider;
    }

    /**
     * Sets the {@link ExecutionDetailsProvider} for this chat pane.
     *
     * @param aExecutionDetailsProvider The {@link ExecutionDetailsProvider} to set.
     */
    public void setExecutionDetailsProvider(ExecutionDetailsProvider aExecutionDetailsProvider) {
        executionDetailsProvider = aExecutionDetailsProvider;
    }

    /**
     * Interface for providing execution details for a given chat message.
     */
    public interface ExecutionDetailsProvider {

        /**
         * Displays the execution details for a given chat message.
         *
         * @param chatMessage The chat message for which to display execution details.
         * @param completion  The completion lambda that will be called when the execution details are displayed.
         */
        void showExecutionDetails(ChatMessage chatMessage, Runnable completion);
    }

    /**
     * Checks if execution details can be shown.
     *
     * @return {@code true} if an {@link ExecutionDetailsProvider} is set, {@code false} otherwise.
     */
    public boolean canShowExecutionDetails() {
        return executionDetailsProvider != null && ! waitingForResponse;
    }

    /**
     * Displays the execution details for a given chat message using the configured {@link ExecutionDetailsProvider}.
     * If no provider is set, this method does nothing.
     *
     * @param chatMessage The chat message for which to display execution details.
     */
    public void showExecutionDetails(ChatMessage chatMessage) {
        if (executionDetailsProvider != null) {
            getChatMessagesPane().selectChatMessage(chatMessage);
            waitingForResponse = true;
            updateSendButton();
            executionDetailsProvider.showExecutionDetails(chatMessage, () -> {
                waitingForResponse = false;
                updateSendButton();
            });
        }
    }

    /**
     * Interface for defining a chat engine that can send messages and provide parameter information.
     */
    public interface ChatEngine {
        Object send(Map<String, Object> message);

        Parameter[] getMessageParameters();

        String getUserMessageTemplate();

        String getSystemMessageTemplate();
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
