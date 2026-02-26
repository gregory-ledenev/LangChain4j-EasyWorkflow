package com.gl.langchain4j.easyworkflow.gui;

import com.gl.appframework.IconFactory;

import javax.swing.*;
import java.util.Objects;

@SuppressWarnings("ALL")
public class Icons {
    public static final String ICON_AGENT = "icons/agent";
    public static final String ICON_SIGNPOST = "icons/signpost";
    public static final String ICON_REFRESH = "icons/refresh";
    public static final String ICON_BOX = "icons/box";
    public static final String ICON_STACK = "icons/stack";
    public static final String ICON_TARGET = "icons/target";
    public static final String ICON_BREAKPOINT = "icons/breakpoint";
    public static final String ICON_PLAY = "icons/play";
    public static final String ICON_STOP = "icons/stop";
    public static final String ICON_SEND = "icons/send";
    public static final String ICON_WRENCH = "icons/wrench";
    public static final String ICON_QUESTION = "icons/question";
    public static final String ICON_HOME = "icons/home";
    public static final String ICON_HOME_PLAIN = "icons/home-plain";
    public static final String ICON_BELL = "icons/bell";
    public static final String ICON_PLAYGOUND = "icons/playground";

    public static void loadIcons() {
        loadIcon(ICON_AGENT, ICON_AGENT);
        loadIcon(ICON_SIGNPOST, ICON_SIGNPOST);
        loadIcon(ICON_REFRESH, ICON_REFRESH);
        loadIcon(ICON_BOX, ICON_BOX);
        loadIcon(ICON_STACK, ICON_STACK);
        loadIcon(ICON_TARGET, ICON_TARGET);
        loadIcon(ICON_BREAKPOINT, ICON_BREAKPOINT);
        loadIcon(ICON_PLAY, ICON_PLAY);
        loadIcon(ICON_STOP, ICON_STOP);
        loadIcon(ICON_SEND, ICON_SEND);
        loadIcon(ICON_WRENCH, ICON_WRENCH);
        loadIcon(ICON_QUESTION, ICON_QUESTION);
        loadIcon(ICON_HOME, ICON_HOME);
        loadIcon(ICON_HOME_PLAIN, ICON_HOME_PLAIN);
        loadIcon(ICON_BELL, ICON_BELL);
        loadIcon(ICON_PLAYGOUND, ICON_PLAYGOUND);
    }

    public static void loadIcon(String iconKey, String fileName) {
        IconFactory.loadIcon(Icons.class, iconKey);
    }

    public static final ImageIcon LOGO_ICON = new ImageIcon(Objects.requireNonNull(ChatScreen.class.getResource("icons/logo.png")));
}
