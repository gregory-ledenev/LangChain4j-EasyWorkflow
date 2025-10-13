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

import javax.swing.*;
import java.awt.*;

/**
 * A component that hosts a ChatMessagesPane within a scrollable view and provides a
 * "Scroll to Bottom" button.
 */
public class ChatMessagesHostPane extends JPanel {
    private final ChatMessagesPane chatMessagesPane;
    private final JScrollPane scrollPane;
    private final JButton btnScrollToBottom;
    private Timer updateScrollToBottomButtonTimer;
    private Timer scrollToBottomTimer;
    private boolean hasNewData;

    /**
     * Constructs a new ChatMessagesHostPane.
     */
    public ChatMessagesHostPane() {
        super(new BorderLayout());
        setOpaque(false);

        chatMessagesPane = new ChatMessagesPane();
        scrollPane = new JScrollPane(chatMessagesPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);

        btnScrollToBottom = new JButton("↓ ↓ ↓");
        btnScrollToBottom.setToolTipText("Scroll to Bottom");
        btnScrollToBottom.addActionListener(e -> scrollToBottom());
        btnScrollToBottom.setVisible(false);
        btnScrollToBottom.setFocusable(false);

        JLayeredPane layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(btnScrollToBottom, JLayeredPane.PALETTE_LAYER);

        layeredPane.setLayout(new LayoutManager() {
            @Override
            public void addLayoutComponent(String name, Component comp) {}

            @Override
            public void removeLayoutComponent(Component comp) {}

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return scrollPane.getPreferredSize();
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return scrollPane.getMinimumSize();
            }

            @Override
            public void layoutContainer(Container parent) {
                scrollPane.setBounds(0, 0, parent.getWidth(), parent.getHeight());

                int buttonWidth = 120;
                int buttonHeight = 24;
                int x = (parent.getWidth() - buttonWidth) / 2;
                int y = parent.getHeight() - buttonHeight - 5;
                btnScrollToBottom.setBounds(x, y, buttonWidth, buttonHeight);
            }
        });

        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            if (scrollBar.isVisible() && ! e.getValueIsAdjusting()) {
                updateScrollToBottomButtonLater();
                if (isAtBottom())
                    setHasNewData(false);
            }
        });
    }

    private void updateScrollToBottomButtonLater() {
        if (updateScrollToBottomButtonTimer == null) {
            updateScrollToBottomButtonTimer = new Timer(200, e -> updateScrollToBottomButton());
            updateScrollToBottomButtonTimer.setRepeats(false);
        }
        updateScrollToBottomButtonTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (updateScrollToBottomButtonTimer != null)
            updateScrollToBottomButtonTimer.stop();
        if (scrollToBottomTimer != null)
            scrollToBottomTimer.stop();
    }

    private void updateScrollToBottomButton() {
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        int value = scrollBar.getValue();
        int extent = scrollBar.getModel().getExtent();
        int max = scrollBar.getMaximum();
        boolean isAtBottom = max - (value + extent) <= 10;
        boolean shouldBeVisible = !isAtBottom;
        if (btnScrollToBottom.isVisible() != shouldBeVisible) {
            btnScrollToBottom.setVisible(shouldBeVisible);
        }
    }

    /**
     * Returns the ChatMessagesPane hosted by this component.
     *
     * @return The ChatMessagesPane instance.
     */
    public ChatMessagesPane getChatMessagesPane() {
        return chatMessagesPane;
    }

    /**
     * Adds a new chat message to the ChatMessagesPane.
     * If the view is already at the bottom, it will auto-scroll.
     *
     * @param chatMessage The ChatMessage to add.
     */
    public void addChatMessage(ChatMessage chatMessage) {
        boolean wasAtBottom = isAtBottom();
        getChatMessagesPane().addChatMessage(chatMessage);
        maybeScrollToBottom(wasAtBottom);
    }

    private void maybeScrollToBottom(boolean wasAtBottom) {
        if (wasAtBottom) {
            scrollToBottomLater();
        } else {
            setHasNewData(true);
        }
    }

    /**
     * Removes the typing indicator from the ChatMessagesPane.
     * If the view is already at the bottom, it will auto-scroll.
     */
    public void removeTypingIndicator() {
        boolean wasAtBottom = isAtBottom();
        getChatMessagesPane().removeTypingIndicator();
        maybeScrollToBottom(wasAtBottom);
    }

    /**
     * Adds a typing indicator to the ChatMessagesPane.
     * If the view is already at the bottom, it will auto-scroll.
     */
    public void addTypingIndicator() {
        boolean wasAtBottom = isAtBottom();
        getChatMessagesPane().addTypingIndicator();
        maybeScrollToBottom(wasAtBottom);
    }

    private boolean isAtBottom() {
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        if (!scrollBar.isVisible()) {
            return true;
        }
        int value = scrollBar.getValue();
        int extent = scrollBar.getModel().getExtent();
        int max = scrollBar.getMaximum();
        return max - (value + extent) <= 10;
    }

    private void scrollToBottomLater() {
        if (scrollToBottomTimer == null) {
            scrollToBottomTimer = new Timer(50, e -> scrollToBottom());
            scrollToBottomTimer.setRepeats(false);
        }
        scrollToBottomTimer.start();
    }

    private void scrollToBottom() {
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
    }

    public boolean isHasNewData() {
        return hasNewData;
    }

    public void setHasNewData(boolean aHasNewData) {
        if (hasNewData != aHasNewData) {
            hasNewData = aHasNewData;
            btnScrollToBottom.setForeground(hasNewData ? Color.GREEN : null);
        }
    }
}
