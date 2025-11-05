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
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.KEY_SESSION_UID;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * A GUI-based playground for interacting with an agent.
 */
public class GUIPlayground extends Playground.BasicPlayground {
    private static final Logger logger = LoggerFactory.getLogger(GUIPlayground.class);
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
     * Retrieves the {@link WorkflowDebugger} instance from the argument map.
     *
     * @return The {@link WorkflowDebugger} instance if present, otherwise {@code null}.
     */
    public WorkflowDebugger getWorkflowDebugger() {
        if (arguments != null && arguments.containsKey(ARG_WORKFLOW_DEBUGGER))
            return (WorkflowDebugger) arguments.get(ARG_WORKFLOW_DEBUGGER);
        return null;
    }

    static {
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    /**
     * Initiates the chat interface for the playground.
     *
     * @param agent       The agent instance to interact with.
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

        if (!showDialog)
            showChatFrame(agent, userMessage, title);
        else
            showChatDialog(agent, userMessage, title);
    }

    private void showChatFrame(Object agent, Map<String, Object> userMessage, String title) {
        System.setProperty("apple.awt.application.name", title);

        WorkflowDebugger workflowDebugger;
        if (arguments != null && arguments.containsKey(ARG_WORKFLOW_DEBUGGER))
            workflowDebugger = (WorkflowDebugger) arguments.get(ARG_WORKFLOW_DEBUGGER);
        else {
            workflowDebugger = null;
        }

        chatFrame = ChatFrame.showChat(title, new ImageIcon(Objects.requireNonNull(GUIPlayground.class.getResource("logo.png"))),
                new ChatPane.ChatEngine() {
                    @Override
                    public Object send(Map<String, Object> message) {
                        if (chatFrame.getWorkflowDebugger() != null)
                            chatFrame.getWorkflowDebugger().setSessionUID((String) message.get(KEY_SESSION_UID));
                        return apply(agent, message);
                    }

                    @Override
                    public Parameter[] getMessageParameters() {
                        return agentMethod.getParameters();
                    }

                    @Override
                    public String getUserMessageTemplate() {
                        return EasyWorkflow.getUserMessageTemplate(agentClass);
                    }

                    @Override
                    public String getSystemMessageTemplate() {
                        return EasyWorkflow.getSystemMessageTemplate(agentClass);
                    }
                },
                workflowDebugger);
        SwingUtilities.invokeLater(() -> {
            if (chatFrame != null) {
                Application.getSharedApplication().launch(chatFrame);
                ChatPane chatPane = chatFrame.getChatPane();
                chatPane.setUserMessage(userMessage);
            }
        });
    }

    private void showChatDialog(Object agent, Map<String, Object> userMessage, String title) {

        chatDialog = new ChatDialog((Frame) arguments.get(ARG_OWNER_FRAME), title,
                new ChatPane.ChatEngine() {
                    @Override
                    public Object send(Map<String, Object> message) {
                        return apply(agent, message);
                    }

                    @Override
                    public String getUserMessageTemplate() {
                        return EasyWorkflow.getUserMessageTemplate(agentClass);
                    }

                    @Override
                    public String getSystemMessageTemplate() {
                        return EasyWorkflow.getSystemMessageTemplate(agentClass);
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
    public void setHumanRequest(HumanInTheLoop agent, String request) {
        humanRequest = request;
        WorkflowDebugger workflowDebugger = getWorkflowDebugger();
        if (workflowDebugger != null) {
            EasyWorkflow.AgentExpression agentMetadata = (EasyWorkflow.AgentExpression) workflowDebugger.getAgentMetadata(agent);
            String outputName = agentMetadata.getOutputName();
            if (outputName == null)
                outputName = "$humanRequest";
            workflowDebugger.inputReceived(agent, agentMetadata.getAgentClass(), UserMessage.userMessage(request));
        }
    }

    /**
     * Prompts the human for a response using a dialog box.
     *
     * @return The human's response, or "canceled" if the dialog is closed without input.
     */
    @Override
    public String getHumanResponse(HumanInTheLoop agent) {
        ChatPane chatPane = chatFrame != null ? chatFrame.getChatPane() : chatDialog.getChatPane();
        String result = JOptionPane.showInputDialog(chatPane, humanRequest);

        WorkflowDebugger workflowDebugger = getWorkflowDebugger();
        if (workflowDebugger != null) {
            EasyWorkflow.AgentExpression agentMetadata = (EasyWorkflow.AgentExpression) workflowDebugger.getAgentMetadata(agent);
            String outputName = agentMetadata.getOutputName();
            if (outputName == null)
                outputName = "$humanResponse";
            workflowDebugger.stateChanged(agent, agentMetadata.getAgentClass(), outputName, result);
        }

        return result != null ? result : "canceled";
    }

    public static class WorkflowExpertAction extends AbstractAction {
        private final WorkflowDebugger workflowDebugger;
        private final Class<?> agentClass;
        private final ChatFrame chatFrame;

        public WorkflowExpertAction(Icon icon, ChatFrame aChatFrame, Class<?> aAgentClass, WorkflowDebugger aWorkflowDebugger) {
            super("Workflow Expert...", icon);
            chatFrame = aChatFrame;
            workflowDebugger = aWorkflowDebugger;
            agentClass = aAgentClass;
            putValue(Action.SHORT_DESCRIPTION, "Workflow Expert");
        }

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
}
