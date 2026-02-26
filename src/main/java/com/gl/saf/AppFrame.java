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

import com.gl.saf.actions.ActionGroup;
import com.gl.saf.actions.StandardActions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.gl.saf.Application.getSharedApplication;
import static com.gl.saf.UISupport.getOptions;
import static com.gl.saf.UISupport.isMacOS;

/**
 * The base class for all application frames.
 * Provides basic window management, including closing behavior and state management.
 */
@SuppressWarnings("ALL")
public class AppFrame extends JFrame implements Updatable {
    private final String uid;
    private final List<AppModuleListener> appModuleListeners = new CopyOnWriteArrayList<>();
    private List<AppModule<AppFrame>> appModules = new CopyOnWriteArrayList<>();
    private Map<String, AppModule> appModulesMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructs a new {@code AppFrame} with the specified unique identifier.
     *
     * @param uid The unique identifier for this frame.
     */
    public AppFrame(String uid) throws HeadlessException {
        this.uid = uid;

        if (! isMacOS())
            initDefaultMenuBarActionGroups(menuBarActionGroups);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close(false);
                getSharedApplication().maybeExit();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                deactivated();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                activated();
            }
        });

        createDefaultAppScreenManager().ifPresent(asm -> installAppModule(asm));
    }

    protected Optional<AppScreenManager> createDefaultAppScreenManager() {
        return Optional.of(new BasicAppScreenManager());
    }

    protected void activated() {
        getAppScreenManager().ifPresent(asm -> {
            if (asm.getActiveAppScreen() == null)
                asm.activateFirstAppScreen();
        });
    }

    protected void deactivated() {
    }

    /**
     * Returns the unique identifier of this application frame.
     *
     * @return The unique identifier (UID) of the frame.
     */
    public String getUid() {
        return uid;
    }

    /**
     * Determines if the frame can be closed. Subclasses can override this method
     * to implement custom logic (e.g., prompting the user to save changes).
     *
     * @return {@code true} if the frame can be closed, {@code false} otherwise.
     */
    public boolean canClose() {
        boolean result = true;
        for (AppModule appModule : appModules) {
            result &= appModule.canUninstall();
            if (!result)
                break;
        }
        return result;
    }

    /**
     * Closes the frame. If {@code forceClose} is {@code true}, the frame will close
     * regardless of the {@link #canClose()} method's return value. Otherwise, it
     * will only close if {@link #canClose()} returns {@code true}.
     *
     * @param forceClose If {@code true}, forces the frame to close without checking {@link #canClose()}.
     */
    public void close(boolean forceClose) {
        if (forceClose || canClose()) {
            saveState();
            setVisible(false);
            dispose();
        }
    }

    private boolean isRestoreFrameBounds() {
        return System.getProperty("com.gl.appframework.AppFrame.restoreFrameBounds",
                        String.valueOf(true)).
                equals(String.valueOf(true));
    }

    /**
     * Restores the state of the frame. Subclasses should override this method
     * to load any saved state (e.g., window position, size, user preferences).
     */
    public void restoreState() {
        if (isRestoreFrameBounds()) {
            Rectangle frameBounds = getOptions().getFrameBounds();
            if (frameBounds != null)
                setBounds(frameBounds);
        }
    }

    /**
     * Saves the current state of the frame. Subclasses should override this method
     * to persist any relevant state information (e.g., window position, size, user preferences).
     */
    public void saveState() {
        getOptions().setFrameBounds(getBounds());
    }

    /**
     * Updates the content or state of the frame. This method can be called by an updater thread so it should return as
     * quickly as possible. Subclasses can override this method
     * to refresh displayed data, update state, or perform other updates as needed.
     */
    public void update() {
        for (AppModule appModule : appModules)
            appModule.update();
    }

    /**
     * Returns an unmodifiable list of the application modules currently installed on this frame.
     *
     * @return A list of {@link AppModule} instances.
     */
    public List<AppModule<AppFrame>> getAppModules() {
        return Collections.unmodifiableList(appModules);
    }

    /**
     * Installs the specified application module into this frame.
     *
     * @param appModule The module to install.
     * @throws IllegalArgumentException If the module cannot be installed on this frame.
     */
    public synchronized void installAppModule(AppModule appModule) {
        if (!appModule.canInstall(this))
            throw new IllegalArgumentException("Can't install app module %s on %s frame".formatted(appModule, this));

        appModules.add(appModule);
        appModulesMap.put(appModule.getId(), appModule);
        appModule.install(this);
        fireAppModuleInstalled(appModule);
    }

    /**
     * Uninstalls the specified application module from this frame.
     *
     * @param appModule The module to uninstall.
     * @throws IllegalArgumentException If the module cannot be uninstalled at this time.
     */
    public synchronized void uninstallAppModule(AppModule appModule) {
        if (appModule.canUninstall())
            throw new IllegalArgumentException("Can't uninstall app module %s from %s frame".formatted(appModule, this));
        appModule.uninstall();
        appModules.remove(appModule);
        appModulesMap.remove(appModule.getId());

        fireAppModuleUninstalled(appModule);
    }

    /**
     * Retrieves an installed application module by its unique identifier.
     *
     * @param id The unique identifier of the module to retrieve.
     * @return The {@link AppModule} instance, or {@code null} if no module with the given ID is installed.
     */
    public AppModule getAppModule(String id) {
        return appModulesMap.get(id);
    }

    /**
     * Adds a listener to be notified of application module events.
     *
     * @param listener The listener to add.
     */
    public void addAppModuleListener(AppModuleListener listener) {
        appModuleListeners.add(listener);
    }

    /**
     * Removes a previously added application module listener.
     *
     * @param listener The listener to remove.
     */
    public void removeAppModuleListener(AppModuleListener listener) {
        appModuleListeners.remove(listener);
    }

    protected void fireAppModuleInstalled(AppModule appModule) {
        appModuleListeners.forEach(listener -> listener.appModuleInstalled(this, appModule));
    }

    protected void fireAppModuleUninstalled(AppModule appModule) {
        appModuleListeners.forEach(listener -> listener.appModuleUninstalled(this, appModule));
    }

    /**
     * Returns the {@link AppScreenManager} module installed on this frame.
     *
     * @return The {@link AppScreenManager} instance.
     */
    public Optional<AppScreenManager> getAppScreenManager() {
        return Optional.ofNullable((AppScreenManager) appModulesMap.get(AppScreenManager.ID));
    }

    /**
     * Returns the currently active application frame based on keyboard focus.
     *
     * @return The active {@link AppFrame}, or {@code null} if no frame or a non-app frame is active.
     */
    public static AppFrame getActiveAppFrame() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow instanceof  AppFrame)
            return (AppFrame) activeWindow;
        else if (activeWindow instanceof JDialog dialog)
            return dialog.getOwner() instanceof AppFrame ? (AppFrame) dialog.getOwner() : null;
        return null;
    }

    public static final String MENUBAR_ACTION_GROUP_FILE = "file";
    public static final String MENUBAR_ACTION_GROUP_EDIT = "edit";
    public static final String MENUBAR_ACTION_GROUP_VIEW = "view";
    public static final String MENUBAR_ACTION_GROUP_OPTIONS = "options";
    public static final String MENUBAR_ACTION_GROUP_HELP = "help";

    public ActionGroup getMenuBarActionGroup(String key) {
        return getMenuNarActionGroups().get(key);
    }

    public LazyValues<ActionGroup> getMenuNarActionGroups() {
        return isMacOS() ? sharedMenuBarActionGroups : menuBarActionGroups;
    }

    static void initDefaultMenuBarActionGroups(LazyValues<ActionGroup> menuBarActionGroups) {
        menuBarActionGroups.addSupplier(MENUBAR_ACTION_GROUP_FILE, () -> {
            ActionGroup result = new ActionGroup("File", null, true, new Action[0]);
            if (!isMacOS())
                result.addAction(StandardActions.createExitAction());
            return result;
        });

        menuBarActionGroups.addSupplier(MENUBAR_ACTION_GROUP_EDIT, () -> {
            return new ActionGroup("Edit", null, true,
                    new ActionGroup(
                            StandardActions.createCutAction(),
                            StandardActions.createCopyAction(),
                            StandardActions.createPasteAction(),
                            StandardActions.createDeleteAction()
                    )
            );
        });

        menuBarActionGroups.addSupplier(MENUBAR_ACTION_GROUP_VIEW, () -> new ActionGroup("View", null, true, new Action[0]));

        menuBarActionGroups.addSupplier(MENUBAR_ACTION_GROUP_OPTIONS, () -> new ActionGroup("Options", null, true,
                new ActionGroup(StandardActions.createAppearanceActionGroup())));

        menuBarActionGroups.addSupplier(MENUBAR_ACTION_GROUP_HELP, () -> new ActionGroup("Help", null, true,
                createVisitLinksActionGroup(Application.getSharedApplication().getAboutProvider()),
                new ActionGroup(StandardActions.createAboutAction())
        ));
    }

    private static ActionGroup createVisitLinksActionGroup(AboutProvider aboutProvider) {
        if (aboutProvider == null || aboutProvider.getAboutLinks().isEmpty())
            return null;

        return new ActionGroup(aboutProvider.getAboutLinks().stream()
                .map(StandardActions::createVisitLinkAction)
                .toArray(Action[]::new));
    }

    private static LazyValues<ActionGroup> sharedMenuBarActionGroups = new LazyValues<>();
    private LazyValues<ActionGroup> menuBarActionGroups = new LazyValues<>();

    static {
        if (isMacOS())
            initDefaultMenuBarActionGroups(sharedMenuBarActionGroups);
    }
}
