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

import com.formdev.flatlaf.themes.FlatMacLightLaf;
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
    private final Map<String, JComponent> inputComponents = new HashMap<>();
    private List<FormElement> formElements;

    /**
     * Example usage of the FormPanel.
     */
//    // Example usage
//    public static void main(String[] args) {
//        try {
//            UIManager.setLookAndFeel(new FlatMacLightLaf());
//        } catch (UnsupportedLookAndFeelException aE) {
//            throw new RuntimeException(aE);
//        }
//        List<FormElement> elements = List.of(
//                new FormElement("fullName", null, String.class, "John Doe", true, true),
//                new FormElement("age", null, Integer.class, 30, true, true),
//                new FormElement("test", null, Integer.class, 30, true, true)
////                new FormElement("bio", "Biography", String.class, false, "A software engineer..."),
////                new FormElement("isStudent", "Is Student", Boolean.class, true, false),
////                new FormElement("metadata", "Metadata (JSON)", Map.class, false, Map.of("department", "IT", "projects", List.of("A", "B")))
//        );
//
//        FormPanel formPanel = new FormPanel();
//        formPanel.setFormElements(elements);
//
//        JButton submitButton = new JButton("Submit");
//        submitButton.addActionListener(e -> {
//            List<String> errors = formPanel.validateForm();
//            if (errors.isEmpty()) {
//                Map<String, Object> formValues = formPanel.getFormValues();
//                JOptionPane.showMessageDialog(formPanel, "Form is valid!\n" + formValues, "Success", JOptionPane.INFORMATION_MESSAGE);
//            } else {
//                JOptionPane.showMessageDialog(formPanel, String.join("\n", errors), "Validation Errors", JOptionPane.ERROR_MESSAGE);
//            }
//        });
//
//        JFrame frame = new JFrame("FormPanel Demo");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(formPanel, BorderLayout.CENTER);
//        frame.getContentPane().add(submitButton, BorderLayout.SOUTH);
//        frame.pack();
//        frame.setLocationRelativeTo(null);
//        frame.setVisible(true);
//    }

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
        boolean result = true;

        for (FormElement element : formElements) {
            if (!element.mandatory)
                continue;

            JTextComponent textComponent = getTextComponent(inputComponents.get(element.name()));
            if (textComponent != null) {
                result = textComponent.getText() != null && !textComponent.getText().isEmpty();
                if (!result)
                    break;
            }
        }

        return result;
    }

    /**
     * Requests focus for the first input component in the form.
     */
    @Override
    public void requestFocus() {
        if (formElements != null && !formElements.isEmpty()) {
            JComponent c = inputComponents.get(formElements.get(0).name());
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
        for (JComponent component : inputComponents.values()) {
            if (component instanceof JTextComponent textComponent) {
                textComponent.setText(null);
            } else if (component instanceof JScrollPane) {
                JViewport viewport = ((JScrollPane) component).getViewport();
                if (viewport.getView() instanceof JTextArea) {
                    ((JTextArea) viewport.getView()).setText(null);
                }
            }
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
            boolean isMultiLine = (element.type() == String.class && (!element.compactEditor() || formElements.size() == 1)) ||
                    Map.class.isAssignableFrom(element.type());
            String rowSpec = isMultiLine ? "max(pref;40dlu)" : "default";
            builder.appendRow(rowSpec);

            JComponent inputComponent = createInputComponent(element);
            inputComponents.put(element.name(), inputComponent);
            if (formElements.size() > 1 || !isMultiLine) {
                JLabel label = new JLabel(element.label() + ":");

                // Add the label, aligning it to the top for multi-line components for better aesthetics.
                if (isMultiLine) {
                    // For multi-line components, add a 4px top margin using an EmptyBorder for better vertical alignment.
                    label.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
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

    private JComponent createInputComponent(FormElement element) {
        Class<?> type = element.type();
        Object defaultValue = element.defaultValue();

        if (type == String.class) {
            if (element.compactEditor() && formElements.size() > 1) {
                JTextField textField = new JTextField(20);
                textField.getDocument().addDocumentListener(this);
                if (defaultValue != null) {
                    textField.setText(defaultValue.toString());
                }
                return textField;
            } else {
                TextEditor textArea = new TextEditor(formElements.size() > 1 ? 3 : 5, 20);
                textArea.getDocument().addDocumentListener(this);
                setupPopupMenu(textArea);
                setupShortcuts(textArea);
                if (defaultValue != null) {
                    textArea.setText(defaultValue.toString());
                }
                JScrollPane scrollPane = new JScrollPane(textArea) {
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
                    textArea.setPlaceHolderText(element.label());
                }
                return scrollPane;
            }
        } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            if (type == Boolean.class || type == boolean.class) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.addActionListener(e -> firePropertyChange(PROPERTY_VALUE_CHANGED, null, null));
                if (defaultValue != null) {
                    checkBox.setSelected((Boolean) defaultValue);
                }
                return checkBox;
            } else {
                JTextField textField = new JTextField(20);
                textField.getDocument().addDocumentListener(this);
                setupPopupMenu(textField);
                setupShortcuts(textField);
                if (defaultValue != null) {
                    textField.setText(defaultValue.toString());
                }
                return textField;
            }
        } else if (Map.class.isAssignableFrom(type)) {
            TextEditor textArea = new TextEditor(5, 20);
            textArea.getDocument().addDocumentListener(this);
            setupPopupMenu(textArea);
            setupShortcuts(textArea);
            if (defaultValue != null) {
                try {
                    textArea.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(defaultValue));
                } catch (JsonProcessingException e) {
                    textArea.setText("Error converting default value to JSON: " + e.getMessage());
                }
            } else {
                textArea.setText("{\n  \n}");
            }
            return new JScrollPane(textArea);
        }

        // Fallback for unsupported types
        return new JLabel("Unsupported type: " + type.getSimpleName());
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
     * Validates the form inputs and returns a list of error messages. An empty list indicates the form is valid.
     *
     * @return A list of validation error messages.
     */
    public List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        for (FormElement element : formElements) {
            JComponent component = inputComponents.get(element.name());
            String value = getComponentValueAsString(component);
            Class<?> type = element.type();

            if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                try {
                    if (type == Integer.class || type == int.class) Integer.parseInt(value);
                    else if (type == Long.class || type == long.class) Long.parseLong(value);
                    else if (type == Double.class || type == double.class) Double.parseDouble(value);
                    else if (type == Float.class || type == float.class) Float.parseFloat(value);
                    // Add other numeric types if needed
                } catch (NumberFormatException e) {
                    errors.add("'" + element.label() + "' must be a valid number.");
                }
            } else if (Map.class.isAssignableFrom(type)) {
                try {
                    OBJECT_MAPPER.readValue(value, Map.class);
                } catch (JsonProcessingException e) {
                    errors.add("'" + element.label() + "' must be a valid JSON object.");
                }
            }
        }
        return errors;
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
            JComponent component = inputComponents.get(element.name());
            String stringValue = getComponentValueAsString(component);
            Class<?> type = element.type();
            Object parsedValue = null;

            try {
                if (type == String.class) {
                    parsedValue = stringValue;
                } else if (type == Boolean.class || type == boolean.class) {
                    parsedValue = ((JCheckBox) component).isSelected();
                } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                    if (type == Integer.class || type == int.class) parsedValue = Integer.parseInt(stringValue);
                    else if (type == Long.class || type == long.class) parsedValue = Long.parseLong(stringValue);
                    else if (type == Double.class || type == double.class)
                        parsedValue = Double.parseDouble(stringValue);
                    else if (type == Float.class || type == float.class) parsedValue = Float.parseFloat(stringValue);
                    else parsedValue = stringValue; // Fallback for other Numbers
                } else if (Map.class.isAssignableFrom(type)) {
                    parsedValue = OBJECT_MAPPER.readValue(stringValue, Map.class);
                }
            } catch (Exception e) {
                // This should ideally not happen if validateForm() is called first
                throw new RuntimeException("Error parsing value for '" + element.name() + "': " + e.getMessage(), e);
            }
            values.put(element.name(), parsedValue);
        }
        return values;
    }

    /**
     * Sets the values of the form elements based on the provided map.
     *
     * @param formValues A map where keys are form element names and values are the corresponding data.
     */
    public void setFormValues(Map<String, Object> formValues) {
        if (formValues == null)
            return;

        for (FormElement element : formElements) {
            if (formValues.containsKey(element.name())) {
                JComponent component = inputComponents.get(element.name());
                Object value = formValues.get(element.name());
                Class<?> type = element.type();

                if (component == null || value == null) {
                    continue;
                }

                if (type == String.class) {
                    JTextComponent textComponent = getTextComponent(component);
                    if (textComponent != null)
                        textComponent.setText(value.toString());
                } else if (type == Boolean.class || type == boolean.class) {
                    if (component instanceof JCheckBox) {
                        ((JCheckBox) component).setSelected((Boolean) value);
                    }
                } else if (Number.class.isAssignableFrom(type)) {
                    if (component instanceof JTextField) {
                        ((JTextField) component).setText(value.toString());
                    }
                } else if (Map.class.isAssignableFrom(type)) {
                    JTextComponent textComponent = getTextComponent(component);
                    if (textComponent != null) {
                        try {
                            textComponent.setText(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
                        } catch (JsonProcessingException e) {
                            textComponent.setText("Error converting value to JSON: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private String getComponentValueAsString(JComponent component) {
        if (component instanceof JTextField) {
            return ((JTextField) component).getText();
        } else if (component instanceof JScrollPane) {
            JViewport viewport = ((JScrollPane) component).getViewport();
            if (viewport.getView() instanceof JTextArea) {
                return ((JTextArea) viewport.getView()).getText();
            }
        } else if (component instanceof JCheckBox) {
            return String.valueOf(((JCheckBox) component).isSelected());
        }
        return "";
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
                              String label,
                              Class<?> type,
                              Object defaultValue,
                              boolean compactEditor,
                              boolean mandatory) {

        /**
         * Constructs a new {@code FormElement}.
         *
         * @param name          The name of the form element.
         * @param label         The human-readable label for the form element. If null or empty, a label will be
         *                      generated from the name.
         * @param type          The data type of the form element's value.
         * @param defaultValue  The initial value of the form element.
         * @param compactEditor A boolean indicating whether to use a compact editor (e.g., JTextField) for String
         *                      types.
         * @param mandatory     A boolean indicating whether the form element value is mandatory.
         * @throws NullPointerException if {@code name} is null.
         */
        public FormElement(String name, String label, Class<?> type, Object defaultValue, boolean compactEditor, boolean mandatory) {
            Objects.requireNonNull(name);
            this.name = name;
            this.label = label != null && label.length() > 0 ? label : labelFromName(name);
            this.type = type;
            this.defaultValue = defaultValue;
            this.compactEditor = compactEditor;
            this.mandatory = mandatory;
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
            setFont(new Font("SansSerif", Font.PLAIN, 14));
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
