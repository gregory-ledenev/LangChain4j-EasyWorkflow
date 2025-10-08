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

package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.Playground;

import javax.swing.*;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;

/**
 * A GUI-based playground for interacting with an agent.
 */
public class GUIPlayground extends Playground.BasicPlayground {
    private ChatFrame chatFrame;
    private String humanRequest;
    private Map<String, Object> arguments;

    /**
     * Constructs a new GUIPlayground with the specified agent class.
     *
     * @param agentClass The class of the agent to be used in the playground.
     */
    public GUIPlayground(Class<?> agentClass) {
        super(agentClass);
    }

    @Override
    public void setup(Map<String, Object> arguments) {
        super.setup(arguments);
        this.arguments = arguments;
    }

    /**
     * Initiates the chat interface for the playground.
     *
     * @param agent The agent instance to interact with.
     * @param userMessage The initial message from the user.
     */
    @Override
    public void play(Object agent, Map<String, Object> userMessage) {
        String title = "Playground - %s".formatted(agentClass.getSimpleName());

        if (arguments != null && arguments.containsKey(ARG_TITLE)) {
            String argTitle = (String) arguments.get(ARG_TITLE);
            if (argTitle != null && ! argTitle.isEmpty())
                title = argTitle;
        }

        System.setProperty("apple.awt.application.name", title);
        System.setProperty("apple.awt.application.appearance", "system");

        chatFrame = ChatFrame.showChat(title, new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource("logo.png"))),
                new ChatPane.ChatEngine() {
                    @Override
                    public Object send(Map<String, Object> message) {
                        return apply(agent, message);
                    }

                    @Override
                    public Parameter[] getMessageParameters() {
                        return agentMethod.getParameters();
                    }
                },
                true);
        SwingUtilities.invokeLater(() -> {
            if (chatFrame != null) {
                ChatPane chatPane = chatFrame.getChatPane();
                chatPane.setUserMessage(userMessage);
            }
        });
    }

    /**
     * Sets the human request message.
     *
     * @param request The request message to be displayed to the human.
     */
    @Override
    public void setHumanRequest(String request) {
        humanRequest = request;
    }

    /**
     * Prompts the human for a response using a dialog box.
     *
     * @return The human's response, or "canceled" if the dialog is closed without input.
     */
    @Override
    public String getHumanResponse() {
        String result = JOptionPane.showInputDialog(chatFrame.getChatPane(), humanRequest);
        return result != null ? result : "canceled";
    }
}
