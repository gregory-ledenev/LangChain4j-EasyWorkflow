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

package com.gl.langchain4j.easyworkflow.gui.platform.form;

import javax.swing.*;


/**
 * A form editor implementation for boolean values using a {@link JCheckBox}.
 */
class BooleanFormEditor implements FormEditor {
    private final FormPanel formPanel;
    private final JCheckBox checkBox;

    /**
     * Constructs a new BooleanFormEditor.
     *
     * @param formPanel   the parent form panel
     * @param formElement the metadata defining the form element
     */
    public BooleanFormEditor(FormPanel formPanel, FormElement<Boolean> formElement) {
        this.formPanel = formPanel;
        this.checkBox = new JCheckBox();
        checkBox.setToolTipText(FormPanel.getTooltipText(formElement));
        checkBox.addActionListener(e -> formPanel.firePropertyChange(FormPanel.PROPERTY_VALUE_CHANGED, null, null));
        setValue(formElement.getDefaultValue());
    }

    @Override
    public void setValue(Object value) {
        checkBox.setSelected(value != null && (Boolean) value);
    }

    @Override
    public Object getValue() {
        return checkBox.isSelected();
    }

    @Override
    public JComponent getComponent() {
        return checkBox;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        return null;
    }

    @Override
    public void requestFocus() {
        checkBox.requestFocus();
    }
}
