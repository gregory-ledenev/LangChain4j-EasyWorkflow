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

import static com.gl.langchain4j.easyworkflow.gui.UISupport.getDefaultBorderColor;

/**
 * A custom panel that serves as a header, displaying a title and an optional subtitle.
 * It provides methods to set and retrieve the title and subtitle.
 */
public class HeaderPane extends JPanel{
    private final JLabel lblTitle = new JLabel();
    private final JLabel lblSubtitle = new JLabel();

    /**
     * Constructs a new HeaderPane.
     * Initializes the layout, sets up the title and subtitle labels, and applies a border.
     */
    public HeaderPane() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        lblTitle.setFont(lblTitle.getFont().deriveFont(18f));
        add(lblTitle);
        add(lblSubtitle);
        lblSubtitle.setForeground(Color.gray);

        updateUI();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        setBorder(BorderFactory.createCompoundBorder(
                UISupport.createCustomLineBorder(getDefaultBorderColor(), false, false,true, false),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    }

    /**
     * Returns the current title text displayed in the header.
     *
     * @return The title text.
     */
    public String getTitle() {
        return lblTitle.getText();
    }

    /**
     * Sets the title text for the header.
     *
     * @param title The new title text to be displayed.
     */
    public void setTitle(String title) {
        lblTitle.setText(title);
    }

    /**
     * Returns the current subtitle text displayed in the header.
     *
     * @return The subtitle text.
     */
    public String getSubtitle() {
        return lblSubtitle.getText();
    }

    /**
     * Sets the subtitle text for the header.
     *
     * @param subtitle The new subtitle text to be displayed.
     */
    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle);
    }
}
