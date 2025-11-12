package com.gl.langchain4j.easyworkflow.gui;

import com.gl.langchain4j.easyworkflow.gui.platform.UISupport;

import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.loadIcon;

public class ToolbarIcons {
    public static final String ICON_COPY = "copy";
    public static final String ICON_PASTE = "paste";
    public static final String ICON_CUT = "cut";
    public static final String ICON_SHARE = "share";
    public static final String ICON_INFO = "info";
    public static final String ICON_INFO_PLAIN = "info-plain";
    public static final String ICON_CHAT = "chat";
    public static final String ICON_DOCUMENT = "document";
    public static final String ICON_EXECUTION_FLOW = "execution-flow";
    public static final String ICON_WORKFLOW = "workflow";
    public static final String ICON_AGENT_TOOLBAR = "agent-toolbar";
    public static final String ICON_HELP = "help";
    public static final String ICON_GLOBE = "globe";
    public static final String ICON_TOOLBAR_SEND = "toolbar-send";
    public static final String ICON_EXPAND = "expand";
    public static final String ICON_COLLAPSE = "collapse";
    public static final String ICON_ALWAYS_EXPAND = "always-expand";
    public static final String ICON_TOOLBAR_REFRESH = "toolbar-refresh";
    public static final String ICON_TOOLBAR_PLAY = "toolbar-play";
    public static final String ICON_TIMER = "timer";
    public static final String ICON_FILING_CABINET = "filling-cabinet";

    public static void loadIcons()  {
        loadIcon(ICON_COPY, "icons/toolbar/copy");
        loadIcon(ICON_PASTE, "icons/toolbar/paste");
        loadIcon(ICON_CUT, "icons/toolbar/cut");
        loadIcon(ICON_SHARE, "icons/toolbar/share");
        loadIcon(ICON_INFO, "icons/toolbar/info");
        loadIcon(ICON_INFO_PLAIN, "icons/toolbar/info-plain");
        loadIcon(ICON_CHAT, "icons/toolbar/chat");
        loadIcon(ICON_DOCUMENT, "icons/toolbar/document");
        loadIcon(ICON_EXECUTION_FLOW, "icons/toolbar/execution-flow");
        loadIcon(ICON_WORKFLOW, "icons/toolbar/workflow");
        loadIcon(ICON_AGENT_TOOLBAR, "icons/toolbar/agent");
        loadIcon(ICON_HELP, "icons/toolbar/help");
        loadIcon(ICON_GLOBE, "icons/toolbar/globe");
        loadIcon(ICON_TOOLBAR_SEND, "icons/toolbar/send");
        loadIcon(ICON_EXPAND, "icons/toolbar/expand");
        loadIcon(ICON_COLLAPSE, "icons/toolbar/collapse");
        loadIcon(ICON_ALWAYS_EXPAND, "icons/toolbar/always-expand");
        loadIcon(ICON_TOOLBAR_REFRESH, "icons/toolbar/refresh");
        loadIcon(ICON_TOOLBAR_PLAY, "icons/toolbar/play-toolbar");
        loadIcon(ICON_TIMER, "icons/toolbar/timer");
        loadIcon(ICON_FILING_CABINET, "icons/toolbar/filing-cabinet");
    }

    public static void loadIcon(String iconKey, String fileName) {
        UISupport.loadIcon(ToolbarIcons.class, iconKey, fileName);
    }
}
