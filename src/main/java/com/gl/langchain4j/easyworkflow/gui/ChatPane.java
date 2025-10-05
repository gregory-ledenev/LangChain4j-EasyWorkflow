/*
 *
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
 * /
 */

package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.UISupport.AutoIcon;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

/**
 * A panel that provides a chat interface, including message input, display, and settings.
 */
public class ChatPane extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ChatPane.class);

    private final ChatMessagesHostPane chatMessagesHostPane = new ChatMessagesHostPane();
    private final MessageEditor edtMessage = new MessageEditor();
    private final JButton btnSend = new JButton("➤");
    private final JButton btnSettings;
    private Function<String, String> chatEngine;
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

        setupMessageEditorPopupMenu();
        edtMessage.setPlaceHolderText("Type a prompt...");
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

        btnSend.setFont(new Font("SansSerif", Font.BOLD, 30));
        btnSend.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnSend.addActionListener(e -> sendMessage());
        btnSend.setEnabled(false);
        btnSend.setToolTipText("Send");
        buttons.add(btnSend);

        inputPanel.add(buttons, BorderLayout.EAST);

        edtMessage.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        edtMessage.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendMessage();
            }
        });
        edtMessage.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSendButon();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSendButon();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSendButon();
            }
        });
    }

    /**
     * Retrieves the ChatPane instance that contains the given subComponent.
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
     * @param darkAppearance The appearance to apply (Light, Dark, or Auto).
     * @param chatPane The ChatPane instance to apply the appearance to.
     */
    public static void applyAppearance(Appearance darkAppearance, ChatPane chatPane) {
        UISupport.applyAppearance(darkAppearance);
        SwingUtilities.updateComponentTreeUI(chatPane.getParent());
        chatPane.chatMessagesHostPane.getChatMessagesPane().updateRenderers();
    }

    private void setupMessageEditorPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(createAction("Copy", new AutoIcon(ICON_COPY), e -> edtMessage.copy()));
        popupMenu.add(createAction("Paste", new AutoIcon(ICON_PASTE), e -> edtMessage.paste()));
        popupMenu.add(new JSeparator());
        popupMenu.add(createAction("Clear", new AutoIcon(ICON_CLEAR), e -> edtMessage.setText("")));
        edtMessage.setComponentPopupMenu(popupMenu);
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

        JMenuItem mniRenderMarkdown = new JCheckBoxMenuItem(createAction("Render Markdown", new AutoIcon(ICON_DOCUMENT), e -> getChatMessagesPane().setRenderMarkdown(!getChatMessagesPane().isRenderMarkdown())));
        mniRenderMarkdown.setSelected(getChatMessagesPane().isRenderMarkdown());
        popupMenu.add(mniRenderMarkdown);

        final AboutProvider aboutProvider = (AboutProvider) SwingUtilities.getAncestorOfClass(AboutProvider.class, this);
        if (aboutProvider != null) {
            popupMenu.add(new JSeparator());
            popupMenu.add(new JCheckBoxMenuItem(createAction("About", new AutoIcon(ICON_HELP), e -> aboutProvider.showAbout(this))));
        }

        popupMenu.pack();
        Dimension size = popupMenu.getPreferredSize();
        popupMenu.show(btnSettings, btnSend.getWidth() - size.width, -size.height);
    }

    /**
     * Returns the ChatMessagesPane instance.
     * @return The ChatMessagesPane instance.
     */
    private ChatMessagesPane getChatMessagesPane() {
        return getChatMessagesHostPane().getChatMessagesPane();
    }

    ChatMessagesHostPane getChatMessagesHostPane() {
        return chatMessagesHostPane;
    }

    private void updateSendButon() {
        btnSend.setEnabled(!waitingForResponse && !edtMessage.getText().trim().isEmpty());
    }

    private void sendMessage() {
        String message = edtMessage.getText();
        if (!message.trim().isEmpty()) {
            addChatMessage(new ChatMessage(message, null, true));
            edtMessage.setText("");
            edtMessage.requestFocus();

            waitingForResponse = true;
            chatMessagesHostPane.addTypingIndicator();
            CompletableFuture.supplyAsync(() -> {
                String response = chatEngine.apply(message);
                String responseAsHtml = convertMarkdownToHtml(response);

                return new ChatMessage(response, responseAsHtml, false);
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
                addChatMessage(new ChatMessage("🛑 " + error + " " + ex.getMessage(), null, false));
            }
            waitingForResponse = false;
            chatMessagesHostPane.removeTypingIndicator();
            updateSendButon();
        });
    }

    private void addChatMessage(ChatMessage message) {
        chatMessagesHostPane.addChatMessage(message);
    }

    /**
     * Returns the currently set chat engine.
     * @return The chat engine function.
     */
    public Function<String, String> getChatEngine() {
        return chatEngine;
    }

    /**
     * Sets the chat engine function. This function will be called to get responses to user messages.
     * @param chatEngine The function that takes a user message (String) and returns a response (String).
     * @throws NullPointerException if chatEngine is null.
     */
    public void setChatEngine(Function<String, String> chatEngine) {
        Objects.requireNonNull(chatEngine);
        this.chatEngine = chatEngine;
    }

    /**
     * Returns a list of all chat messages currently displayed.
     * @return A List of ChatMessage objects.
     */
    public List<ChatMessage> getChatMessages() {
        return getChatMessagesPane().getChatMessages();
    }

    /**
     * Returns the text currently in the message editor.
     * @return The user's current message.
     */
    public String getUserMessage() {
        return edtMessage.getText();
    }

    /**
     * Sets the text in the message editor.
     * @param userMessage The text to set.
     */
    public void setUserMessage(String userMessage) {
        edtMessage.setText(userMessage);
        updateSendButon();
    }

    public enum Appearance {
        Light, Dark, Auto
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

    static class MessageEditor extends JTextArea {
        private String placeHolderText;

        public MessageEditor() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setRows(3);
            setFont(new Font("SansSerif", Font.PLAIN, 14));
        }

        public String getPlaceHolderText() {
            return placeHolderText;
        }

        public void setPlaceHolderText(String aPlaceHolderText) {
            placeHolderText = aPlaceHolderText;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (placeHolderText != null && getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIManager.getColor("textInactiveText"));
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(placeHolderText, getInsets().left, getInsets().top + metrics.getAscent());
                g2.dispose();
            }
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setOpaque(false);
        }
    }
}
