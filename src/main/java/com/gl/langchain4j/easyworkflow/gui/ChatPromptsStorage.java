/*
 * Copyright 2026 Gregory Ledenev (gregory.ledenev37@gmail.com)
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.USER_HOME_FOLDER;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.KEY_SESSION_UID;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.deepClone;

/**
 * Manages the persistence and retrieval of chat prompts for a specific agent.
 * Supports pinning, ordering, and limiting the number of stored prompts.
 */
public class ChatPromptsStorage {
    /**
     * Maximum number of prompts to store.
     */
    static final int MAX_COUNT = 50;
    private static final Logger logger = EasyWorkflow.getLogger(ChatPromptsStorage.class);
    private static final ObjectMapper OBJECT_MAPPER = WorkflowDebugger.createObjectMapper();
    private final String agentClassName;
    private List<ChatPrompt> chatPrompts = new ArrayList<>();
    private boolean autocommit = true;
    private boolean dirty = false;

    /**
     * Creates a new storage instance for the specified agent class.
     *
     * @param agentClassName the name of the agent class associated with these prompts
     */
    public ChatPromptsStorage(String agentClassName) {
        this.agentClassName = Objects.requireNonNull(agentClassName);
    }

    /**
     * Checks if the storage has unsaved changes.
     *
     * @return true if there are unsaved changes, false otherwise
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the dirty flag for the storage.
     *
     * @param dirty true to mark as dirty, false to clear
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Checks if changes to the storage are automatically persisted to disk.
     *
     * @return true if autocommit is enabled, false otherwise
     */
    public boolean isAutocommit() {
        return autocommit;
    }

    /**
     * Sets whether changes to the storage should be automatically persisted to disk.
     * If disabled, {@link #store()} must be called manually.
     *
     * @param autocommit true to enable autocommit, false to disable
     */
    public void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    /**
     * Returns an unmodifiable list of the currently stored chat prompts.
     *
     * @return a list of {@link ChatPrompt} objects
     */
    public synchronized List<ChatPrompt> getChatPrompts() {
        return Collections.unmodifiableList(chatPrompts);
    }

    /**
     * Adds a chat prompt to the storage. If the prompt already exists, it is moved to the top
     * (preserving its pinned status). Maintains the {@link #MAX_COUNT} limit.
     *
     * @param chatPrompt the prompt to add
     */
    public synchronized void addChatPrompt(ChatPrompt chatPrompt) {
        int index = chatPrompts.indexOf(chatPrompt);
        if (index >= 0) {
            if (chatPrompts.get(index).isPinned())
                chatPrompt.setPinned(true);
            chatPrompts.remove(index);
        }

        // Insert logic for new or moved items
        int insertIndex = 0;
        while (!chatPrompt.isPinned() && insertIndex < chatPrompts.size() && chatPrompts.get(insertIndex).isPinned())
            insertIndex++;

        chatPrompts.add(insertIndex, chatPrompt);

        if (chatPrompts.size() > MAX_COUNT) {
            chatPrompts.remove(chatPrompts.size() - 1);
        }

        setDirty(true);
        maybeStore();
    }

    private void maybeStore() {
        if (isAutocommit())
            store();
    }

    /**
     * Removes a specific chat prompt from the storage.
     *
     * @param chatPrompt the prompt to remove
     */
    public synchronized void removeChatPrompt(ChatPrompt chatPrompt) {
        chatPrompts.remove(chatPrompt);

        setDirty(true);
        maybeStore();
    }

    /**
     * Returns the name of the agent class associated with this storage.
     *
     * @return the agent class name
     */
    public String getAgentClassName() {
        return agentClassName;
    }

    /**
     * Replaces the entire list of stored chat prompts with a new list.
     *
     * @param chatPrompts the new list of {@link ChatPrompt} objects
     */
    public synchronized void replaceChatPrompts(List<ChatPrompt> chatPrompts) {
        this.chatPrompts.clear();
        this.chatPrompts.addAll(chatPrompts);

        setDirty(true);
        maybeStore();
    }

