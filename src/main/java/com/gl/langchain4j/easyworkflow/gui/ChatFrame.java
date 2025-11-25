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
import com.gl.langchain4j.easyworkflow.*;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatHistoryDialog;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatMessage;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorDetailsPane;
import com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorListPane;
import com.gl.langchain4j.easyworkflow.gui.platform.*;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.AgentInvocationTraceEntryArchive;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.Breakpoint;
import static com.gl.langchain4j.easyworkflow.gui.Icons.ICON_SPACER;
import static com.gl.langchain4j.easyworkflow.gui.Icons.LOGO_ICON;
import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.*;
import static com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorDetailsPane.PROP_SELECTED_VARIABLE;
import static com.gl.langchain4j.easyworkflow.gui.platform.Actions.*;
import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.*;

/**
 * A frame that provides a chat interface. It can be used to display a chat conversation and interact with a chat
 * engine.
 */
@SuppressWarnings("ALL")
public class ChatFrame extends AppFrame implements AboutProvider, ChatPane.ExecutionDetailsProvider {

    public static final String PROP_FLOW_CHART_FILE = "flow-chart-file";
    public static final String PROP_STRUCTURE_FILE = "structure-file";
    public static final String PROP_EXECUTION_FILE = "execution-file";
    public static final String PROP_SUMMARY_FILE = "summary-file";
    public static final String PROP_USER_MESSAGES_FILE = "user-messages-file";
    public static final String PROP_CHAT_FILE = "chat-file";

    private static final Logger logger = LoggerFactory.getLogger(ChatFrame.class);
    private final ChatPane pnlChat = new ChatPane();
    private final List<Playground.PlaygroundChatModel> chatModels;
    private JScrollPane pnlWorkflowSummary;
    private JEditorPane pnlWorkflowSummaryView;
    private JPanel pnlWorkflowContents;
    private WorkflowDebugger workflowDebugger;
    private WorkflowInspectorDetailsPane pnlWorkflowInspectorDetails;
    private WorkflowInspectorListPane pnlWorkflowInspectorStructure;
    private WorkflowInspectorListPane pnlWorkflowInspectorExecution;
    private BasicAction copyAction;
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

    private final UserMessagesStorage userMessagesStorage;
    private BasicAction chatHistoryAction;
    private BasicAction newChatAction;
    private ActionGroup chatToolbarActionGroup;

    private ChatHistoryStorage chatHistoryStorage;
    private String chatHistoryItemUid;

