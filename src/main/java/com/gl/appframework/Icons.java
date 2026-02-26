package com.gl.appframework;

public class Icons {
    public static final String ICON_NOTIFICATION_INFORMATION = "icons/notification-success";
    public static final String ICON_NOTIFICATION_WARNING = "icons/notification-information";
    public static final String ICON_NOTIFICATION_ERROR = "icons/notification-warning";
    public static final String ICON_NOTIFICATION_SUCCESS = "icons/notification-error";

    public static void loadIcons()  {
        loadIcon(ICON_NOTIFICATION_SUCCESS);
        loadIcon(ICON_NOTIFICATION_INFORMATION);
        loadIcon(ICON_NOTIFICATION_WARNING);
        loadIcon(ICON_NOTIFICATION_ERROR);
    }

    public static void loadIcon(String iconKey) {
        IconFactory.loadIcon(Icons.class, iconKey, true);
    }
}
