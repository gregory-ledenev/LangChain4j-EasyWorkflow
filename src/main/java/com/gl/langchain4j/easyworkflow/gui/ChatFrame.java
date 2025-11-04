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
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import com.gl.langchain4j.easyworkflow.WorkflowExpert;
import com.gl.langchain4j.easyworkflow.WorkflowExpertSupport;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatMessage;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorDetailsPane;
import com.gl.langchain4j.easyworkflow.gui.inspector.WorkflowInspectorListPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.*;
import static com.gl.langchain4j.easyworkflow.gui.Actions.*;
import static com.gl.langchain4j.easyworkflow.gui.ToolbarIcons.*;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

/**
 * A frame that provides a chat interface. It can be used to display a chat conversation and interact with a chat
 * engine.
 */
public class ChatFrame extends AppFrame implements UISupport.AboutProvider {

    public static final String PROP_FLOW_CHART_FILE = "flow-chart-file";
    public static final String PROP_STRUCTURE_FILE = "structure-file";
    public static final String PROP_EXECUTION_FILE = "execution-file";
    public static final String PROP_SUMMARY_FILE = "summary-file";
    public static final String PROP_CHAT_FILE = "chat-file";

    private static final Logger logger = LoggerFactory.getLogger(ChatFrame.class);
    private final ChatPane chatPane = new ChatPane();
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

