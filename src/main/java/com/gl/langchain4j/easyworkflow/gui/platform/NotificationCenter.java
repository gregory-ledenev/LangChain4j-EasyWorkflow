/*
 *
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
 * /
 */

package com.gl.langchain4j.easyworkflow.gui.platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manages and displays notifications within a Swing application.
 * Notifications appear as small, temporary windows in the corner of the owner frame.
 */
public class NotificationCenter {
    private static final NotificationCenter INSTANCE = new NotificationCenter();

    private final Map<JWindow, Notification> activeNotifications = new LinkedHashMap<>();
    private final Map<JWindow, Timer> notificationTimers = new HashMap<>();
    private final ComponentListener componentListener;

    /**
     * Defines the type of notification, influencing its appearance (e.g., icon).
     */
    public enum NotificationType {
        /**
         * A success message, indicating an operation completed successfully.
         */
        SUCCESS,
        /**
         * General informational message.
         */
        INFORMATION,
        /**
         * A warning message, indicating a potential issue.
         */
        WARNING,
        /**
         * An error message, indicating a problem.
         */
        ERROR
    }

    /**
     * Represents a single notification with its content and behavior.
     */
    public static class Notification {
        public static final String PROP_COUNTER = "counter";
        private final NotificationType type;
        private final String title;
        private final String text;
        private final int delay;
        private final ActionListener clickAction;
        private int counter = 1;
        private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

        /**
         * Constructs a new Notification.
         *
         * @param type        The type of the notification (INFORMATION, WARNING, ERROR).
         * @param title       The title of the notification.
         * @param text        The main text content of the notification.
         * @param clickAction An {@link ActionListener} to be executed when the notification is clicked. Can be null.
         */
        public Notification(NotificationType type, String title, String text, ActionListener clickAction) {
            this(type, title, text, 10000, clickAction);
        }

        /**
         * Constructs a new Notification.
         * @param type The type of the notification (INFORMATION, WARNING, ERROR).
         * @param title The title of the notification.
         * @param text The main text content of the notification.
         * @param delay The duration in milliseconds after which the notification will automatically close. A value of 0 or less means it won't auto-close.
         * @param clickAction An {@link ActionListener} to be executed when the notification is clicked. Can be null.
         */
        public Notification(NotificationType type, String title, String text, int delay, ActionListener clickAction) {
            this.type = type;
            this.title = title;
            this.text = text;
            this.delay = delay;
            this.clickAction = clickAction;
        }

        /**
         * Returns the type of the notification.
         * @return The {@link NotificationType}.
         */
        public NotificationType getType() {
            return type;
        }

        /**
         * Returns the title of the notification.
         * @return The title string.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the text content of the notification.
         * @return The text string.
         */
        public String getText() {
            return text;
        }

        /**
         * Returns the auto-close delay of the notification in milliseconds.
         * @return The delay in milliseconds.
         */
        public int getDelay() {
            return delay;
        }

        /**
         * Returns the {@link ActionListener} associated with clicking the notification.
         * @return The {@link ActionListener}, or null if none is set.
         */
        public ActionListener getClickAction() {
            return clickAction;
        }

        /**
         * Returns the appropriate icon for the notification based on its type.
         * @return An {@link Icon} representing the notification type.
         */
        public Icon getIcon() {
            return switch (type) {
                case SUCCESS -> new UISupport.AutoIcon(Icons.ICON_NOTIFICATION_SUCCESS);
                case INFORMATION -> new UISupport.AutoIcon(Icons.ICON_NOTIFICATION_INFORMATION);
                case WARNING -> new UISupport.AutoIcon(Icons.ICON_NOTIFICATION_WARNING);
                case ERROR -> new UISupport.AutoIcon(Icons.ICON_NOTIFICATION_ERROR);
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Notification that = (Notification) o;
            return type == that.type &&
                    Objects.equals(title, that.title) &&
                    Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, title, text);
        }

        /**
         * Returns the current counter for this notification.
         * This counter can be used to track how many times an identical notification has been posted.
         * @return The current counter.
         */
        public int getCounter() {
            return counter;
        }

        /**
         * Sets the counter for this notification.
         * @param aCounter The new counter to set.
         */
        public void setCounter(int aCounter) {
            counter = aCounter;
        }

        public void incCounter() {
            setCounter(getCounter() + 1);
            propertyChangeSupport.firePropertyChange(PROP_COUNTER, counter - 1, counter);
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    private NotificationCenter() {
        componentListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repositionNotifications();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                repositionNotifications();
            }
        };

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow", evt -> {
            Window oldActiveWindow = (Window) evt.getOldValue();
            Window newActiveWindow = (Window) evt.getNewValue();

