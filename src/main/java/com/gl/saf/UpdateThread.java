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

import org.slf4j.Logger;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A background thread that periodically triggers updates on registered {@link Updatable} components
 * and active UI windows. All windows or other components, implementing {@link Updatable} can use it to update their
 * elements like UI or enabling/disabling actions.
 */
public class UpdateThread extends Thread {
    /**
     * The default sleep time between updates in milliseconds.
     */
    public static final long SLEEP_TIME = 300;
    protected long sleepTime = SLEEP_TIME;
    private static final Logger logger = LoggerFactory.getLogger(UpdateThread.class);
    protected CopyOnWriteArrayList<Updatable> updatableList = new CopyOnWriteArrayList<>();
    protected Runnable updateRunnable = () -> {
        updateWindows();
        update();
    };

    /**
     * Constructs a new UpdateThread with the default sleep time.
     */
    public UpdateThread() {
    }

    /**
     * Constructs a new UpdateThread with a specified sleep time.
     *
     * @param sleepTime the delay between updates in milliseconds.
     */
    public UpdateThread(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    /**
     * The main execution loop of the thread. Periodically invokes the update logic
     * on the Event Dispatch Thread (EDT).
     */
    public void run() {
        while (!isInterrupted()) {
            try {
                sleep(sleepTime);
                EventQueue.invokeAndWait(updateRunnable);
            } catch (InterruptedException e) {
                interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in UpdateThread loop", e);
            }
        }
    }

    /**
     * Adds an {@link Updatable} object to the list of components to be updated.
     *
     * @param updatable the component to add.
     */
    public void addUpdatable(Updatable updatable) {
        updatableList.addIfAbsent(updatable);
    }

    /**
     * Removes an {@link Updatable} object from the update list.
     *
     * @param updatable the component to remove.
     */
    public void removeUpdatable(Updatable updatable) {
        updatableList.remove(updatable);
    }

    protected void updateWindow(Window window) {
        if (window.isShowing() && window instanceof Updatable updatable)
            updatable.update();
    }

    protected void updateWindows() {
        try {
            Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (activeWindow != null)
                updateWindow(activeWindow);

            for (Window window : Window.getWindows())
                if (activeWindow != window)
                    updateWindow(window);
        } catch (Exception ex) {
            logger.error("Failed to update windows", ex);
        }
    }

    protected void update() {
        try {
            for (Updatable updatable : updatableList)
                updatable.update();
        } catch (Exception e) {
            logger.error("Failed to update updatable's", e);
        }
    }
}
