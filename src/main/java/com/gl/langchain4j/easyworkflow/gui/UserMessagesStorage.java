/*
 *
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
 * /
 */

package com.gl.langchain4j.easyworkflow.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.USER_HOME_FOLDER;

/**
 * This class provides a persistent storage for user-defined user messages
 */
public class UserMessagesStorage {
    private static final Logger logger = EasyWorkflow.getLogger(UserMessagesStorage.class);
    private final WorkflowDebugger workflowDebugger;
    private final Function<String, String> originalUserMessageProvider;

    /**
     * Creates a new instance of {@code UserMessagesPersistentStorage} and initializes it with a workflow debugger and a
     * function that provides original user messages
     *
     * @param workflowDebugger            A {@code WorkflowDebugger} to work with
     * @param originalUserMessageProvider A function that provides original user messages
     */
    public UserMessagesStorage(WorkflowDebugger workflowDebugger, Function<String, String> originalUserMessageProvider) {
        Objects.requireNonNull(workflowDebugger);
        Objects.requireNonNull(originalUserMessageProvider);

        this.workflowDebugger = workflowDebugger;
        this.originalUserMessageProvider = originalUserMessageProvider;
    }

    public record UserMessageEntry(String agentClassName, String originalUserMessage, String userMessage) {}
    private static final ObjectMapper OBJECT_MAPPER = WorkflowDebugger.createObjectMapper();

    /**
     * Exports user messages to a JSON string
     * @return a JSON string representing user messages or {@code null} if no user messages as present
     */
    public String asJson() {
        Map<String, String> userMessageTemplates = workflowDebugger.getUserMessageTemplates();
        if (userMessageTemplates.isEmpty())
            return null;

        List<UserMessageEntry> userMessageEntries = new ArrayList<>();
        userMessageTemplates.forEach((agentClassName, userMessage) -> userMessageEntries.add(new UserMessageEntry(agentClassName,
                getUserMessage(agentClassName),
                userMessage)));

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(userMessageEntries);
        } catch (JsonProcessingException ex) {
            logger.error("Failed to serialize user messages", ex);
            return null;
        }
    }

    private String getUserMessage(String agentClassName) {
        return originalUserMessageProvider.apply(agentClassName);
    }

    /**
     * Stores the user-defined user messages for the agents in the current workflow to a file under the user home folder.
     */
    public synchronized void store() {
        File userHome = new File(System.getProperty("user.home"), USER_HOME_FOLDER);
        if (!userHome.exists()) {
            boolean result = userHome.mkdirs();
            if (! result)
                logger.error("Failed to create folders for path: " + userHome);
        }

        Class<?> agentClass = workflowDebugger.getAgentWorkflowBuilder().getAgentClass();
        File agentFile = new File(userHome, getFileName(agentClass));

        if (! workflowDebugger.hasUserMessageTemplates()) {
            if (agentFile.exists()) {
                boolean result = agentFile.delete();
                if (! result)
                    logger.warn("Failed to delete stored user messages: " + agentFile);
            }
        } else {
            try {
                Files.writeString(Paths.get(agentFile.getAbsolutePath()), asJson());
            } catch (Exception ex) {
                logger.error("Failed to store user messages for agent {}", agentClass.getName(), ex);
            }
        }
    }

    private static String getFileName(Class<?> agentClass) {
        return "user-messages-" + agentClass.getName() + ".json";
    }

    /**
     * Loads the user-defined user messages from a file for the current agent.
     */
    public synchronized void load() {
        File userHome = new File(System.getProperty("user.home"), USER_HOME_FOLDER);
        if (!userHome.exists())
            return;

        Class<?> agentClass = workflowDebugger.getAgentWorkflowBuilder().getAgentClass();
        File agentFile = new File(userHome, getFileName(agentClass));

        if (!agentFile.exists())
            return;

        try {
            String json = Files.readString(Paths.get(agentFile.getAbsolutePath()));
            List<UserMessageEntry> userMessageEntries = OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, UserMessageEntry.class));

            userMessageEntries.forEach(userMessageEntry -> {
                String agentClassName = userMessageEntry.agentClassName();
                if (Objects.equals(userMessageEntry.originalUserMessage(), getUserMessage(agentClassName)))
                    workflowDebugger.setUserMessageTemplate(agentClassName, userMessageEntry.userMessage());
                else
                    logger.debug("Skipping custom user message for class: " + agentClassName);
            });
        } catch (IOException ex) {
            logger.error("Failed to load user messages for agent {}", agentClass.getName(), ex);
        }
    }
}
