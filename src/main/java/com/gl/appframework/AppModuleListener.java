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
 * Listener interface for receiving notifications when application modules are installed or uninstalled.
 */
public interface AppModuleListener {
    /**
     * Invoked after an application module has been successfully installed.
     *
     * @param appFrame  the application frame where the module was installed
     * @param appModule the module that was installed
     */
    void appModuleInstalled(AppFrame appFrame, AppModule<AppFrame> appModule);

    /**
     * Invoked after an application module has been uninstalled.
     *
     * @param appFrame  the application frame where the module was removed
     * @param appModule the module that was uninstalled
     */
    void appModuleUninstalled(AppFrame appFrame, AppModule<AppFrame> appModule);
}
