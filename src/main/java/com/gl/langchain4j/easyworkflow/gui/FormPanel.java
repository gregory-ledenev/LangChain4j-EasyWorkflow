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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

/**
 * A panel that dynamically generates a form based on a list of {@link FormElement} objects. It supports various input
 * types, validation, and value retrieval.
 */
public class FormPanel extends JPanel implements Scrollable, DocumentListener {
    public static final String PROPERTY_VALUE_CHANGED = "valueChanged";
    public static final String PROPERTY_ENTER_PRESSED = "enterPressed";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<String, Editor> editors = new HashMap<>();
    private List<FormElement> formElements;

    /**
     * Interface for form element editors.
     */
    interface Editor {

        /**
         * Sets the value of the editor.
         * @param value The value to set.
         */
        void setValue(Object value);

        /**
         * Returns the current value of the editor.
         * @return The current value.
         */
        Object getValue();

        /**
         * Returns the component used for the editor.
         * @return The editor's component.
         */
        JComponent getComponent();

        /**
         * Checks the validity of the editor's current value.
         * @return An error message if the value is invalid, or {@code null} if valid.
         */
        String checkValidity(boolean strictCheck);

        /**
         * Requests focus for the editor's component.
         */
        void requestFocus();
    }    

    private static JTextComponent getTextComponent(JComponent c) {
        if (c instanceof JTextComponent)
            return (JTextComponent) c;
        else if (c instanceof JScrollPane)
            return getTextComponent((JComponent) ((JScrollPane) c).getViewport().getView());
        else
            return null;
    }

    /**
     * Checks if all mandatory text components in the form have content.
     *
     * @return true if all mandatory text components have content, false otherwise.
     */
    public boolean hasRequiredContent() {
        String result = null;
        for (FormElement element : formElements) {
            Editor editor = editors.get(element.name());
            result = editor.checkValidity(false);
            if(result != null)
                break;
        }
        return result == null;
    }

    /**
     * Requests focus for the first input component in the form.
     */
    @Override
    public void requestFocus() {
        if (formElements != null && !formElements.isEmpty()) {
            JComponent c = editors.get(formElements.get(0).name()).getComponent();
            if (c instanceof JScrollPane scrollPane)
                scrollPane.getViewport().getView().requestFocus();
            else
                c.requestFocus();
        } else {
            super.requestFocus();
        }
    }

