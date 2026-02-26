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

package com.gl.saf.form;

import com.gl.saf.UISupport;

import javax.swing.*;

import static com.gl.saf.UISupport.setupUndomanager;

/**
 * A {@link FormEditor} implementation for editing {@link String} values using a {@link JTextArea}.
 * It supports multi-line input, undo/redo functionality, and placeholder text when used as a single element.
 */
class StringFormEditor implements FormEditor<String> {
    private final FormPanel.FormTextEditor textArea;
    private final JScrollPane scrollPane;
    private final FormElement<String> formElement;
    private final UISupport.DefaultUndoableEditListener undoableEditListener;

    public StringFormEditor(FormPanel formPanel, FormElement<String> formElement) {
        this.formElement = formElement;
        this.textArea = new FormPanel.FormTextEditor(formPanel.getFormElements().size() > 1 ? 3 : 5, 20);
        textArea.getDocument().addDocumentListener(formPanel);
        textArea.setToolTipText(FormPanel.getTooltipText(formElement));
        formPanel.setupPopupMenu(textArea);
        formPanel.setupShortcuts(textArea);
        undoableEditListener = setupUndomanager(textArea);

        this.scrollPane = new JScrollPane(textArea) {
            @Override
            public void updateUI() {
                super.updateUI();
                if (formPanel.getFormElements().size() == 1) {
                    setBorder(null);
                    setOpaque(false);
                }
            }
        };

        if (formPanel.getFormElements().size() == 1) {
            textArea.setOpaque(false);
            textArea.setPlaceHolderText(formElement.getLabel());
        }
        setValue(formElement.getDefaultValue());
    }

    @Override
    public void setValue(String value) {
        textArea.setText(value);
        undoableEditListener.getUndoManager().discardAllEdits();
    }

    @Override
    public String getValue() {
        return textArea.getText();
    }

    @Override
    public JComponent getComponent() {
        return scrollPane;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        String text = textArea.getText();
        boolean valid = !formElement.isMandatory() || (text != null && !text.isEmpty());
        return valid ? null : "'%s' is not specified.".formatted(formElement.getLabel());
    }

    @Override
    public void requestFocus() {
        textArea.requestFocus();
    }
}
