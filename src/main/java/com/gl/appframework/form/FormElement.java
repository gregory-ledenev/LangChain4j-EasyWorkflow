package com.gl.appframework.form;

import com.gl.appframework.UISupport;

import javax.swing.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Represents a single element in a form, containing its name, label, type, default value, whether it should use a
 * specific editor, if it's mandatory etc.
 */
public class FormElement<T> {
    protected String name;
    protected String label;
    protected String description;
    protected Class<T> type;
    protected T defaultValue;
    protected FormEditorType editorType;
    protected T[] editorChoices;
    protected boolean mandatory;
    protected float sortOrder = Float.MAX_VALUE;
    private Icon icon;

    protected FormElement() {
    }

    /**
     * Constructs a new {@code FormElement} with a name and type.
     *
     * @param name The name of the form element.
     * @param type The data type of the form element's value.
     */
    public FormElement(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Constructs a new {@code FormElement}.
     *
     * @param name          The name of the form element. Should be unique in the scope of a form.
     * @param label         The human-readable label for the form element. If null or empty, a label will be
     *                      generated from the name.
     * @param description   A tooltip description for the form element.
     * @param type          The data type of the form element's value.
     * @param defaultValue  The initial value of the form element.
     * @param editorType    A boolean indicating whether to use a compact editor (e.g., JTextField) for String
     *                      types.
     * @param editorChoices An array of options for dropdown editors.
     * @param mandatory     A boolean indicating whether the form element value is mandatory
     * @throws NullPointerException if {@code name} is null.
     */
    public FormElement(String name, String label, String description, Class<T> type, T defaultValue, FormEditorType editorType, T[] editorChoices, boolean mandatory) {
        this(name, type);

        this.label = label != null && !label.isEmpty() ? label : labelFromName(name);
        this.description = description;
        this.defaultValue = defaultValue;
        this.editorType = editorType;
        this.mandatory = mandatory;
        this.editorChoices = editorChoices;
    }

    /**
     * Introspects the given class and returns a list of {@code FormElement}s based on its properties.
     *
     * @param aClass The class to introspect.
     * @return A list of form elements.
     */
    public static List<FormElement<?>> getFormElements(Class<?> aClass) {
        List<FormElement<?>> elements = new ArrayList<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(aClass);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                Method readMethod = propertyDescriptor.getReadMethod();
                if (readMethod == null ||
                        readMethod.getDeclaringClass() == Object.class ||
                        readMethod.getAnnotation(IgnoreFormProperty.class) != null ||
                        propertyDescriptor.getWriteMethod() == null)
                    continue;

                Class<?> formElementClass = FormElement.class;
                if (readMethod.getAnnotation(FormProperty.class) != null)
                    formElementClass = readMethod.getAnnotation(FormProperty.class).elementClass();

                FormElement<?> formElement = (FormElement<?>) formElementClass.getDeclaredConstructor().newInstance();
                formElement.init(propertyDescriptor);
                elements.add(formElement);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get form elements for class: " + aClass.getName(), e);
        }

        elements.sort(Comparator.comparing(FormElement::getSortOrder));

        return elements;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    /**
     * @return The sort order of the form element.
     */
    public float getSortOrder() {
        return sortOrder;
    }

    @SuppressWarnings("unchecked")
    protected void init(PropertyDescriptor propertyDescriptor) {
        Objects.requireNonNull(propertyDescriptor);
        Objects.requireNonNull(propertyDescriptor.getReadMethod());

        Method readMethod = propertyDescriptor.getReadMethod();
        FormProperty annotation = readMethod.getAnnotation(FormProperty.class);
        String label = null;
        String description = null;
        FormEditorType editorType = FormEditorType.Default;
        Object[] editorChoices = null;
        Class<T> propertyType = (Class<T>) propertyDescriptor.getPropertyType();
        T defaultValue = null;
        Icon icon = null;

        boolean mandatory = propertyType.isPrimitive();

        if (annotation != null) {
            label = annotation.label();
            description = annotation.description();
            editorType = annotation.editorType();
            if (annotation.editorChoices().length > 0)
                editorChoices = Arrays
                        .stream(annotation.editorChoices())
                        .map(s -> convertValue(s, propertyType))
                        .toArray(Object[]::new);

            if (!annotation.defaultValue().isEmpty())
                defaultValue = convertValue(annotation.defaultValue(), propertyType);
            sortOrder = annotation.sortOrder();
            mandatory = annotation.mandatory();
            if (!annotation.icon().isEmpty())
                icon = new UISupport.AutoIcon(annotation.icon());
        } else {
            if (propertyType.isEnum())
                editorChoices = propertyType.getEnumConstants();
        }

        this.name = propertyDescriptor.getName();
        this.label = label == null || label.isEmpty() ? labelFromName(this.name) : label;
        this.icon = icon;
        this.description = description;
        this.type = propertyType;
        this.defaultValue = defaultValue;
        this.editorType = editorType;
        this.editorChoices = (T[]) editorChoices;
        this.mandatory = mandatory;

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private T convertValue(String defaultValue, Class<?> propertyType) {
        if (defaultValue == null) return null;
        if (propertyType == String.class) return (T) defaultValue;
        if (propertyType == Integer.class || propertyType == int.class) return (T) Integer.valueOf(defaultValue);
        if (propertyType == Long.class || propertyType == long.class) return (T) Long.valueOf(defaultValue);
        if (propertyType == Double.class || propertyType == double.class) return (T) Double.valueOf(defaultValue);
        if (propertyType == Float.class || propertyType == float.class) return (T) Float.valueOf(defaultValue);
        if (propertyType == Boolean.class || propertyType == boolean.class) return (T) Boolean.valueOf(defaultValue);
        if (propertyType.isEnum()) {
            try {
                return (T) Enum.valueOf((Class<Enum>) propertyType, defaultValue);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return (T) defaultValue;
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

    /**
     * @return The name of the form element.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The human-readable label for the form element.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label The human-readable label to set.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return A tooltip description for the form element.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The tooltip description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The data type of the form element's value.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return The initial value of the form element.
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue The initial value to set.
     */
    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * @return The preferred editor type for this element.
     */
    public FormEditorType getEditorType() {
        return editorType;
    }

    /**
     * @param editorType The preferred editor type to set.
     */
    public void setEditorType(FormEditorType editorType) {
        this.editorType = editorType;
    }

    /**
     * @return An array of options for dropdown editors.
     */
    public Object[] getEditorChoices() {
        return editorChoices;
    }

    /**
     * @param editorChoices An array of options to set for dropdown editors.
     */
    public void setEditorChoices(T[] editorChoices) {
        this.editorChoices = editorChoices;
    }

    /**
     * @return True if the form element value is mandatory.
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * @param mandatory Boolean indicating whether the form element value is mandatory.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FormElement<?>) obj;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "FormElement[" +
                "name=" + name + ", " +
                "label=" + label + ", " +
                "description=" + description + ", " +
                "type=" + type + ", " +
                "defaultValue=" + defaultValue + ", " +
                "editorType=" + editorType + ", " +
                "editorChoices=" + java.util.Arrays.toString(editorChoices) + ", " +
                "mandatory=" + mandatory + ']';
    }

    @SuppressWarnings("unchecked")
    protected FormEditor<?> createFormEditor(FormPanel formPanel, FormElement<?> element) {
        Class<?> type = element.getType();

        int formSize = formPanel.getFormElements().size();
        if (type == String.class) {
            if ((element.getEditorType() == FormEditorType.EditableDropdown) && formSize > 1) {
                return new EditableDropdownFormEditor(formPanel, element);
            } else if (element.getEditorType() == FormEditorType.Dropdown && formSize > 1) {
                return new DropdownFormEditor(formPanel, element);
            } else if ((element.getEditorType() == FormEditorType.Text ||
                    element.getEditorType() == FormEditorType.Default)) {
                return new CompactStringFormEditor(formPanel, (FormElement<String>) element);
            } else {
                return new StringFormEditor(formPanel, (FormElement<String>) element);
            }
        } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            if (type == Boolean.class || type == boolean.class) {
                return new BooleanFormEditor(formPanel, (FormElement<Boolean>) element);
            } else {
                if (element.getEditorType() == FormEditorType.EditableDropdown && formSize > 1) {
                    return new EditableDropdownFormEditor(formPanel, element);
                } else if (element.getEditorType() == FormEditorType.Dropdown && formSize > 1) {
                    return new DropdownFormEditor(formPanel, element);
                } else {
                    return new NumberFormEditor(formPanel, (FormElement<Number>) element);
                }
            }
        } else if (Map.class.isAssignableFrom(type)) {
            return new MapFormEditor(formPanel, (FormElement<Map<?, ?>>) element);
        }

        // Fallback for unsupported types
        return new UnsupportedTypeFormEditor(element);
    }
}
