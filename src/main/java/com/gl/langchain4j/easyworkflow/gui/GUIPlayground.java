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

import com.gl.langchain4j.easyworkflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * A GUI-based playground for interacting with an agent.
 */
public class GUIPlayground extends Playground.BasicPlayground {
    private ChatFrame chatFrame;
    private ChatDialog chatDialog;
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

        boolean showDialog = false;

        if (arguments != null) {
            if (arguments.containsKey(ARG_TITLE)) {
                String argTitle = (String) arguments.get(ARG_TITLE);
                if (argTitle != null && !argTitle.isEmpty())
                    title = argTitle;
            }

            showDialog = Boolean.valueOf(true).equals(arguments.get(ARG_SHOW_DIALOG));
        }

        System.setProperty("apple.awt.application.appearance", "system");

        if (! showDialog)
            showChatFrame(agent, userMessage, title);
        else
            showChatDialog(agent, userMessage, title);
    }

    private void showChatFrame(Object agent, Map<String, Object> userMessage, String title) {
        System.setProperty("apple.awt.application.name", title);

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
                if (arguments != null && arguments.containsKey(ARG_WORKFLOW_DEBUGGER)) {
                    WorkflowDebugger workflowDebugger = (WorkflowDebugger) arguments.get(ARG_WORKFLOW_DEBUGGER);
                    if (workflowDebugger != null) {
                        Action action = new WorkflowExpertAction(new UISupport.AutoIcon(UISupport.ICON_EXPERT),
                                chatFrame,
                                agentClass, workflowDebugger
                        );
                        chatFrame.getChatPane().setupToolActions(new Action[] {action});
                    }
                }
            }
        });
    }

    private static class WorkflowExpertAction extends AbstractAction {
        private final WorkflowDebugger workflowDebugger;
        private final Class<?> agentClass;
        private final ChatFrame chatFrame;

        public WorkflowExpertAction(Icon icon, ChatFrame aChatFrame, Class<?> aAgentClass, WorkflowDebugger aWorkflowDebugger) {
            super(null, icon);
            chatFrame = aChatFrame;
            workflowDebugger = aWorkflowDebugger;
            agentClass = aAgentClass;
            putValue(Action.SHORT_DESCRIPTION, "Workflow Expert");        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            chatFrame.getChatPane().setWaitState(true, "Preparing Workflow Expert...");
            CompletableFuture.supplyAsync(() -> WorkflowExpertSupport.getWorkflowExpert(workflowDebugger)).
                    whenComplete((workflowExpert, ex) -> {
                        if (ex == null) {
                            Playground playground = Playground.createPlayground(WorkflowExpert.class, Type.GUI);
                            playground.setup(Map.of(ARG_TITLE, "Workflow Expert - %s".formatted(agentClass.getSimpleName()),
                                    ARG_SHOW_DIALOG, true,
                                    ARG_OWNER_FRAME, chatFrame));
                            playground.play(workflowExpert, null);
                        } else {
                            logger.error("Failed to get workflow expert", ex);
                        }
                        setEnabled(true);
                        chatFrame.getChatPane().setWaitState(false);
                    });
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(GUIPlayground.class);

    private void showChatDialog(Object agent, Map<String, Object> userMessage, String title) {

        chatDialog = new ChatDialog((Frame) arguments.get(ARG_OWNER_FRAME), title,
                new ChatPane.ChatEngine() {
                    @Override
                    public Object send(Map<String, Object> message) {
                        return apply(agent, message);
                    }

                    @Override
                    public Parameter[] getMessageParameters() {
                        return agentMethod.getParameters();
                    }
                });
        SwingUtilities.invokeLater(() -> {
            if (chatDialog != null) {
                ChatPane chatPane = chatDialog.getChatPane();
                chatPane.setUserMessage(userMessage);
                chatDialog.setVisible(true);
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
        ChatPane chatPane = chatFrame != null ? chatFrame.getChatPane() : chatDialog.getChatPane();
        String result = JOptionPane.showInputDialog(chatPane, humanRequest);
        return result != null ? result : "canceled";
    }
}
