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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gl.saf.*;
import com.gl.saf.IconFactory.AutoIcon;
import com.gl.saf.actions.*;
import com.gl.saf.comp.*;
import com.gl.langchain4j.easyworkflow.*;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatMessage;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorDetailsPane;
import com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorListPane;
import com.gl.langchain4j.easyworkflow.playground.PlaygroundContext;
import com.gl.langchain4j.easyworkflow.playground.PlaygroundMetadata;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.gl.saf.Application.getUserPreferences;
import static com.gl.saf.ApplicationPreferences.getApplicationPreferences;
import static com.gl.saf.Icons.*;
import static com.gl.saf.UISupport.*;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.AgentInvocationTraceEntryArchive;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.Breakpoint;
import static com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorDetailsPane.PROP_SELECTED_VARIABLE;
import static com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorListPane.*;

/**
 * A frame that provides a chat interface. It can be used to display a chat conversation and interact with a chat
 * engine.
 */
@SuppressWarnings("ALL")
public class ChatScreen extends BasicAppScreen<AppFrame> implements ChatPane.ExecutionDetailsProvider {

    public static final String PROP_FLOW_CHART_FILE = "flow-chart-file";
    public static final String PROP_STRUCTURE_FILE = "structure-file";
    public static final String PROP_EXECUTION_FILE = "execution-file";
    public static final String PROP_SUMMARY_FILE = "summary-file";
    public static final String PROP_USER_MESSAGES_FILE = "user-messages-file";
    public static final String PROP_CHAT_FILE = "chat-file";

    private static final Logger logger = LoggerFactory.getLogger(ChatScreen.class);
    public static final String ID = "chatScreen";
    private final ChatPane pnlChat = new ChatPane();
    private final PlaygroundContext playgroundContext;
    private final UserMessagesStorage userMessagesStorage;
    private ChatPromptsStorage chatPromptsStorage;
    private JScrollPane pnlWorkflowSummary;
    private JEditorPane pnlWorkflowSummaryView;
    private JPanel pnlWorkflowContents;
    private WorkflowDebugger workflowDebugger;
    private WorkflowInspectorDetailsPane pnlWorkflowInspectorDetails;
    private WorkflowInspectorListPane pnlWorkflowInspectorStructure;
    private WorkflowInspectorListPane pnlWorkflowInspectorExecution;
    private FileChooserUtils fileChooserUtils;
    private boolean summaryGenerated = false;
    private boolean summaryGenerating = false;
    private StateAction showExecutionAction;
    private StateAction showStructureAction;
    private StateAction showSummaryAction;
    private BasicAction shareAction;
    private GUIPlayground.WorkflowExpertAction workflowExpertAction;
    private ActionGroup menuBarViewActionGroup;
    private ActionGroup menuBarOptionsActionGroup;
    private ActionGroup menuBarActionGroup;
    private ActionGroup menuBarFileActionGroup;
    private ActionGroup menuBarHelpActionGroup;
    private ActionGroup menuBarEditActionGroup;
    private ActionGroup inspectorToolbarActionGroup;
    private AgentInvocationTraceEntryArchive agentInvocationTraceEntryArchive;
    private final Breakpoint sessionStartedBreakpoint = Breakpoint.builder(
                    Breakpoint.Type.SESSION_STARTED,
                    (b, m) -> agentInvocationTraceEntryArchive = null)
            .build();
    private Object agent;
    private ComponentAction chatModelsAction;
    private BasicAction editUserMessageAction;
    private BasicAction chatHistoryAction;
    private BasicAction newChatAction;
    private ActionGroup chatToolbarActionGroup;
    private ChatHistoryStorage chatHistoryStorage;
    private String chatHistoryItemUid;

