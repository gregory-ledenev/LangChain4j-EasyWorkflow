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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.appframework.ToolbarIcons;
import com.gl.appframework.UISupport;
import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.actions.BasicAction;
import com.gl.appframework.comp.ActionPopupMenu;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

import static com.gl.appframework.UISupport.*;
import static com.gl.appframework.comp.ActionMenuSupport.createMenuItem;

/**
 * A panel that dynamically generates a form based on a list of {@link FormElement} objects. It supports various input
 * types, validation, and value retrieval.
 */
public class FormPanel extends JPanel implements Scrollable, DocumentListener {
    public static final String PROPERTY_VALUE_CHANGED = "valueChanged";
    public static final String PROPERTY_ENTER_PRESSED = "enterPressed";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<String, FormEditor<?>> editors = new HashMap<>();
    private List<FormElement<?>> formElements;

    /**
     * Requests focus for the first input component in the form.
     */
    @Override
    public void requestFocus() {
        if (formElements != null && !formElements.isEmpty()) {
            JComponent c = editors.get(formElements.get(0).getName()).getComponent();
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
    public void clearForm() {
        for (FormEditor<?> editor : editors.values()) {
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
    public List<FormElement<?>> getFormElements() {
        return Collections.unmodifiableList(formElements);
    }

    /**
     * Sets the form elements to be displayed and rebuilds the form.
     *
     * @param aFormElements the list of {@link FormElement} objects to display.
     */
    public void setFormElements(List<FormElement<?>> aFormElements) {
        formElements = aFormElements;
        buildForm();

        revalidate();
        repaint();
    }

    private void buildForm() {
        removeAll();
        editors.clear();

        boolean singleEditorNoLabel = false;
        if (formElements.size() == 1) {
            FormElement<?> element = formElements.get(0);
            FormEditor<?> editor = createEditor(element);
            editors.put(element.getName(), editor);
            if (!editor.needLabel()) {
                singleEditorNoLabel = true;
            }
        }

        // Define the layout columns: Right-aligned label, gap, and a component that grows.
        String colSpec = singleEditorNoLabel ? "default:grow" : "right:pref, 3dlu, default:grow";

        // Use the modern DefaultFormBuilder, which handles row creation and gaps automatically.
        FormLayout layout = new FormLayout(colSpec, ""); // Rows are added dynamically
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        for (int i = 0; i < formElements.size(); i++) {
            FormElement<?> element = formElements.get(i);
            // Define the row specification based on whether the component is multi-line.
            // 'default' prevents single-line components from collapsing vertically.
            boolean isMultiLine = (element.getType() == String.class &&
                    (element.getEditorType() == FormEditorType.Note ||
                            formElements.size() == 1)) ||
                    Map.class.isAssignableFrom(element.getType());
            String rowSpec = isMultiLine ? "max(pref;40dlu)" : "default";
            builder.appendRow(rowSpec);
            
            FormEditor<?> editor = editors.get(element.getName());
            if (editor == null) {
                editor = createEditor(element);
                editors.put(element.getName(), editor);
            }
            JComponent inputComponent = editor.getComponent();

            if (!singleEditorNoLabel && editor.needLabel() && (formElements.size() > 1 || !isMultiLine)) {
                JLabel label = new JLabel(element.getLabel() + ":", element.getIcon(), JLabel.LEFT);
                setupFont(label);

                if (isMultiLine) {
                    label.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
                    builder.add(label, cc.xy(1, builder.getRowCount(), "left, top"));
                } else {
                    builder.add(label, cc.xy(1, builder.getRowCount(), "left, default"));
                }
            }

            // Add the input component with specific constraints.
            // JCheckBox uses default (left) alignment and does not fill.
            // Other components fill the horizontal space.
            int colIndex = singleEditorNoLabel ? 1 : 3;
            if (inputComponent instanceof JCheckBox) {
                builder.add(inputComponent, cc.xy(colIndex, builder.getRowCount()));
            } else {
                builder.add(inputComponent, cc.xy(colIndex, builder.getRowCount(), "fill, default"));
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

    protected FormEditor<?> createEditor(FormElement<?> element) {
        return element.createFormEditor(this, element);
    }
    
    /**
     * Configures a standard popup menu for text components, including a "Clear All" action.
     *
     * @param textComponent the text component to attach the popup menu to.
     */
    public void setupPopupMenu(JTextComponent textComponent) {
        UISupport.setupPopupMenu(textComponent);

        if (formElements.size() > 1) {
            ActionPopupMenu popupMenu = (ActionPopupMenu) textComponent.getComponentPopupMenu();
            popupMenu.getActionGroup().addAction(new ActionGroup(
                    new BasicAction("Clear All", new AutoIcon(ToolbarIcons.ICON_SPACER), e -> clearForm())
            ));
            popupMenu.setActionGroup(popupMenu.getActionGroup()); //todo: remove me when changes will be picked up automatically
        }
    }

    public void setupShortcuts(JTextComponent c) {
        c.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        c.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FormPanel.this.firePropertyChange(PROPERTY_ENTER_PRESSED, null, null);
            }
        });
    }

    /**
     * Checks if all form elements contain valid data without showing any UI dialogs.
     *
     * @return {@code true} if the form is valid, {@code false} otherwise.
     */
    public boolean isFormValid() {
        return checkFormValidity(false) == null;
    }

    /**
     * Validates the form inputs and returns an error message or {@code null} if form is valid.
     * It also shows the message and focuses on invalid editor.
     *
     * @return the validation error message, or {@code null} if all elements are valid.
     */
    public String checkFormValidity() {
        return checkFormValidity(true);
    }

    /**
     * Validates the form inputs and returns an error message or {@code null} if form is valid.
     * It also shows the message and focuses on invalid editor.
     *
     * @param showError if {@code true}, a message will be shown for the first validation error found.
     * @return the validation error message, or {@code null} if all elements are valid.
     */
    public String checkFormValidity(boolean showError) {
        String result = null;
        for (FormElement<?> element : formElements) {
            FormEditor editor = editors.get(element.getName());
            if (editor == null)
                continue;
            result = editor.checkValidity(true);
            if(result != null && showError) {
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
    public Map<String, Object> fromForm() {
        Map<String, Object> values = new HashMap<>();
        for (FormElement<?> element : formElements) {
            FormEditor editor = editors.get(element.getName());
            Object parsedValue = editor.getValue();
            values.put(element.getName(), parsedValue);
        }
        return values;
    }

    /**
     * Sets the values of the form elements based on the provided map.
     *
     * @param formValues A map where keys are form element names and values are the corresponding data.
     * @return {@code true} if any value was set, {@code false} otherwise.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean toForm(Map<String, Object> formValues) {
        if (formValues == null)
            return false;

        boolean result = false;

        for (FormElement<?> element : formElements) {
            if (formValues.containsKey(element.getName())) {
                FormEditor editor = editors.get(element.getName());
                Object value = formValues.get(element.getName());
                if (editor != null) {
                    editor.setValue(value);
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Populates the form using the properties of a Java Bean.
     *
     * @param bean the object containing values to be set in the form.
     * @return {@code true} if any value was successfully set, {@code false} otherwise.
     */
    public boolean toForm(Object bean) {
        return toForm(getFormValues(bean));
    }

    /**
     * Populates a Java Bean with the current values from the form.
     *
     * @param bean the object to be updated with form values.
     * @throws RuntimeException if a property cannot be written.
     */
    public void fromForm(Object bean) {
        Objects.requireNonNull(bean);
        Map<String, Object> values = fromForm();
        try {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                PropertyDescriptor pd = new PropertyDescriptor(entry.getKey(), bean.getClass());
                pd.getWriteMethod().invoke(bean, entry.getValue());
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to populate bean from form", e);
        }
    }

    protected Map<String, Object> getFormValues(Object bean) {
        Objects.requireNonNull(bean);

        Map<String, Object> values = new HashMap<>();
        try {
            for (FormElement<?> element : formElements)
                values.put(element.getName(), getFormValue(element.getName(), bean));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to populate form from bean", e);
        }

        return values;
    }

    private Object getFormValue(String name, Object bean) throws Exception {
        PropertyDescriptor pd = new PropertyDescriptor(name, bean.getClass());
        return pd.getReadMethod().invoke(bean);
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

    // --- Editor Implementations ---

    public static Number parseNumber(String text, Class<?> type) {
        if (text == null || text.isEmpty()) {
            return null;
        }
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

    public static String getTooltipText(FormElement<?> formElement) {
        return formElement.getDescription() != null && formElement.getDescription().isEmpty() ?
                "%s %s".formatted(formElement.getType().getSimpleName(), formElement.getName()) :
                formElement.getDescription();
    }

    static void setupFont(JComponent c) {
        c.setFont(c.getFont().deriveFont(c.getFont().getSize() + 2.0f));
    }

    @Override
    public void firePropertyChange(String propertyValueChanged, Object o, Object o1) {
        super.firePropertyChange(propertyValueChanged, o, o1);
    }

    static class FormTextEditor extends JTextArea {
        private String placeHolderText;

        public FormTextEditor(int rows, int columns) {
            super(rows, columns);
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
