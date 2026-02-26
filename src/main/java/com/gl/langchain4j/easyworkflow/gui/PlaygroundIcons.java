package com.gl.langchain4j.easyworkflow.gui;

import com.gl.saf.IconFactory;

import javax.swing.*;
import java.util.Objects;

@SuppressWarnings("ALL")
public class PlaygroundIcons {
    public static final String ICON_AGENT = "icons/agent";
    public static final String ICON_SIGNPOST = "icons/signpost";
    public static final String ICON_BOX = "icons/box";
    public static final String ICON_STACK = "icons/stack";
    public static final String ICON_TARGET = "icons/target";
    public static final String ICON_BREAKPOINT = "icons/breakpoint";
    public static final String ICON_STOP = "icons/stop";
    public static final String ICON_SEND = "icons/send";
    public static final String ICON_WRENCH = "icons/wrench";
    public static final String ICON_QUESTION = "icons/question";
    public static final String ICON_HOME = "icons/home";
    public static final String ICON_BELL = "icons/bell";
    public static final String ICON_PLAYGOUND = "icons/playground";
    public static final String ICON_WORKFLOW = "icons/workflow";
    public static final String ICON_EXECUTION_FLOW = "icons/execution-flow";
    public static final String ICON_PROMPTS = "icons/prompts";

    public static void loadIcons() {
        loadIcon(ICON_AGENT);
        loadIcon(ICON_SIGNPOST);
        loadIcon(ICON_BOX);
        loadIcon(ICON_STACK);
        loadIcon(ICON_TARGET);
        loadIcon(ICON_BREAKPOINT);
        loadIcon(ICON_STOP);
        loadIcon(ICON_SEND);
        loadIcon(ICON_WRENCH);
        loadIcon(ICON_QUESTION);
        loadIcon(ICON_HOME);
        loadIcon(ICON_BELL);
        loadIcon(ICON_PLAYGOUND);
        loadIcon(ICON_WORKFLOW);
        loadIcon(ICON_PROMPTS);
        loadIcon(ICON_EXECUTION_FLOW);
    }

    public static void loadIcon(String iconKey) {
        IconFactory.loadIcon(PlaygroundIcons.class, iconKey);
    }

    public static final ImageIcon LOGO_ICON = new ImageIcon(Objects.requireNonNull(ChatScreen.class.getResource("icons/logo.png")));
}
