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

package com.gl.appframework.form;

import com.gl.appframework.UISupport;

import javax.swing.*;

import static com.gl.appframework.UISupport.setupUndomanager;

/**
 * A compact implementation of {@link FormEditor} for single-line string input.
 * Uses a {@link JTextField} as the underlying UI component.
 */
class CompactStringFormEditor implements FormEditor<String> {
    private final JTextField textField;
    private final FormElement<String> formElement;
    private final UISupport.DefaultUndoableEditListener undoableEditListener;

    /**
     * Constructs a new CompactStringFormEditor.
     * @param formPanel the parent form panel
     * @param formElement the metadata describing this form element
     */
    public CompactStringFormEditor(FormPanel formPanel, FormElement<String> formElement) {
        this.formElement = formElement;
        this.textField = new JTextField(20);
        textField.getDocument().addDocumentListener(formPanel);
        textField.setToolTipText(FormPanel.getTooltipText(formElement));
        undoableEditListener = setupUndomanager(textField);
        formPanel.setupPopupMenu(textField);
        formPanel.setupShortcuts(textField);
        setValue(formElement.getDefaultValue());
    }

    @Override
    public void setValue(String value) {
        textField.setText(value);
        undoableEditListener.getUndoManager().discardAllEdits();
    }

    @Override
    public String getValue() {
        return textField.getText();
    }

    @Override
    public JComponent getComponent() {
        return textField;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        String text = textField.getText();
        boolean valid = !formElement.isMandatory() || (text != null && !text.isEmpty());
        return valid ? null : "'%s' is not specified.".formatted(formElement.getLabel());
    }

    @Override
    public void requestFocus() {
        textField.requestFocus();
    }
}
