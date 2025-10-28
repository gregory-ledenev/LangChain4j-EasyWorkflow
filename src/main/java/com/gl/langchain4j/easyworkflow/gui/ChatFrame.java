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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.gl.langchain4j.easyworkflow.gui.UISupport.*;

/**
 * A frame that provides a chat interface. It can be used to display a chat conversation and interact with a chat
 * engine.
 */
public class ChatFrame extends JFrame implements UISupport.AboutProvider {

    public static final String PROP_FLOW_CHART_FILE = "flow-chart-file";
    private static final Logger logger = LoggerFactory.getLogger(ChatFrame.class);
    private final ChatPane chatPane = new ChatPane();
    private JEditorPane pnlWorkflowSummaryView;
    private JPanel pnlWorkflowContents;
    private WorkflowDebugger workflowDebugger;
    private WorkflowInspectorDetailsPane pnlWorkflowInspectorDetails;
    private WorkflowInspectorListPane pnlWorkflowInspectorStructure;
    private WorkflowInspectorListPane pnlWorkflowInspectorExecution;
    private boolean exitOnClose;
    private Actions.BasicAction copyAction;
    private FileChooserUtils fileChooserUtils;
    private boolean summaryGenerated = false;
    private boolean summaryGenerating = false;

    /**
     * Constructs a new ChatFrame.
     *
     * @param title      The title of the chat frame.
     * @param icon       The icon to be displayed for the chat frame.
     * @param chatEngine A function that takes a user message and returns a chat engine's response.
     */
    public ChatFrame(String title, ImageIcon icon, ChatPane.ChatEngine chatEngine, WorkflowDebugger workflowDebugger) {
        setWorkflowDebugger(workflowDebugger);
        this.chatPane.setChatEngine(chatEngine);

        setTitle(title);

        Rectangle frameBounds = getOptions().getFrameBounds();
        if (frameBounds != null) {
            setBounds(frameBounds);
        } else {
            setSize(workflowDebugger != null ? 1080 : 500, 700);
            setLocationRelativeTo(null);
        }
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

            pnlWorkflowSummaryView = new JEditorPane() {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }
            };
            pnlWorkflowSummaryView.setContentType("text/html");
            pnlWorkflowSummaryView.setEditable(false);
            pnlWorkflowSummaryView.setFont(pnlWorkflowSummaryView.getFont().deriveFont(15f));
            JScrollPane pnlWorkflowSummary = UISupport.createScrollPane(pnlWorkflowSummaryView, true, false, true, false, true);
            pnlWorkflowSummary.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            JPanel pnlWorkflowContentsHost = new JPanel(new BorderLayout());
            HeaderPane headerPane = new HeaderPane();
            headerPane.setTitle("Workflow");
//            headerPane.setSubtitle("Workflow structure, summary, its agents and execution steps");
            pnlWorkflowContentsHost.add(headerPane, BorderLayout.NORTH);
            setupToolbar(headerPane.getToolbar());

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

