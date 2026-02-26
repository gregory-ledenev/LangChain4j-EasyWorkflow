package com.gl.langchain4j.easyworkflow.gui;

import com.gl.saf.*;
import com.gl.saf.IconFactory.AutoIcon;
import com.gl.saf.actions.ActionGroup;
import com.gl.langchain4j.easyworkflow.Version;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatPane;
import com.gl.langchain4j.easyworkflow.playground.PlaygroundContext;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.gl.langchain4j.easyworkflow.gui.PlaygroundIcons.LOGO_ICON;

public class ChatFrame extends AppFrame {

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

        BasicAppScreen<ChatFrame> testScreen = new BasicAppScreen<>("test", "Test", new AutoIcon(PlaygroundIcons.ICON_HOME), "Test screen");
        installAppModule(testScreen);
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

    private static final List<AboutProvider.AboutLink> ABOUT_LINKS = List.of(
            new AboutProvider.AboutLink("EasyWorkflow for LangChain4j", "https://github.com/gregory-ledenev/LangChain4j-EasyWorkflow"),
            new AboutProvider.AboutLink("LangChain4j", "https://docs.langchain4j.dev/")
    );

    /**
     * Creates an {@link AboutProvider} that displays information about the application.
     *
     * @return A new instance of AboutProvider.
     */
    public static AboutProvider createAboutProvider() {
        return new AboutProvider() {
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
                    openLink(getAboutLinks().get(0));
                }
            }

            @Override
            public List<AboutLink> getAboutLinks() {
                return ABOUT_LINKS;
            }
        };
    }
}
