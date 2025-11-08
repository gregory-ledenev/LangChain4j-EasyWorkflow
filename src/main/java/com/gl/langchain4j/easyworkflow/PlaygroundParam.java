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

package com.gl.langchain4j.easyworkflow;

import com.gl.langchain4j.easyworkflow.gui.platform.FormEditorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to mark a parameter in a method as a playground parameter.
 * This is used for generating UI forms for testing and demonstration purposes.
 */
@Retention(RUNTIME)
@Target({PARAMETER})
public @interface PlaygroundParam {
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
}
