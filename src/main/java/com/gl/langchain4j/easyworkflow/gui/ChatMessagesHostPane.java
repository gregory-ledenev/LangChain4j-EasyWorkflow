package com.gl.langchain4j.easyworkflow.gui;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * A JScrollPane subclass that hosts a ChatMessagesPane and provides functionality
 * for scrolling.
 */
public class ChatMessagesHostPane extends JScrollPane {
    private boolean needScrollToBottom;

    /**
     * Constructs a new ChatMessagesHostPane.
     * It initializes with a new ChatMessagesPane as its viewport view.
     */
    public ChatMessagesHostPane() {
        super(new ChatMessagesPane());
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    @Override
    public void updateUI() {
        super.updateUI();

        setBorder(null);
    }

    /**
     * Returns the ChatMessagesPane hosted by this scroll pane.
     *
     * @return The ChatMessagesPane instance.
     */
    public ChatMessagesPane getChatMessagesPane() {
        return (ChatMessagesPane) getViewport().getView();
    }

    /**
     * Adds a new chat message to the ChatMessagesPane and scrolls to the bottom.
     *
     * @param chatMessage The ChatMessage to add.
     */
    public void addChatMessage(ChatMessage chatMessage) {
        getChatMessagesPane().addChatMessage(chatMessage);
        scrollToBottomLater();
    }

    /**
     * Removes the typing indicator from the ChatMessagesPane and scrolls to the bottom.
     */
    public void removeTypingIndicator() {
        getChatMessagesPane().removeTypingIndicator();
        scrollToBottomLater();
    }

    /**
     * Adds a typing indicator to the ChatMessagesPane and scrolls to the bottom.
     */
    public void addTypingIndicator() {
        getChatMessagesPane().addTypingIndicator();
        scrollToBottomLater();
    }

    private void scrollToBottomLater() {
        Timer timer = new Timer(500, e -> scrollToBottom());
        timer.setRepeats(false);
        timer.start();
    }

    private void scrollToBottom() {
        JScrollBar verticalScrollBar = getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
    }
}
