package com.gl.langchain4j.easyworkflow.gui;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.loadIcon;

@SuppressWarnings("ALL")
public class Icons {
    private static final Map<String, ImageIcon> icons = new HashMap<>();

    public static final String ICON_EXPERT = "expert";
    public static final String ICON_SIGNPOST = "signpost";
    public static final String ICON_REFRESH = "refresh";
    public static final String ICON_BOX = "box";
    public static final String ICON_STACK = "stack";
    public static final String ICON_TARGET = "target";
    public static final String ICON_BREAKPOINT = "breakpoint";
    public static final String ICON_PLAY = "play";
    public static final String ICON_STOP = "stop";
    public static final String ICON_SPACER = "spacer";
    public static final String ICON_SEND = "send";

    public static void loadIcons() {
        loadIcon(ICON_EXPERT, "../expert");
        loadIcon(ICON_SIGNPOST, "../signpost");
        loadIcon(ICON_REFRESH, "../refresh");
        loadIcon(ICON_BOX, "../box");
        loadIcon(ICON_STACK, "../stack");
        loadIcon(ICON_TARGET, "../target");
        loadIcon(ICON_BREAKPOINT, "../breakpoint");
        loadIcon(ICON_PLAY, "../play");
        loadIcon(ICON_STOP, "../stop");
        loadIcon(ICON_SPACER, "../spacer");
        loadIcon(ICON_SEND, "../send");
    }

    public static final ImageIcon LOGO_ICON = new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource("logo.png")));
}
