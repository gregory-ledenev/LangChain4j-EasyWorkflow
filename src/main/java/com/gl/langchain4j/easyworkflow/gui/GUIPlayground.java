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
import com.gl.langchain4j.easyworkflow.gui.platform.Application;
import com.gl.langchain4j.easyworkflow.gui.platform.NotificationCenter;
import com.gl.langchain4j.easyworkflow.playground.LocalPlaygroundContext;
import com.gl.langchain4j.easyworkflow.playground.Playground;
import com.gl.langchain4j.easyworkflow.playground.PlaygroundMetadata;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.KEY_SESSION_UID;

/**
 * A GUI-based playground for interacting with an agent.
 */
public class GUIPlayground extends Playground.BasicPlayground {
    private static final Logger logger = EasyWorkflow.getLogger(GUIPlayground.class);

    static {
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

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

    /**
     * Creates and returns a {@link EasyWorkflow.LoggerAspect} that intercepts log messages.
     * Specifically, it captures error messages and displays them as notifications.
     *
     * @return A new {@link EasyWorkflow.LoggerAspect} instance.
     */
    public static EasyWorkflow.LoggerAspect createLoggerAspect() {
        return (logger, method, args) -> {
            if (method.getName().equals("error")) {
                String text = args[0] != null ? args[0].toString() : "";

                if (args.length > 1 && args[1] instanceof Throwable ex) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    ex.printStackTrace(pw);
                    text += "\n" + sw;
                }

                String finalText = text;
                SwingUtilities.invokeLater(() -> NotificationCenter.getInstance().postNotification(
                        new NotificationCenter.Notification(NotificationCenter.NotificationType.ERROR, "Error", finalText, null)));
            }
            return method.invoke(logger, args);
        };
    }

    @Override
    public void setup(Map<String, Object> arguments) {
        super.setup(arguments);
        if (this.arguments != null) {
            this.arguments = new HashMap<>(this.arguments);
            this.arguments.putAll(arguments);
        } else {
            this.arguments = arguments;
        }
    }

    /**
     * Retrieves the {@link WorkflowDebugger} instance from the argument map.
     *
     * @return The {@link WorkflowDebugger} instance if present, otherwise {@code null}.
     */
    public WorkflowDebugger getWorkflowDebugger() {
        return (arguments != null && arguments.get(ARG_WORKFLOW_DEBUGGER) instanceof WorkflowDebugger workflowDebugger) ?
                workflowDebugger : null;
    }

