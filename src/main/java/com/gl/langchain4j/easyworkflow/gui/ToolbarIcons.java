package com.gl.langchain4j.easyworkflow.gui;

import com.gl.appframework.UISupport;

public class ToolbarIcons {
    public static final String ICON_AGENT_TOOLBAR = "agent-toolbar";
    public static final String ICON_EXPERT_TOOLBAR = "expert-toolbar";
    public static final String ICON_EXECUTION_FLOW = "execution-flow";
    public static final String ICON_WORKFLOW = "workflow";
    public static final String ICON_PROMPTS = "prompts";

    public static void loadIcons()  {
        loadIcon(ICON_AGENT_TOOLBAR, "icons/toolbar/agent");
        loadIcon(ICON_EXPERT_TOOLBAR, "icons/toolbar/expert");
        loadIcon(ICON_EXECUTION_FLOW, "icons/toolbar/execution-flow");
        loadIcon(ICON_WORKFLOW, "icons/toolbar/workflow");
        loadIcon(ICON_PROMPTS, "icons/toolbar/prompts");
    }

    public static void loadIcon(String iconKey, String fileName) {
        loadIcon(iconKey, fileName, false);
    }

    public static void loadIcon(String iconKey, String fileName, boolean preserveOriginal) {
        UISupport.loadIcon(ToolbarIcons.class, iconKey, fileName, preserveOriginal);
    }
}