    /**
     * Constructs a new ChatScreen.
     *
     * @param chatEngine       A function that takes a user message and returns a chat engine's response.
     * @param agent            The agent object associated with this chat frame.
     * @param workflowDebugger The workflow debugger to be used for inspecting workflows.
     */
    public ChatScreen(ChatPane.ChatEngine chatEngine,
                      PlaygroundContext playgroundContext,
                      Object agent,
                      WorkflowDebugger workflowDebugger) {
        super(ID, "Playground", new AutoIcon(PlaygroundIcons.ICON_PLAYGOUND), "Playground for agents");

        setLongDescription("Playground that allows to test agents, observe their structure, check the execution flow, inspect their results, and fine tune agents");

        this.playgroundContext = playgroundContext;
        this.agent = agent;

        setWorkflowDebugger(workflowDebugger);
        this.pnlChat.setChatEngine(chatEngine);

        pnlChat.setPreferredSize(new Dimension(420, 700));

        if (workflowDebugger != null) {
            userMessagesStorage = new UserMessagesStorage(getPlaygroundContext(),
                    agentClassName -> pnlWorkflowInspectorStructure.getUserMessage(agentClassName));
            chatHistoryStorage = new ChatHistoryStorage(getPlaygroundContext().getAgentMetadata().getType().name());
            chatPromptsStorage = new ChatPromptsStorage(getPlaygroundContext().getAgentMetadata().getType().name());

            setMinimumSize(new Dimension(1280, 720));

            JSplitPane contentPane = new AppSplitPane();
            contentPane.setResizeWeight(0);
            contentPane.setLeftComponent(pnlChat);
            pnlChat.getHeaderPane().setVisible(true);

            pnlWorkflowInspectorStructure = new Structure();
            pnlWorkflowInspectorStructure.setPlaygroundContext(playgroundContext);
            pnlWorkflowInspectorStructure.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorStructure.setWorkflowDebugger(workflowDebugger);

            pnlWorkflowInspectorExecution = new Execution();
            pnlWorkflowInspectorExecution.setPlaygroundContext(playgroundContext);
            pnlWorkflowInspectorExecution.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorExecution.setWorkflowDebugger(workflowDebugger);
            pnlWorkflowInspectorExecution.setPlaceHolderText("Run workflow to see execution results");
            pnlWorkflowInspectorExecution.setPlaceHolderIcon(new AutoIcon(ICON_INFO));
            pnlWorkflowInspectorExecution.setPlaceHolderVisible(true);

            pnlWorkflowSummaryView = new PreviewTextPane();
            pnlWorkflowSummary = createScrollPane(pnlWorkflowSummaryView, false, false, false, false, false);
            pnlWorkflowSummary.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            JPanel pnlWorkflowContentsHost = new JPanel(new BorderLayout());
            HeaderPane headerPane = new HeaderPane();
            headerPane.setTitle("Workflow");
            headerPane.setSubtitle("Workflow structure, its agents, execution steps, and summary");
            pnlWorkflowContentsHost.add(headerPane, BorderLayout.NORTH);

            setupActions();
            setupMenuBar();
            setupChatToolbar(pnlChat.getHeaderPane().getToolbar());
            setupToolbar(headerPane.getToolbar());
            setupPopupMenu();

            pnlWorkflowContents = new JPanel(new CardLayout());
            pnlWorkflowContents.add(pnlWorkflowInspectorStructure, "structure");
            pnlWorkflowContents.add(pnlWorkflowInspectorExecution, "execution");
            pnlWorkflowContents.add(pnlWorkflowSummary, "summary");

            pnlWorkflowContentsHost.add(pnlWorkflowContents, BorderLayout.CENTER);

            JSplitPane pnlWorkflow = new AppSplitPane();
            pnlWorkflow.setLeftComponent(pnlWorkflowContentsHost);
            pnlWorkflowInspectorDetails = new WorkflowInspectorDetailsPane();

            pnlWorkflow.setRightComponent(pnlWorkflowInspectorDetails);
            pnlWorkflow.setResizeWeight(0.5);
            contentPane.setRightComponent(pnlWorkflow);
            add(contentPane, BorderLayout.CENTER);

            pnlWorkflowInspectorStructure.getListView().addListSelectionListener(e -> {
                if (pnlWorkflowInspectorStructure.isVisible())
                    pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorStructure.getSelectedData());
            });
            pnlWorkflowInspectorExecution.getListView().addListSelectionListener(e -> {
                if (pnlWorkflowInspectorExecution.isVisible())
                    pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorExecution.getSelectedData());
            });

