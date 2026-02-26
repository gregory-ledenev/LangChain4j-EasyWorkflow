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

package com.gl.appframework.actions;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

public class ComponentAction extends BasicAction {
    private final JComponent component;

    public ComponentAction(String name, JComponent component) {
        super(name, null, null);

        Objects.requireNonNull(component);
        this.component = component;
        this.component.putClientProperty(COMPONENT_ACTION_KEY, this);
    }

    public ComponentAction(String name, AbstractButton component, Consumer<ActionEvent> actionListener) {
        this(name, component, actionListener, null);
    }

    public ComponentAction(String name, AbstractButton component, Consumer<ActionEvent> actionListener, Consumer<? extends BasicAction> actionUpdater) {
        super(name, null, actionListener, actionUpdater);

        Objects.requireNonNull(component);
        this.component = component;
        this.component.putClientProperty(COMPONENT_ACTION_KEY, this);
        component.addActionListener(actionListener::accept);
    }

    public ComponentAction(String name, JTextComponent component, Consumer<ActionEvent> actionListener) {
        this(name, component, actionListener, null);
    }

    public ComponentAction(String name, JTextComponent component, Consumer<ActionEvent> actionListener, Consumer<? extends BasicAction> actionUpdater) {
        super(name, null, actionListener, actionUpdater);

        Objects.requireNonNull(component);
        this.component = component;
        this.component.putClientProperty(COMPONENT_ACTION_KEY, this);
        component.getDocument().addDocumentListener(createDocumentListener(component, actionListener));
    }

    public ComponentAction(String name, JComboBox<?> component, Consumer<ActionEvent> actionListener) {
        this(name, component, actionListener, null);
    }

    public ComponentAction(String name, JComboBox<?> component, Consumer<ActionEvent> actionListener, Consumer<? extends BasicAction> actionUpdater) {
        super(name, null, actionListener, actionUpdater);
        this.component = component;
        this.component.putClientProperty(COMPONENT_ACTION_KEY, this);

        if (component.isEditable() && component.getEditor().getEditorComponent() instanceof JTextComponent textComponent)
            textComponent.getDocument().addDocumentListener(createDocumentListener(textComponent, actionListener));
        component.addActionListener(actionListener::accept);
    }

    private static DocumentListener createDocumentListener(JTextComponent component, Consumer<ActionEvent> actionListener) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actionListener.accept(new ActionEvent(component, 0, "insertUpdate"));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actionListener.accept(new ActionEvent(component, 0, "removeUpdate"));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actionListener.accept(new ActionEvent(component, 0, "changedUpdate"));
            }
        };
    }

    public JComponent getComponent() {
        return component;
    }
}
