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

package com.gl.langchain4j.easyworkflow.gui.platform;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static com.gl.langchain4j.easyworkflow.gui.platform.UISupport.isMac;

/**
 * The central application class responsible for managing the lifecycle of GUI frames,
 * handling application-level events like quit and about, and providing a singleton instance.
 */
public class Application {
    public interface ScheduledUpdatable {
        void scheduledUpdate();
    }

    private static Application sharedApplication;

    static {
        System.setProperty("apple.awt.application.appearance", "system");
//        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    /**
     * Returns the shared instance of the Application.
     * If no instance exists, a new one is created.
     *
     * @return The shared Application instance.
     */
    public static Application getSharedApplication() {
        if (sharedApplication == null) {
            sharedApplication = new Application();
        }
        return sharedApplication;
    }

    /**
     * Launches the application with the given AppFrame.
     * Sets up desktop handlers for quit and about actions.
     *
     * @param frame The initial AppFrame to display.
     */
    public void launch(AppFrame frame) {
        Objects.requireNonNull(frame);

        startUpdates();

        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, response) -> {
                response.cancelQuit();
                getSharedApplication().exit(false);
            });
        }

        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(e -> getSharedApplication().about());
        }

        showFrame(frame);
    }

    /**
     * Displays the given AppFrame, restoring its state if applicable.
     *
     * @param frame The AppFrame to show.
     */
    public void showFrame(AppFrame frame) {
        frame.restoreState();
        frame.setVisible(true);
    }

    /**
     * Exits the application, closing all open windows.
     *
     * @param forceExit If true, forces the closure of windows without prompting for save.
     */
    public void exit(boolean forceExit) {
        for (Window window : Window.getWindows()) {
            if (window instanceof AppFrame appFrame) {
                appFrame.close(forceExit);
            } else {
                window.setVisible(false);
                window.dispose();
            }
        }

        getSharedApplication().maybeExit();
    }

    /**
     * Checks if all windows are closed and, if so, exits the system.
     */
    public void maybeExit() {
        boolean canExit = true;
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                canExit = false;
                break;
            }
        }

        stopUpdates();

        if (canExit)
            System.exit(0);
    }

    /**
     * Displays the "About" dialog provided by an {@link UISupport.AboutProvider} if available.
     */
    public void about() {
        // disallow showing second dialog on Mac when invoked via system menu
        if (isMac()) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing())  {
                    return;
                }
            }
        }

        for (Window window : Window.getWindows()) {
            if (window instanceof UISupport.AboutProvider aboutProvider) {
                aboutProvider.showAbout(window);
                return;
            }
        }
    }

    private UpdateThread fUpdateThread;

    public void startUpdates() {
        if (fUpdateThread == null) {
            fUpdateThread = new UpdateThread();
            fUpdateThread.start();
        }
    }

    public void stopUpdates() {
        if (fUpdateThread != null) {
            fUpdateThread.interrupt();
            fUpdateThread = null;
        }
    }

    private static class UpdateThread extends Thread {
        public static final long SLEEP_TIME = 300;

        protected long sleepTime = SLEEP_TIME;

        public UpdateThread() {
        }

        public UpdateThread(long aSleepTime) {
            sleepTime = aSleepTime;
        }

        public void run() {
            while (!isInterrupted()) {
                try {
                    sleep(SLEEP_TIME);
                    EventQueue.invokeAndWait(this::update);

                } catch (Exception e) {
                    // do nothing
                }
            }
        }

        protected void update() {
            try {
                Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                ScheduledUpdatable scheduledUpdatable = activeWindow instanceof ScheduledUpdatable ? (ScheduledUpdatable) activeWindow : null;
                if (scheduledUpdatable != null)
                    scheduledUpdatable.scheduledUpdate();

                for (Window w : Window.getWindows())
                    if (scheduledUpdatable != w && w.isShowing() && (w instanceof ScheduledUpdatable scheduledUpdatable1))
                        scheduledUpdatable1.scheduledUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
