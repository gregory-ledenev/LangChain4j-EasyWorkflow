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

/**
 * Represents a modular component that can be installed into an {@link AppFrame}.
 * Modules extend the functionality of the application framework and follow a lifecycle
 * of installation, updates, and uninstallation.
 */
public interface AppModule<T extends AppFrame> extends Updatable {

    /**
     * Returns the unique identifier for this module.
     * @return the module ID.
     */
    public String getId();

    /**
     * Returns the human-readable name of the module.
     * @return the module name.
     */
    public String getName();

    /**
     * Performs periodic updates or maintenance tasks for the module.
     */
    public void update();

    /**
     * Returns the {@link AppFrame} this module is currently installed in.
     * @return the owner AppFrame, or null if not installed.
     */
    public T getAppFrame();

    /**
     * Checks if the module can be installed into the specified {@link AppFrame}.
     * @param owner the potential owner frame.
     * @return true if installation is possible, false otherwise.
     */
    public boolean canInstall(T owner);

    /**
     * Installs the module into the specified {@link AppFrame}.
     * @param owner the frame to install into.
     */
    public void install(T owner);

    /**
     * Checks if the module can currently be uninstalled.
     * @return true if the module can be safely removed.
     */
    public boolean canUninstall();

    /**
     * Uninstalls the module from its current {@link AppFrame} and performs cleanup.
     */
    public void uninstall();
}
