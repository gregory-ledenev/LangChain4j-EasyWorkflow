package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    public static final String ICON_WRENCH = "wrench";


    public static void loadIcons() {
        loadIcon(ICON_EXPERT, "icons/expert");
        loadIcon(ICON_SIGNPOST, "icons/signpost");
        loadIcon(ICON_REFRESH, "icons/refresh");
        loadIcon(ICON_BOX, "icons/box");
        loadIcon(ICON_STACK, "icons/stack");
        loadIcon(ICON_TARGET, "icons/target");
        loadIcon(ICON_BREAKPOINT, "icons/breakpoint");
        loadIcon(ICON_PLAY, "icons/play");
        loadIcon(ICON_STOP, "icons/stop");
        loadIcon(ICON_SPACER, "icons/spacer");
        loadIcon(ICON_SEND, "icons/send");
        loadIcon(ICON_WRENCH, "icons/wrench");
    }

    public static void loadIcon(String iconKey, String fileName) {
        UISupport.loadIcon(Icons.class, iconKey, fileName);
    }

    public static final ImageIcon LOGO_ICON = new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource("icons/logo.png")));
}