    /**
     * Sets the pinned status of a chat prompt and reorders the list accordingly.
     *
     * @param chatPrompt the prompt to update
     * @param pinned     true to pin the prompt, false to unpin
     */
    public synchronized void setPinned(ChatPrompt chatPrompt, boolean pinned) {
        if (chatPrompt.isPinned() == pinned)
            return;

        int index = chatPrompts.indexOf(chatPrompt);
        if (index < 0)
            return;

        chatPrompts.remove(index);
        chatPrompt.setPinned(pinned);

        // insert it to the top if pinned; or right after the last pinned prompt
        int insertIndex = 0;
        while (!chatPrompt.isPinned() && insertIndex < chatPrompts.size() && chatPrompts.get(insertIndex).isPinned())
            insertIndex++;
        chatPrompts.add(insertIndex, chatPrompt);

        setDirty(true);
        maybeStore();
    }

    /**
     * Checks if a prompt can be moved up in the list.
     *
     * @param chatPrompt the prompt to check
     * @return true if the prompt can be moved up
     */
    public synchronized boolean canMoveUp(ChatPrompt chatPrompt) {
        int index = chatPrompts.indexOf(chatPrompt);
        if (index <= 0) return false;
        ChatPrompt prev = chatPrompts.get(index - 1);
        return chatPrompt.isPinned() || !prev.isPinned();
    }

    /**
     * Moves a prompt up in the list if possible.
     *
     * @param chatPrompt the prompt to move
     */
    public synchronized void moveUp(ChatPrompt chatPrompt) {
        if (!canMoveUp(chatPrompt)) return;
        int index = chatPrompts.indexOf(chatPrompt);
        Collections.swap(chatPrompts, index, index - 1);

        setDirty(true);
        maybeStore();
    }

    /**
     * Checks if a prompt can be moved down in the list.
     *
     * @param chatPrompt the prompt to check
     * @return true if the prompt can be moved down
     */
    public synchronized boolean canMoveDown(ChatPrompt chatPrompt) {
        int index = chatPrompts.indexOf(chatPrompt);
        if (index < 0 || index >= chatPrompts.size() - 1) return false;
        ChatPrompt next = chatPrompts.get(index + 1);
        return !chatPrompt.isPinned() || next.isPinned();
    }

    /**
     * Moves a prompt down in the list if possible.
     *
     * @param chatPrompt the prompt to move
     */
    public synchronized void moveDown(ChatPrompt chatPrompt) {
        if (!canMoveDown(chatPrompt)) return;
        int index = chatPrompts.indexOf(chatPrompt);
        Collections.swap(chatPrompts, index, index + 1);

        setDirty(true);
        maybeStore();
    }

    /**
     * Persists the current list of prompts to a JSON file.
     */
    public synchronized void store() {
        File userHome = new File(System.getProperty("user.home"), USER_HOME_FOLDER);
        if (!userHome.exists()) {
            boolean result = userHome.mkdirs();
            if (!result)
                logger.error("Failed to create folders for path: {}", userHome);
        }

        File file = new File(userHome, getFileName());

        if (chatPrompts.isEmpty()) {
            if (file.exists()) {
                boolean result = file.delete();
                if (!result)
                    logger.warn("Failed to delete chat history: {}", file);
            }
        } else {
            try {
                Files.writeString(Paths.get(file.getAbsolutePath()),
                        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(chatPrompts));
                setDirty(false);
            } catch (Exception ex) {
                logger.error("Failed to store prompts for agent {}", agentClassName, ex);
            }
        }
    }

