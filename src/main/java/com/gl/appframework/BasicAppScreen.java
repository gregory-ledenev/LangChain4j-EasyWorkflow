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
    private final String id;
    private T appFrame;
    private String name;
    private Icon icon;
    private String shortDescription;
    private String longDescription;
    private String disableReason;
    private Icon rolloverIcon;
    private Icon largeIcon;
    private Icon largeRolloverIcon;
    private Icon selectedIcon;
    private Icon selectedLargeIcon;
    private StateAction activationAction;

    /**
     * Constructs a new {@code BasicAppScreen} with the specified identifier.
     * @param id the unique identifier for this screen
     */
    public BasicAppScreen(String id) {
        super(new BorderLayout(0, 0));
        this.id = id;
    }

    /**
     * Constructs a new {@code BasicAppScreen} with the specified metadata.
     *
     * @param id               the unique identifier for this screen
     * @param name             the display name of the screen
     * @param icon             the icon representing the screen; if an {@link IconFactory.AutoIcon} is provided, a full icon set is generated
     * @param shortDescription a brief description of the screen's purpose
     */
    public BasicAppScreen(String id, String name, Icon icon, String shortDescription) {
        this(id);

        this.name = name;
        if (icon instanceof IconFactory.AutoIcon autoIcon)
            setupIcons(autoIcon);
        else
            this.icon = icon;
        this.shortDescription = shortDescription;
    }

    /**
     * Configures a suite of icons (standard, large, rollover, and selected states)
     * based on a single {@link IconFactory.AutoIcon} template.
     * @param icon the base auto-icon to use for generating the icon set
     */
    public void setupIcons(IconFactory.AutoIcon icon) {
        if (icon != null) {
            setIcon(icon);
            setLargeIcon(new IconFactory.AutoIcon(icon.getKey(), IconFactory.IconSize.Large));
            setSelectedIcon(new IconFactory.AutoIcon(icon.getKey(), IconFactory.IconSize.Auto, true, false));
            setSelectedLargeIcon(new IconFactory.AutoIcon(icon.getKey(), IconFactory.IconSize.Large, true, false));
            setRolloverIcon(new IconFactory.AutoIcon(icon.getKey(), IconFactory.IconSize.Auto, false, true));
            setLargeRolloverIcon(new IconFactory.AutoIcon(icon.getKey(), IconFactory.IconSize.Large, false, true));
        }
    }

    @Override
    public Icon getSelectedIcon() {
        return selectedIcon;
    }

    /**
     * Sets the icon to be displayed when the screen is selected.
     * @param selectedIcon the icon for the selected state
     */
    public void setSelectedIcon(Icon selectedIcon) {
        this.selectedIcon = selectedIcon;
        getActivationAction().setSelectedIcon(selectedIcon);
    }

    @Override
    public Icon getLargeSelectedIcon() {
        return selectedLargeIcon;
    }

    /**
     * Sets the large icon to be displayed when the screen is selected.
     * @param selectedLargeIcon the large icon for the selected state
     */
    public void setSelectedLargeIcon(Icon selectedLargeIcon) {
        this.selectedLargeIcon = selectedLargeIcon;
        getActivationAction().setSelectedLargeIcon(selectedLargeIcon);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String getShortDescription() {
        return shortDescription;
    }

    /**
     * Sets the short description for this screen and updates the activation action.
     * @param shortDescription a brief description of the screen
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        getActivationAction().setShortDescription(shortDescription);
    }

    @Override
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * Sets the long description for this screen and updates the activation action.
     * @param longDescription a detailed description of the screen
     */
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
        getActivationAction().setLongDescription(longDescription);
    }

    @Override
    public String getDisableReason() {
        return disableReason;
    }

    /**
     * Sets the reason why this screen is disabled and updates the activation action.
     * @param disableReason the text explaining why the screen is unavailable
     */
    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
        getActivationAction().setDisableReason(disableReason);
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    /**
     * Sets the standard icon for this screen and updates the activation action.
     * @param icon the icon to display
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
        if (activationAction != null)
            activationAction.putValue(Action.SMALL_ICON, icon);
    }

    @Override
    public Icon getRolloverIcon() {
        return rolloverIcon;
    }

    /**
     * Sets the icon to be displayed when the mouse rolls over the screen's activation component.
     * @param rolloverIcon the icon for the rollover state
     */
    public void setRolloverIcon(Icon rolloverIcon) {
        this.rolloverIcon = rolloverIcon;
    }

    @Override
    public Icon getLargeIcon() {
        return largeIcon;
    }

    /**
     * Sets the large icon for this screen and updates the activation action.
     * @param largeIcon the large icon to display
     */
    public void setLargeIcon(Icon largeIcon) {
        this.largeIcon = largeIcon;
        getActivationAction().putValue(Action.LARGE_ICON_KEY, largeIcon);
    }

    @Override
    public Icon getLargeRolloverIcon() {
        return largeRolloverIcon;
    }

    /**
     * Sets the large icon to be displayed when the mouse rolls over the screen's activation component.
     * @param largeRolloverIcon the large icon for the rollover state
     */
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
            activationAction.setLongDescription(getLongDescription());
            activationAction.setRetainName(true);
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
