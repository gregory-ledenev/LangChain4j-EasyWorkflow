package com.gl.langchain4j.easyworkflow.gui;

import com.gl.appframework.AppFrame;
import com.gl.appframework.BasicAppScreen;
import com.gl.appframework.MenuBarModule;
import com.gl.appframework.UISupport;
import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.actions.BasicAction;
import com.gl.langchain4j.easyworkflow.Version;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.gl.langchain4j.easyworkflow.playground.PlaygroundContext;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

import static com.gl.appframework.ToolbarIcons.ICON_GLOBE;
import static com.gl.appframework.ToolbarIcons.ICON_HELP;
import static com.gl.langchain4j.easyworkflow.gui.Icons.LOGO_ICON;

public class ChatFrame extends AppFrame implements UISupport.AboutProvider {

    private final ChatScreen chatScreen;

    /**
     * Constructs a new ChatFrame.
     *
     * @param title            The title of the chat frame.
     * @param icon             The icon to be displayed for the chat frame.
     * @param chatEngine       A function that takes a user message and returns a chat engine's response.
     * @param agent            The agent object associated with this chat frame.
     * @param workflowDebugger The workflow debugger to be used for inspecting workflows.
     */
    public ChatFrame(String title, ImageIcon icon,
                      ChatPane.ChatEngine chatEngine,
                      PlaygroundContext playgroundContext,
                      Object agent,
                      WorkflowDebugger workflowDebugger) {
        super("chatFrame");

        setTitle(title);

        if (icon != null) {
            setIconImage(icon.getImage());
            Taskbar.getTaskbar().setIconImage(icon.getImage());
        }

        setSize(workflowDebugger != null ? 1280 : 500, 720);
        setMinimumSize(new Dimension(500, 700));
        setLocationRelativeTo(null);

        setupMenuBarHelpActionGroup();
        getAppScreenManager().ifPresent(asm -> getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW).addAction(asm.getAppScreenManagerActionGroup()));

        installAppModule(new MenuBarModule<ChatFrame>(new ActionGroup(
                getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_FILE),
                getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_EDIT),
                getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_VIEW),
                getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_OPTIONS),
                getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_HELP)
        )));

        chatScreen = new ChatScreen(chatEngine, playgroundContext, agent, workflowDebugger);
        installAppModule(chatScreen);
        installAppModule(new BasicAppScreen<ChatFrame>("test", "Test", new UISupport.AutoIcon(Icons.ICON_BELL), "Test screen"));
    }

    WorkflowDebugger getWorkflowDebugger() {
        return chatScreen.getWorkflowDebugger();
    }

    ChatPromptsStorage getChatPromptsStorage() {
        return chatScreen.getChatPromptsStorage();
    }

    ChatPane getChatPane() {
        return chatScreen.getChatPane();
    }

    /**
     * Creates a new chat frame with the given parameters.
     *
     * @param title            The title of the chat frame.
     * @param icon             The icon to be displayed for the chat frame.
     * @param chatEngine       A function that takes a user message and returns a chat engine's response.
     * @param agent            The agent object associated with this chat frame.
     * @param workflowDebugger The workflow debugger to be used for inspecting workflows.
     * @return The newly created and displayed ChatFrame instance.
     */
    public static ChatFrame createChatFrame(String title, ImageIcon icon,
                                             ChatPane.ChatEngine chatEngine,
                                             PlaygroundContext playgroundContext,
                                             Object agent,
                                             WorkflowDebugger workflowDebugger) {

        return new ChatFrame(title,
                icon,
                chatEngine,
                playgroundContext,
                agent,
                workflowDebugger
        );
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
                        Copyright © 2025-2026 Gregory Ledenev <i>(gregory.ledenev37@gmail.com)</i></html>""".formatted(
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

    private void setupMenuBarHelpActionGroup() {
        getMenuBarActionGroup(AppFrame.MENUBAR_ACTION_GROUP_HELP).addAction(0,
                new ActionGroup(null, null, false,
                        new BasicAction("Visit 'EasyWorkflow for LangChain4j'", new UISupport.AutoIcon(ICON_GLOBE), e -> visitSite()),
                        new BasicAction("Visit 'LangChain4j'", new UISupport.AutoIcon(ICON_GLOBE), e -> visitSite("https://docs.langchain4j.dev/"))
                )
        );
    }
}