    /**
     * Constructs a new ChatFrame.
     *
     * @param title      The title of the chat frame.
     * @param icon       The icon to be displayed for the chat frame.
     * @param chatEngine A function that takes a user message and returns a chat engine's response.
     */
    public ChatFrame(String title, ImageIcon icon, ChatPane.ChatEngine chatEngine, WorkflowDebugger workflowDebugger) {
        super("chatFrame");

        setWorkflowDebugger(workflowDebugger);
        this.chatPane.setChatEngine(chatEngine);

        setTitle(title);

        setSize(workflowDebugger != null ? 1080 : 500, 700);
        setLocationRelativeTo(null);

        chatPane.setPreferredSize(new Dimension(400, 700));
        setMinimumSize(new Dimension(500, 700));

        if (icon != null) {
            setIconImage(icon.getImage());
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }

        if (workflowDebugger != null) {
            setMinimumSize(new Dimension(1080, 700));

            JSplitPane contentPane = new JSplitPane();
            contentPane.setResizeWeight(0);
            contentPane.setLeftComponent(chatPane);

            pnlWorkflowInspectorStructure = new WorkflowInspectorListPane.Structure();
            pnlWorkflowInspectorStructure.setWorkflow(workflowDebugger.getAgentWorkflowBuilder());
            pnlWorkflowInspectorStructure.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorStructure.setWorkflowDebugger(workflowDebugger);

            pnlWorkflowInspectorExecution = new WorkflowInspectorListPane.Execution();
            pnlWorkflowInspectorExecution.setWorkflow(workflowDebugger.getAgentWorkflowBuilder());
            pnlWorkflowInspectorExecution.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorExecution.setWorkflowDebugger(workflowDebugger);

            BasicAction focusInspectorAction = new BasicAction("focusInspector", null, e -> pnlWorkflowInspectorDetails.requestFocus());
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
            UISupport.bindAction(pnlWorkflowInspectorStructure.getListView(),
                    "focusInspector",
                    keyStroke,
                    focusInspectorAction);
            UISupport.bindAction(pnlWorkflowInspectorExecution.getListView(),
                    "focusInspector",
                    keyStroke,
                    focusInspectorAction);

            pnlWorkflowSummaryView = new JEditorPane() {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }
            };
            pnlWorkflowSummaryView.setContentType("text/html");
            pnlWorkflowSummaryView.setEditable(false);
            pnlWorkflowSummaryView.setFont(pnlWorkflowSummaryView.getFont().deriveFont(15f));
            pnlWorkflowSummary = UISupport.createScrollPane(pnlWorkflowSummaryView, true, false, true, false, true);
            pnlWorkflowSummary.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            JPanel pnlWorkflowContentsHost = new JPanel(new BorderLayout());
            HeaderPane headerPane = new HeaderPane();
            headerPane.setTitle("Workflow");
            headerPane.setSubtitle("Workflow structure, its agents, execution steps, and summary");
            pnlWorkflowContentsHost.add(headerPane, BorderLayout.NORTH);
            setupActions();
            setupMenuBar();
            setupToolbar(headerPane.getToolbar());
            setupPopupMenu();

            pnlWorkflowContents = new JPanel(new CardLayout());
            pnlWorkflowContents.add(pnlWorkflowInspectorStructure, "structure");
            pnlWorkflowContents.add(pnlWorkflowInspectorExecution, "execution");
            pnlWorkflowContents.add(pnlWorkflowSummary, "summary");

            pnlWorkflowContentsHost.add(pnlWorkflowContents, BorderLayout.CENTER);

            JSplitPane pnlWorkflow = new JSplitPane();
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
        } else {
            setContentPane(chatPane);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                getOptions().setFrameBounds(getBounds());
            }
        });
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
                        new BasicAction("About...", new AutoIcon(ICON_HELP), e -> showAbout(chatPane))
                )
        );
    }

    private void setupMenuBarOptionsActionGroup() {
        String exclusiveGroup = "appearance";

        menuBarOptionsActionGroup = new ActionGroup("Options", null, true,
                new ActionGroup("Chat", new AutoIcon(ICON_CHAT), true,
                        chatPane.getRenderMarkdownAction(),
                        chatPane.getClearAfterSendingAction()
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
        ActionGroup toolActionGroup = workflowExpertAction != null ? new ActionGroup(workflowExpertAction)  : null;

        BasicAction chatAction = new BasicAction("Chat", new AutoIcon(ICON_CHAT),e -> chatPane.requestFocus());
        chatAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        BasicAction inspectorAction = new BasicAction("Inspector", new AutoIcon(ICON_INFO),e -> pnlWorkflowInspectorDetails.requestFocus());
        inspectorAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        menuBarViewActionGroup = new ActionGroup("View", null, true,
                new ActionGroup(null, null, false,
                        chatAction                ),
                new ActionGroup(null, null, false,
                        showStructureAction,
                        showExecutionAction,
                        showSummaryAction
                ),
                new ActionGroup(null, null, false,
                        inspectorAction                ),
                toolActionGroup
        );
    }

    private void setupMenuBarEditActionGroup() {
        BasicAction cutAction = new BasicAction("Cut", new AutoIcon(ICON_CUT), aActionEvent -> cut());
        cutAction.setMnemonic('x');
        cutAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        BasicAction copyAction = new BasicAction("Copy", new AutoIcon(ICON_COPY), aActionEvent -> copy());
        copyAction.setMnemonic('c');
        copyAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        BasicAction pasteAction = new BasicAction("Paste", new AutoIcon(ICON_PASTE), aActionEvent -> paste());
        pasteAction.setMnemonic('v');
        pasteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

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
                new ActionGroup("Share", new AutoIcon(ICON_SHARE), true,
                        new ActionGroup(null, null, false,
                                new BasicAction("Chat", null,
                                        e -> shareChat(),
                                        a -> a.setEnabled(! chatPane.getChatMessages().isEmpty()))
                        ),
                        new ActionGroup(null, null, false,
                        new BasicAction("Structure", null, e -> shareStructure()),
                        new BasicAction("Execution", null,
                                e -> shareExecution(),
                                a -> a.setEnabled(pnlWorkflowInspectorExecution.hasContent())),
                        new BasicAction("Summary", null,
                                e -> shareSummary(),
                                a -> a.setEnabled(summaryGenerated))
                        ),
                        new ActionGroup(null, null, false,
                            new BasicAction("Flow Chart", null, e -> shareFlowChart())
                        )
                ),
                isMac() ? null :
                        new ActionGroup(null, null, false,
                                new BasicAction("Exit", null, e -> System.exit(0))
                        )
        );
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
            chatPane.copy();
        } else if (focusOwner instanceof JComponent c) {
            Action action = c.getActionMap().get("copy");
            if (action != null)
                action.actionPerformed(new ActionEvent(c, 0, null));
        }
    }

    private void shareChat() {
        if (chatPane.getChatMessages().isEmpty())
            return;

        try {
            shareContent(ChatPane.OBJECT_MAPPER.
                            enable(SerializationFeature.INDENT_OUTPUT).
                            writeValueAsString(chatPane.getChatMessages()),
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
        shareContent(workflowDebugger.toString(true),
                PROP_EXECUTION_FILE,
                "workflow-execution.txt");
    }

    private void shareStructure() {
        shareContent(workflowDebugger.getAgentWorkflowBuilder().toJson(),
                PROP_STRUCTURE_FILE,
                "workflow-structure.json");
    }

    private void shareFlowChart() {
        shareContent(workflowDebugger.toHtml(true),
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
            } catch (Exception ex) {
                logger.error("Failed to share flow chart", ex);
            }
        }
    }

    /**
     * Displays a new ChatFrame with the given title, icon, chat engine, and exit behavior.
     *
     * @param title       The title of the chat frame.
     * @param icon        The icon to be displayed for the chat frame.
     * @param chatEngine  A function that takes a user message and returns a chat engine's response.
     * @param exitOnClose If true, the application will exit when this frame is closed.
     * @return The created ChatFrame instance.
     */
    public static ChatFrame showChat(String title, ImageIcon icon, ChatPane.ChatEngine chatEngine, WorkflowDebugger workflowDebugger) {
        applyAppearance();

        ChatFrame chatFrame = new ChatFrame(title,
                icon,
                chatEngine,
                workflowDebugger
        );

        return chatFrame;
    }

    /**
     * Displays a new ChatFrame configured to interact with a WorkflowExpert.
     *
     * @param userMessage      The initial message to be displayed in the chat pane.
     * @param workflowDebugger The WorkflowDebugger instance to be used by the WorkflowExpert.
     * @return The created ChatFrame instance.
     */

    public static ChatFrame showChat(Map<String, Object> userMessage, WorkflowDebugger workflowDebugger) {
        String title = "Chat with Workflow";
        System.setProperty("apple.awt.application.name", title);
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        final WorkflowExpert workflowExpert = WorkflowExpertSupport.getWorkflowExpert(workflowDebugger);
        ChatFrame result = ChatFrame.showChat(title,
                new ImageIcon(ChatFrame.class.getResource("logo.png")),
                new ChatPane.ChatEngine() {
                    private static Method getAskMethod() {
                        return EasyWorkflow.getAgentMethod(WorkflowExpert.class);
                    }

                    @Override
                    public Object send(Map<String, Object> message) {
                        return workflowExpert.askMap(message);
                    }

                    @Override
                    public Parameter[] getMessageParameters() {
                        return getAskMethod().getParameters();
                    }

                    @Override
                    public String getUserMessageTemplate() {
                        return EasyWorkflow.getUserMessageTemplate(WorkflowExpert.class);
                    }

                    @Override
                    public String getSystemMessageTemplate() {
                        return EasyWorkflow.getSystemMessageTemplate(WorkflowExpert.class);
                    }
                }
                ,
                workflowDebugger);
        SwingUtilities.invokeLater(() -> result.getChatPane().setUserMessage(userMessage));

        return result;
    }

    private void setupActions() {
        final String showGroup = "show";
        showStructureAction = new StateAction("Structure", new AutoIcon(ICON_WORKFLOW), showGroup,
                e -> showWorkflowStructure(),
                a -> a.setSelected(pnlWorkflowInspectorStructure.isVisible()));
        showStructureAction.setShortDescription("Show workflow structure");
        showStructureAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        showExecutionAction = new StateAction("Execution", new AutoIcon(ICON_EXECUTION_FLOW), showGroup,
                e -> showWorkflowExecution(),
                a -> {
                    a.setEnabled(pnlWorkflowInspectorExecution.hasContent());
                    a.setSelected(pnlWorkflowInspectorExecution.isVisible());
                });
        showExecutionAction.setShortDescription("Show workflow execution");
        showExecutionAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        showSummaryAction = new StateAction("Summary", new AutoIcon(ICON_DOCUMENT), showGroup,
                e -> showWorkflowSummary(),
                a -> a.setSelected(pnlWorkflowSummary.isVisible()));
        showSummaryAction.setShortDescription("Show workflow summary");
        showSummaryAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        this.copyAction = new BasicAction("Copy", new AutoIcon(ICON_COPY),
                e -> copy(),
                aBasicAction -> {
                    boolean e = true;
                    if (pnlWorkflowSummaryView.isVisible())
                        e = !summaryGenerating;
                    setEnabled(e);
                });
        copyAction.setShortDescription("Copy");

        shareAction = new BasicAction("Share", new AutoIcon(ICON_SHARE), e -> {
            shareFlowChart();
        });
        shareAction.setShortDescription("Share");

        if (workflowDebugger != null) {
            workflowExpertAction = new GUIPlayground.WorkflowExpertAction(new AutoIcon(ICON_AGENT_TOOLBAR),
                    this,
                    workflowDebugger.getAgentWorkflowBuilder().getAgentClass(), workflowDebugger
            );
            workflowExpertAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E,
                    KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            chatPane.setupToolActions(workflowExpertAction);
        }
    }

    private void setupToolbar(JToolBar toolbar) {
        inspectorToolbarActionGroup = new ActionGroup(
                showStructureAction,
                showExecutionAction,
                showSummaryAction
        );
        UISupport.setupToolbar(toolbar, inspectorToolbarActionGroup);
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
                )
        );
        JPopupMenu popupMenu = new JPopupMenu();
        UISupport.setupPopupMenu(popupMenu, actionGroup);
        pnlWorkflowInspectorStructure.setComponentPopupMenu(popupMenu);
        pnlWorkflowInspectorExecution.setComponentPopupMenu(popupMenu);
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

    private void showWorkflowSummary() {
        ((CardLayout) pnlWorkflowContents.getLayout()).last(pnlWorkflowContents);
        pnlWorkflowInspectorDetails.setValues(null);
        pnlWorkflowSummaryView.requestFocus();
        if (!summaryGenerated && !summaryGenerating) {
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
        if (! pnlWorkflowInspectorStructure.isVisible())
            ((CardLayout) pnlWorkflowContents.getLayout()).first(pnlWorkflowContents);
        if (pnlWorkflowInspectorStructure.getListView().getSelectedIndex() == -1)
            pnlWorkflowInspectorStructure.getListView().setSelectedIndex(0);
        else
            pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorStructure.getSelectedData());
        pnlWorkflowInspectorStructure.requestFocus();
    }

    private void showWorkflowExecution() {
        if (! pnlWorkflowInspectorExecution.isVisible())
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

    private final Breakpoint sessionStartedBreakpoint = Breakpoint.builder(
            Breakpoint.Type.SESSION_STARTED,
            (b, m) -> SwingUtilities.invokeLater(() -> showExecutionAction.setEnabled(true)))
            .build();

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
        return chatPane;
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
        // disallow showing second dialog on Mac when invoked via system menu
        if (isMac()) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing())  {
                    return;
                }
            }
        }

        Object[] options = {"Site", "OK"};
        int result = JOptionPane.showOptionDialog(
                parent,
                """
                <html>%s<br>
                <b>%s</b><br>
                Copyright (c) 2025 Gregory Ledenev <i>(gregory.ledenev37@gmail.com)</i></html>""".formatted(EasyWorkflow.PROJECT_NAME, EasyWorkflow.VERSION),
                "About",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,    // Icon, or null
                options, // Custom button labels
                options[1] // Default selected
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
        menuBarActionGroup.update();
        inspectorToolbarActionGroup.update();
        chatPane.scheduledUpdate();
    }
}