    /**
     * Initiates the chat interface for the playground.
     *
     * @param agent       The agent instance to interact with.
     * @param userMessage The initial message from the user.
     */
    @Override
    public void play(Object agent, Map<String, Object> userMessage) {
        String title = null;

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
        System.setProperty("apple.awt.application.name", title != null ? title : "Playground");

        if (title == null)
            title = "Playground - %s".formatted(agentClass.getSimpleName());

        Icons.loadIcons();
        ToolbarIcons.loadIcons();

        WorkflowDebugger debugger = getWorkflowDebugger();
        LocalPlaygroundContext playgroundContext = new LocalPlaygroundContext(agent,
                debugger != null ? debugger.getAgentWorkflowBuilder() : null,
                debugger != null ? new PlaygroundChatModel(debugger.getAgentWorkflowBuilder().getChatModel()) : null,
                getChatModels());

        chatFrame = ChatFrame.createChatFrame(title,
                new ImageIcon(Objects.requireNonNull(GUIPlayground.class.getResource("icons/logo.png"))),
                new ChatPane.ChatEngine() {
                    @Override
                    public Object send(Map<String, Object> message) {
                        if (chatFrame.getWorkflowDebugger() != null)
                            chatFrame.getWorkflowDebugger().setSessionUID((String) message.get(KEY_SESSION_UID));
                        return playgroundContext.sendMessage(message);
                    }

                    @Override
                    public String getChatModel() {
                        return playgroundContext.getChatModel().name();
                    }

                    @Override
                    public PlaygroundMetadata.Argument[] getMessageParameters() {
                        return playgroundContext.getAgentMetadata().getArguments().toArray(PlaygroundMetadata.Argument[]::new);
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
                playgroundContext,
                agent,
                debugger);
        SwingUtilities.invokeLater(() -> {
            if (chatFrame != null) {
                EasyWorkflow.setLoggerAspect(createLoggerAspect());
                Application.getSharedApplication().launch(chatFrame);
                ChatPane chatPane = chatFrame.getChatPane();
                chatPane.setUserMessage(userMessage);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<PlaygroundChatModel> getChatModels() {
        if (!(arguments != null && arguments.get(ARG_CHAT_MODELS) instanceof List<?> chatModels))
            return null;

        return chatModels.stream().map(o -> {
            if (o instanceof PlaygroundChatModel playgroundChatModel)
                return playgroundChatModel;
            else if (o instanceof ChatModel chatModel)
                return new PlaygroundChatModel(chatModel.defaultRequestParameters().modelName(), chatModel);
            else
                throw new IllegalArgumentException("Object is not a chat model: " + o);
        }).toList();
    }

    private void showChatDialog(Object agent, Map<String, Object> userMessage, String title) {
        System.setProperty("apple.awt.application.name", title != null ? title : "Playground");

        if (title == null)
            title = "Playground - %s".formatted(agentClass.getSimpleName());

        Icons.loadIcons();
        ToolbarIcons.loadIcons();

        WorkflowDebugger debugger = getWorkflowDebugger();
        LocalPlaygroundContext playgroundContext = new LocalPlaygroundContext(agent,
                debugger != null ? debugger.getAgentWorkflowBuilder() : null,
                debugger != null ? new PlaygroundChatModel(debugger.getAgentWorkflowBuilder().getChatModel()) : null,
                getChatModels());

        chatDialog = new ChatDialog((Frame) arguments.get(ARG_OWNER_FRAME), title,
                new ChatPane.ChatEngine() {
                    @Override
                    public Object send(Map<String, Object> message) {
                        return playgroundContext.sendMessage(message);
                    }

                    @Override
                    public String getChatModel() {
                        PlaygroundMetadata.Model chatModel = playgroundContext.getChatModel();
                        return chatModel!= null ? chatModel.name() : null;
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
                    public PlaygroundMetadata.Argument[] getMessageParameters() {
                        return playgroundContext.getAgentMetadata().getArguments().toArray(PlaygroundMetadata.Argument[]::new);
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

            WorkflowDebugger.AgentInvocationTraceEntryArchive archive = chatFrame.getAgentInvocationTraceEntryArchive();
            String executionLog = archive == null ?
                    workflowDebugger.toString(true) :
                    workflowDebugger.toString(archive.workflowInput(),
                            archive.agentInvocationTraceEntries(),
                            archive.workflowResult(),
                            archive.workflowFailure());

            CompletableFuture.supplyAsync(() -> WorkflowExpertSupport.getWorkflowExpert(workflowDebugger, executionLog)).
                    whenComplete((workflowExpert, ex) -> {
                        try {
                            if (ex == null) {
                                Playground playground = Playground.createPlayground(WorkflowExpert.class, Type.GUI);
                                playground.setup(Map.of(ARG_TITLE, "Workflow Expert - %s".formatted(agentClass.getSimpleName()),
                                        ARG_SHOW_DIALOG, true,
                                        ARG_OWNER_FRAME, chatFrame));
                                playground.play(workflowExpert, null);
                            } else {
                                logger.error("Failed to get workflow expert", ex);
                            }
                        } catch (Exception ex1) {
                            logger.error("Failed to show workflow expert", ex1);
                        } finally {
                            setEnabled(true);
                            chatFrame.getChatPane().setWaitState(false);
                        }
                    });
        }
    }
}
