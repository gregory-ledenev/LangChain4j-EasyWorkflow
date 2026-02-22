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

    private AppFrame appFrame;
    private final List<AppScreen<AppFrame>> appScreens = new CopyOnWriteArrayList<>();
    private AppScreen<AppFrame> activeAppScreen;
    private final ActionToolBar actionToolBar = new ActionToolBar(SwingConstants.VERTICAL, getAppScreenManagerActionGroup());
    private ActionGroup appScreenManagerActionGroup;

    public BasicAppScreenManager() {
        setLayout(new CardLayout());
        actionToolBar.setFloatable(false);
    }

    @Override
    public List<AppScreen<AppFrame>> getAppScreens() {
        return Collections.unmodifiableList(appScreens);
    }

    @Override
    public void setActiveAppScreen(AppScreen<AppFrame> anAppScreen) {
        Objects.requireNonNull(anAppScreen);

        if (! anAppScreen.canActivate())
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
            anAppScreen.activate();
            this.activeAppScreen = anAppScreen;
            CardLayout cardLayout = (CardLayout) getLayout();
            cardLayout.show(this, anAppScreen.getId());
        }
    }

    @Override
    public AppScreen<AppFrame> getActiveAppScreen() {
        return activeAppScreen;
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
        actionToolBar.getActionGroup().update();
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
        for (AppModule<AppFrame> appModule : appFrame.getAppModules()) {
            if (appModule instanceof AppScreen<AppFrame> appScreen) {
                appScreens.add(appScreen);
                add(appScreen.getComponent(), appScreen.getId());
                appScreenManagerActionGroup.addAction(appScreen.getActivationAction());
            }
        }
        appFrame.add(this, BorderLayout.CENTER);
        appFrame.add(actionToolBar, BorderLayout.WEST);
    }

    @Override
    public boolean canUninstall() {
        for (AppScreen<?> appScreen : getAppScreens()) {
            if (! appScreen.canUninstall())
                return false;
        }
        return true;
    }

    @Override
    public void uninstall() {
        appFrame.removeAppModuleListener(this);
        for (AppScreen<?> appScreen : appScreens)
            remove(appScreen.getComponent());
        appScreens.clear();
        appFrame.remove(this);
        appFrame.remove(actionToolBar);
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
}
