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

package com.gl.langchain4j.easyworkflow.gui.platform;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A base class for creating modal dialogs in an application.
 * This dialog provides standard "OK" and "Cancel" buttons and
 * allows for custom content to be displayed. It also supports
 * returning a result of type T when closed with "OK".
 *
 * @param <T> The type of data that the dialog can return.
 */
public class AppDialog<T> extends JDialog {
    /**
     * Action command for the "OK" button.
     */
    public static final String ACTION_COMMAND_OK = "ok";
    /**
     * Action command for the "Cancel" button.
     */
    public static final String ACTION_COMMAND_CANCEL = "cancel";

    private Box pnlButtons;
    private String modalResult;
    private JComponent content;
    private JPanel contentPane;

    /**
     * Constructs a new AppDialog.
     *
     * @param owner The {@link JFrame} from which the dialog is displayed.
     * @param title The String to be displayed in the dialog's title bar.
     */
    public AppDialog(JFrame owner, String title) {
        super(owner, title);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        init();
    }

    /**
     * Sets the modal result of the dialog. This is typically set when the dialog is closed.
     *
     * @param aModalResult The string representing the modal result (e.g., {@link #ACTION_COMMAND_OK}, {@link #ACTION_COMMAND_CANCEL}).
     */
    public void setModalResult(String aModalResult) {
        modalResult = aModalResult;
    }

    /**
     * Returns the modal result of the dialog.
     *
     * @return The string representing the modal result.
     */
    public String getModalResult() {
        return modalResult;
    }

    /**
     * Returns the current content component displayed in the dialog.
     *
     * @return The {@link JComponent} currently set as the dialog's content.
     */
    public JComponent getContent() {
        return content;
    }

    /**
     * Sets the content component of the dialog. If there was a previous content component,
     * it is removed before the new one is added.
     *
     * @param aContent The {@link JComponent} to be set as the dialog's content.
     */
    public void setContent(JComponent aContent) {
        if (this.content != null)
            contentPane.remove(this.content);

        this.content = aContent;

        if (this.content != null)
            contentPane.add(this.content, BorderLayout.CENTER);
    }

    private void init() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close(ACTION_COMMAND_CANCEL);
            }
        });

        setLocationRelativeTo(getParent());

        contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        pnlButtons = new Box(BoxLayout.X_AXIS);
        pnlButtons.setBorder(new EmptyBorder(5, 0, 0, 0));
        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.setOpaque(false);

        JButton okButton = new JButton("OK");
        okButton.setActionCommand(ACTION_COMMAND_OK);
        okButton.setDefaultCapable(true);
        okButton.addActionListener(this::actionPerformed);
        getRootPane().setDefaultButton(okButton);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setActionCommand(ACTION_COMMAND_CANCEL);
        btnCancel.addActionListener(this::actionPerformed);

        if (UISupport.isMac()) {
            addButton(btnCancel, false);
            addButton(okButton, false);
        } else {
            addButton(okButton, false);
            addButton(btnCancel, false);
        }

        contentPane.add(pnlButtons, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    /**
     * Closes the dialog with the specified modal result.
     * If the modal result is {@link #ACTION_COMMAND_OK}, {@link #canClose()} is checked before closing.
     * @param modalResult The string representing the modal result (e.g., {@link #ACTION_COMMAND_OK}, {@link #ACTION_COMMAND_CANCEL}).
     */
    public void close(String modalResult) {
        boolean shouldClose = true;

        if (modalResult.equals(ACTION_COMMAND_OK))
            shouldClose = canClose();

        if (shouldClose) {
            setModalResult(modalResult);
            setVisible(false);
        }
    }

    /**
     * Adds a button to the dialog's button panel.
     *
     * @param button The {@link JButton} to add.
     * @param isLeft If true, the button is added to the left side; otherwise, it's added to the right.
     */
    public void addButton(JButton button, boolean isLeft) {
        pnlButtons.add(button, isLeft ? 0 : pnlButtons.getComponentCount());
    }

    /**
     * Populates the dialog's form components with data from the provided object.
     * Subclasses should override this method to implement data binding from the model to the view.
     *
     * @param data The data object to populate the form with.
     */
    protected void toForm(T data) {
    }

    /**
     * Extracts data from the dialog's form components into a new object of type T.
     * Subclasses should override this method to implement data binding from the view to the model.
     *
     * @return An object of type T populated with data from the form.
     */
    protected T fromForm() {
        return null;
    }

    /**
     * Determines if the dialog can be closed when the "OK" button is pressed.
     * Subclasses can override this method to perform validation before closing.
     *
     * @return true if the dialog can be closed, false otherwise.
     */
    public boolean canClose() {
        return true;
    }

    /**
     * Displays the dialog modally and returns a result of type T if the dialog is closed with "OK".
     *
     * @param data The initial data to populate the dialog's form with.
     * @return An object of type T if the dialog was closed with "OK", otherwise null.
     */
    public T executeModal(T data) {
        T result = null;
        setModal(true);
        toForm(data);
        pack();
        setVisible(true);
        if (getModalResult().equals(ACTION_COMMAND_OK))
            result = fromForm();
        dispose();
        return result;
    }

    private void actionPerformed(ActionEvent e) {
        close(e.getActionCommand());
    }

    /**
     * Displays an information message dialog.
     *
     * @param title The title of the message dialog. If null, the dialog's own title is used.
     * @param message The message to display.
     */
    public void showMessage(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title != null ? title : getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }
}