    /**
     * Loads the list of prompts from the JSON file.
     */
    public synchronized void load() {
        File userHome = new File(System.getProperty("user.home"), USER_HOME_FOLDER);
        if (!userHome.exists())
            return;

        File agentFile = new File(userHome, getFileName());

        if (!agentFile.exists())
            return;

        try {
            String json = Files.readString(Paths.get(agentFile.getAbsolutePath()));
            chatPrompts = Collections.synchronizedList(OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ChatPrompt.class)));
            setDirty(false);
        } catch (IOException ex) {
            logger.error("Failed to load prompts for agent {}", agentClassName, ex);
        }
    }

    private String getFileName() {
        return "prompts-" + agentClassName + ".json";
    }

    /**
     * Defines the type of the prompt content.
     */
    public enum ChatPromptType {
        String, Map
    }

    /**
     * Represents a single chat prompt entry with metadata.
     */
    public static class ChatPrompt implements Cloneable {
        /**
         * Maximum length of the string representation in HTML.
         */
        public static final int MAX_HTML_STRING_LENGTH = 100;
        private final ChatPromptType type;
        private final long timestamp;
        private final Object prompt;
        private volatile boolean pinned;

        /**
         * Creates a new ChatPrompt.
         *
         * @param type   the type of the prompt
         * @param prompt the prompt content
         */
        public ChatPrompt(ChatPromptType type, Object prompt) {
            this(type, System.currentTimeMillis(), prompt, false);
        }

        /**
         * Constructor used for JSON deserialization.
         */
        @JsonCreator
        public ChatPrompt(@JsonProperty("type") ChatPromptType type,
                          @JsonProperty("timestamp") long timestamp,
                          @JsonProperty("prompt") Object prompt,
                          @JsonProperty("pinned") boolean pinned) {
            Objects.requireNonNull(prompt);

            this.type = type;
            this.timestamp = timestamp;
            this.pinned = pinned;

            if (type == ChatPromptType.Map) {
                Map<?, ?> promptCopy = new HashMap<>((Map<?, ?>) prompt);
                promptCopy.remove(KEY_SESSION_UID);
                this.prompt = promptCopy;
            } else {
                this.prompt = prompt;
            }
        }

        /**
         * Returns the type of the prompt.
         *
         * @return the {@link ChatPromptType}
         */
        public ChatPromptType getType() {
            return type;
        }

        /**
         * Returns the creation timestamp of the prompt.
         *
         * @return timestamp in milliseconds
         */
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ChatPrompt chatPrompt1 = (ChatPrompt) o;
            return Objects.equals(prompt, chatPrompt1.prompt);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(prompt);
        }

        /**
         * Returns the raw prompt object.
         *
         * @return the prompt content
         */
        public Object getPrompt() {
            return prompt;
        }

        /**
         * Checks if the prompt is pinned.
         *
         * @return true if pinned
         */
        public boolean isPinned() {
            return pinned;
        }

        void setPinned(boolean pinned) {
            this.pinned = pinned;
        }

        /**
         * Returns an HTML representation of the prompt for display in the UI.
         *
         * @return HTML formatted string
         */
        public String toHtmlString() {
            String result = "";

            if (type == ChatPromptType.Map) {
                StringBuilder sb = new StringBuilder();
                Map<?, ?> map = (Map<?, ?>) prompt;
                if (map.size() > 1) {
                    map.forEach((k, v) -> {
                        if (!sb.isEmpty())
                            sb.append(", ");
                        sb.append("<b>")
                                .append(GUIPlayground.getHtmlSafeString(k))
                                .append(":</b> ")
                                .append(GUIPlayground.getHtmlSafeString(v));
                    });
                    result = sb.toString();
                } else {
                    result = map.values().iterator().next().toString();
                }
            } else {
                result = prompt.toString();
            }

            if (result.length() > MAX_HTML_STRING_LENGTH)
                result = result.substring(0, MAX_HTML_STRING_LENGTH) + "...";

            return "<html>%s %s</html>".formatted(pinned ? "◼" : "◻", result);
        }

        @Override
        public String toString() {
            return prompt.toString();
        }

        @Override
        public ChatPrompt clone() {
            return new ChatPrompt(this.type, this.timestamp, deepClone(prompt), this.pinned);
        }
    }
}
