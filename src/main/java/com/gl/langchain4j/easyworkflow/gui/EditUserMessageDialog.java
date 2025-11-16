/*
 *
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
 * /
 */

package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.platform.Actions;
import com.gl.langchain4j.easyworkflow.gui.platform.AppDialog;
import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

/**
 * A dialog for editing a user message.
 */
public class EditUserMessageDialog extends AppDialog<String> {
    /**
     * The action command for the reset button.
     */
    public static final String ACTION_COMMAND_RESET = "reset";

    private final UserMessageEditor edtUserMessage = new UserMessageEditor();

    /**
     * Constructs a new {@code EditUserMessageDialog}.
     *
     * @param owner The parent frame of the dialog.
     */
    public EditUserMessageDialog(JFrame owner) {
        super(owner, "Edit User Message");

        setSize(400, 300);
        setPreferredSize(new Dimension(400, 300));
        setLocationRelativeTo(getParent());
        setMinimumSize(new Dimension(400, 300));
        setMaximumSize(new Dimension(600, 400));

        JScrollPane content = new JScrollPane(edtUserMessage);
        setContent(content);

        JButton resetButton = new JButton("Reset");
        resetButton.setActionCommand(ACTION_COMMAND_RESET);
        resetButton.addActionListener(e -> reset());
        addButton(resetButton, true);
    }

    /**
     * Resets the dialog's state, setting the modal result to {@link #ACTION_COMMAND_RESET}
     * and making the dialog invisible.
     */
    public void reset() {
        setModalResult(ACTION_COMMAND_RESET);
        setVisible(false);
    }

    /**
     * Returns the current user message from the editor.
     *
     * @return The user message.
     */
    public String getUserMessage() {
        return edtUserMessage.getText();
    }

    /**
     * Sets the user message in the editor.
     *
     * @param userMessage The user message to set.
     */
    public void setUserMessage(String userMessage) {
        edtUserMessage.setText(userMessage);
    }


    @Override
    protected String fromForm() {
        return getUserMessage();
    }

    @Override
    protected void toForm(String data) {
        setUserMessage(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canClose() {
        boolean result = getUserMessage() != null && ! getUserMessage().isBlank();
        if (! result) {
            showMessage(null, "User message cannot be empty");
            edtUserMessage.requestFocus();
        }
        return result;
    }

    /**
     * A record representing the result of the dialog, including the modal result
     * and the edited user message.
     *
     * @param modalResult The modal result of the dialog (e.g., "ok", "cancel", "reset").
     * @param userMessage The edited user message.
     */
    public record Result(String modalResult, String userMessage) {}

    /**
     * Displays a modal dialog to edit a user message.
     *
     * @param owner The parent frame for the dialog.
     * @param userMessage The initial user message to display.
     * @param variables A list of variables that can be inserted into the message.
     * @return A {@link Result} object containing the modal result and the edited user message.
     */
    public static Result editUserMessage(JFrame owner, String userMessage, List<String> variables) {
        EditUserMessageDialog dialog = new EditUserMessageDialog(owner);

        Actions.ActionGroup insertActionGroup = null;
        if (variables != null && ! variables.isEmpty()) {
            insertActionGroup = new Actions.ActionGroup("Insert Variable", null, true);
            for (String variable : variables) {
                insertActionGroup.addAction(new InsertVariableAction(dialog, variable));
            }
        }

        UISupport.setupPopupMenu(dialog.edtUserMessage, insertActionGroup);

        dialog.setUserMessage(userMessage);
        String result = dialog.executeModal(userMessage);
        return new Result(dialog.getModalResult(), result);
    }

    /**
     * An action to insert a variable into the user message editor.
     */
    static class InsertVariableAction extends Actions.BasicAction {
        /**
         * Constructs a new {@code InsertVariableAction}.
         * @param dialog The parent dialog.
         * @param variableName The name of the variable to insert.
         */
        public InsertVariableAction(EditUserMessageDialog dialog, String variableName) {
            super(variableName, null, e -> {
                dialog.edtUserMessage.replaceSelection("{{%s}}".formatted(variableName));
            });
        }
    }

    static class UserMessageEditor extends JTextPane {
        private static final Pattern TAG_PATTERN = Pattern.compile("\\{\\{.*?}}");

        public UserMessageEditor() {
            super(new DefaultStyledDocument());
            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    SwingUtilities.invokeLater(UserMessageEditor.this::highlight);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    SwingUtilities.invokeLater(UserMessageEditor.this::highlight);
                }

                @Override
                public void changedUpdate(DocumentEvent e) { }
            });
        }

        private void highlight() {
            DefaultStyledDocument doc = (DefaultStyledDocument) getDocument();
            StyleContext sc = StyleContext.getDefaultStyleContext();
            doc.setCharacterAttributes(0, doc.getLength(), sc.getEmptySet(), true);

            SimpleAttributeSet boldStyle = new SimpleAttributeSet();
            StyleConstants.setBold(boldStyle, true);
            try {
                String text = doc.getText(0, doc.getLength());
                Matcher matcher = TAG_PATTERN.matcher(text);
                while (matcher.find()) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), boldStyle, false);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
}
