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
import javax.swing.text.JTextComponent;

import static com.gl.appframework.UISupport.setupUndomanager;

/**
 * A {@link FormEditor} implementation that provides an editable {@link JComboBox}.
 * It supports both selection from a list and manual text entry, with undo/redo capabilities.
 */
class EditableDropdownFormEditor implements FormEditor {
    private final JComboBox<Object> comboBox;
    private final FormElement<?> formElement;
    private final UISupport.DefaultUndoableEditListener undoableEditListener;

    public EditableDropdownFormEditor(FormPanel formPanel, FormElement<?> formElement) {
        this.formElement = formElement;
        this.comboBox = new JComboBox<>(formElement.getEditorChoices() != null ? formElement.getEditorChoices() : new Object[0]);
        FormPanel.setupFont(this.comboBox);
        this.comboBox.setEditable(true);
        comboBox.setToolTipText(FormPanel.getTooltipText(formElement));
        comboBox.addActionListener(e -> formPanel.firePropertyChange(FormPanel.PROPERTY_VALUE_CHANGED, null, null));

        JTextComponent textComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
        textComponent.getDocument().addDocumentListener(formPanel);
        undoableEditListener = setupUndomanager(textComponent);
        formPanel.setupPopupMenu(textComponent);
        formPanel.setupShortcuts(textComponent);

        setValue(formElement.getDefaultValue());
    }

    @Override
    public void setValue(Object value) {
        comboBox.setSelectedItem(value);
        undoableEditListener.getUndoManager().discardAllEdits();
    }

    @Override
    public Object getValue() {
        Object item = comboBox.getEditor().getItem();
        if (item == null) {
            return null;
        }

        Class<?> type = formElement.getType();
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class && type != void.class)) {
            if (item instanceof Number) {
                return item;
            }
            return FormPanel.parseNumber(item.toString(), type);
        }

        return item;
    }

    @Override
    public JComponent getComponent() {
        return comboBox;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        Object item = comboBox.getEditor().getItem();
        String text = item != null ? item.toString() : "";

        if (text.isEmpty()) {
            return formElement.isMandatory() ? "'%s' is not specified.".formatted(formElement.getLabel()) : null;
        }

        Class<?> type = formElement.getType();
        boolean isNumeric = Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class && type != void.class);

        if (isNumeric && strictCheck) {
            if (getValue() == null) {
                return "'%s' has an invalid number format.".formatted(formElement.getLabel());
            }
        }

        return null;
    }

    @Override
    public void requestFocus() {
        comboBox.requestFocus();
    }
}
