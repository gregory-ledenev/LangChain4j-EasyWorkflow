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

import javax.swing.*;

/**
 * Interface for form element editors.
 */
public interface FormEditor<T> {

    /**
     * Sets the value of the editor.
     *
     * @param value The value to set.
     */
    void setValue(T value);

    /**
     * Returns the current value of the editor.
     *
     * @return The current value.
     */
    T getValue();

    /**
     * Returns the component used for the editor.
     *
     * @return The editor's component.
     */
    JComponent getComponent();

    /**
     * Checks the validity of the editor's current value.
     *
     * @return An error message if the value is invalid, or {@code null} if valid.
     */
    String checkValidity(boolean strictCheck);

    /**
     * Requests focus for the editor's component.
     */
    void requestFocus();

    /**
     * Indicates whether this editor requires a separate label to be displayed.
     *
     * @return {@code true} if a label is needed, {@code false} otherwise.
     */
    default boolean needLabel() {
        return true;
    }
}
