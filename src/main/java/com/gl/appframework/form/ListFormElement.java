package com.gl.appframework.form;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * A form element representing a list of objects.
 * This element uses {@link ListFormProperty} metadata to handle collection-specific
 * UI logic such as element types and allowed operations (add, remove, etc.).
 */
public class ListFormElement extends FormElement<List<?>> {
    protected Class<?> elementClass;
    protected String elementDisplayName;
    protected EnumSet<ListFormProperty.ListCapability> capabilities;

    public ListFormElement() {
    }

    @Override
    protected void init(PropertyDescriptor propertyDescriptor) {
        super.init(propertyDescriptor);

        ListFormProperty annotation = propertyDescriptor.getReadMethod().getAnnotation(ListFormProperty.class);
        if (annotation != null) {
            this.elementClass = annotation.elementClass();
            this.elementDisplayName = annotation.elementDisplayName();
            this.capabilities = EnumSet.copyOf(Arrays.asList(annotation.capabilities()));
        }
    }

    /**
     * Gets the class type of the elements contained in the list.
     *
     * @return the element class type
     */
    public Class<?> getElementClass() {
        return elementClass;
    }

    /**
     * Gets the display name used for individual elements in the UI.
     *
     * @return the element display name
     */
    public String getElementDisplayName() {
        return elementDisplayName;
    }

    /**
     * Gets the set of allowed operations (capabilities) for this list element.
     *
     * @return an EnumSet of {@link ListFormProperty.ListCapability}
     */
    public EnumSet<ListFormProperty.ListCapability> getCapabilities() {
        return capabilities;
    }

    @Override
    protected FormEditor<?> createFormEditor(FormPanel formPanel, FormElement<?> element) {
        return new ListFormEditor(formPanel, (ListFormElement) element);
    }
}
