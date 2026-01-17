package com.gl.langchain4j.easyworkflow.playground;

import java.util.List;
import java.util.Map;

/**
 * The {@code PlaygroundContext} interface defines the contract for interacting with the playground environment.
 * It provides methods to access agent metadata, send messages, manage chat models, and handle user message templates.
 */
public interface PlaygroundContext {

    /**
     * Retrieves the metadata for the current agent in the playground environment.
     *
     * @return The {@link PlaygroundMetadata.Agent} object containing information about the agent.
     */
    PlaygroundMetadata.Agent getAgentMetadata();

    /**
     * Generates a summary of the current agent in the playground environment.
     *
     * @return An {@link Object} representing the agent's summary.
     */
    String generateAgentSummary();

    /**
     * Sends a message to a current agent in the playground environment. The structure and content of the message
     * are defined by the provided map.
     *
     * @param message A {@link Map} representing the message to be sent. The keys and values
     *                within the map depend on the specific message type and playground implementation.
     * @return An {@link Object} representing the response or result of sending the message.
     *         The actual type of the returned object depends on the playground implementation.
     */
    Object sendMessage(Map<String, Object> message);

    // region Models

    /**
     * Retrieves the currently selected chat model for the playground environment.
     *
     * @return The {@link PlaygroundMetadata.Model} object representing the current chat model.
     */
    PlaygroundMetadata.Model getChatModel();

    /**
     * Sets the current chat model for the playground environment.
     *
     * @param chatModel The {@link PlaygroundMetadata.Model} object to set as the current chat model.
     */
    void setChatModel(PlaygroundMetadata.Model chatModel);

    /**
     * Retrieves a list of all available chat models in the playground environment.
     *
     * @return A {@link List} of {@link PlaygroundMetadata.Model} objects representing
     *         all available chat models.
     */
    List<PlaygroundMetadata.Model> getChatModels();
    // endregion

    //region User Message Templates

    /**
     * Indicates whether the playground environment supports user message templates.
     *
     * @return {@code true} if user message templates are supported, {@code false} otherwise.
     */
    boolean supportsUserMessageTemplates();
    /**
     * Returns an unmodifiable map of user message templates, where keys are agent classes and values are their
     * corresponding user message template strings.
     *
     * @return An unmodifiable map of user message templates.
     */
    Map<String, String> getUserMessageTemplates();

    /**
     * Checks if there are any user message templates configured.
     *
     * @return {@code true} if there are user message templates, {@code false} otherwise.
     */
    boolean hasUserMessageTemplates();

    /**
     * Returns the user message template associated with the given agent class name.
     *
     * @param agentClassName The fully qualified name of the agent class.
     * @return The user message template string, or {@code null} if not found or if the class cannot be loaded.
     */
    String getUserMessageTemplate(String agentClassName);

    /**
     * Sets the user message template for a given agent class name. Use this method to alter a user message for a particular
     * agent class.
     *
     * @param agentClassName      The class name of the agent.
     * @param userMessageTemplate The user message template string to associate with the agent class.
     */
    void setUserMessageTemplate(String agentClassName, String userMessageTemplate);
    // endregion
}
