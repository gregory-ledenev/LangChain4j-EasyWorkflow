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

import com.gl.saf.actions.BasicAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class ActionTooltip extends JToolTip implements ActionListener {

    public static final int MAX_WIDTH = 300;
    public static boolean SHOW_LATER_DETAILED_DESCRIPTION = true;
    public static final Factory FACTORY = new Factory();

    public static class Factory {
        public JToolTip createToolTip(JComponent aTarget) {
            ActionTooltip result = new ActionTooltip();
            result.setComponent(aTarget);
            return result;
        }
    }

    protected final ActionTooltipRenderer actionTooltipRenderer = new ActionTooltipRenderer();

    public ActionTooltip() {
        setLayout(new BorderLayout());
        add(actionTooltipRenderer, BorderLayout.CENTER);
    }

    public Dimension getPreferredSize() {
        Dimension result = actionTooltipRenderer.getPreferredSize();
        Insets insets = getInsets();
        result.width += insets.left+insets.top+PREFERRED_WIDTH_FIX;
        result.height += insets.top+insets.bottom;
        return result;
    }

    public static final int PREFERRED_WIDTH_FIX = 0;

    protected String getAcceleratorKey() {
        String result = null;

        Action action = getAction(getComponent());
        if (action != null) {
            KeyStroke accelerator = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
            if (accelerator != null) {
                result = "";
                int modifiers = accelerator.getModifiers();
                if (modifiers > 0) {
                    result = KeyEvent.getKeyModifiersText(modifiers);
                    String acceleratorDelimiter = UIManager.getString("MenuItem.acceleratorDelimiter");
                    if (acceleratorDelimiter == null) // it can be null on nimbus
                        acceleratorDelimiter = "+";
                    result += acceleratorDelimiter;
                }

                int keyCode = accelerator.getKeyCode();
                if (keyCode != 0)
                    result += KeyEvent.getKeyText(keyCode);
                else
                    result += accelerator.getKeyChar();
            }
        }
        return result;
    }

    private Action getAction(JComponent component) {
        return component instanceof AbstractButton ? ((AbstractButton) component).getAction() : null;
    }

    protected boolean isShowLaterDetailedDescription() {
        return SHOW_LATER_DETAILED_DESCRIPTION;
    }

    protected String getDescription(boolean aForceUseDetailedDescription) {
        String result = null;

        Action action = getAction(getComponent());

        if (action != null) {
            if (aForceUseDetailedDescription)
                result = (String) action.getValue(Action.LONG_DESCRIPTION);
            if (result == null)
                result = (String) action.getValue(Action.SHORT_DESCRIPTION);
        }

        return result;
    }

    protected String normalizeText(String aText) {
        String result = aText;

        if (result != null && result.endsWith("..."))
            result = result.substring(0, result.length() - "...".length());

        return result;
    }

    protected String getShortDescription() {
        Action action = getAction (getComponent());
        return action != null ? normalizeText((String) action.getValue(Action.NAME)) : null;
    }

    protected String getDisableReason() {
        Action action = getAction (getComponent());
        return action != null ? (String) action.getValue(BasicAction.DISABLE_REASON_KEY) : null;
    }

    public String getTooltipText() {
        return tooltipText;
    }

    protected String tooltipText;

    public void setTipText(String tipText) {
        this.tooltipText = tipText;

        actionTooltipRenderer.update(this, ! isShowLaterDetailedDescription());
    }

    protected final Timer timer = new Timer(2000, this);

    public void addNotify() {
        super.addNotify();

        if (isShowLaterDetailedDescription()) {
            timer.setRepeats(false);
            timer.start();
        }
    }

    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    static {
        ToolTipManager.sharedInstance().setDismissDelay(10000);
    }

    public void actionPerformed(ActionEvent e) {
        actionTooltipRenderer.update(this, true);

        Dimension prefSize = getPreferredSize();
        if (getParent() instanceof JComponent) {
            Insets insets = ((JComponent) getParent()).getInsets();
            prefSize.width += insets.left+insets.right;
            prefSize.height += insets.top+insets.bottom;
        }

        Window w = SwingUtilities.getWindowAncestor(this);
        if (! (w instanceof JFrame || w instanceof JDialog)) {
            //heavyweight popup
            w.setSize(prefSize);
        } else {
            //lightweight popup
            getParent().setSize(prefSize);
            revalidate();
            repaint();
        }
    }
}