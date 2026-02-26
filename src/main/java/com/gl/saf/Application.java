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

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import static com.gl.saf.Appearance.applyAppearance;
import static com.gl.saf.Appearance.getAppearanceDetector;
import static com.gl.saf.UISupport.*;

/**
 * The central application class responsible for managing the lifecycle of GUI frames,
 * handling application-level events like quit and about, and providing a singleton instance.
 */
@SuppressWarnings("ALL")
public class Application<T extends ApplicationPreferences> {

    private static final AtomicReference<Application> sharedApplication = new AtomicReference<>();
    private static Supplier<Application> applicationSupplier = () -> new Application();

    static {
        Icons.loadIcons();
        System.setProperty("apple.awt.application.appearance", "system");
        UIManager.put("ScrollBar.width", 8);
//        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    private final String id;
    protected T applicationPreferences;
    /**
     * Gets the singleton instance of {@link ApplicationPreferences}.
     *
     * @return The {@link ApplicationPreferences} instance.
     */


    private final Consumer<Boolean> appearanceChangeHandler = isDarkMode -> {
        if (getApplicationPreferences().getAppearance() == Appearance.Type.Auto)
            SwingUtilities.invokeLater(() -> applyAppearance());
    };
    private AboutProvider aboutProvider;
    private UpdateThread fUpdateThread;

    /**
     * Constructs a new Application instance using the class name as the default identifier.
     */
    public Application() {
        this(Application.class.getName());
    }

    /**
     * Constructs a new Application instance with a specific identifier.
     *
     * @param id The unique identifier for this application, used for preference storage.
     */
    public Application(String id) {
        this.id = id;
    }

    /**
     * Returns the shared instance of the Application.
     * If no instance exists, a new one is created.
     *
     * @return The shared Application instance.
     */
    public static Application getSharedApplication() {
        return sharedApplication.updateAndGet(a -> a == null ? applicationSupplier.get() : a);
    }

    /**
     * Gets the current supplier used to create the Application instance.
     *
     * @return The {@link Supplier} of {@link Application}.
     */
    public static Supplier<Application> getApplicationSupplier() {
        return applicationSupplier;
    }

    /**
     * Sets the supplier used to create the Application instance. Tkes no effect if shared application is already created.
     *
     * @param applicationSupplier The {@link Supplier} to be used for application creation.
     */
    public static void setApplicationSupplier(Supplier<Application> applicationSupplier) {
        Application.applicationSupplier = Objects.requireNonNull(applicationSupplier);
    }

    /**
     * Retrieves the user preferences node for the application.
     *
     * @return The {@link Preferences} object for the application.
     */
    public static Preferences getUserPreferences() {
        return Preferences.userRoot().node(Application.getSharedApplication().getId().replace(".", "/"));
    }

    public T getApplicationPreferences() {
        if (applicationPreferences == null) {
            applicationPreferences = (T) new ApplicationPreferences();
        }
        return applicationPreferences;
    }

    /**
     * Gets the unique identifier for this application.
     *
     * @return The application ID string.
     */
    public String getId() {
        return id;
    }

    /**
     * Launches the application with the given AppFrame.
     * Sets up desktop handlers for quit and about actions.
     *
     * @param frame The initial AppFrame to display.
     */
    public void launch(AppFrame frame) {
        Objects.requireNonNull(frame);

        applyAppearance();
        getAppearanceDetector().registerListener(appearanceChangeHandler);

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

        getAppearanceDetector().removeListener(appearanceChangeHandler);

        stopUpdates();

        if (canExit)
            System.exit(0);
    }

    /**
     * Displays the "About" dialog provided by an {@link AboutProvider} if available.
     */
    public void about() {
        // disallow showing second dialog on Mac when invoked via system menu
        if (isMacOS()) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing()) {
                    return;
                }
            }
        }

        for (Window window : Window.getWindows()) {
            if (window instanceof AboutProvider aboutProvider) {
                aboutProvider.showAbout(window);
                return;
            }
        }
    }

    /**
     * Starts the background update thread if it is not already running.
     * The update thread is responsible for periodic application tasks.
     */
    public void startUpdates() {
        if (fUpdateThread == null) {
            fUpdateThread = new UpdateThread();
            fUpdateThread.start();
        }
    }

    /**
     * Stops the background update thread by interrupting it and clearing the reference.
     * This should be called during application shutdown.
     */
    public void stopUpdates() {
        if (fUpdateThread != null) {
            fUpdateThread.interrupt();
            fUpdateThread = null;
        }
    }

    /**
     * Gets the current AboutHandler.
     *
     * @return The AboutHandler instance.
     */
    public AboutProvider getAboutProvider() {
        return aboutProvider;
    }

    /**
     * Sets the AboutHandler to be used by the application. Note: it should be setup before the application launch,
     * othrwise the changes may not be picked up.
     *
     * @param aboutProvider The AboutHandler to set.
     */
    public void setAboutProvider(AboutProvider aboutProvider) {
        this.aboutProvider = aboutProvider;
    }
}
