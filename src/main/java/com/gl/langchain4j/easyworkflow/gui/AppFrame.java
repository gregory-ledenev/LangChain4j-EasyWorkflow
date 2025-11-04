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

package com.gl.langchain4j.easyworkflow.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.gl.langchain4j.easyworkflow.gui.Application.getSharedApplication;

/**
 * The base class for all application frames.
 * Provides basic window management, including closing behavior and state management.
 */
public class AppFrame extends JFrame implements Application.ScheduledUpdatable {
    private final String uid;

    /**
     * Constructs a new {@code AppFrame} with the specified unique identifier.
     *
     * @param uid The unique identifier for this frame.
     */
    public AppFrame(String uid) throws HeadlessException {
        this.uid = uid;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close(false);
                getSharedApplication().maybeExit();
            }
        });
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
        return true;
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

    /**
     * Restores the state of the frame. Subclasses should override this method
     * to load any saved state (e.g., window position, size, user preferences).
     */
    public void restoreState() {
    }

    /**
     * Saves the current state of the frame. Subclasses should override this method
     * to persist any relevant state information (e.g., window position, size, user preferences).
     */
    public void saveState() {
    }

    /**
     * Updates the content or state of the frame. This method can be called by an updater thread so it should return as
     * quickly as possible. Subclasses can override this method
     * to refresh displayed data, update state, or perform other updates as needed.
     */
    public void scheduledUpdate() {
    }
}
