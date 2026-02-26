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

import java.awt.*;
import java.net.URI;
import java.util.List;

/**
 * Interface for providing an "About" dialog or information. Implementations of this interface can be
 * used to display application-specific information when an "About" action is triggered.
 */
public interface AboutProvider {

    /**
     * Displays the "About" dialog or information.
     *
     * @param parent The parent component to which the dialog should be relative.
     */
    void showAbout(Component parent);

    /**
     * Opens a specific link associated with the "About" information. This method should be overridden if there links to open.
     *
     * @param aboutLink The link to be opened.
     */
    default void openLink(AboutLink aboutLink) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(aboutLink.url()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a list of links relevant to the application's "About" information.
     *
     * @return A list of {@link AboutLink} objects.
     */
    default List<AboutLink> getAboutLinks() {
        return List.of();
    }

    /**
     * Represents a link with a title and a URL.
     *
     * @param title The display title of the link.
     * @param url   The destination URL.
     */
    record AboutLink(String title, String url) {}
}
