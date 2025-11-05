package com.gl.langchain4j.easyworkflow.gui;

import static com.gl.langchain4j.easyworkflow.gui.UISupport.ICON_REFRESH;
import static com.gl.langchain4j.easyworkflow.gui.UISupport.loadIcon;

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
    public static final String ICON_SEND = "send";
    public static final String ICON_EXPAND = "expand";
    public static final String ICON_COLLAPSE = "collapse";
    public static final String ICON_ALWAYS_EXPAND = "always-expand";
    public static final String ICON_TOOLBAR_REFRESH = "toolbar-refresh";
    public static final String ICON_TOOLBAR_PLAY = "toolbar-play";

    public static int test = 1;
    static {
        loadIcon(ICON_COPY, "toolbar/copy");
        loadIcon(ICON_PASTE, "toolbar/paste");
        loadIcon(ICON_CUT, "toolbar/cut");
        loadIcon(ICON_SHARE, "toolbar/share");
        loadIcon(ICON_INFO, "toolbar/info");
        loadIcon(ICON_INFO_PLAIN, "toolbar/info-plain");
        loadIcon(ICON_CHAT, "toolbar/chat");
        loadIcon(ICON_DOCUMENT, "toolbar/document");
        loadIcon(ICON_EXECUTION_FLOW, "toolbar/execution-flow");
        loadIcon(ICON_WORKFLOW, "toolbar/workflow");
        loadIcon(ICON_AGENT_TOOLBAR, "toolbar/agent");
        loadIcon(ICON_HELP, "toolbar/help");
        loadIcon(ICON_GLOBE, "toolbar/globe");
        loadIcon(ICON_SEND, "toolbar/send");
        loadIcon(ICON_EXPAND, "toolbar/expand");
        loadIcon(ICON_COLLAPSE, "toolbar/collapse");
        loadIcon(ICON_ALWAYS_EXPAND, "toolbar/always-expand");
        loadIcon(ICON_TOOLBAR_REFRESH, "toolbar/refresh");
        loadIcon(ICON_TOOLBAR_PLAY, "toolbar/play-toolbar");
    }
}
