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

import com.gl.appframework.actions.BasicAction;
import com.gl.appframework.actions.StateAction;

import javax.swing.*;
import java.awt.*;

/**
 * A basic implementation of the {@link AppScreen} interface extending {@link JPanel}.
 * Provides standard management for screen metadata, lifecycle, and activation actions.
 */
public class BasicAppScreen<T extends AppFrame> extends JPanel implements AppScreen<T> {
    private T appFrame;
    private final String id;
    private String name;
    private Icon icon;
    private String shortDescription;
    private String longDescription;
    private String disableReason;
    private Icon rolloverIcon;
    private Icon largeIcon;
    private Icon largeRolloverIcon;

    private StateAction activationAction;

    public BasicAppScreen(String id) {
        super(new BorderLayout(0, 0));
        this.id = id;
    }

    public BasicAppScreen(String id, String name, Icon icon, String shortDescription) {
        this(id);

        this.name = name;
        this.icon = icon;
        this.shortDescription = shortDescription;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        getActivationAction().setShortDescription(shortDescription);
    }

    @Override
    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
        getActivationAction().setLongDescription(longDescription);
    }

    @Override
    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
        getActivationAction().setDisableReason(disableReason);
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        getActivationAction().putValue(Action.SMALL_ICON, icon);
    }

    @Override
    public Icon getRolloverIcon() {
        return rolloverIcon;
    }

    public void setRolloverIcon(Icon rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
    }

    @Override
    public Icon getLargeIcon() {
        return largeIcon;
    }

    public void setLargeIcon(Icon largeIcon) {
        this.largeIcon = largeIcon;
        getActivationAction().putValue(Action.LARGE_ICON_KEY, largeIcon);
    }

    @Override
    public Icon getLargeRolloverIcon() {
        return largeRolloverIcon;
    }

    public void setLargeRolloverIcon(Icon largeRolloverIcon) {
        this.largeRolloverIcon = largeRolloverIcon;
    }

    @Override
    public boolean canActivate() {
        return true;
    }

    @Override
    public void activate() {
        requestFocus();
    }

    @Override
    public boolean canPassivate() {
        return true;
    }

    @Override
    public void passivate() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public BasicAction getActivationAction() {
        if (activationAction == null) {
            activationAction = new StateAction(getName(), getIcon(), AppScreenManager.ID,
                    actionEvent -> ((AppScreenManager) appFrame.getAppModule(AppScreenManager.ID)).setActiveAppScreen((AppScreen<AppFrame>) this),
                    stateAction -> {
                        stateAction.setEnabled(isEnabled());
                        stateAction.setSelected(((AppScreenManager) appFrame.getAppModule(AppScreenManager.ID)).getActiveAppScreen() == this);
                    });
            activationAction.setShortDescription(getShortDescription());
            activationAction.setLongDescription(getLongDescription());
        }
        return activationAction;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void update() {
    }

    @Override
    public T getAppFrame() {
        return appFrame;
    }

    @Override
    public boolean canInstall(T owner) {
        return true;
    }

    @Override
    public void install(T owner) {
        appFrame = owner;
    }

    @Override
    public boolean canUninstall() {
        return true;
    }

    @Override
    public void uninstall() {
        appFrame = null;
    }
}
