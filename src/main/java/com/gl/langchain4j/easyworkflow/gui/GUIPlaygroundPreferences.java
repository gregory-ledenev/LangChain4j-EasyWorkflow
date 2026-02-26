package com.gl.langchain4j.easyworkflow.gui;

import com.gl.saf.Application;
import com.gl.saf.ApplicationPreferences;

public class GUIPlaygroundPreferences extends ApplicationPreferences {

    public static final String PROP_CLEAR_AFTER_SENDING = "clearAfterSending";
    public static final String PROP_RENDER_MARKDOWN = "renderMarkdown";


    /**
     * Retrieves the application preferences instance for the GUI Playground.
     *
     * @return the current {@link GUIPlaygroundPreferences} instance.
     */
    public static GUIPlaygroundPreferences getApplicationPreferences() {
        return ((GUIPlayground.PlaygroudApplication) Application.getSharedApplication()).getApplicationPreferences();
    }

    /**
     * Checks if markdown rendering is enabled.
     *
     * @return true if markdown rendering is enabled, false otherwise.
     */
    public boolean isRenderMarkdown() {
        return getPreferences().getBoolean(PROP_RENDER_MARKDOWN, true);
    }

    /**
     * Sets whether markdown rendering should be enabled.
     *
     * @param renderMarkdown true to enable markdown rendering, false to disable.
     */
    public void setRenderMarkdown(boolean renderMarkdown) {
        if (isRenderMarkdown() != renderMarkdown) {
            getPreferences().putBoolean(PROP_RENDER_MARKDOWN, renderMarkdown);
            propertyChangeSupport.firePropertyChange(PROP_RENDER_MARKDOWN, !renderMarkdown, renderMarkdown);
        }
    }

    /**
     * Checks if the "clear after sending" option is enabled.
     *
     * @return true if the user message form should be cleared after sending, false otherwise.
     */
    public boolean isClearAfterSending() {
        return getPreferences().getBoolean(PROP_CLEAR_AFTER_SENDING, true);
    }

    /**
     * Sets whether the user message form should be cleared after sending.
     *
     * @param isCleaAfterSending true to enable clearing after sending, false to disable.
     */
    public void setClearAfterSending(boolean isCleaAfterSending) {
        if (isClearAfterSending() != isCleaAfterSending) {
            getPreferences().putBoolean(PROP_CLEAR_AFTER_SENDING, isCleaAfterSending);
            propertyChangeSupport.firePropertyChange(PROP_CLEAR_AFTER_SENDING, !isCleaAfterSending, isCleaAfterSending);
        }
    }
}