            pnlWorkflowInspectorDetails.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equals(PROP_SELECTED_VARIABLE)) {
                    pnlWorkflowInspectorStructure.highlightUsage((String) evt.getNewValue());
                    pnlWorkflowInspectorExecution.highlightUsage((String) evt.getNewValue());
                }
            });
            pnlChat.setExecutionDetailsProvider(this);
        } else {
            userMessagesStorage = null;
            add(pnlChat, BorderLayout.CENTER);
        }
    }

    private static void openFile(File file) {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    return null;
                }
        ).exceptionally(ex -> {
            logger.error("Failed to open file", ex);
            return null;
        });
    }

    public ChatPromptsStorage getChatPromptsStorage() {
        return chatPromptsStorage;
    }

    public PlaygroundContext getPlaygroundContext() {
        return playgroundContext;
    }

    /**
     * Returns the {@link AgentInvocationTraceEntryArchive} associated with the currently displayed execution details.
     * This archive contains a snapshot of the workflow's state and execution trace for a specific invocation.
     *
     * @return The {@link AgentInvocationTraceEntryArchive} for the current execution, or {@code null} if none is set.
     */
    public AgentInvocationTraceEntryArchive getAgentInvocationTraceEntryArchive() {
        return agentInvocationTraceEntryArchive;
    }

    private void setupMenuBar() {
        ActionMenuBar menuBar = new ActionMenuBar();

        setupMenuBarFileActionGroup();
        setupMenuBarEditActionGroup();
        setupMenuBarViewActionGroup();
        setupMenuBarOptionsActionGroup();
    }

    private void setupMenuBarOptionsActionGroup() {
        String exclusiveGroup = "appearance";

        ActionGroup modelsActionGroup = null;
        if (playgroundContext.getChatModels() != null && !playgroundContext.getChatModels().isEmpty()) {
            String models = "models";
            modelsActionGroup = new ActionGroup("Models", new AutoIcon(ICON_SPACER), true);
            for (PlaygroundMetadata.Model chatModel : playgroundContext.getChatModels()) {
                modelsActionGroup.addAction(new StateAction(chatModel.name(), null, models,
                        e -> playgroundContext.setChatModel(chatModel),
                        a -> a.setSelected(chatModel.id().equals(playgroundContext.getChatModel().id()))));
            }
        }

        menuBarOptionsActionGroup = new ActionGroup(
                new ActionGroup(
                        pnlChat.getRenderMarkdownAction(),
                        pnlChat.getClearAfterSendingAction()
                ),
                new ActionGroup(
                        new ActionGroup(modelsActionGroup)
                )
        );
    }

    private void setupMenuBarViewActionGroup() {
        ActionGroup toolActionGroup = workflowExpertAction != null ? new ActionGroup(workflowExpertAction) : null;

        BasicAction chatAction = new BasicAction("Chat", new AutoIcon(ICON_CHAT), e -> pnlChat.requestFocus());
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        chatAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, menuShortcutKeyMask));
        BasicAction inspectorAction = new BasicAction("Inspector", new AutoIcon(ICON_INFO), e -> pnlWorkflowInspectorDetails.requestFocus());
        inspectorAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, menuShortcutKeyMask));

        menuBarViewActionGroup = new ActionGroup(
                new ActionGroup(null, null, false,
                        chatAction),
                new ActionGroup(null, null, false,
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction
                ),
                new ActionGroup(null, null, false,
                        inspectorAction),
                toolActionGroup
        );
    }

    private void setupMenuBarEditActionGroup() {
        menuBarEditActionGroup = new ActionGroup(
                editUserMessageAction
        );
    }

    private void setupMenuBarFileActionGroup() {
        ActionGroup shareActionGroup = new ActionGroup("Share", new AutoIcon(ICON_SHARE), true,
                new ActionGroup(null, null, false,
                        new BasicAction("Chat...", null,
                                e -> shareChat(),
                                a -> a.setEnabled(!pnlChat.getChatMessages().isEmpty()))
                ),
                new ActionGroup(null, null, false,
                        new BasicAction("Structure...", null, e -> shareStructure()),
                        new BasicAction("Execution...", null,
                                e -> shareExecution(),
                                a -> a.setEnabled(pnlWorkflowInspectorExecution.hasContent())),
                        new BasicAction("Summary...", null,
                                e -> shareSummary(),
                                a -> a.setEnabled(summaryGenerated))
                ),
                new ActionGroup(null, null, false,
                        new BasicAction("User Messages...", null,
                                e -> shareUserMessages(),
                                a -> a.setEnabled(canShareUserMessages()))
                ),
                new ActionGroup(null, null, false,
                        new BasicAction("Flow Chart...", null, e -> shareFlowChart())
                ),
                new ActionGroup(null, null, false,
                        new StateAction("Open File After Sharing", null, null,
                                e -> getApplicationPreferences().setOpenFileAfterSharing(!getApplicationPreferences().isOpenFileAfterSharing()),
                                a -> a.setSelected(getApplicationPreferences().isOpenFileAfterSharing()))
                )
        );

        menuBarFileActionGroup = new ActionGroup(
                new ActionGroup(
                        newChatAction,
                        chatHistoryAction
                ),
                new ActionGroup(
                        shareActionGroup
                )
        );
    }

    @Override
    public void activate() {
        super.activate();

        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_FILE).addAction(0, menuBarFileActionGroup);
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_EDIT).addAction(menuBarEditActionGroup);
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).addAction(menuBarViewActionGroup);
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_OPTIONS).addAction(0, menuBarOptionsActionGroup);
    }

    @Override
    public void passivate() {
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_FILE).removeAction(menuBarFileActionGroup);
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_EDIT).removeAction(menuBarEditActionGroup);
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).removeAction(menuBarViewActionGroup);
        getAppFrame().getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_OPTIONS).removeAction(menuBarOptionsActionGroup);

        super.passivate();
    }

    @Override
    public void addNotify() {
        super.addNotify();

        CompletableFuture.runAsync(() -> {
            if (userMessagesStorage != null)
                userMessagesStorage.load();
        }).thenRun(() -> SwingUtilities.invokeLater(() -> repaint()));
        CompletableFuture.runAsync(() -> chatHistoryStorage.load());
        CompletableFuture.runAsync(() -> chatPromptsStorage.load());
    }

    private boolean canShareUserMessages() {
        return workflowDebugger.hasUserMessageTemplates();
    }

    private void shareUserMessages() {
        String userMessagesAsJson = userMessagesStorage.asJson();
        if (userMessagesAsJson != null)
            shareContent("User Messages", userMessagesAsJson,
                    PROP_USER_MESSAGES_FILE,
                    "user-messages.json");
        else
            logger.warn("No user messages to share");
    }

    private void shareChat() {
        if (pnlChat.getChatMessages().isEmpty())
            return;

        try {
            shareContent("Chat", ChatPane.OBJECT_MAPPER.
                            enable(SerializationFeature.INDENT_OUTPUT).
                            writeValueAsString(pnlChat.getChatMessages()),
                    PROP_CHAT_FILE,
                    "chat.json");
        } catch (JsonProcessingException ex) {
            logger.error("Failed to share chat", ex);
        }
    }

    private void shareSummary() {
        shareContent("Summary", pnlWorkflowSummaryView.getText(),
                PROP_SUMMARY_FILE,
                "workflow-summary.html");
    }

    private void shareExecution() {
        String content = agentInvocationTraceEntryArchive == null ?
                workflowDebugger.toString(true) :
                workflowDebugger.toString(agentInvocationTraceEntryArchive.workflowInput(),
                        agentInvocationTraceEntryArchive.agentInvocationTraceEntries(),
                        agentInvocationTraceEntryArchive.workflowResult(),
                        agentInvocationTraceEntryArchive.workflowFailure());
        shareContent("Execution", content,
                PROP_EXECUTION_FILE,
                "workflow-execution.txt");
    }

    private void shareStructure() {
        try {
            shareContent("Structure", getPlaygroundContext().getAgentMetadata().toJson(),
                    PROP_STRUCTURE_FILE,
                    "workflow-structure.json");
        } catch (JsonProcessingException e) {
            logger.error("Failed to share structure", e);
        }
    }

    private void shareFlowChart() {
        String content = agentInvocationTraceEntryArchive == null ?
                workflowDebugger.toHtml(true) :
                workflowDebugger.toHtml(true,
                        agentInvocationTraceEntryArchive.workflowInput(),
                        agentInvocationTraceEntryArchive.agentInvocationTraceEntries(),
                        agentInvocationTraceEntryArchive.workflowResult(),
                        agentInvocationTraceEntryArchive.workflowFailure());
        shareContent("Flow Chart", content,
                PROP_FLOW_CHART_FILE,
                "workflow.html");
    }

    private void shareContent(String contentType, String content, String fileNameProperty, String defaultFileName) {
        FileChooserUtils fileChooserUtils = getFileChooserUtils();

        String fileStr = getUserPreferences().get(fileNameProperty, defaultFileName);
        File file = fileChooserUtils.chooseFileToSave(new File(fileStr), true);
        if (file != null) {
            try {
                getUserPreferences().put(fileNameProperty, file.getAbsolutePath());
                Files.write(Paths.get(file.getAbsolutePath()), content.getBytes());

                if (getApplicationPreferences().isOpenFileAfterSharing() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    openFile(file);
                } else {
                    NotificationCenter.getInstance().postNotification(new NotificationCenter.Notification(
                            NotificationCenter.NotificationType.SUCCESS,
                            "Sharing Finished",
                            "%s saved to %s".formatted(contentType, file.getPath()),
                            e -> openFile(file)));
                }
            } catch (Exception ex) {
                logger.error("Failed to share", ex);
            }
        }
    }

    private void setupActions() {
        final String showGroup = "show";
        showStructureAction = new StateAction("Structure", new AutoIcon(PlaygroundIcons.ICON_WORKFLOW), showGroup,
                e -> showWorkflowStructure(),
                a -> a.setSelected(pnlWorkflowInspectorStructure.isVisible()));
        showStructureAction.setShortDescription("Show workflow structure");
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        showStructureAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, menuShortcutKeyMask));

        showExecutionAction = new StateAction("Execution", new AutoIcon(PlaygroundIcons.ICON_EXECUTION_FLOW), showGroup,
                e -> showWorkflowExecution(),
                a -> a.setSelected(pnlWorkflowInspectorExecution.isVisible()));
        showExecutionAction.setShortDescription("Show workflow execution");
        showExecutionAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, menuShortcutKeyMask));

        showSummaryAction = new StateAction("Summary", new AutoIcon(ICON_DOCUMENT), showGroup,
                e -> showWorkflowSummary(false),
                a -> a.setSelected(pnlWorkflowSummary.isVisible()));
        showSummaryAction.setShortDescription("Show workflow summary");
        showSummaryAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, menuShortcutKeyMask));

        setupModelsAction();

        editUserMessageAction = new BasicAction("Edit User Message...", new AutoIcon(ICON_COMPOSE, IconFactory.IconSize.Auto, true, false),
                e -> editUserMessage(),
                a -> a.setEnabled(canEditUserMessage()));
        editUserMessageAction.putValue(BasicAction.MENU_ITEM_NAME_KEY, "User Message...");
        editUserMessageAction.setShortDescription("Edit user message");
        bindAction(pnlWorkflowInspectorStructure.getListView(),
                "editUserMessage",
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                editUserMessageAction);
        bindAction(pnlWorkflowInspectorExecution.getListView(),
                "editUserMessage",
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                editUserMessageAction);
        bindDoubleClickAction(pnlWorkflowInspectorStructure.getListView(), editUserMessageAction);
        bindDoubleClickAction(pnlWorkflowInspectorExecution.getListView(), editUserMessageAction);

        shareAction = new BasicAction("Share", new AutoIcon(ICON_SHARE), e -> shareFlowChart());
        shareAction.setShortDescription("Share");

        if (workflowDebugger != null) {
            //todo: fix me
//            workflowExpertAction = new GUIPlayground.WorkflowExpertAction(new AutoIcon(ICON_EXPERT_TOOLBAR),
//                    this,
//                    workflowDebugger.getAgentWorkflowBuilder().getAgentClass(), workflowDebugger
//            );
//            workflowExpertAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E,
//                    menuShortcutKeyMask));
        }

        chatHistoryAction = new BasicAction("Open Chat...", new AutoIcon(ICON_TIMER),
                e -> showChats((JComponent) e.getSource()),
                a -> a.setEnabled(canShowChats()));
        chatHistoryAction.setShortDescription("Chat history");
        chatHistoryAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask));

        newChatAction = new BasicAction("New Chat", new AutoIcon(ICON_PLUS, IconFactory.IconSize.Auto, true, false),
                e -> newChat(),
                a -> a.setEnabled(!getChatMessages().isEmpty() && !getChatPane().isWaitingForResponse()));
        newChatAction.setShortDescription("New chat");
        newChatAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask));
    }

    private boolean canShowChats() {
        return !getChatPane().isWaitingForResponse() && chatHistoryStorage.getChatHistoryItemsSize() > 0;
    }

    @Override
    public void uninstall() {
        try {
            storeNewChat();
        } catch (Exception ex) {
            logger.error("Failed to store new chat", ex);
        }

        super.uninstall();
    }

    private void newChat() {
        storeNewChat();
        chatHistoryItemUid = null;

        getChatPane().clearChatMessages();

        if (workflowDebugger != null) {
            workflowDebugger.clearAgentInvocation();

            pnlWorkflowInspectorStructure.clearItems();
            pnlWorkflowInspectorStructure.revalidate();
            pnlWorkflowInspectorStructure.repaint();
            pnlWorkflowInspectorExecution.reset();
            pnlWorkflowInspectorDetails.setValues(null);
        }
    }

    private void storeNewChat() {
        List<ChatMessage> chatMessages = getChatMessages();
        if (!chatMessages.isEmpty()) {
            chatHistoryStorage.addChatMessages(chatHistoryItemUid, new ArrayList<>(chatMessages));
        }
    }

    private void showChats(JComponent source) {
        ChatHistoryDialog dialog = new ChatHistoryDialog(getAppFrame(), chatHistoryStorage);
        ChatHistoryStorage.ChatHistoryItem chatHistoryItem = dialog.executeModal(chatHistoryStorage.getChatHistoryItems());
        if (chatHistoryItem != null)
            openChat(chatHistoryItem);
    }

    private void openChat(ChatHistoryStorage.ChatHistoryItem chatHistoryItem) {
        newChat();
        chatHistoryItemUid = chatHistoryItem.uid();
        getChatPane().restore(chatHistoryItem);
    }

    private boolean canEditUserMessage() {
        WorkflowInspectorListPane pane = getVisibleWorkflowInspectorListPane();

        return pane != null &&
                pane.isVisible() &&
                pane.getSelectedWorkflowItem() != null &&
                pane.getSelectedWorkflowItem().getType() == WorkflowItem.Type.Agent;
    }

    private WorkflowInspectorListPane getVisibleWorkflowInspectorListPane() {
        WorkflowInspectorListPane pane = null;
        if (pnlWorkflowInspectorStructure.isVisible())
            pane = pnlWorkflowInspectorStructure;
        else if (pnlWorkflowInspectorExecution.isVisible())
            pane = pnlWorkflowInspectorExecution;
        return pane;
    }

    private void editUserMessage() {
        WorkflowInspectorListPane pane = getVisibleWorkflowInspectorListPane();

        WorkflowItem workflowItem = pane != null ? pane.getSelectedWorkflowItem() : null;
        if (workflowItem == null)
            return;

        String agentClassName = workflowItem.getAgentClassName();
        String userMessage = workflowDebugger.getUserMessageTemplate(agentClassName);
        boolean canReset = userMessage != null;
        if (userMessage == null)
            userMessage = workflowItem.getUserMessage();

        EditUserMessageDialog.Result result = EditUserMessageDialog.editUserMessage(getAppFrame(),
                userMessage,
                getPlaygroundContext().getAgentMetadata().getArguments().stream().map(arg -> arg.name()).toList(),
                canReset);
        switch (result.modalResult()) {
            case AppDialog.ACTION_COMMAND_OK:
                if (!Objects.equals(userMessage, result.userMessage())) {
                    workflowDebugger.setUserMessageTemplate(agentClassName, result.userMessage());
                    agentsChanged();
                }
                break;
            case EditUserMessageDialog.ACTION_COMMAND_RESET:
                workflowDebugger.setUserMessageTemplate(agentClassName, null);
                agentsChanged();
                break;
            default:
        }
    }

    private void agentsChanged() {
        if (userMessagesStorage != null)
            userMessagesStorage.store();

        WorkflowInspectorListPane pane = getVisibleWorkflowInspectorListPane();

        if (pane != null) {
            pane.repaint();
            pnlWorkflowInspectorDetails.setValues(pane.getSelectedData());
        }
    }

    private void setupModelsAction() {
        if (playgroundContext.getChatModels() != null && playgroundContext.getChatModels().size() > 1) {
            JComboBox modelsCombobox = new JComboBox(playgroundContext.getChatModels().toArray()) {
                @Override
                public String getToolTipText(MouseEvent event) {
                    return getSelectedItem().toString();
                }
            };
            modelsCombobox.setToolTipText("");
            modelsCombobox.setFocusable(false);
            Dimension preferredSize = modelsCombobox.getPreferredSize();
            preferredSize.width = 150;
            modelsCombobox.setPreferredSize(preferredSize);
            modelsCombobox.setMaximumSize(preferredSize);
            chatModelsAction = new ComponentAction("Model: ", modelsCombobox,
                    e -> playgroundContext.setChatModel((PlaygroundMetadata.Model) modelsCombobox.getSelectedItem()),
                    a -> {
                        boolean enabled = !getChatPane().isWaitingForResponse();
                        modelsCombobox.setEnabled(enabled);
                        if (enabled) {
                            for (PlaygroundMetadata.Model playgroundChatModel : playgroundContext.getChatModels()) {
                                if (playgroundChatModel.id().equals(playgroundContext.getChatModel().id())) {
                                    modelsCombobox.setSelectedItem(playgroundChatModel);
                                    break;
                                }
                            }
                        }
                    });
            chatModelsAction.putValue(Action.MNEMONIC_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }
    }

    private void setupToolbar(ActionToolBar toolbar) {
        inspectorToolbarActionGroup = new ActionGroup(
                new ActionGroup(
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction),
                new ActionGroup(
                        editUserMessageAction
                )
        );
        toolbar.setActionGroup(inspectorToolbarActionGroup);
    }

    private void setupChatToolbar(ActionToolBar toolbar) {
        ActionGroup chatModelsActionGroup = chatModelsAction != null ? new ActionGroup(chatModelsAction) : null;
        chatToolbarActionGroup = new ActionGroup(
                chatModelsActionGroup,
                new ActionGroup(
                        chatHistoryAction,
                        newChatAction
                ),
                new ActionGroup(
                        workflowExpertAction
                )
        );
        toolbar.setActionGroup(chatToolbarActionGroup);
    }

    private void setupPopupMenu() {
        ActionGroup actionGroup = new ActionGroup(
                new ActionGroup(
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction
                ),
                new ActionGroup(
                        StandardActions.createCopyAction()
                ),
                new ActionGroup(
                        editUserMessageAction
                )
        );
        ActionPopupMenu popupMenu = new ActionPopupMenu();
        popupMenu.setActionGroup(actionGroup);
        pnlWorkflowInspectorStructure.setComponentPopupMenu(popupMenu);
        pnlWorkflowInspectorExecution.setComponentPopupMenu(popupMenu);

        actionGroup = new ActionGroup(
                new ActionGroup(
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction
                ),
                new ActionGroup(
                        StandardActions.createCopyAction()
                ),
                new ActionGroup(
                        new BasicAction("Refresh", new AutoIcon(ICON_REFRESH),
                                e -> generateWorkflowSummary(true),
                                a -> a.setEnabled(!summaryGenerating))
                )
        );
        popupMenu = new ActionPopupMenu();
        popupMenu.setActionGroup(actionGroup);
        pnlWorkflowSummaryView.setComponentPopupMenu(popupMenu);
    }

    private FileChooserUtils getFileChooserUtils() {
        if (fileChooserUtils == null) {
            fileChooserUtils = new FileChooserUtils(this);
            fileChooserUtils.setChoosableFileFilters(new FileFilter[]{FileChooserUtils.GenericFileFilter.FILE_FILTER_HTML});
            fileChooserUtils.setUseNativeFileChooser(true);
        }
        return fileChooserUtils;
    }

    private void showWorkflowSummary(boolean forceGeneration) {
        ((CardLayout) pnlWorkflowContents.getLayout()).last(pnlWorkflowContents);
        pnlWorkflowInspectorDetails.setValues(null);
        pnlWorkflowSummaryView.requestFocus();
        generateWorkflowSummary(forceGeneration);
    }

    private void generateWorkflowSummary(boolean forceGeneration) {
        if ((!summaryGenerated || forceGeneration) && !summaryGenerating) {
            summaryGenerating = true;
            pnlWorkflowSummaryView.setText("Generating summary..."); // Set initial text immediately
            pnlWorkflowSummaryView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // Set cursor immediately
            CompletableFuture.supplyAsync(() -> getPlaygroundContext().generateAgentSummary()).
                    thenAccept(summary -> {
                        summaryGenerated = true;
                        SwingUtilities.invokeLater(() -> {
                            pnlWorkflowSummaryView.setText("<html><body style=\"padding: 5px 10px;\">%s</body></html>".formatted(convertMarkdownToHtml(summary)));
                            pnlWorkflowSummaryView.setCaretPosition(0);
                            pnlWorkflowSummaryView.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            summaryGenerating = false;
                        });
                    }).exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            pnlWorkflowSummaryView.setText("<html>Error generating summary: %s</html>".formatted(ex.getMessage()));
                            pnlWorkflowSummaryView.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            summaryGenerating = false;
                            logger.error("Failed to generate summary", ex);
                        });
                        return null; // Return null to complete the exceptionally stage
                    });
        }
    }

    private void showWorkflowStructure() {
        if (!pnlWorkflowInspectorStructure.isVisible())
            ((CardLayout) pnlWorkflowContents.getLayout()).first(pnlWorkflowContents);
        if (pnlWorkflowInspectorStructure.getListView().getSelectedIndex() == -1)
            pnlWorkflowInspectorStructure.getListView().setSelectedIndex(0);
        else
            pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorStructure.getSelectedData());
        pnlWorkflowInspectorStructure.requestFocus();
    }

    private void showWorkflowExecution() {
        if (!pnlWorkflowInspectorExecution.isVisible())
            ((CardLayout) pnlWorkflowContents.getLayout()).show(pnlWorkflowContents, "execution");
        if (pnlWorkflowInspectorExecution.getListView().getSelectedIndex() == -1)
            pnlWorkflowInspectorExecution.getListView().setSelectedIndex(0);
        else
            pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorExecution.getSelectedData());
        pnlWorkflowInspectorExecution.requestFocus();
    }

    public WorkflowDebugger getWorkflowDebugger() {
        return workflowDebugger;
    }

    public void setWorkflowDebugger(WorkflowDebugger workflowDebugger) {
        if (this.workflowDebugger != null)
            this.workflowDebugger.removeBreakpoint(sessionStartedBreakpoint);

        this.workflowDebugger = workflowDebugger;

        if (this.workflowDebugger != null)
            this.workflowDebugger.addBreakpoint(sessionStartedBreakpoint);

        if (pnlWorkflowInspectorStructure != null)
            pnlWorkflowInspectorStructure.setWorkflowDebugger(workflowDebugger);
    }

    /**
     * Returns the ChatPane associated with this ChatFrame.
     *
     * @return The ChatPane instance.
     */
    public ChatPane getChatPane() {
        return pnlChat;
    }

    /**
     * Returns a list of all chat messages currently in the chat pane.
     *
     * @return A list of ChatMessage objects.
     */
    public List<ChatMessage> getChatMessages() {
        return getChatPane().getChatMessages();
    }

    @Override
    public void update() {
        super.update();

        if (workflowDebugger != null) {
            inspectorToolbarActionGroup.update();
            chatToolbarActionGroup.update();
            pnlWorkflowInspectorDetails.scheduledUpdate();
        }
        pnlChat.update();
    }

    @Override
    public void showExecutionDetails(ChatMessage chatMessage, Consumer<Boolean> completion) {
        agentInvocationTraceEntryArchive = workflowDebugger.getAgentInvocationTraceEntryArchives().stream()
                .filter(current -> current.uid().equals(chatMessage.uid()))
                .findFirst().orElse(null);

        CompletableFuture<Void> task1 = new CompletableFuture<>();
        CompletableFuture<Void> task2 = new CompletableFuture<>();
        CompletableFuture.allOf(task1, task2).whenComplete((aUnused, ex) -> {
            if (completion != null)
                completion.accept(ex == null);
            if (ex != null)
                logger.error("Failed to show execution details", ex);
        });

        pnlWorkflowInspectorStructure.setTraceEntryArchive(agentInvocationTraceEntryArchive, task1);
        pnlWorkflowInspectorExecution.setTraceEntryArchive(agentInvocationTraceEntryArchive, task2);
    }

    @SuppressWarnings("unused")
    static class ExecutionDetailsCompletion implements Runnable {
        private final Runnable completion;
        private final AtomicInteger counter = new AtomicInteger();

        public ExecutionDetailsCompletion(Runnable aCompletion) {
            completion = aCompletion;
        }

        @Override
        public void run() {
            int i = counter.incrementAndGet();
            if (i == 2)
                completion.run();
        }
    }
}
