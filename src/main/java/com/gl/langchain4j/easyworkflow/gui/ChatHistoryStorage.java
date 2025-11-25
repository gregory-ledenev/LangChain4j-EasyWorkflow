package com.gl.langchain4j.easyworkflow.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import com.gl.langchain4j.easyworkflow.gui.chat.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.USER_HOME_FOLDER;

/**
 * Manages the storage and retrieval of chat history for a specific agent class. Chat history is stored as a list of
 * {@link ChatHistoryItem} objects in a JSON file within the user's home directory.
 */
public class ChatHistoryStorage {
    private static final Logger logger = LoggerFactory.getLogger(ChatHistoryStorage.class);
    private static final ObjectMapper OBJECT_MAPPER = WorkflowDebugger.createObjectMapper();
    private final Class<?> agentClass;
    protected List<ChatHistoryItem> chatHistoryItems = Collections.synchronizedList(new ArrayList<>());
    protected Map<String, ChatHistoryItem> chatHistoryItemsByUid = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructs a new ChatHistoryStorage for a given agent class.
     *
     * @param agentClass The class of the agent for which chat history is being stored.
     * @throws NullPointerException if agentClass is null.
     */
    public ChatHistoryStorage(Class<?> agentClass) {
        this.agentClass = Objects.requireNonNull(agentClass);
    }

    /**
     * Returns an unmodifiable list of all chat history items.
     *
     * @return A list of {@link ChatHistoryItem} objects.
     */
    public List<ChatHistoryItem> getChatHistoryItems() {
        return chatHistoryItems;
    }

    /**
     * Returns the number of chat history items currently stored.
     *
     * @return The size of the chat history.
     */
    public int getChatHistoryItemsSize() {
        return chatHistoryItems.size();
    }

    /**
     * Retrieves a specific chat history item by its unique identifier.
     *
     * @param uid The unique identifier of the chat history item.
     * @return The {@link ChatHistoryItem} corresponding to the given UID, or null if not found.
     */
    public ChatHistoryItem getChatHistoryItem(String uid) {
        return chatHistoryItemsByUid.get(uid);
    }

    /**
     * Deletes a chat history item with the specified unique identifier. The changes are then stored to persistent
     * storage.
     *
     * @param uid The unique identifier of the chat history item to delete.
     * @return true if the item was found and deleted, false otherwise.
     */
    public synchronized boolean deleteChatHistoryItem(String uid) {
        boolean result = false;
        ChatHistoryItem chatHistoryItem = getChatHistoryItem(uid);
        if (chatHistoryItem != null) {
            chatHistoryItems.remove(chatHistoryItem);
            chatHistoryItemsByUid.remove(chatHistoryItem.uid());

            store();
            result = true;
        }

        return result;
    }

    /**
     * Adds new chat messages to the history. If a {@code chatHistoryItemUid} is provided, it replaces an existing item
     * with the same UID and moves it to the top.
     *
     * @param chatHistoryItemUid The UID of an existing chat history item to update, or null to add a new item.
     * @param chatMessages       The list of {@link ChatMessage} to add or update.
     */
    public synchronized void addChatMessages(String chatHistoryItemUid, List<ChatMessage> chatMessages) {
        if (chatHistoryItemUid == null) {
            // add new item
            ChatHistoryItem item = new ChatHistoryItem(chatMessages);
            add(item);
        } else {
            // replace existing and put it to the top
            ChatHistoryItem oldItem = getChatHistoryItem(chatHistoryItemUid);
            if (oldItem != null)
                chatHistoryItems.remove(oldItem);
            ChatHistoryItem item = new ChatHistoryItem(chatHistoryItemUid, chatMessages, null);
            add(item);
        }

        store();
    }

    private void add(ChatHistoryItem item) {
        chatHistoryItems.add(0, item);
        chatHistoryItemsByUid.put(item.uid, item);
    }

    /**
     * Stores the current chat history to a JSON file in the user's home directory.
     */
    public synchronized void store() {
        File userHome = new File(System.getProperty("user.home"), USER_HOME_FOLDER);
        if (!userHome.exists()) {
            boolean result = userHome.mkdirs();
            if (!result)
                logger.error("Failed to create folders for path: " + userHome);
        }

        File agentFile = new File(userHome, getFileName());

        if (chatHistoryItems.isEmpty()) {
            if (agentFile.exists()) {
                boolean result = agentFile.delete();
                if (!result)
                    logger.warn("Failed to delete chat history: " + agentFile);
            }
        } else {
            try {
                Files.writeString(Paths.get(agentFile.getAbsolutePath()),
                        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(chatHistoryItems));
            } catch (Exception ex) {
                logger.error("Failed to store chat history for agent {}", agentClass.getName(), ex);
            }
        }
    }

    private String getFileName() {
        return "chat-history-" + agentClass.getName() + ".json";
    }

    /**
     * Loads the chat history from a JSON file in the user's home directory.
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
            chatHistoryItems = Collections.synchronizedList(OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ChatHistoryItem.class)));

            chatHistoryItemsByUid.clear();
            for (ChatHistoryItem item : chatHistoryItems)
                chatHistoryItemsByUid.put(item.uid(), item);
        } catch (IOException ex) {
            logger.error("Failed to load chat history for agent {}", agentClass.getName(), ex);
        }
    }

    /**
     * Represents a single item in the chat history, containing a unique ID, a list of chat messages, and the date it
     * was created or last updated.
     */
    public record ChatHistoryItem(String uid, List<ChatMessage> chatMessages, Date date) {
        /**
         * Constructs a ChatHistoryItem with a specified UID, messages, and date. If the date is null, it defaults to
         * the current date.
         *
         * @param uid          The unique identifier for this chat history item.
         * @param chatMessages The list of chat messages.
         * @param date         The date of this chat history item, or null to use the current date.
         */
        public ChatHistoryItem(String uid, List<ChatMessage> chatMessages, Date date) {
            this.uid = uid;
            this.chatMessages = chatMessages.stream().map(aChatMessage -> new ChatMessage(aChatMessage, true)).toList();
            this.date = date != null ? date : new Date();
        }

        /**
         * Constructs a new ChatHistoryItem with a randomly generated UID and the current date.
         *
         * @param chatMessages The list of chat messages for this item.
         */
        public ChatHistoryItem(List<ChatMessage> chatMessages) {
            this(UUID.randomUUID().toString(), chatMessages, null);
        }
    }
}
