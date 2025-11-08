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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;

import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.isDarkAppearance;

/**
 * A custom panel that renders a chat message within a bubble shape.
 */
public class ChatMessageRendererBubble extends JPanel {
    private final int cornerRadius;
    private final ChatMessage chatMessage;

    /**
     * Constructs a new ChatMessageRendererBubble.
     *
     * @param chatMessage The chat message to be rendered.
     * @param cornerRadius The radius for the rounded corners of the bubble.
     */
    public ChatMessageRendererBubble(ChatMessage chatMessage, int cornerRadius) {
        super();
        
        this.chatMessage = chatMessage;
        setLayout(new BorderLayout());
        // The EmptyBorder provides padding around the content within the bubble.
        // This ensures that the text or other components inside the bubble
        // do not touch the edges of the bubble shape.
        setBorder(new EmptyBorder(10, 15, 10, 15));
        this.cornerRadius = cornerRadius;
        setOpaque(false);

        updateColors();
    }

    /**
     * Updates the background and foreground colors of the chat bubble based on the message sender and current UI appearance.
     */
    public void updateColors() {
        if (chatMessage == null)
            return;
        if (chatMessage.outgoing()) {
            setBackground(chatMessage.type() == ChatMessage.Type.User ?
                    new Color(0, 122, 255) :
                    new Color(32, 155, 255));
            setForeground(new Color(0, 80, 200));
        } else {
            if (isDarkAppearance()) {
                setBackground(new Color(64, 64, 64));
                setForeground(Color.GRAY);
            } else {
                setBackground(new Color(240, 240, 240));
                setForeground(Color.LIGHT_GRAY);
            }
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();

        updateColors();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1;
        GeneralPath path = getPath(cornerRadius, w);
        path.closePath();

        graphics.setColor(getBackground());
        graphics.fill(path);
        graphics.setColor(getForeground());
        graphics.draw(path);

        graphics.dispose();
    }

    private GeneralPath getPath(int r, int w) {
        int h = getHeight() - 1;

        GeneralPath path = new GeneralPath();
        if (chatMessage.outgoing()) {
            path.moveTo(r, 0);
            path.lineTo(w - r, 0);
            path.quadTo(w, 0, w, r);
            path.lineTo(w, h);
            path.lineTo(r, h);
            path.quadTo(0, h, 0, h - r);
            path.lineTo(0, r);
            path.quadTo(0, 0, r, 0);
        } else {
            path.moveTo(0, 0);
            path.lineTo(w - r, 0);
            path.quadTo(w, 0, w, r);
            path.lineTo(w, h - r);
            path.quadTo(w, h, w - r, h);
            path.lineTo(r, h);
            path.quadTo(0, h, 0, h - r);
            path.lineTo(0, 0);
        }
        return path;
    }
}
