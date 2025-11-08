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
 */

package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.gl.langchain4j.easyworkflow.gui.platform.Application;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A dialog that provides a chat interface. It can be used to display a chat conversation and interact with a chat
 * engine.
 */
public class ChatDialog extends JDialog implements Application.ScheduledUpdatable {

    private final ChatPane chatPane = new ChatPane();

    /**
     * Constructs a new ChatDialog with the specified owner frame, title, and chat engine.
     *
     * @param owner The {@link Frame} from which the dialog is displayed.
     * @param title The title of the dialog.
     * @param chatEngine The {@link ChatPane.ChatEngine} to be used by the chat pane.
     */
    public ChatDialog(Frame owner, String title, ChatPane.ChatEngine chatEngine) {
        super(owner, title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        init(chatEngine);
    }

    /**
     * Constructs a new ChatDialog with the specified owner dialog, title, and chat engine.
     *
     * @param owner The {@link Dialog} from which the dialog is displayed.
     * @param title The title of the dialog.
     * @param chatEngine The {@link ChatPane.ChatEngine} to be used by the chat pane.
     */
    public ChatDialog(Dialog owner, String title, ChatPane.ChatEngine chatEngine) {
        super(owner);
        init(chatEngine);
    }

    private void init(ChatPane.ChatEngine chatEngine) {
        chatPane.setChatEngine(chatEngine);
        setModal(true);
        setSize(500, 700);
        setLocationRelativeTo(getParent());
        setMinimumSize(new Dimension(500, 700));

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(chatPane, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPane.setOpaque(false);
        buttonPane.setBorder(new EmptyBorder(0, 0, 10, 0));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        buttonPane.add(okButton);

        contentPane.add(buttonPane, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    /**
     * Returns the {@link ChatPane} instance used within this dialog.
     *
     * @return The {@link ChatPane} instance.
     */
    public ChatPane getChatPane() {
        return chatPane;
    }

    @Override
    public void scheduledUpdate() {
        chatPane.scheduledUpdate();
    }
}
