package com.gl.appframework;

public class ToolbarIcons {
    public static final String ICON_CHAT = "icons/toolbar/chat";
    public static final String ICON_COPY = "icons/toolbar/copy";
    public static final String ICON_PASTE = "icons/toolbar/paste";
    public static final String ICON_CUT = "icons/toolbar/cut";
    public static final String ICON_SHARE = "icons/toolbar/share";
    public static final String ICON_INFO = "icons/toolbar/info";
    public static final String ICON_INFO_PLAIN = "icons/toolbar/info-plain";
    public static final String ICON_DOCUMENT = "icons/toolbar/document";
    public static final String ICON_HELP = "icons/toolbar/help";
    public static final String ICON_GLOBE = "icons/toolbar/globe";
    public static final String ICON_TOOLBAR_SEND = "icons/toolbar/send";
    public static final String ICON_EXPAND = "icons/toolbar/expand";
    public static final String ICON_COLLAPSE = "icons/toolbar/collapse";
    public static final String ICON_ALWAYS_EXPAND = "icons/toolbar/always-expand";
    public static final String ICON_TOOLBAR_REFRESH = "icons/toolbar/refresh";
    public static final String ICON_TOOLBAR_PLAY = "icons/toolbar/play-toolbar";
    public static final String ICON_TIMER = "icons/toolbar/timer";
    public static final String ICON_FILING_CABINET = "icons/toolbar/filing-cabinet";
    public static final String ICON_COMPOSE = "icons/toolbar/compose";
    public static final String ICON_PLUS = "icons/toolbar/plus";
    public static final String ICON_CLOSE = "icons/toolbar/close";
    public static final String ICON_DOWN = "icons/toolbar/down";
    public static final String ICON_UP = "icons/toolbar/up";
    public static final String ICON_PIN = "icons/toolbar/pin";
    public static final String ICON_DELETE = "icons/toolbar/delete";
    public static final String ICON_SPACER = "icons/toolbar/spacer";

    public static void loadIcons()  {
        loadIcon(ICON_CHAT);
        loadIcon(ICON_COPY);
        loadIcon(ICON_PASTE);
        loadIcon(ICON_CUT);
        loadIcon(ICON_SHARE);
        loadIcon(ICON_INFO);
        loadIcon(ICON_INFO_PLAIN);
        loadIcon(ICON_DOCUMENT);
        loadIcon(ICON_HELP);
        loadIcon(ICON_GLOBE);
        loadIcon(ICON_TOOLBAR_SEND);
        loadIcon(ICON_EXPAND);
        loadIcon(ICON_COLLAPSE);
        loadIcon(ICON_ALWAYS_EXPAND);
        loadIcon(ICON_TOOLBAR_REFRESH);
        loadIcon(ICON_TOOLBAR_PLAY);
        loadIcon(ICON_TIMER);
        loadIcon(ICON_FILING_CABINET);
        loadIcon(ICON_COMPOSE);
        loadIcon(ICON_PLUS);
        loadIcon(ICON_CLOSE);
        loadIcon(ICON_DOWN);
        loadIcon(ICON_UP);
        loadIcon(ICON_PIN);
        loadIcon(ICON_DELETE, true);
        loadIcon(ICON_SPACER);
    }

    public static void loadIcon(String iconKey) {
        loadIcon(iconKey, false);
    }

    public static void loadIcon(String iconKey, boolean preserveOriginal) {
        IconFactory.loadIcon(ToolbarIcons.class, iconKey, preserveOriginal);
    }
}