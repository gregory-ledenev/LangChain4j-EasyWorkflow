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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.util.Map;

import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.setupUndomanager;

/**
 * A {@link FormEditor} implementation for editing {@link Map} objects as JSON text.
 */
class MapFormEditor implements FormEditor {
    private final FormPanel formPanel;
    private final FormPanel.FormTextEditor textArea;
    private final FormElement formElement;
    private final UISupport.DefaultUndoableEditListener undoableEditListener;

    /**
     * Constructs a new MapFormEditor.
     *
     * @param formPanel   the parent form panel
     * @param formElement the metadata for the form element being edited
     */
    public MapFormEditor(FormPanel formPanel, FormElement formElement) {
        this.formPanel = formPanel;
        this.formElement = formElement;
        this.textArea = new FormPanel.FormTextEditor(5, 20);
        textArea.getDocument().addDocumentListener(formPanel);
        textArea.setToolTipText(FormPanel.getTooltipText(formElement));
        undoableEditListener = setupUndomanager(textArea);
        formPanel.setupPopupMenu(textArea);
        formPanel.setupShortcuts(textArea);
        setValue(formElement.getDefaultValue());
    }

    @Override
    public void setValue(Object value) {
        if (value == null) {
            textArea.setText("{\n  \n}");
            return;
        }
        try {
            textArea.setText(FormPanel.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        } catch (JsonProcessingException e) {
            textArea.setText("Error converting value to JSON: " + e.getMessage());
        }
        undoableEditListener.getUndoManager().discardAllEdits();
    }

    @Override
    public Object getValue() {
        try {
            return FormPanel.OBJECT_MAPPER.readValue(textArea.getText(), Map.class);
        } catch (JsonProcessingException e) {
            return textArea.getText(); // Return raw text if parsing fails, validation will catch it
        }
    }

    @Override
    public JComponent getComponent() {
        return new JScrollPane(textArea);
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        String text = textArea.getText();
        boolean valid = !formElement.isMandatory() || (text != null && !text.isEmpty());
        if (valid && strictCheck) {
            try {
                FormPanel.OBJECT_MAPPER.readValue(text, Map.class);
            } catch (JsonProcessingException e) {
                valid = false;
            }
        }
        return valid ? null : "'%s' is not specified or can't be converted to JSON.".formatted(formElement.getLabel());
    }

    @Override
    public void requestFocus() {
        textArea.requestFocus();
    }

}
