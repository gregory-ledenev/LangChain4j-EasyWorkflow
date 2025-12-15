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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.isToday;

/**
 * Represents a chat message, which can be either from the user or the system. It can contain both plain text and HTML
 * formatted content.
 *
 * @param date        The date and time when the message was sent.
 * @param chatModel   The name of the chat model used for the message.
 * @param uid         Unique identifier for the message.
 * @param rawMessage  The message in original format
 * @param message     The plain text content of the message.
 * @param htmlMessage The HTML formatted content of the message. Can be null if only plain text is available.
 * @param type        The type of the message.
 */
public record ChatMessage(Date date, String chatModel, String uid, Object rawMessage, String message, String htmlMessage, Type type, boolean history) {
    public enum Type {
        User,
        Agent,
        System
    }

    public ChatMessage(String uid, Object rawMessage, String message, String htmlMessage, Type type, boolean history) {
        this(null, null, uid, rawMessage, message, htmlMessage, type, history);
    }

    public ChatMessage(String aUid, Object aRawMessage, String aMessage, String aHtmlMessage, Type aType) {
        this(aUid, aRawMessage, aMessage, aHtmlMessage, aType, false);
    }

    public ChatMessage(ChatMessage chatMessage, boolean history) {
        this(chatMessage.date(), chatMessage.chatModel(), chatMessage.uid(), chatMessage.rawMessage(), chatMessage.message(), chatMessage.htmlMessage(), chatMessage.type(), history);
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

    public String title() {
        List<String> parts = new ArrayList<>();

        if (date() != null)
            parts.add(String.format(isToday(date()) ? "Today %1$tT" : "%1$tD %1$tT", date()));

        if (chatModel() != null)
            parts.add(chatModel());

        if (parts.isEmpty())
            parts.add(type().toString());

        return String.join(", ", parts);
    }
}
