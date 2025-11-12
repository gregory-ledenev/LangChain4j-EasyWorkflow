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
import java.awt.*;

/**
 * A component that displays a typing indicator with an animating ellipsis.
 * This indicator can be started and stopped, and it automatically manages its animation
 * when added to or removed from a parent component.
 */
@SuppressWarnings("ALL")
public class TypingIndicator extends JPanel {
    private final JLabel typingLabel = new JLabel();
    private boolean animating;
    private Timer timer;

    /**
     * Constructs a new TypingIndicator.
     * It initializes the layout and adds a static "Typing" label followed by the animating ellipsis label.
     */
    public TypingIndicator() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 5));
        JLabel label = new JLabel("  Thinking");
        label.setFont(label.getFont().deriveFont(15f));
        add(label);
        add(typingLabel);
    }

    /**
     * Checks if the typing indicator is currently animating.
     *
     * @return {@code true} if the animation is active, {@code false} otherwise.
     */
    public boolean isAnimating() {
        return animating;
    }

    /**
     * Sets whether the typing indicator should be animating.
     * If the state changes, it starts or stops the animation timer accordingly.
     * @param animating {@code true} to start the animation, {@code false} to stop it.
     */
    public void setAnimating(boolean animating) {
        if (this.animating != animating) {
            this.animating = animating;
            if (animating) {
                typingLabel.setText(".");
                if (timer == null)
                    timer = new Timer(750, e -> updateTypingLabel());
                timer.setRepeats(true);
                timer.start();
            } else {
                timer.stop();
            }
        }
    }

    private void updateTypingLabel() {
        if (typingLabel.getText().length() < 5)
            typingLabel.setText(typingLabel.getText() + ".");
        else
            typingLabel.setText(".");
    }

    @Override
    public void addNotify() {
        super.addNotify();
        setAnimating(true);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        setAnimating(false);
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
