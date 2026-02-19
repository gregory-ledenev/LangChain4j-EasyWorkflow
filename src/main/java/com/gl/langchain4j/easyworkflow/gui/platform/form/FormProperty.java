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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({METHOD})
public @interface FormProperty {
    /**
     * The label for the parameter in the UI form.
     * If not specified, the parameter name will be used.
     * @return The label for the parameter.
     */
    String label() default "";

    /**
     * The description for the parameter in the UI form that can be rendered as a tooltip.
     * @return The description for the parameter.
     */
    String description() default "";

    /**
     * The type of editor to use for this parameter in the UI form.
     * @return The editor type.
     */
    FormEditorType editorType() default FormEditorType.Default;

    /**
     * An array of choices for editor types that support predefined options (e.g., dropdowns).
     * @return An array of editor choices.
     */
    String[] editorChoices() default {};

    /**
     * The default value for the parameter in the UI form.
     * @return The default value.
     */
    String defaultValue() default "";

    /**
     * The class of the form element to be used for this property.
     * This allows for custom UI components to be associated with the property.
     * @return The form element class.
     */
    Class<? extends FormElement> elementClass() default FormElement.class;

    /**
     * The sort order for the property in the UI form.
     * Properties with lower values will be displayed first. Use integer values to define initial order like 1,
     * 2, 3 etc. Use floating point values like 1.5f to insert some elements between others without reordering neighbors.
     * @return The sort order.
     */
    float sortOrder() default Float.MAX_VALUE;

    /**
     * Indicates whether the property is mandatory and must be filled in the UI form.
     * If true, the UI should validate that a value is provided.
     * @return True if mandatory, false otherwise.
     */
    boolean mandatory() default false;
}