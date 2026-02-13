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

public class ChatPromptsStorage {
    private static final Logger logger = EasyWorkflow.getLogger(ChatPromptsStorage.class);
    private static final ObjectMapper OBJECT_MAPPER = WorkflowDebugger.createObjectMapper();
    private final String agentClassName;

    private List<ChatPrompt> chatPrompts = new ArrayList<>();

    public ChatPromptsStorage(String agentClassName) {
        this.agentClassName = Objects.requireNonNull(agentClassName);
    }

    public List<ChatPrompt> getChatPrompts() {
        return Collections.unmodifiableList(chatPrompts);
    }

    static final int MAX_COUNT = 20;

    public synchronized void addChatPrompt(ChatPrompt chatPrompt) {
        chatPrompts.remove(chatPrompt);
        chatPrompts.add(0, chatPrompt);
        if (chatPrompts.size() > MAX_COUNT)
            chatPrompts.remove(chatPrompts.size()-1);

        store();
    }

    public synchronized void removeChatPrompt(ChatPrompt chatPrompt) {
        chatPrompts.remove(chatPrompt);

        store();
    }

    public synchronized void store() {
        File userHome = new File(System.getProperty("user.home"), USER_HOME_FOLDER);
        if (!userHome.exists()) {
            boolean result = userHome.mkdirs();
            if (!result)
                logger.error("Failed to create folders for path: " + userHome);
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
            } catch (Exception ex) {
                logger.error("Failed to store prompts for agent {}", agentClassName, ex);
            }
        }
    }

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
        } catch (IOException ex) {
            logger.error("Failed to load prompts for agent {}", agentClassName, ex);
        }
    }

    private String getFileName() {
        return "prompts-" + agentClassName + ".json";
    }

    public enum PromptType {
        String, Map
    }
    public static class ChatPrompt {
        private final PromptType promptType;
        private final long timestamp;
        private final Object chatPrompt;

        public ChatPrompt(PromptType promptType, Object prompt) {
            this(promptType, System.currentTimeMillis(), prompt);
        }

        @JsonCreator
        public ChatPrompt(@JsonProperty("promptType") PromptType promptType,
                          @JsonProperty("timestamp") long timestamp,
                          @JsonProperty("chatPrompt") Object prompt) {
            Objects.requireNonNull(prompt);

            this.promptType = promptType;
            this.timestamp = timestamp;
            if (promptType == PromptType.Map) {
                Map<?, ?> promptCopy = new HashMap<>((Map<?, ?>) prompt);
                promptCopy.remove(KEY_SESSION_UID);
                this.chatPrompt = promptCopy;
            } else {
                this.chatPrompt = prompt;
            }
        }

        public PromptType getPromptType() {
            return promptType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ChatPrompt chatPrompt1 = (ChatPrompt) o;
            return Objects.equals(chatPrompt, chatPrompt1.chatPrompt);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(chatPrompt);
        }

        public Object getChatPrompt() {
            return chatPrompt;
        }
    }
}