    /**
     * Clears the content of all text-based input components in the form.
     */
    public void clearContent() {
        for (Editor editor : editors.values()) {
            editor.setValue(null);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        firePropertyChange(PROPERTY_VALUE_CHANGED, null, null);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        firePropertyChange(PROPERTY_VALUE_CHANGED, null, null);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        firePropertyChange(PROPERTY_VALUE_CHANGED, null, null);
    }

    /**
     * Returns the list of form elements currently displayed in the panel.
     *
     * @return a {@link List} of {@link FormElement} objects.
     */
    public List<FormElement> getFormElements() {
        return formElements;
    }

    /**
     * Sets the form elements to be displayed and rebuilds the form.
     *
     * @param aFormElements the list of {@link FormElement} objects to display.
     */
    public void setFormElements(List<FormElement> aFormElements) {
        formElements = aFormElements;
        buildForm();

        revalidate();
        repaint();
    }

    private void buildForm() {
        removeAll();
        editors.clear();

        // Define the layout columns: Right-aligned label, gap, and a component that grows.
        String colSpec = "right:pref, 3dlu, default:grow";

        // Use the modern DefaultFormBuilder, which handles row creation and gaps automatically.
        FormLayout layout = new FormLayout(colSpec, ""); // Rows are added dynamically
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        for (int i = 0; i < formElements.size(); i++) {
            FormElement element = formElements.get(i);
            // Define the row specification based on whether the component is multi-line.
            // 'default' prevents single-line components from collapsing vertically.
            boolean isMultiLine = (element.type() == String.class &&
                    (element.editorType() == FormEditorType.Note ||
                            formElements.size() == 1)) ||
                    Map.class.isAssignableFrom(element.type());
            String rowSpec = isMultiLine ? "max(pref;40dlu)" : "default";
            builder.appendRow(rowSpec);
            
            Editor editor = createEditor(element);
            editors.put(element.name(), editor);
            JComponent inputComponent = editor.getComponent();
            if (formElements.size() > 1 || !isMultiLine) {
                JLabel label = new JLabel(element.label() + ":");
                setupFont(label);

                if (isMultiLine) {
                    label.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
                    builder.add(label, cc.xy(1, builder.getRowCount(), "right, top"));
                } else {
                    builder.add(label, cc.xy(1, builder.getRowCount(), "right, default"));
                }
            }

            // Add the input component with specific constraints.
            // JCheckBox uses default (left) alignment and does not fill.
            // Other components fill the horizontal space.
            if (inputComponent instanceof JCheckBox) {
                builder.add(inputComponent, cc.xy(3, builder.getRowCount()));
            } else {
                builder.add(inputComponent, cc.xy(3, builder.getRowCount(), "fill, default"));
            }

            // Add a standard gap for the next component.
            // If the current component was multi-line, use a slightly smaller gap
            // to make the transition to a single-line component look better.
            if (i < formElements.size() - 1) {
                if (isMultiLine) {
                    builder.appendRow("1dlu");
                } else {
                    builder.appendRelatedComponentsGapRow();
                }
            }
        }
    }

    private Editor createEditor(FormElement element) {
        Class<?> type = element.type();

        if (type == String.class) {
            if ((element.editorType() == FormEditorType.EditableDropdown) && formElements.size() > 1) {
                return new EditableDropdownEditor(element);
            } else if (element.editorType() == FormEditorType.Dropdown && formElements.size() > 1) {
                return new DropdownEditor(element);
            } else if ((element.editorType() == FormEditorType.Text ||
                    element.editorType() == FormEditorType.Default)) {
                return new CompactStringEditor(element);
            } else {
                return new StringEditor(element);
            }
        } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            if (type == Boolean.class || type == boolean.class) {
                return new BooleanEditor(element);
            } else {
                return new NumberEditor(element);
            }
        } else if (Map.class.isAssignableFrom(type)) {
            return new MapEditor(element);
        }

        // Fallback for unsupported types
        return new UnsupportedTypeEditor(element);
    }
    
    private void setupPopupMenu(JTextComponent textComponent) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(createAction("Copy", new AutoIcon(ICON_COPY), e -> textComponent.copy()));
        popupMenu.add(createAction("Paste", new AutoIcon(ICON_PASTE), e -> textComponent.paste()));
        popupMenu.add(new JSeparator());
        if (formElements.size() == 1) {
            popupMenu.add(createAction("Clear", new AutoIcon(ICON_CLEAR), e -> textComponent.setText("")));
        } else {
            JMenu mnuCLear = new JMenu(createAction("Clear", new AutoIcon(ICON_CLEAR), null));
            mnuCLear.add(createAction("Clear", null, e -> textComponent.setText("")));
            mnuCLear.add(createAction("Clear All", null, e -> clearContent()));
            popupMenu.add(mnuCLear);
        }

