/*
 * Copyright 2026 Gregory Ledenev (gregory.ledenev37@gmail.com)
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

package com.gl.saf.comp;

import com.gl.saf.IconFactory.AutoIcon;
import com.gl.saf.UISupport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.View;
import java.awt.*;
import java.util.Objects;

import static com.gl.saf.Icons.*;

public class ActionTooltipRenderer extends JPanel {
    public static final Color DEFAULT_BACKGROUND = new Color(255, 255, 224);
    private final JLabel lblToolTip = new JLabel();
    private final JLabel lblAcceleratorKey = new JLabel();
    private final JLabel lblDetails = new FixedWidthLabel();
    private final Divider lblNoteDivider = new Divider();
    private final JLabel lblNote = new FixedWidthLabel();

    public ActionTooltipRenderer() {
        jbInit();
        updateUI();
    }

    public static Color getDarker(Color aColor, double aFactor) {
        return new Color(Math.max((int) (aColor.getRed() * aFactor), 0),
                Math.max((int) (aColor.getGreen() * aFactor), 0),
                Math.max((int) (aColor.getBlue() * aFactor), 0),
                aColor.getAlpha());
    }

    public void updateUI() {
        super.updateUI();

        if (lblToolTip != null)
            lblToolTip.setFont(lblToolTip.getFont().deriveFont(Font.BOLD));
        if (lblAcceleratorKey != null)
            lblAcceleratorKey.setFont(lblAcceleratorKey.getFont().deriveFont(Font.PLAIN));
    }

    private void jbInit() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(2, 5, 2, 5));
        lblDetails.setVisible(false);

        lblDetails.setText("Opens a document");
        lblToolTip.setText("Open");
        lblAcceleratorKey.setText("Ctrl+O");
        lblAcceleratorKey.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        lblNoteDivider.setVisible(false);
        lblNote.setVisible(false);
        lblNote.setText("Disable reason here");
        lblNote.setIcon(new AutoIcon(ICON_INFO));

        Box row1 = Box.createHorizontalBox();
        row1.add(lblToolTip);
        row1.add(Box.createHorizontalGlue());
        row1.add(Box.createHorizontalStrut(5));
        row1.add(lblAcceleratorKey);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(row1);

        this.add(Box.createVerticalStrut(2));
        lblDetails.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(lblDetails);

        lblNoteDivider.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        lblNoteDivider.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblNoteDivider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        this.add(lblNoteDivider);

        lblNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.add(lblNote);

        this.add(Box.createVerticalStrut(2));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        return preferredSize;
    }

    public void update(ActionTooltip c, boolean aForceUseDetailedDescription) {
        setEnabled(c.getComponent().isEnabled());

        setTipText(c.getShortDescription());
        setAcceleratorKey(c.getAcceleratorKey());
        String shortDescription = c.getDescription(false);
        setDetails(shortDescription);
        if (isEnabled()) {
            String longDescription = c.getDescription(aForceUseDetailedDescription);
            if (!Objects.equals(shortDescription, longDescription))
                setNote(longDescription);
        } else {
            setDisableReason(!aForceUseDetailedDescription ? null : c.getDisableReason());
        }

        boolean enabled = isEnabled();
        Color foreColor = enabled ? UIManager.getColor("ToolTip.foreground") :
                UIManager.getColor("ToolTip.foregroundInactive");

        lblToolTip.setForeground(foreColor);
        lblDetails.setForeground(foreColor);
        lblNote.setForeground(foreColor);

        Color backColor = enabled ? UIManager.getColor("ToolTip.background") :
                UIManager.getColor("ToolTip.backgroundInactive");
        if (backColor == null) // workaround for Win L&F
            backColor = UIManager.getColor("ToolTip.background");
        if (backColor == null) // workaround for Synth
            backColor = getDefaultBackground();

        lblNoteDivider.setForeground(getDarker(backColor, UISupport.isDarkAppearance() ? 8 : 0.7));

        lblAcceleratorKey.setForeground(getDarker(backColor, UISupport.isDarkAppearance() ? 10 : 0.5));
    }

    public boolean isShowing() {
        return true;
    }

    protected Color getDefaultBackground() {
        return DEFAULT_BACKGROUND;
    }

    public boolean isOpaque() {
        return false;
    }

    public String getTipText() {
        return lblToolTip.getText();
    }

    public void setTipText(String aTipText) {
        lblToolTip.setText(aTipText);
    }

    protected String toHTML(String aText) {
        String result = aText;

        if (aText != null && !aText.toLowerCase().startsWith("<html>"))
            result = "<html>" + result + "</html>";

        return result;
    }

    public String getDetails() {
        return lblDetails.getText();
    }

    public void setDetails(String aDetails) {
        lblDetails.setText(toHTML(aDetails));

        boolean vis = aDetails != null && !"".equals(aDetails);
        lblDetails.setVisible(vis);
    }

    public void setNote(String aNote) {
        lblNote.setText(toHTML(aNote));

        boolean vis = aNote != null && !"".equals(aNote);
        lblNote.setVisible(vis);
        lblNoteDivider.setVisible(vis);
    }

    public void setAcceleratorKey(String anAcceleratorKey) {
        lblAcceleratorKey.setText(anAcceleratorKey);
    }

    public void setDisableReason(String aValue) {
        lblNote.setText(toHTML(aValue));

        boolean vis = aValue != null && !"".equals(aValue);

        lblNote.setVisible(vis);
        lblNoteDivider.setVisible(vis);
    }

    static class Divider extends JComponent {
        protected void paintComponent(Graphics g) {
            g.setColor(getForeground());
            Insets insets = getInsets();
            g.drawLine(insets.left, insets.top, getWidth() - insets.left - insets.right, insets.top);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            return new Dimension(insets.left + insets.right, insets.top + insets.bottom + 1);
        }
    }

    static class FixedWidthLabel extends JLabel {
        public Dimension getPreferredSize() {
            Dimension result = super.getPreferredSize();
            if (result.width > ActionTooltip.MAX_WIDTH) {
                View v = (View) getClientProperty("html");
                if (v != null) {
                    v.setSize(ActionTooltip.MAX_WIDTH, Integer.MAX_VALUE);
                    result = super.getPreferredSize();
                } else {
                    result.width = Math.min(result.width, ActionTooltip.MAX_WIDTH);
                }
            }
            result.width += 2;
            return result;
        }

        public void updateUI() {
            super.updateUI();

            setFont(getFont().deriveFont(Font.PLAIN));
        }
    }
}