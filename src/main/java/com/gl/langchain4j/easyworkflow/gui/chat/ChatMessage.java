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

package com.gl.langchain4j.easyworkflow.gui.chat;

/**
 * Represents a chat message, which can be either from the user or the system. It can contain both plain text and HTML
 * formatted content.
 *
 * @param uid         Unique identifier for the message.
 * @param rawMessage  The message in original format
 * @param message     The plain text content of the message.
 * @param htmlMessage The HTML formatted content of the message. Can be null if only plain text is available.
 * @param type        The type of the message.
 */
public record ChatMessage(String uid, Object rawMessage, String message, String htmlMessage, Type type) {
    public enum Type {
        User,
        Agent,
        System
    }
    /**
     * Returns the best available representation of the message. Prioritizes HTML content if available, otherwise returns the plain text message.
     * @return The HTML message if {@code htmlMessage} is not null and not empty, otherwise the plain text {@code message}.
     */
    public String bestMessage() {
        return htmlMessage() != null && ! htmlMessage().isEmpty() ? htmlMessage() : message();
    }


    /**
     * Checks if the message is outgoing (i.e., from the User or System).
     * @return {@code true} if the message type is not {@code Agent}, {@code false} otherwise.
     */
    public boolean outgoing(){
        return type() != ChatMessage.Type.Agent;
    }
}
