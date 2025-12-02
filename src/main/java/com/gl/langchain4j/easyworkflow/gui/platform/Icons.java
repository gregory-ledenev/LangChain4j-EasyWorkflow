package com.gl.langchain4j.easyworkflow.gui.platform;

public class Icons {
    public static final String ICON_NOTIFICATION_INFORMATION = "notification-information";
    public static final String ICON_NOTIFICATION_WARNING = "notification-warning";
    public static final String ICON_NOTIFICATION_ERROR = "notification-error";
    public static final String ICON_NOTIFICATION_SUCCESS = "notification-success";

    public static void loadIcons()  {
        loadIcon(ICON_NOTIFICATION_SUCCESS, "icons/notification-success");
        loadIcon(ICON_NOTIFICATION_INFORMATION, "icons/notification-information");
        loadIcon(ICON_NOTIFICATION_WARNING, "icons/notification-warning");
        loadIcon(ICON_NOTIFICATION_ERROR, "icons/notification-error");
    }

    public static void loadIcon(String iconKey, String fileName) {
        UISupport.loadIcon(Icons.class, iconKey, fileName, true);
    }
}
