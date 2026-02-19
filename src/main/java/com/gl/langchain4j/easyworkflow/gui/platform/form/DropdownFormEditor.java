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
 * A {@link FormEditor} implementation that provides a dropdown selection using a {@link JComboBox}.
 */
class DropdownFormEditor implements FormEditor {
    private final FormPanel formPanel;
    private final JComboBox<Object> comboBox;
    private final FormElement formElement;

    /**
     * Constructs a new {@code DropdownFormEditor}.
     *
     * @param formPanel   The parent form panel.
     * @param formElement The metadata defining the field properties and choices.
     */
    public DropdownFormEditor(FormPanel formPanel, FormElement formElement) {
        this.formPanel = formPanel;
        this.formElement = formElement;
        this.comboBox = new JComboBox<>(formElement.getEditorChoices() != null ? formElement.getEditorChoices() : new Object[0]);
        FormPanel.setupFont(this.comboBox);
        comboBox.setToolTipText(FormPanel.getTooltipText(formElement));
        comboBox.addActionListener(e -> formPanel.firePropertyChange(FormPanel.PROPERTY_VALUE_CHANGED, null, null));
        setValue(formElement.getDefaultValue());
    }

    @Override
    public void setValue(Object value) {
        comboBox.setSelectedItem(value);
    }

    @Override
    public Object getValue() {
        Object item = comboBox.getSelectedItem();
        if (item == null) {
            return null;
        }

        Class<?> type = formElement.getType();
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class && type != void.class)) {
            if (item instanceof Number) {
                return item;
            }
            return formPanel.parseNumber(item.toString(), type);
        }

        return item;
    }

    @Override
    public JComponent getComponent() {
        return comboBox;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        Object selected = comboBox.getSelectedItem();
        String text = selected != null ? selected.toString() : "";

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
