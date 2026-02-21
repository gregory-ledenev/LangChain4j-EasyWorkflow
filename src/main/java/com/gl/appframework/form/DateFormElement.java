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

import java.beans.PropertyDescriptor;
import java.util.Date;

/**
 * Represents a form element for date input.
 */
public class DateFormElement extends FormElement<Date> {
    private String dateFormatPattern;

    protected DateFormElement() {
    }

    /**
     * Gets the pattern used for date formatting.
     *
     * @return the date format pattern
     */
    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected FormEditor<?> createFormEditor(FormPanel formPanel, FormElement<?> element) {
        return new DateFormEditor(formPanel, (FormElement<Date>) element);
    }

    @Override
    protected void init(PropertyDescriptor propertyDescriptor) {
        super.init(propertyDescriptor);
        DateFormProperty annotation = propertyDescriptor.getReadMethod().getAnnotation(DateFormProperty.class);
        if (annotation != null)
            this.dateFormatPattern = annotation.datePattern();
    }
}
