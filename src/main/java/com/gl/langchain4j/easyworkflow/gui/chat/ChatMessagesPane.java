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

import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.getOptions;

/**
 * A panel that displays a list of chat messages, supporting markdown rendering and a typing indicator.
 */
public class ChatMessagesPane extends JPanel implements Scrollable, PropertyChangeListener {
    List<ChatMessage> chatMessages = new ArrayList<>();
    Map<ChatMessage, ChatMessageRenderer> chatMessageRenderers = new HashMap<>();
    private TypingIndicator typingIndicator;

    @Override
    public boolean isOpaque() {
        return false;
    }

    /**
     * Constructs a new ChatMessagesPane.
     * Initializes the layout and sets up a popup menu.
     */
    public ChatMessagesPane() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new JMenuItem("Some Text"));
        popupMenu.add(new JMenuItem("Some Text 123"));
        setComponentPopupMenu(popupMenu);
    }

    /**
     * Returns an unmodifiable list of the chat messages currently displayed in the pane.
     *
     * @return A {@link List} of {@link ChatMessage} objects.
     */
    public List<ChatMessage> getChatMessages() {
        return Collections.unmodifiableList(chatMessages);
    }

    /**
     * Retrieves the {@code ChatMessagesPane} ancestor of the given sub-component.
     *
     * @param subComponent The component from which to start searching for the parent {@code ChatMessagesPane}.
     * @return The {@code ChatMessagesPane} ancestor, or {@code null} if not found.
     */
    public static ChatMessagesPane getChatMessagesPane(JComponent subComponent) {
        Container parent = subComponent.getParent();
        while (parent != null && !(parent instanceof ChatMessagesPane)) {
            parent = parent.getParent();
        }
        return (ChatMessagesPane) parent;
    }

    /**
     * Updates all chat message renderers, revalidates and repaints the pane.
     */
    public void updateRenderers() {
        UISupport.updateAndPreserveScrollPosition((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this),
                () -> {
                    for (ChatMessageRenderer renderer : chatMessageRenderers.values()) {
                        renderer.updateFromChatMessage();
                        renderer.updateLayout(renderer.getTextPaneWidth(), true);
                    }
                    revalidate();
                    repaint();
                });
    }

    /**
     * Adds a typing indicator to the pane. If one doesn't exist, it creates it.
     * Revalidates and repaints the pane.
     */
    public void addTypingIndicator() {
        if (typingIndicator == null)
            typingIndicator = new TypingIndicator();

        add(typingIndicator);
        revalidate();
        repaint();
    }

    /**
     * Removes the typing indicator from the pane if it exists.
     */
    public void removeTypingIndicator() {
        if (typingIndicator.getParent() != null) {
            remove(typingIndicator);
            revalidate();
            repaint();
        }
    }

    /**
     * Adds a new chat message to the pane.
     * @param chatMessage The chat message to add.
     */
    public void addChatMessage(ChatMessage chatMessage) {
        ChatMessageRenderer renderer = new ChatMessageRenderer(chatMessage);
        add(renderer);

        chatMessages.add(chatMessage);
        chatMessageRenderers.put(chatMessage, renderer);

        revalidate();
        repaint();
    }

    /**
     * Removes a chat message from the pane.
     * @param chatMessage The chat message to remove.
     */
    public void removeChatMessage(ChatMessage chatMessage) {
        ChatMessageRenderer renderer = chatMessageRenderers.remove(chatMessage);
        if (renderer != null) {
            remove(renderer);
            chatMessages.remove(chatMessage);
            revalidate();
            repaint();
        }
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
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void selectChatMessage(ChatMessage chatMessage) {
        for (ChatMessageRenderer value : chatMessageRenderers.values()) {
            value.setShowExecutionResults(value.getChatMessage() == chatMessage);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();

        getOptions().addPropertyChangeListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();

        getOptions().removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == getOptions() && evt.getPropertyName().equals("renderMarkdown")) {
            updateRenderers();
        }
    }
}