            pnlWorkflowInspectorStructure.getList().addListSelectionListener(e -> {
                if (pnlWorkflowInspectorStructure.isVisible())
                    pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorStructure.getSelectedData());
            });
            pnlWorkflowInspectorExecution.getList().addListSelectionListener(e -> {
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
                if (exitOnClose)
                    System.exit(0);
            }
        });
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
    public static ChatFrame showChat(String title, ImageIcon icon, ChatPane.ChatEngine chatEngine, WorkflowDebugger workflowDebugger, boolean exitOnClose) {
        applyAppearance();

        ChatFrame chatFrame = new ChatFrame(title,
                icon,
                chatEngine,
                workflowDebugger
        );
        chatFrame.exitOnClose = exitOnClose;
        chatFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        SwingUtilities.invokeLater(() -> {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((e, response) -> {
                    getOptions().setFrameBounds(chatFrame.getBounds());
                    response.performQuit();
                });
            }

            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(e -> chatFrame.showAbout(chatFrame.getChatPane()));
            }
            chatFrame.setVisible(true);
        });

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
                workflowDebugger,
                true);
        SwingUtilities.invokeLater(() -> result.getChatPane().setUserMessage(userMessage));

        return result;
    }

    private void setupToolbar(JToolBar toolbar) {
        final String showGroup = "show";
        Actions.StateAction showStructureAction = new Actions.StateAction("Structure", new AutoIcon(ICON_WORKFLOW), e -> {
            showWorkflowStructure();
        });
//            showStructureAction.setCopyName(true);
        showStructureAction.setShortDescription("Show workflow structure");
        showStructureAction.setExclusiveGroup(showGroup);
        showStructureAction.setSelected(true);

        Actions.StateAction showExecutionAction = new Actions.StateAction("Execution", new AutoIcon(ICON_EXECUTION_FLOW), e -> {
            showWorkflowExecution();
        });
//            showExecutionAction.setCopyName(true);
        showExecutionAction.setShortDescription("Show workflow execution");
        showExecutionAction.setExclusiveGroup(showGroup);

        Actions.StateAction showSummaryAction = new Actions.StateAction("Summary", new AutoIcon(ICON_DOCUMENT), e -> {
            showWorkflowSummary();
        });
//            showSummaryAction.setCopyName(true);
        showSummaryAction.setShortDescription("Show workflow summary");
        showSummaryAction.setExclusiveGroup(showGroup);

        this.copyAction = new Actions.BasicAction("Copy", new AutoIcon(ICON_COPY), e -> {
            copy();
        });
        copyAction.setShortDescription("Copy");

        Actions.BasicAction shareAction = new Actions.BasicAction("Share", new AutoIcon(ICON_SHARE), e -> {
            share();
        });
        shareAction.setShortDescription("Share");

        UISupport.setupToolbar(toolbar,
                new Actions.ActionGroup(
                        new Actions.ActionGroup(
                                showStructureAction,
                                showExecutionAction,
                                showSummaryAction
                        ),
                        new Actions.ActionGroup(
                                copyAction
                        ),
                        new Actions.ActionGroup(
                                shareAction
                        )
                )
        );
    }

    private void share() {
        FileChooserUtils fileChooserUtils = getFileChooserUtils();

        String fileStr = getPreferences().get(PROP_FLOW_CHART_FILE, "workflow.html");
        File file = fileChooserUtils.chooseFileToSave(new File(fileStr), true);
        if (file != null) {
            try {
                getPreferences().put(PROP_FLOW_CHART_FILE, file.getAbsolutePath());
                workflowDebugger.toHtmlFile(file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to share flow chart", ex);
            }
        }
    }

    private FileChooserUtils getFileChooserUtils() {
        if (fileChooserUtils == null) {
            fileChooserUtils = new FileChooserUtils(this);
            fileChooserUtils.setChoosableFileFilters(new FileFilter[]{FileChooserUtils.GenericFileFilter.FILE_FILTER_HTML});
            fileChooserUtils.setUseNativeFileChooser(true);
        }
        return fileChooserUtils;
    }

    /**
     * Copies the currently selected content to the clipboard.
     */
    public void copy() {
        if (pnlWorkflowInspectorStructure.isVisible()) {
            pnlWorkflowInspectorStructure.copy();
        } else if (pnlWorkflowInspectorExecution.isVisible()) {
            pnlWorkflowInspectorExecution.copy();
        } else if (pnlWorkflowSummaryView.isVisible()) {
            UISupport.copy(pnlWorkflowSummaryView);
        }
    }

    private void showWorkflowSummary() {
        ((CardLayout) pnlWorkflowContents.getLayout()).last(pnlWorkflowContents);
        if (!summaryGenerated && !summaryGenerating) {
            summaryGenerating = true;
            updateCopyAction();
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
                            updateCopyAction();
                        });
                    }).exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            pnlWorkflowSummaryView.setText("<html>Error generating summary: %s</html>".formatted(ex.getMessage()));
                            pnlWorkflowSummaryView.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            summaryGenerating = false;
                            ex.printStackTrace();
                            updateCopyAction();
                        });
                        return null; // Return null to complete the exceptionally stage
                    });
        } else {
            copyAction.setEnabled(true);
        }
    }

    private void updateCopyAction() {
        boolean e = true;
        if (pnlWorkflowSummaryView.isVisible())
            e = !summaryGenerating;
        copyAction.setEnabled(e);
    }

    private void showWorkflowStructure() {
        ((CardLayout) pnlWorkflowContents.getLayout()).first(pnlWorkflowContents);
        pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorStructure.getSelectedData());
        pnlWorkflowInspectorStructure.requestFocus();
        updateCopyAction();
    }

    private void showWorkflowExecution() {
        ((CardLayout) pnlWorkflowContents.getLayout()).show(pnlWorkflowContents, "execution");
        pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorExecution.getSelectedData());
        pnlWorkflowInspectorExecution.requestFocus();
        updateCopyAction();
    }

    public WorkflowDebugger getWorkflowDebugger() {
        return workflowDebugger;
    }

    public void setWorkflowDebugger(WorkflowDebugger workflowDebugger) {
        this.workflowDebugger = workflowDebugger;
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
    public void showAbout(JComponent parent) {
        Object[] options = {"Site", "OK"};
        int result = JOptionPane.showOptionDialog(
                null,
                EasyWorkflow.FULL_VERSION,
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
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI("https://github.com/gregory-ledenev/LangChain4j-EasyWorkflow"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