        textComponent.setComponentPopupMenu(popupMenu);
    }

    private void setupShortcuts(JTextComponent c) {
        c.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        c.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                FormPanel.this.firePropertyChange(PROPERTY_ENTER_PRESSED, null, null);
            }
        });
    }

    /**
     * Validates the form inputs and returns an error message or {@code null} if form is valid.
     * It also shows the message and focuses on invalid editor.
     * @return A list of validation error messages.
     */
    public String checkValidity() {
        String result = null;
        for (FormElement element : formElements) {
            Editor editor = editors.get(element.name());
            result = editor.checkValidity(true);
            if(result != null) {
                JOptionPane.showMessageDialog(this, result, "Validation Error", JOptionPane.ERROR_MESSAGE);
                editor.requestFocus();
                break;
            }

        }
        return result;
    }

    /**
     * Retrieves the current values from the form. This method should be called after a successful validation.
     *
     * @return A map of form element names to their values.
     * @throws RuntimeException if parsing fails (should be prevented by pre-validation).
     */
    public Map<String, Object> getFormValues() {
        Map<String, Object> values = new HashMap<>();
        for (FormElement element : formElements) {
            Editor editor = editors.get(element.name());
            Object parsedValue = editor.getValue();
            values.put(element.name(), parsedValue);
        }
        return values;
    }

    /**
     * Sets the values of the form elements based on the provided map.
     *
     * @param formValues A map where keys are form element names and values are the corresponding data.
     * @return {@code true} if any value was set, {@code false} otherwise.
     */
    public boolean setFormValues(Map<String, Object> formValues) {
        if (formValues == null)
            return false;

        boolean result = false;

        for (FormElement element : formElements) {
            if (formValues.containsKey(element.name())) {
                Editor editor = editors.get(element.name());
                Object value = formValues.get(element.name());
                if (editor != null) {
                    editor.setValue(value);
                    result = true;
                }
            }
        }

        return result;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * Represents a single element in a form, containing its name, label, type, default value, whether it should use a
     * compact editor, and if it's mandatory.
     */
    public record FormElement(String name,
                              String label, String description, Class<?> type,
                              Object defaultValue,
                              FormEditorType editorType,
                              Object[] editorChoices, boolean mandatory
    ) {

        /**
         * Constructs a new {@code FormElement}.
         *
         * @param name          The name of the form element.
         * @param label         The human-readable label for the form element. If null or empty, a label will be
         *                      generated from the name.
         * @param description
         * @param type          The data type of the form element's value.
         * @param defaultValue  The initial value of the form element.
         * @param editorType    A boolean indicating whether to use a compact editor (e.g., JTextField) for String
         *                      types.
         * @param editorChoices An array of options for dropdown editors.
         * @param mandatory     A boolean indicating whether the form element value is mandatory
         * @throws NullPointerException if {@code name} is null.
         */
        public FormElement(String name, String label, String description, Class<?> type, Object defaultValue, FormEditorType editorType, Object[] editorChoices, boolean mandatory) {
            Objects.requireNonNull(name);
            this.name = name;
            this.label = label != null && label.length() > 0 ? label : labelFromName(name);
            this.description = description;
            this.type = type;
            this.defaultValue = defaultValue;
            this.editorType = editorType;
            this.mandatory = mandatory;
            this.editorChoices = editorChoices;
        }

        public FormElement(String name, String label, Class<?> type, Object defaultValue, FormEditorType editorType, boolean mandatory) {
            this(name, label, null, type, defaultValue, editorType, null, mandatory);
        }

        public FormElement(String name, String label, Class<?> type, Object defaultValue, boolean mandatory, Object[] options) {
            this(name, label, null, type, defaultValue, options != null ? FormEditorType.Dropdown : FormEditorType.Default, options, mandatory);
        }

        private String labelFromName(String name) {
            StringBuilder label = new StringBuilder();
            char[] charArray = name.toCharArray();
            for (int i = 0, length = charArray.length; i < length; i++) {
                char c = charArray[i];
                if (i == 0) {
                    label.append(Character.toUpperCase(c));
                } else if (Character.isUpperCase(c) && !Character.isUpperCase(charArray[i - 1])) {
                    label.append(" ").append(c);
                } else {
                    label.append(c);
                }
            }
            return label.toString();
        }
    }

    // --- Editor Implementations ---

    private static String getTooltipText(FormElement formElement) {
        return formElement.description != null && formElement.description.isEmpty() ?
                "%s %s".formatted(formElement.type().getSimpleName(), formElement.name()) :
                formElement.description();
    }

    private class CompactStringEditor implements Editor {
        private final JTextField textField;
        private final FormElement formElement;

        public CompactStringEditor(FormElement formElement) {
            this.formElement = formElement;
            this.textField = new JTextField(20);
            textField.getDocument().addDocumentListener(FormPanel.this);
            textField.setToolTipText(getTooltipText(formElement));
            setupPopupMenu(textField);
            setupShortcuts(textField);
            setValue(formElement.defaultValue());
        }

        @Override
        public void setValue(Object value) {
            textField.setText(value != null ? value.toString() : null);
        }

        @Override
        public Object getValue() {
            return textField.getText();
        }

        @Override
        public JComponent getComponent() {
            return textField;
        }

        @Override
        public String checkValidity(boolean strictCheck) {
            String text = textField.getText();
            boolean valid = !formElement.mandatory || (text != null && !text.isEmpty());
            return valid ? null : "'%s' is not specified.".formatted(formElement.label());
        }

        @Override
        public void requestFocus() {
            textField.requestFocus();
        }
    }

    private class StringEditor implements Editor {
        private final TextEditor textArea;
        private final JScrollPane scrollPane;
        private final FormElement formElement;

        public StringEditor(FormElement formElement) {
            this.formElement = formElement;
            this.textArea = new TextEditor(formElements.size() > 1 ? 3 : 5, 20);
            textArea.getDocument().addDocumentListener(FormPanel.this);
            textArea.setToolTipText(getTooltipText(formElement));
            setupPopupMenu(textArea);
            setupShortcuts(textArea);

            this.scrollPane = new JScrollPane(textArea) {
                @Override
                public void updateUI() {
                    super.updateUI();
                    if (formElements.size() == 1) {
                        setBorder(null);
                        setOpaque(false);
                    }
                }
            };

            if (formElements.size() == 1) {
                textArea.setOpaque(false);
                textArea.setPlaceHolderText(formElement.label());
            }
            setValue(formElement.defaultValue());
        }

        @Override
        public void setValue(Object value) {
            textArea.setText(value != null ? value.toString() : null);
        }

        @Override
        public Object getValue() {
            return textArea.getText();
        }

        @Override
        public JComponent getComponent() {
            return scrollPane;
        }

        @Override
        public String checkValidity(boolean strictCheck) {
            String text = textArea.getText();
            boolean valid = !formElement.mandatory || (text != null && !text.isEmpty());
            return valid ? null : "'%s' is not specified.".formatted(formElement.label());
        }

        @Override
        public void requestFocus() {
            textArea.requestFocus();
        }
    }

    static void setupFont(JComponent c) {
        c.setFont(c.getFont().deriveFont(c.getFont().getSize() + 2.0f));
    }

    private class NumberEditor implements Editor {
        private final JTextField textField;
        private final FormElement formElement;

        public NumberEditor(FormElement formElement) {
            this.formElement = formElement;
            this.textField = new JTextField(20);
            setupFont(this.textField);
            textField.getDocument().addDocumentListener(FormPanel.this);
            textField.setToolTipText(getTooltipText(formElement));
            setupPopupMenu(textField);
            setupShortcuts(textField);
            setValue(formElement.defaultValue());
        }

        @Override
        public void setValue(Object value) {
            textField.setText(value != null ? value.toString() : null);
        }

        @Override
        public Object getValue() {
            String text = textField.getText();
            if (text == null || text.isEmpty()) return null;
            Class<?> type = formElement.type();
            try {
                if (type == Integer.class || type == int.class) return Integer.parseInt(text);
                if (type == Long.class || type == long.class) return Long.parseLong(text);
                if (type == Double.class || type == double.class) return Double.parseDouble(text);
                if (type == Float.class || type == float.class) return Float.parseFloat(text);
                return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public JComponent getComponent() {
            return textField;
        }

        @Override
        public String checkValidity(boolean strictCheck) {
            boolean valid = !formElement.mandatory;
            if (valid)
                return null;

            if (strictCheck) {
                valid = getValue() != null;
            } else {
                String text = textField.getText();
                valid = text != null && !text.isEmpty();
            }

            return valid ? null : "'%s' is not specified or invalid.".formatted(formElement.label());
        }

        @Override
        public void requestFocus() {
            textField.requestFocus();
        }
    }

    private class BooleanEditor implements Editor {
        private final JCheckBox checkBox;

        public BooleanEditor(FormElement formElement) {
            this.checkBox = new JCheckBox();
            checkBox.setToolTipText(getTooltipText(formElement));
            checkBox.addActionListener(e -> firePropertyChange(PROPERTY_VALUE_CHANGED, null, null));
            setValue(formElement.defaultValue());
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

    private class MapEditor implements Editor {
        private final TextEditor textArea;
        private final FormElement formElement;

        public MapEditor(FormElement formElement) {
            this.formElement = formElement;
            this.textArea = new TextEditor(5, 20);
            textArea.getDocument().addDocumentListener(FormPanel.this);
            textArea.setToolTipText(getTooltipText(formElement));
            setupPopupMenu(textArea);
            setupShortcuts(textArea);
            setValue(formElement.defaultValue());
        }

        @Override
        public void setValue(Object value) {
            if (value == null) {
                textArea.setText("{\n  \n}");
                return;
            }
            try {
                textArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
            } catch (JsonProcessingException e) {
                textArea.setText("Error converting value to JSON: " + e.getMessage());
            }
        }

        @Override
        public Object getValue() {
            try {
                return OBJECT_MAPPER.readValue(textArea.getText(), Map.class);
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
            boolean valid = !formElement.mandatory || (text != null && !text.isEmpty());
            if (valid && strictCheck) {
                try {
                    OBJECT_MAPPER.readValue(text, Map.class);
                } catch (JsonProcessingException e) {
                    valid = false;
                }
            }
            return valid ? null : "'%s' is not specified or can't be converted to JSON.".formatted(formElement.label());
        }

        @Override
        public void requestFocus() {
            textArea.requestFocus();
        }

    }

    private class DropdownEditor implements Editor {
        private final JComboBox<Object> comboBox;
        private final FormElement formElement;

        public DropdownEditor(FormElement formElement) {
            this.formElement = formElement;
            this.comboBox = new JComboBox<>(formElement.editorChoices() != null ? formElement.editorChoices() : new Object[0]);
            setupFont(this.comboBox);
            comboBox.setToolTipText(getTooltipText(formElement));
            comboBox.addActionListener(e -> firePropertyChange(PROPERTY_VALUE_CHANGED, null, null));
            setValue(formElement.defaultValue());
        }

        @Override
        public void setValue(Object value) {
            comboBox.setSelectedItem(value);
        }

        @Override
        public Object getValue() {
            return comboBox.getSelectedItem();
        }

        @Override
        public JComponent getComponent() {
            return comboBox;
        }

        @Override
        public String checkValidity(boolean strictCheck) {
            Object selected = comboBox.getSelectedItem();
            boolean valid = !formElement.mandatory() || (selected != null && !selected.toString().isEmpty());
            return valid ? null : "'%s' is not specified.".formatted(formElement.label());
        }

        @Override
        public void requestFocus() {
            comboBox.requestFocus();
        }
    }

    private class EditableDropdownEditor implements Editor {
        private final JComboBox<Object> comboBox;
        private final FormElement formElement;

        public EditableDropdownEditor(FormElement formElement) {
            this.formElement = formElement;
            this.comboBox = new JComboBox<>(formElement.editorChoices() != null ? formElement.editorChoices() : new Object[0]);
            setupFont(this.comboBox);
            this.comboBox.setEditable(true);
            comboBox.setToolTipText(getTooltipText(formElement));
            comboBox.addActionListener(e -> firePropertyChange(PROPERTY_VALUE_CHANGED, null, null));

            JTextComponent textComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
            textComponent.getDocument().addDocumentListener(FormPanel.this);
            setupPopupMenu(textComponent);
            setupShortcuts(textComponent);

            setValue(formElement.defaultValue());
        }

        @Override
        public void setValue(Object value) {
            comboBox.setSelectedItem(value);
        }

        @Override
        public Object getValue() {
            return comboBox.getEditor().getItem();
        }

        @Override
        public JComponent getComponent() {
            return comboBox;
        }

        @Override
        public String checkValidity(boolean strictCheck) {
            Object item = comboBox.getEditor().getItem();
            String text = item != null ? item.toString() : "";
            boolean valid = !formElement.mandatory() || !text.isEmpty();
            return valid ? null : "'%s' is not specified.".formatted(formElement.label());
        }

        @Override
        public void requestFocus() {
            comboBox.requestFocus();
        }
    }

    private record UnsupportedTypeEditor(FormElement element) implements Editor {
        @Override
        public void setValue(Object value) { /* No-op */ }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public JComponent getComponent() {
            return new JLabel("🛑 " + "Unsupported type: " + element.type().getSimpleName());
        }

        @Override
        public String checkValidity(boolean strictCheck) {
            return null;
        }

        @Override
        public void requestFocus() {
        }
    }

    static class TextEditor extends JTextArea {
        private String placeHolderText;

        public TextEditor(int rows, int columns) {
            super(rows, columns);
            init();
        }

        public TextEditor(int rows, int columns, String aPlaceHolderText) {
            super(rows, columns);
            init();
            placeHolderText = aPlaceHolderText;
        }

        public TextEditor() {
            init();
        }

        private void init() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setRows(3);
            setupFont(this);
            setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        }

        public String getPlaceHolderText() {
            return placeHolderText;
        }

        public void setPlaceHolderText(String aPlaceHolderText) {
            placeHolderText = aPlaceHolderText;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (placeHolderText != null && getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIManager.getColor("textInactiveText"));
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                g2.drawString(placeHolderText, getInsets().left, getInsets().top + metrics.getAscent());
                g2.dispose();
            }
        }
    }
}
