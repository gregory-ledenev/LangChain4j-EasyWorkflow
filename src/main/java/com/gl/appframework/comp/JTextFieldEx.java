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

package com.gl.appframework.comp;

import com.gl.appframework.UISupport;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.util.Objects;

/**
 * An extended {@link JTextField} that supports placeholder text.
 */
public class JTextFieldEx extends JTextField {
    private String placeholderText;

    /**
     * {@inheritDoc}
     */
    public JTextFieldEx() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public JTextFieldEx(String text) {
        super(text);
    }

    /**
     * {@inheritDoc}
     */
    public JTextFieldEx(int columns) {
        super(columns);
    }

    /**
     * {@inheritDoc}
     */
    public JTextFieldEx(String text, int columns) {
        super(text, columns);
    }

    /**
     * {@inheritDoc}
     */
    public JTextFieldEx(Document doc, String text, int columns) {
        super(doc, text, columns);
    }

    /**
     * Gets the text that is displayed when the text field is empty.
     *
     * @return the placeholder text
     */
    public String getPlaceholderText() {
        return placeholderText;
    }

    /**
     * Sets the text that is displayed when the text field is empty.
     *
     * @param placeholderText the text to display as a placeholder
     */
    public void setPlaceholderText(String placeholderText) {
        if (Objects.equals(this.placeholderText, placeholderText))
            return;

        String oldPlaceholderText = this.placeholderText;
        this.placeholderText = placeholderText;
        firePropertyChange("placeholderText", oldPlaceholderText, placeholderText);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholderText == null || placeholderText.isEmpty() || !getText().isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            Color placeholderColor = getDisabledTextColor();
            if (placeholderColor == null) {
                placeholderColor = Color.GRAY;
            }

            g2.setColor(UISupport.isDarkAppearance() ? placeholderColor.darker() : placeholderColor.brighter());
            g2.setFont(getFont());

            FontMetrics fm = g2.getFontMetrics();
            Insets insets = getInsets();

            int x = insets.left;
            int textWidth = fm.stringWidth(placeholderText);
            int availableWidth = getWidth() - insets.left - insets.right;

            int alignment = getHorizontalAlignment();
            if (alignment == JTextField.CENTER) {
                x = insets.left + (availableWidth - textWidth) / 2;
            } else if (alignment == JTextField.RIGHT || alignment == JTextField.TRAILING) {
                x = getWidth() - insets.right - textWidth;
            }

            int y = insets.top + (getHeight() - insets.top - insets.bottom - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(placeholderText, x, y);
        } finally {
            g2.dispose();
        }
    }
}
