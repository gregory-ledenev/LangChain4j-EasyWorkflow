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

package com.gl.appframework;

import com.gl.appframework.actions.ActionGroup;
import com.gl.appframework.comp.ActionToolBar;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A basic implementation of {@link AppScreenManager} that uses a {@link CardLayout}
 * to manage and switch between different {@link AppScreen} components.
 */
public class BasicAppScreenManager extends JPanel implements AppScreenManager, AppModuleListener {

    public static final String ID_PLACEHOLDER = "$placeholder";
    private final List<AppScreen<AppFrame>> appScreens = new CopyOnWriteArrayList<>();
    private final boolean installSwitcher;
    private final JComponent placeholder;
    private AppFrame appFrame;
    private AppScreen<AppFrame> activeAppScreen;
    private ActionGroup appScreenManagerActionGroup;
    private final Switcher switcher = new Switcher(getAppScreenManagerActionGroup());

    /**
     * Constructs a new BasicAppScreenManager.
     *
     * @param installSwitcher if true, a switcher for switching screens will be installed in the frame.
     * @param placeholder     an optional component to display when no screen is active.
     */
    public BasicAppScreenManager(boolean installSwitcher, JComponent placeholder) {
        this.installSwitcher = installSwitcher;
        this.placeholder = placeholder;
        setLayout(new CardLayout());
    }

    /**
     * Constructs a new BasicAppScreenManager with the switcher installed and no placeholder.
     */
    public BasicAppScreenManager() {
        this(true, new JPanel());
    }

    public Switcher getSwitcher() {
        return switcher;
    }

    /**
     * @return the optional placeholder component.
     */
    public JComponent getPlaceholder() {
        return placeholder;
    }

    @Override
    public List<AppScreen<AppFrame>> getAppScreens() {
        return Collections.unmodifiableList(appScreens);
    }

    @Override
    public void activateFirstAppScreen() {
        for (AppScreen<AppFrame> screen : appScreens) {
            if (screen.canActivate()) {
                setActiveAppScreen(screen);
                break;
            }
        }
    }

    @Override
    public AppScreen<AppFrame> getActiveAppScreen() {
        return activeAppScreen;
    }

    @Override
    public void setActiveAppScreen(AppScreen<AppFrame> anAppScreen) {

        if (anAppScreen == getActiveAppScreen() || (anAppScreen != null && !anAppScreen.canActivate()))
            return;

        AppScreen<AppFrame> activeAppScreen = getActiveAppScreen();

        boolean canProceed = true;
        if (activeAppScreen != null) {
            if (activeAppScreen.canPassivate())
                activeAppScreen.passivate();
            else
                canProceed = false;
        }

        if (canProceed) {
            if (anAppScreen != null)
                anAppScreen.activate();
            this.activeAppScreen = anAppScreen;
            CardLayout cardLayout = (CardLayout) getLayout();
            cardLayout.show(this, anAppScreen != null ? anAppScreen.getId() : ID_PLACEHOLDER);
        }
    }

    @Override
    public ActionGroup getAppScreenManagerActionGroup() {
        if (appScreenManagerActionGroup == null)
            appScreenManagerActionGroup = new ActionGroup();
        return appScreenManagerActionGroup;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public String toString() {
        return ID;
    }

    @Override
    public void update() {
        switcher.getActionToolBar().getActionGroup().update();
        for (AppScreen<AppFrame> appScreen : appScreens)
            appScreen.update();
    }

    @Override
    public AppFrame getAppFrame() {
        return appFrame;
    }

    @Override
    public boolean canInstall(AppFrame owner) {
        return true;
    }

    @Override
    public void install(AppFrame owner) {
        appFrame = Objects.requireNonNull(owner);
        appFrame.addAppModuleListener(this);

        if (placeholder != null)
            add(placeholder, ID_PLACEHOLDER);

        for (AppModule<AppFrame> appModule : appFrame.getAppModules()) {
            if (appModule instanceof AppScreen<AppFrame> appScreen) {
                appScreens.add(appScreen);
                add(appScreen.getComponent(), appScreen.getId());
                appScreenManagerActionGroup.addAction(appScreen.getActivationAction());
            }
        }
        appFrame.add(this, BorderLayout.CENTER);
        if (installSwitcher)
            appFrame.add(switcher, BorderLayout.WEST);
    }

    @Override
    public boolean canUninstall() {
        for (AppScreen<?> appScreen : getAppScreens()) {
            if (!appScreen.canUninstall())
                return false;
        }
        return true;
    }

    @Override
    public void uninstall() {
        appFrame.removeAppModuleListener(this);

        if (placeholder != null)
            remove(placeholder);

        for (AppScreen<?> appScreen : appScreens)
            remove(appScreen.getComponent());
        appScreens.clear();
        appFrame.remove(this);
        appFrame.remove(switcher);
        appFrame = null;
        activeAppScreen = null;
    }

    @Override
    public void appModuleInstalled(AppFrame appFrame, AppModule<AppFrame> appModule) {
        if (appModule instanceof AppScreen<AppFrame> appScreen) {
            appScreens.add(appScreen);
            add(appScreen.getComponent(), appScreen.getId());
            appScreenManagerActionGroup.addAction(appScreen.getActivationAction());
        }
    }

    @Override
    public void appModuleUninstalled(AppFrame appFrame, AppModule appModule) {
        if (appModule instanceof AppScreen appScreen) {
            appScreens.remove(appScreen);
            remove(appScreen.getComponent());
            appScreenManagerActionGroup.removeAction(appScreen.getActivationAction());
        }
    }

    public static class Switcher extends JPanel {
        private final ActionToolBar actionToolBar;

        public Switcher(ActionGroup actionGroup) {
            super(new BorderLayout());

            actionToolBar = new ActionToolBar(SwingConstants.VERTICAL, actionGroup);
            actionToolBar.revalidate();
            actionToolBar.setFloatable(false);
            actionToolBar.setMargin(new Insets(2, 2, 2, 2));
            actionToolBar.setOpaque(false);

            add(actionToolBar, BorderLayout.CENTER);
        }

        public ActionToolBar getActionToolBar() {
            return actionToolBar;
        }

        @Override
        public void updateUI() {
            super.updateUI();

            setBorder(UISupport.createCustomLineBorder(UISupport.getDefaultBorderColor(), false, false, false, true));
            setOpaque(true);
            Color background = UIManager.getColor("Panel.background");
            setBackground(UISupport.isDarkAppearance() ?
                    new Color(5, 5, 20) :
                    new Color(Math.min(background.getRed() - 2, 255),
                            Math.min(background.getGreen() - 2, 255),
                            Math.min(background.getBlue(), 255),
                            background.getAlpha()));
        }
    }
}
