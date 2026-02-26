/*
 * Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gl.saf;

public class Icons {
    public static final String ICON_NOTIFICATION_INFORMATION = "icons/notification-success";
    public static final String ICON_NOTIFICATION_WARNING = "icons/notification-information";
    public static final String ICON_NOTIFICATION_ERROR = "icons/notification-warning";
    public static final String ICON_NOTIFICATION_SUCCESS = "icons/notification-error";
    public static final String ICON_CHAT = "icons/chat";
    public static final String ICON_COPY = "icons/copy";
    public static final String ICON_PASTE = "icons/paste";
    public static final String ICON_CUT = "icons/cut";
    public static final String ICON_SHARE = "icons/share";
    public static final String ICON_INFO = "icons/info";
    public static final String ICON_INFO_PLAIN = "icons/info-plain";
    public static final String ICON_DOCUMENT = "icons/document";
    public static final String ICON_HELP = "icons/help";
    public static final String ICON_GLOBE = "icons/globe";
    public static final String ICON_SEND = "icons/send";
    public static final String ICON_EXPAND = "icons/expand";
    public static final String ICON_COLLAPSE = "icons/collapse";
    public static final String ICON_ALWAYS_EXPAND = "icons/always-expand";
    public static final String ICON_REFRESH = "icons/refresh";
    public static final String ICON_PLAY = "icons/play";
    public static final String ICON_TIMER = "icons/timer";
    public static final String ICON_FILING_CABINET = "icons/filing-cabinet";
    public static final String ICON_COMPOSE = "icons/compose";
    public static final String ICON_PLUS = "icons/plus";
    public static final String ICON_CLOSE = "icons/close";
    public static final String ICON_DOWN = "icons/down";
    public static final String ICON_UP = "icons/up";
    public static final String ICON_PIN = "icons/pin";
    public static final String ICON_DELETE = "icons/delete";
    public static final String ICON_SPACER = "icons/spacer";

    public static void loadIcons()  {
        loadIcon(ICON_NOTIFICATION_SUCCESS);
        loadIcon(ICON_NOTIFICATION_INFORMATION);
        loadIcon(ICON_NOTIFICATION_WARNING);
        loadIcon(ICON_NOTIFICATION_ERROR);
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
        loadIcon(ICON_SEND);
        loadIcon(ICON_EXPAND);
        loadIcon(ICON_COLLAPSE);
        loadIcon(ICON_ALWAYS_EXPAND);
        loadIcon(ICON_REFRESH);
        loadIcon(ICON_PLAY);
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
        IconFactory.loadIcon(Icons.class, iconKey, preserveOriginal);
    }
}
