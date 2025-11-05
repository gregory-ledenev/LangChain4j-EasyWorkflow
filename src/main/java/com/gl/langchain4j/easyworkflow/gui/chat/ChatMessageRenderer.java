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

import com.gl.langchain4j.easyworkflow.gui.Actions;
import com.gl.langchain4j.easyworkflow.gui.UISupport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.View;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;

import static com.gl.langchain4j.easyworkflow.gui.Actions.*;
import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.*;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;
import static com.gl.langchain4j.easyworkflow.gui.chat.ChatPane.getChatPane;

/**
 * Renders a single chat message within the chat interface. This panel displays the message content, handles markdown
 * rendering, and provides context menu options like copy and resend.
 */
public class ChatMessageRenderer extends JPanel implements Scrollable {
    private final ChatMessageTextPane textPane;
    private final ChatMessage chatMessage;
    private final ChatMessageRendererBubble bubbleBackground;

    private int lastWidth;

    /**
     * Constructs a ChatMessageRenderer for a given chat message.
     *
     * @param chatMessage The chat message to be rendered.
     */
    public ChatMessageRenderer(ChatMessage chatMessage) {
        super(new GridBagLayout()); // Use GridBagLayout for robust alignment
        this.chatMessage = chatMessage;

        textPane = setupChatMessageTextPane(chatMessage);

        setOpaque(false);
        setBorder(new EmptyBorder(5, 10, 5, 10));

        bubbleBackground = new ChatMessageRendererBubble(this.chatMessage, 15);
        bubbleBackground.add(textPane, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = chatMessage.outgoing() ? GridBagConstraints.LINE_END : GridBagConstraints.LINE_START;
        gbc.insets = new Insets(0, chatMessage.outgoing() ? 20 : 0, 0, chatMessage.outgoing() ? 0 : 20);
        add(bubbleBackground, gbc);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateLayout(getTextPaneWidth(), false);
            }
        });

        setupPopupMenu();
    }

    int getTextPaneWidth() {
        Insets borderInsets = bubbleBackground.getBorder().getBorderInsets(bubbleBackground);
        return getWidth() - borderInsets.left - borderInsets.right;
    }

    /**
     * Updates the text pane's content based on the chat message and markdown rendering preference.
     */
    public void updateFromChatMessage() {
        String text = isRenderMarkdown() ?
                chatMessage.bestMessage() :
                chatMessage.message();

        String color = chatMessage.outgoing() ?
                "White" :
                isDarkAppearance() ? "White" : "DarkGray";
        textPane.setText("""
                         <!DOCTYPE html>
                         <html>
                         <head>
                         <style>
                           body {
                             color: %s;
                           }
                         </style>
                         </head>""".formatted(color) + text.replace("\n", "<br>") + "</html>");
    }

    private ChatMessageTextPane setupChatMessageTextPane(ChatMessage chatMessage) {
        final ChatMessageTextPane textPane;
        textPane = new ChatMessageTextPane();
        textPane.setForeground(chatMessage.outgoing() ? Color.WHITE : Color.DARK_GRAY);
        return textPane;
    }

    private void setupPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        ActionGroup actionGroup = new ActionGroup(
                new ActionGroup(
                        new BasicAction("Copy", new AutoIcon(ICON_COPY), e -> copy())
                ),
                new ActionGroup(
                        new BasicAction("Resend", new AutoIcon(ICON_SEND), e -> resend()),
                        new BasicAction("Execution Details", new AutoIcon(ICON_TOOLBAR_PLAY),
                                e -> showExecutionDetails(),
                                a -> a.setEnabled(canShowExecutionDetails()))
                ),
                new ActionGroup(
                        new StateAction("Render Markdown", new AutoIcon(ICON_DOCUMENT),
                                null,
                                e -> setRenderMarkdown(! isRenderMarkdown()),
                                a -> a.setSelected(isRenderMarkdown()))
                )
        );
        UISupport.setupPopupMenu(popupMenu, actionGroup);
        setComponentPopupMenu(popupMenu);
        textPane.setComponentPopupMenu(popupMenu);
    }

    private boolean canShowExecutionDetails() {
        return chatMessage.type() == ChatMessage.Type.User &&
                getChatPane(this).getExecutionDetailsProvider() != null;
    }

    private void showExecutionDetails() {
        ChatPane.ExecutionDetailsProvider executionDetailsProvider = getChatPane(this).getExecutionDetailsProvider();
        if (executionDetailsProvider != null)
            executionDetailsProvider.showExecutionDetails(chatMessage);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        updateFromChatMessage();
    }

    public boolean isRenderMarkdown() {
        return ChatMessagesPane.getChatMessagesPane(this).isRenderMarkdown();
    }

    /**
     * Sets whether markdown should be rendered for chat messages.
     *
     * @param isRenderMarkdown True to render markdown, false otherwise.
     */
    public void setRenderMarkdown(boolean isRenderMarkdown) {
        ChatMessagesPane.getChatMessagesPane(this).setRenderMarkdown(isRenderMarkdown);
    }

    private void copy() {
        String text = textPane.getSelectedText();
        if (text == null || text.isEmpty())
            text = chatMessage.message();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void resend() {
        String selectedText = textPane.getSelectedText();
        boolean selectedTextPresent = selectedText != null && !selectedText.isEmpty();

        //noinspection rawtypes
        if (! selectedTextPresent && chatMessage.rawMessage() instanceof Map map)
            //noinspection unchecked
            getChatPane(this).setUserMessage(map);
        else {
            getChatPane(this).setUserMessage(selectedTextPresent ?
                    selectedText :
                    chatMessage.message());
        }
    }

    void updateLayout(int availableWidth, boolean forceUpdate) {
        if (!forceUpdate && (availableWidth == 0 || availableWidth == lastWidth))
            return; // Avoid unnecessary recalculations

        lastWidth = availableWidth;

        int maxBubbleWidth = availableWidth - 50;
        textPane.updatePreferredSize(maxBubbleWidth);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    static class ChatMessageTextPane extends JEditorPane {
        private Dimension preferredSize = new Dimension(0, 0);

        public ChatMessageTextPane() {

            HTMLEditorKit kit = new HTMLEditorKit();

            setEditorKit(kit);

            StyleSheet styleSheet = kit.getStyleSheet();

            styleSheet.addRule("ul, ol { padding-top: 0; padding-bottom: 0; }");
            styleSheet.addRule("ul, ol { margin-top: -5; margin-bottom: -5: margin-left-ltr: 50; margin-right-rtl: 50; list-style-type: decimal;}");
            styleSheet.addRule("li { margin-top: 0; margin-bottom: 0; padding-top: 0; padding-bottom: 0; }");
            styleSheet.addRule("p { margin-top: 0; margin-bottom: 0; padding-top: 0; padding-bottom: 0; }");
            styleSheet.addRule("ul, ol, li, p { line-height: 1.0; }");

            setEditable(false);
            setContentType("text/html");
            setOpaque(false);
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            setFont(getFont().deriveFont(15f));
        }

        public static int getPreferredHeight(JEditorPane editor, int width) {
            editor.setSize(width, Short.MAX_VALUE); // Set fixed width, max height
            View view = editor.getUI().getRootView(editor);
            Insets insets = editor.getInsets();
            view.setSize(width - insets.left - insets.right - 1, 0); // Trigger layout for width
            return (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + 4; // Get height
        }

        public void updatePreferredSize(int fixedWidth) {
            Dimension superPreferredSize = super.getPreferredSize();
            if (superPreferredSize.getWidth() < fixedWidth)
                preferredSize = superPreferredSize;
            else
                preferredSize = new Dimension(fixedWidth, getPreferredHeight(this, fixedWidth));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return preferredSize;
        }
    }
}
