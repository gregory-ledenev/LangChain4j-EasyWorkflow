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

package com.gl.langchain4j.easyworkflow.gui.platform;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

/**
 * A custom {@link JSplitPane} that provides a thin, visually distinct divider.
 * It extends {@link JSplitPane} and overrides its UI to use a custom {@link BasicSplitPaneUIEx}
 * which in turn uses a {@link BasicSplitPaneDividerEx} to draw a border on the divider.
 */
@SuppressWarnings("MagicConstant")
public class AppSplitPane extends JSplitPane {
    /**
     * Constructs a new {@code AppSplitPane} with a default divider size of 1.
     */
    public AppSplitPane() {
        setDividerSize(1);
    }

    /**
     * Constructs a new {@code AppSplitPane} with the specified orientation.
     *
     * @param newOrientation the orientation for the split pane, either
     *                       {@code JSplitPane.HORIZONTAL_SPLIT} or
     *                       {@code JSplitPane.VERTICAL_SPLIT}.
     */
    public AppSplitPane(int newOrientation) {
        super(newOrientation);
        setDividerSize(1);
    }

    /**
     * Constructs a new {@code AppSplitPane} with the specified orientation and
     * continuous layout.
     *
     * @param newOrientation the orientation for the split pane, either
     *                       {@code JSplitPane.HORIZONTAL_SPLIT} or
     *                       {@code JSplitPane.VERTICAL_SPLIT}.
     * @param newContinuousLayout a boolean, true for continuous layout, false for
     *                            non-continuous layout.
     */
    public AppSplitPane(int newOrientation, boolean newContinuousLayout) {
        super(newOrientation, newContinuousLayout);
        setDividerSize(1);
    }

    /**
     * Constructs a new {@code AppSplitPane} with the specified orientation,
     * continuous layout, and left and right components.
     *
     * @param newOrientation the orientation for the split pane.
     * @param newContinuousLayout a boolean, true for continuous layout.
     * @param newLeftComponent the component that appears on the left or top.
     * @param newRightComponent the component that appears on the right or bottom.
     */
    public AppSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
        setDividerSize(1);
    }

    /**
     * Constructs a new {@code AppSplitPane} with the specified orientation,
     * and left and right components.
     *
     * @param newOrientation the orientation for the split pane.
     * @param newLeftComponent the component that appears on the left or top.
     * @param newRightComponent the component that appears on the right or bottom.
     */
    public AppSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
        super(newOrientation, newLeftComponent, newRightComponent);
        setDividerSize(1);
    }

    @Override
    public void updateUI() {
        setUI(new BasicSplitPaneUIEx());
        revalidate();
    }

    /**
     * An extended {@link BasicSplitPaneUI} that uses {@link BasicSplitPaneDividerEx}
     * for the divider.
     */
    static class BasicSplitPaneUIEx extends BasicSplitPaneUI {
        /**
         * Creates and returns a new {@link BasicSplitPaneDividerEx}.
         * @return a new {@link BasicSplitPaneDividerEx}.
         */
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDividerEx(this);
        }
    }

    /**
     * An extended {@link BasicSplitPaneDivider} that customizes the painting of the divider.
     */
    static class BasicSplitPaneDividerEx extends BasicSplitPaneDivider {
        /**
         * Constructs a new {@code BasicSplitPaneDividerEx} with the specified UI.
         * @param ui the {@link BasicSplitPaneUI} that owns this divider.
         */
        public BasicSplitPaneDividerEx(BasicSplitPaneUI ui) {
            super(ui);
        }

        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(UISupport.getDefaultBorderColor());
            // Draw a line along the divider to visually separate the components.
            // For horizontal split, draw a vertical line.
            // For vertical split, draw a horizontal line.

            if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                g.drawLine(0, 0, 0, getHeight());
            } else {
                g.drawLine(0, 0, getWidth(), 0);
            }
        }
    }
}
