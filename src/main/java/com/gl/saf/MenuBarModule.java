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
import com.gl.saf.comp.ActionMenuBar;

import java.util.Objects;

/**
 * A module that manages and installs a JMenuBar into an AppFrame.
 * It uses an ActionGroup to define the menu structure and handles updates to the menu items.
 */
public class MenuBarModule<T extends AppFrame> implements AppModule<T> {
    public static final String ID = "MenuBar";
    private T appFrame;
    private final ActionGroup actionGroup;

    /**
     * Constructs a new MenuBarModule with the specified action group.
     * @param actionGroup the action group to be used for the menu bar; must not be null.
     */
    public MenuBarModule(ActionGroup actionGroup) {
        this.actionGroup = Objects.requireNonNull(actionGroup);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void update() {
        actionGroup.update();
    }

    @Override
    public T getAppFrame() {
        return appFrame;
    }

    @Override
    public boolean canInstall(AppFrame owner) {
        return true;
    }

    @Override
    public void install(T owner) {
        appFrame = owner;
        appFrame.setJMenuBar(new ActionMenuBar(actionGroup));
    }

    @Override
    public boolean canUninstall() {
        return true;
    }

    @Override
    public void uninstall() {
        appFrame.setJMenuBar(null);
        appFrame = null;
    }
}