    /**
     * Constructs a new ChatFrame.
     *
     * @param title            The title of the chat frame.
     * @param icon             The icon to be displayed for the chat frame.
     * @param chatEngine       A function that takes a user message and returns a chat engine's response.
     * @param agent            The agent object associated with this chat frame.
     * @param workflowDebugger The workflow debugger to be used for inspecting workflows.
     * @param chatModels       The chat models
     */
    public ChatFrame(String title, ImageIcon icon,
                     ChatPane.ChatEngine chatEngine,
                     Object agent,
                     WorkflowDebugger workflowDebugger,
                     List<Playground.PlaygroundChatModel> chatModels) {
        super("chatFrame");

        this.chatModels = chatModels;
        this.agent = agent;

        setWorkflowDebugger(workflowDebugger);
        this.pnlChat.setChatEngine(chatEngine);

        setTitle(title);

        setSize(workflowDebugger != null ? 1080 : 500, 700);
        setLocationRelativeTo(null);

        pnlChat.setPreferredSize(new Dimension(400, 700));
        setMinimumSize(new Dimension(500, 700));

        if (icon != null) {
            setIconImage(icon.getImage());
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }

        if (workflowDebugger != null) {
            userMessagesStorage = new UserMessagesStorage(workflowDebugger,
                    agentClassName -> pnlWorkflowInspectorStructure.getUserMessage(agentClassName));
            chatHistoryStorage = new ChatHistoryStorage(workflowDebugger.getAgentWorkflowBuilder().getAgentClass());

            setMinimumSize(new Dimension(1280, 720));

            JSplitPane contentPane = new AppSplitPane();
            contentPane.setResizeWeight(0);
            contentPane.setLeftComponent(pnlChat);
            pnlChat.getHeaderPane().setVisible(true);

            pnlWorkflowInspectorStructure = new WorkflowInspectorListPane.Structure();
            pnlWorkflowInspectorStructure.setWorkflow(workflowDebugger.getAgentWorkflowBuilder());
            pnlWorkflowInspectorStructure.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorStructure.setWorkflowDebugger(workflowDebugger);

            pnlWorkflowInspectorExecution = new WorkflowInspectorListPane.Execution();
            pnlWorkflowInspectorExecution.setWorkflow(workflowDebugger.getAgentWorkflowBuilder());
            pnlWorkflowInspectorExecution.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorExecution.setWorkflowDebugger(workflowDebugger);
            pnlWorkflowInspectorExecution.setPlaceHolderText("Run workflow to see execution results");
            pnlWorkflowInspectorExecution.setPlaceHolderIcon(new AutoIcon(ICON_INFO_PLAIN));
            pnlWorkflowInspectorExecution.setPlaceHolderVisible(true);

            pnlWorkflowSummaryView = new PreviewTextPane();
            pnlWorkflowSummary = UISupport.createScrollPane(pnlWorkflowSummaryView, false, false, false, false, false);
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
            setContentPane(contentPane);

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
            setContentPane(pnlChat);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                getOptions().setFrameBounds(getBounds());
            }
        });
    }


    /**
     * Displays a new chat frame with the given parameters.
     *
     * @param title            The title of the chat frame.
     * @param icon             The icon to be displayed for the chat frame.
     * @param chatEngine       A function that takes a user message and returns a chat engine's response.
     * @param agent            The agent object associated with this chat frame.
     * @param workflowDebugger The workflow debugger to be used for inspecting workflows.
     * @param chatModels       The chat models
     * @return The newly created and displayed ChatFrame instance.
     */
    public static ChatFrame createChatFrame(String title, ImageIcon icon,
                                            ChatPane.ChatEngine chatEngine,
                                            Object agent,
                                            WorkflowDebugger workflowDebugger,
                                            List<Playground.PlaygroundChatModel> chatModels) {
        applyAppearance();

        ChatFrame chatFrame = new ChatFrame(title,
                icon,
                chatEngine,
                agent,
                workflowDebugger,
                chatModels
        );

        return chatFrame;
    }

    private static void visitSite(String url) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the agent object associated with this chat frame.
     *
     * @return The agent object.
     */
    public Object getAgent() {
        return agent;
    }

    /**
     * Sets the agent object for this chat frame.
     *
     * @param aAgent The agent object to be set.
     */
    public void setAgent(Object aAgent) {
        agent = aAgent;
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
        JMenuBar menuBar = new JMenuBar();

        setupMenuBarFileActionGroup();
        setupMenuBarEditActionGroup();
        setupMenuBarViewActionGroup();
        setupMenuBarOptionsActionGroup();
        setupMenuBarHelpActionGroup();

        menuBarActionGroup = new ActionGroup(
                menuBarFileActionGroup,
                menuBarEditActionGroup,
                menuBarViewActionGroup,
                menuBarOptionsActionGroup,
                menuBarHelpActionGroup
        );
        UISupport.setupMenuBar(menuBar, menuBarActionGroup);
        setJMenuBar(menuBar);
    }

    private void setupMenuBarHelpActionGroup() {
        menuBarHelpActionGroup = new ActionGroup("Help", null, true,
                new ActionGroup(null, null, false,
                        new BasicAction("Visit 'EasyWorkflow for LangChain4j'", new AutoIcon(ICON_GLOBE), e -> visitSite()),
                        new BasicAction("Visit 'LangChain4j'", new AutoIcon(ICON_GLOBE), e -> visitSite("https://docs.langchain4j.dev/"))
                ),
                new ActionGroup(null, null, false,
                        new BasicAction("About...", new AutoIcon(ICON_HELP), e -> showAbout(this))
                )
        );
    }

    private void setupMenuBarOptionsActionGroup() {
        String exclusiveGroup = "appearance";

        menuBarOptionsActionGroup = new ActionGroup("Options", null, true,
                new ActionGroup(
                        pnlChat.getRenderMarkdownAction(),
                        pnlChat.getClearAfterSendingAction()
                ),
                new ActionGroup(),
                new ActionGroup("Appearance", new AutoIcon(ICON_SPACER), true,
                        new StateAction("Light", null, exclusiveGroup,
                                e -> applyAppearance(Appearance.Light),
                                a -> a.setSelected(getOptions().getAppearance() == Appearance.Light)),
                        new StateAction("Dark", null, exclusiveGroup,
                                e -> applyAppearance(Appearance.Dark),
                                a -> a.setSelected(getOptions().getAppearance() == Appearance.Dark)),
                        new StateAction("Auto", null, exclusiveGroup,
                                e -> applyAppearance(Appearance.Auto),
                                a -> a.setSelected(getOptions().getAppearance() == Appearance.Auto))
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

        menuBarViewActionGroup = new ActionGroup("View", null, true,
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
        BasicAction cutAction = new BasicAction("Cut", new AutoIcon(ICON_CUT), aActionEvent -> cut());
        cutAction.setMnemonic('x');
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        cutAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask));

        BasicAction copyAction = new BasicAction("Copy", new AutoIcon(ICON_COPY), aActionEvent -> copy());
        copyAction.setMnemonic('c');
        copyAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask));

        BasicAction pasteAction = new BasicAction("Paste", new AutoIcon(ICON_PASTE), aActionEvent -> paste());
        pasteAction.setMnemonic('v');
        pasteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask));

        BasicAction deleteAction = new BasicAction("Delete", new AutoIcon(ICON_SPACER), aActionEvent -> delete());
        deleteAction.setMnemonic('d');
        deleteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        menuBarEditActionGroup = new ActionGroup("Edit", null, true,
                cutAction,
                copyAction,
                pasteAction,
                deleteAction
        );
    }

    private void setupMenuBarFileActionGroup() {
        menuBarFileActionGroup = new ActionGroup("File", null, true,
                new ActionGroup(
                        newChatAction,
                        chatHistoryAction
                ),
                new ActionGroup("Share", new AutoIcon(ICON_SHARE), true,
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
                                        e -> getOptions().setOpenFileAfterSharing(!getOptions().isOpenFileAfterSharing()),
                                        a -> a.setSelected(getOptions().isOpenFileAfterSharing()))
                        )
                )
        );

        if (!isMac()) {
            BasicAction exitAction = new BasicAction("Exit", null, e -> Application.getSharedApplication().exit(false));
            exitAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));

            menuBarFileActionGroup.addAction(new ActionGroup());
            menuBarFileActionGroup.addAction(new ActionGroup(exitAction));
        }
    }

    private void paste() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (focusOwner instanceof JComponent c) {
            Action action = c.getActionMap().get("paste");
            if (action != null)
                action.actionPerformed(new ActionEvent(c, 0, null));
        }
    }

    private void delete() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (focusOwner instanceof JComponent c) {
            Action action = c.getActionMap().get("delete");
            if (action != null)
                action.actionPerformed(new ActionEvent(c, 0, null));
        }
    }

    private void cut() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (focusOwner instanceof JComponent c) {
            Action action = c.getActionMap().get("cut");
            if (action != null)
                action.actionPerformed(new ActionEvent(c, 0, null));
        }
    }

    /**
     * Copies the currently selected content to the clipboard.
     */
    public void copy() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (focusOwner != null && SwingUtilities.getAncestorOfClass(ChatPane.class, focusOwner) != null) {
            pnlChat.copy();
        } else if (focusOwner instanceof JComponent c) {
            Action action = c.getActionMap().get("copy");
            if (action != null)
                action.actionPerformed(new ActionEvent(c, 0, null));
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();

        CompletableFuture.runAsync(() -> {
            if (userMessagesStorage != null)
                userMessagesStorage.load();
        }).thenRun(() -> SwingUtilities.invokeLater(() -> repaint()));
        CompletableFuture.runAsync(() -> chatHistoryStorage.load());
    }

    private boolean canShareUserMessages() {
        return workflowDebugger.hasUserMessageTemplates();
    }

    private void shareUserMessages() {
        String userMessagesAsJson = userMessagesStorage.asJson();
        if (userMessagesAsJson != null)
            shareContent(userMessagesAsJson,
                    PROP_USER_MESSAGES_FILE,
                    "user-messages.json");
        else
            logger.warn("No user messages to share");
    }

    private void shareChat() {
        if (pnlChat.getChatMessages().isEmpty())
            return;

        try {
            shareContent(ChatPane.OBJECT_MAPPER.
                            enable(SerializationFeature.INDENT_OUTPUT).
                            writeValueAsString(pnlChat.getChatMessages()),
                    PROP_CHAT_FILE,
                    "chat.json");
        } catch (JsonProcessingException ex) {
            logger.error("Failed to share chat", ex);
        }
    }

    private void shareSummary() {
        shareContent(pnlWorkflowSummaryView.getText(),
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
        shareContent(content,
                PROP_EXECUTION_FILE,
                "workflow-execution.txt");
    }

    private void shareStructure() {
        shareContent(workflowDebugger.getAgentWorkflowBuilder().toJson(),
                PROP_STRUCTURE_FILE,
                "workflow-structure.json");
    }

    private void shareFlowChart() {
        String content = agentInvocationTraceEntryArchive == null ?
                workflowDebugger.toHtml(true) :
                workflowDebugger.toHtml(true,
                        agentInvocationTraceEntryArchive.workflowInput(),
                        agentInvocationTraceEntryArchive.agentInvocationTraceEntries(),
                        agentInvocationTraceEntryArchive.workflowResult(),
                        agentInvocationTraceEntryArchive.workflowFailure());
        shareContent(content,
                PROP_FLOW_CHART_FILE,
                "workflow.html");
    }

    private void shareContent(String content, String fileNameProperty, String defaultFileName) {
        FileChooserUtils fileChooserUtils = getFileChooserUtils();

        String fileStr = getPreferences().get(fileNameProperty, defaultFileName);
        File file = fileChooserUtils.chooseFileToSave(new File(fileStr), true);
        if (file != null) {
            try {
                getPreferences().put(fileNameProperty, file.getAbsolutePath());
                Files.write(Paths.get(file.getAbsolutePath()), content.getBytes());

                if (getOptions().isOpenFileAfterSharing() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                    Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                logger.error("Failed to share flow chart", ex);
            }
        }
    }

    private void setupActions() {
        final String showGroup = "show";
        showStructureAction = new StateAction("Structure", new AutoIcon(ICON_WORKFLOW), showGroup,
                e -> showWorkflowStructure(),
                a -> a.setSelected(pnlWorkflowInspectorStructure.isVisible()));
        showStructureAction.setShortDescription("Show workflow structure");
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        showStructureAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, menuShortcutKeyMask));

        showExecutionAction = new StateAction("Execution", new AutoIcon(ICON_EXECUTION_FLOW), showGroup,
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

        editUserMessageAction = new BasicAction("Edit User Message...", new AutoIcon(ICON_COMPOSE),
                e -> editUserMessage(),
                a -> a.setEnabled(canEditUserMessage()));
        editUserMessageAction.setShortDescription("Edit user message");
        UISupport.bindAction(pnlWorkflowInspectorStructure.getListView(),
                "editUserMessage",
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                editUserMessageAction);
        UISupport.bindAction(pnlWorkflowInspectorExecution.getListView(),
                "editUserMessage",
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                editUserMessageAction);
        UISupport.bindDoubleClickAction(pnlWorkflowInspectorStructure.getListView(), editUserMessageAction);
        UISupport.bindDoubleClickAction(pnlWorkflowInspectorExecution.getListView(), editUserMessageAction);

        this.copyAction = new BasicAction("Copy", new AutoIcon(ICON_COPY),
                e -> copy(),
                aBasicAction -> {
                    boolean e = true;
                    if (pnlWorkflowSummaryView.isVisible())
                        e = !summaryGenerating;
                    setEnabled(e);
                });
        copyAction.setShortDescription("Copy");

        shareAction = new BasicAction("Share", new AutoIcon(ICON_SHARE), e -> shareFlowChart());
        shareAction.setShortDescription("Share");

        if (workflowDebugger != null) {
            workflowExpertAction = new GUIPlayground.WorkflowExpertAction(new AutoIcon(ICON_AGENT_TOOLBAR),
                    this,
                    workflowDebugger.getAgentWorkflowBuilder().getAgentClass(), workflowDebugger
            );
            workflowExpertAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E,
                    KeyEvent.SHIFT_DOWN_MASK | menuShortcutKeyMask));
        }

        chatHistoryAction = new BasicAction("Open Chat...", new AutoIcon(ICON_TIMER),
                e -> showChatHistory((JComponent) e.getSource()),
                a -> a.setEnabled(chatHistoryStorage.getChatHistoryItemsSize() > 0));
        chatHistoryAction.setShortDescription("Chat history");
        chatHistoryAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask));

        newChatAction = new BasicAction("New Chat", new AutoIcon(ICON_PLUS),
                e -> newChat(),
                a -> a.setEnabled(! getChatMessages().isEmpty() && ! getChatPane().isWaitingForResponse()));
        newChatAction.setShortDescription("New chat");
        newChatAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask));
    }

    @Override
    public void close(boolean forceClose) {
        try {
            storeNewChat();
        } catch (Exception ex) {
            logger.error("Failed to store new chat", ex);
        }

        super.close(forceClose);
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
        if (! chatMessages.isEmpty()) {
            chatHistoryStorage.addChatMessages(chatHistoryItemUid, new ArrayList<>(chatMessages));
        }
    }

    private void showChatHistory(JComponent source) {
        ChatHistoryDialog dialog = new ChatHistoryDialog(this, chatHistoryStorage);
        ChatHistoryStorage.ChatHistoryItem chatHistoryItem = dialog.executeModal(chatHistoryStorage.getChatHistoryItems());
        if (chatHistoryItem != null)
            restore(chatHistoryItem);
    }

    private void restore(ChatHistoryStorage.ChatHistoryItem chatHistoryItem) {
        newChat();
        chatHistoryItemUid = chatHistoryItem.uid();
        getChatPane().restore(chatHistoryItem);
    }

    private boolean canEditUserMessage() {
        WorkflowInspectorListPane pane = getVisibleWorkflowInspectorListPane();

        return pane != null &&
                pane.isVisible() &&
                pane.getSelectedWorkflowItem() != null &&
                pane.getSelectedWorkflowItem().getType().equals(EasyWorkflow.JSON_TYPE_AGENT);
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

        WorkflowInspectorListPane.WorkflowItem workflowItem = pane != null ? pane.getSelectedWorkflowItem() : null;
        if (workflowItem == null)
            return;

        try {
            String agentClassName = workflowItem.getAgentClassName();
            Class<?> agentClass = Class.forName(agentClassName);
            String userMessage = workflowDebugger.getUserMessageTemplate(agentClass.getName());
            boolean canReset = userMessage != null;
            if (userMessage == null)
                userMessage = workflowItem.getUserMessage();

            EditUserMessageDialog.Result result = EditUserMessageDialog.editUserMessage(this,
                    userMessage,
                    EasyWorkflow.getAgentMethodParameterNames(EasyWorkflow.getAgentMethod(agentClass)),
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
        } catch (ClassNotFoundException ex) {
            logger.error("Failed to edit user message", ex);
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
        if (chatModels != null && chatModels.size() > 1) {
            JComboBox modelsCombobox = new JComboBox(chatModels.toArray()) {
                @Override
                public String getToolTipText(MouseEvent event) {
                    return getSelectedItem().toString();
                }
            };
            modelsCombobox.setToolTipText("");
            ChatModel chatModel = workflowDebugger.getAgentWorkflowBuilder().getChatModel();
            for (Playground.PlaygroundChatModel playgroundChatModel : chatModels) {
                if (playgroundChatModel.chatModel().equals(chatModel)) {
                    modelsCombobox.setSelectedItem(playgroundChatModel);
                    break;
                }
            }

            modelsCombobox.setFocusable(false);
            Dimension preferredSize = modelsCombobox.getPreferredSize();
            preferredSize.width = 150;
            modelsCombobox.setPreferredSize(preferredSize);
            modelsCombobox.setMaximumSize(preferredSize);
            chatModelsAction = new ComponentAction("Model: ", modelsCombobox, e ->
                    setChatModel(((Playground.PlaygroundChatModel) modelsCombobox.getSelectedItem()).chatModel()));
        }
    }

    private void setChatModel(ChatModel chatModel) {
        workflowDebugger.getAgentWorkflowBuilder().chatModel(chatModel);
        agent = workflowDebugger.getAgentWorkflowBuilder().build();
    }

    private void setupToolbar(JToolBar toolbar) {
        inspectorToolbarActionGroup = new ActionGroup(
                new ActionGroup(
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction),
                new ActionGroup(
                        editUserMessageAction
                )
        );
        UISupport.setupToolbar(toolbar, inspectorToolbarActionGroup);
    }

    private void setupChatToolbar(JToolBar toolbar) {
        ActionGroup chatModelsActionGroup = chatModelsAction != null ? new ActionGroup(chatModelsAction) : null;
        chatToolbarActionGroup =new ActionGroup(
                chatModelsActionGroup,
                new ActionGroup(
                        chatHistoryAction,
                        newChatAction
                ),
                new ActionGroup(
                        workflowExpertAction
                )
        );
        UISupport.setupToolbar(toolbar, chatToolbarActionGroup);
    }

    private void setupPopupMenu() {
        ActionGroup actionGroup = new ActionGroup(
                new ActionGroup(
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction
                ),
                new ActionGroup(
                        copyAction
                ),
                new ActionGroup(
                        editUserMessageAction
                )
        );
        JPopupMenu popupMenu = new JPopupMenu();
        UISupport.setupPopupMenu(popupMenu, actionGroup);
        pnlWorkflowInspectorStructure.setComponentPopupMenu(popupMenu);
        pnlWorkflowInspectorExecution.setComponentPopupMenu(popupMenu);

        actionGroup = new ActionGroup(
                new ActionGroup(
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction
                ),
                new ActionGroup(
                        copyAction
                ),
                new ActionGroup(
                        new BasicAction("Refresh", new AutoIcon(ICON_TOOLBAR_REFRESH),
                                e -> generateWorkflowSummary(true),
                                a -> a.setEnabled(!summaryGenerating))
                )
        );
        popupMenu = new JPopupMenu();
        UISupport.setupPopupMenu(popupMenu, actionGroup);
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
            CompletableFuture.supplyAsync(() -> workflowDebugger.getAgentWorkflowBuilder().generateAISummary()).
                    thenAccept(summary -> {
                        summaryGenerated = true;
                        SwingUtilities.invokeLater(() -> {
                            pnlWorkflowSummaryView.setText("<html><body style=\"padding: 5px 10px;\">%s</body></html>".formatted(UISupport.convertMarkdownToHtml(summary)));
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
    public void showAbout(Component parent) {
        Object[] options = {"Site", "OK"};
        Version version = Version.getInstance();
        int result = JOptionPane.showOptionDialog(
                parent,
                """
                <html><b>Playground</b> by "%s"<br><br>
                <b>v%s</b>#%s <i>%s</i><br><br>
                Copyright © 2025 Gregory Ledenev <i>(gregory.ledenev37@gmail.com)</i></html>""".formatted(
                        version.getProjectName(),
                        version.getProjectVersion(), version.getBuildNumber(), version.getBuildDate().toString()),
                "About",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                LOGO_ICON,
                options,
                options[1]
        );
        if (result == 0) {
            visitSite();
        }
    }

    @Override
    public void visitSite() {
        String url = "https://github.com/gregory-ledenev/LangChain4j-EasyWorkflow";
        visitSite(url);
    }

    @Override
    public void restoreState() {
        super.restoreState();
        Rectangle frameBounds = getOptions().getFrameBounds();
        if (frameBounds != null)
            setBounds(frameBounds);
    }

    @Override
    public void saveState() {
        super.saveState();
        UISupport.getOptions().setFrameBounds(getBounds());
    }

    @Override
    public void scheduledUpdate() {
        super.scheduledUpdate();

        if (workflowDebugger != null) {
            menuBarActionGroup.update();
            inspectorToolbarActionGroup.update();
            chatToolbarActionGroup.update();
            pnlWorkflowInspectorDetails.scheduledUpdate();
        }
        pnlChat.scheduledUpdate();
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
