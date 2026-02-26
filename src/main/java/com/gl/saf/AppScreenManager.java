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

import java.util.List;

/**
 * Manages the collection of screens within an application module.
 * Handles screen navigation and provides actions for screen activation.
 */
public interface AppScreenManager extends AppModule<AppFrame> {
    String ID = "AppScreenManager";

    /**
     * Returns the list of all screens managed by this manager.
     * @return a list of {@link AppScreen} instances.
     */
    List<AppScreen<AppFrame>> getAppScreens();

    /**
     * Sets the currently active screen.
     * @param anAppScreen the screen to make active.
     */
    void setActiveAppScreen(AppScreen<AppFrame> anAppScreen);

    /**
     * Activates the first screen (that can be activated) in the list of managed screens.
     */
    void activateFirstAppScreen();

    /**
     * Returns the currently active screen.
     * @return the active {@link AppScreen}.
     */
    AppScreen<AppFrame> getActiveAppScreen();

    /**
     * Returns an action group containing actions to activate the screens
     * managed by this manager (e.g., for menu items or navigation buttons).
     * @return the {@link ActionGroup} for screen activation.
     */
    ActionGroup getAppScreenManagerActionGroup();
}