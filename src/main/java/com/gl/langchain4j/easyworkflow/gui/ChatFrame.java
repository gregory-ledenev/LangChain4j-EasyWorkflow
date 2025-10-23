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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.gl.langchain4j.easyworkflow.gui.UISupport.applyAppearance;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.getOptions;

/**
 * A frame that provides a chat interface. It can be used to display a chat conversation and interact with a chat
 * engine.
 */
public class ChatFrame extends JFrame implements UISupport.AboutProvider {

    private final ChatPane chatPane = new ChatPane();
    private WorkflowDebugger workflowDebugger;
    private WorkflowInspectorDetailsPane pnlWorkflowInspectorDetails;
    private WorkflowInspectorListPane pnlWorkflowInspectorList;
    private boolean exitOnClose;

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
            setSize(workflowDebugger != null ? 1200 : 500, 700);
            setLocationRelativeTo(null);
        }
        chatPane.setPreferredSize(new Dimension(400, 700));
        setMinimumSize(new Dimension(500, 700));

        if (icon != null) {
            setIconImage(icon.getImage());
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }

        if (workflowDebugger != null) {
            JSplitPane contentPane = new JSplitPane();
            contentPane.setResizeWeight(0);
            contentPane.setLeftComponent(chatPane);
            pnlWorkflowInspectorList = new WorkflowInspectorListPane();
            pnlWorkflowInspectorList.setWorkflow(workflowDebugger.getAgentWorkflowBuilder());
            pnlWorkflowInspectorList.setPreferredSize(new Dimension(400, 700));
            pnlWorkflowInspectorList.setWorkflowDebugger(workflowDebugger);

            JSplitPane pnlWorkflow = new JSplitPane();
            pnlWorkflow.setLeftComponent(pnlWorkflowInspectorList);
            pnlWorkflowInspectorDetails = new WorkflowInspectorDetailsPane();
            pnlWorkflow.setRightComponent(pnlWorkflowInspectorDetails);
            pnlWorkflow.setResizeWeight(0.5);
            contentPane.setRightComponent(pnlWorkflow);
            setContentPane(contentPane);

            pnlWorkflowInspectorList.getList().addListSelectionListener(e -> {
                pnlWorkflowInspectorDetails.setValues(pnlWorkflowInspectorList.getSelectedData());
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

    public WorkflowDebugger getWorkflowDebugger() {
        return workflowDebugger;
    }

    public void setWorkflowDebugger(WorkflowDebugger workflowDebugger) {
        this.workflowDebugger = workflowDebugger;
        if (pnlWorkflowInspectorList != null)
            pnlWorkflowInspectorList.setWorkflowDebugger(workflowDebugger);
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
