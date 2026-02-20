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

import com.gl.appframework.AppDialog;

import javax.swing.*;

public class FormDialog<T> extends AppDialog<T, T> {
    private FormPanel formPanel;
    private T formData;

    /**
     * Constructs a new AppDialog.
     *
     * @param owner The {@link JFrame} from which the dialog is displayed.
     * @param title The String to be displayed in the dialog's title bar.
     */
    public FormDialog(JFrame owner, String title, Class<?> dataClass) {
        super(owner, title);

        init(dataClass);
    }

    /**
     * Constructs a new AppDialog.
     *
     * @param owner The {@link JDialog} from which the dialog is displayed.
     * @param title The String to be displayed in the dialog's title bar.
     */
    public FormDialog(JDialog owner, String title, Class<?> dataClass) {
        super(owner, title);

        init(dataClass);
    }

    private void init(Class<?> dataClass) {
        final FormPanel formPanel;
        this.formPanel = new FormPanel();
//        this.formPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        this.formPanel.setFormElements(FormElement.getFormElements(dataClass));
        setContent(this.formPanel);
    }

    @Override
    protected void toForm(T formData) {
        this.formData = formData;
        formPanel.toForm(formData);
    }

    @Override
    protected T fromForm() {
        formPanel.fromForm(formData);
        return formData;
    }
}