            if (oldActiveWindow != null) {
                oldActiveWindow.removeComponentListener(componentListener);
            }
            if (newActiveWindow != null) {
                newActiveWindow.addComponentListener(componentListener);
            }

            if (newActiveWindow == null) {
                // App lost focus
                for (JWindow window : activeNotifications.keySet()) {
                    window.setVisible(false);
                }
                pauseAllTimers();
            } else {
                // App gained focus or changed active window
                for (JWindow window : activeNotifications.keySet()) {
                    window.setVisible(true);
                }
                repositionNotifications();
                resumeTimersIfMouseIsOut();
            }
        });

        Window activeWindow = getActiveWindow();
        if (activeWindow != null) {
            activeWindow.addComponentListener(componentListener);
        }
    }

    /**
     * Returns the singleton instance of the NotificationCenter.
     * @return The {@link NotificationCenter} instance.
     */
    public static NotificationCenter getInstance() {
        return INSTANCE;
    }

    /**
     * Posts a new notification to be displayed.
     * If an identical notification (same type, title, and text) is already active, it will not be shown again.
     * @param notification The {@link Notification} to be displayed.
     */
    public void postNotification(Notification notification) {
        // Check if an identical notification is already active
        for (Map.Entry<JWindow, Notification> entry : activeNotifications.entrySet()) {
            if (entry.getValue().equals(notification)) {
                entry.getValue().incCounter();
                notificationTimers.get(entry.getKey()).restart();
                return;
            }
        }

        JWindow notificationWindow = new JWindow(getActiveWindow());
        notificationWindow.setAlwaysOnTop(true);
        notificationWindow.setOpacity(0.0f);

        final Runnable closeNotification = () -> {
            Timer timer = notificationTimers.remove(notificationWindow);
            if (timer != null) {
                timer.stop();
            }
            activeNotifications.remove(notificationWindow);
            notificationWindow.dispose();
            repositionNotifications();
        };

        NotificationPanel panel = new NotificationPanel(notification, e -> closeNotification.run());
        notificationWindow.add(panel);
        notificationWindow.pack();

        activeNotifications.put(notificationWindow, notification);
        repositionNotifications();

        if (notification.getDelay() > 0) {
            Timer timer = new Timer(notification.getDelay(), e -> closeNotification.run());
            timer.setRepeats(false);
            notificationTimers.put(notificationWindow, timer);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    pauseAllTimers();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    resumeTimersIfMouseIsOut();
                }
            };

            addRecursiveMouseListener(notificationWindow, mouseAdapter);
        }

        if (getActiveWindow() != null) {
            notificationWindow.setVisible(true);

            Timer fadeInTimer = new Timer(50, null);
            fadeInTimer.addActionListener(e -> {
                float opacity = notificationWindow.getOpacity();
                opacity += 0.1f;
                if (opacity >= 1.0f) {
                    opacity = 1.0f;
                    fadeInTimer.stop();
                    Timer autoCloseTimer = notificationTimers.get(notificationWindow);
                    if (autoCloseTimer != null && !autoCloseTimer.isRunning()) {
                        autoCloseTimer.start();
                    }
                }
                notificationWindow.setOpacity(opacity);
            });
            fadeInTimer.start();
        }
    }

    private void pauseAllTimers() {
        for (Timer timer : notificationTimers.values()) {
            timer.stop();
        }
    }

    private void resumeTimersIfMouseIsOut() {
        if (getActiveWindow() == null) {
            return;
        }
        Point mousePosition = MouseInfo.getPointerInfo().getLocation();
        for (JWindow window : activeNotifications.keySet()) {
            if (window.isVisible() && window.getBounds().contains(mousePosition)) {
                // Mouse is still inside another notification, so don't resume
                return;
            }
        }
        // Mouse is not in any notification, so resume all
        for (Timer timer : notificationTimers.values()) {
            timer.restart();
        }
    }

    private void addRecursiveMouseListener(Component component, MouseAdapter adapter) {
        component.addMouseListener(adapter);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                addRecursiveMouseListener(child, adapter);
            }
        }
    }

    private void repositionNotifications() {
        Window owner = getActiveWindow();
        if (owner == null || !owner.isShowing()) {
            return;
        }

        Point ownerLocation = owner.getLocationOnScreen();
        Dimension ownerSize = owner.getSize();

        final int gap = 10;
        int currentY = ownerLocation.y + ownerSize.height;
        for (JWindow notification : activeNotifications.keySet()) {
            int x = ownerLocation.x + ownerSize.width - notification.getWidth() - gap;
            currentY -= (notification.getHeight() + gap); // 5px spacing
            notification.setLocation(x, currentY);
        }
    }

    private Window getActiveWindow() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    }
}
