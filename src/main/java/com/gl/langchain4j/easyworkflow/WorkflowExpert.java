package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.out;

/**
 * An AI expert interface that can answer questions about a workflow.
 */
public interface WorkflowExpert {
    /**
     * Asks the workflow expert a question.
     *
     * @param userMessage The user's message or question.
     * @return The expert's response.
     */
    @UserMessage("{{rawMessage}}")
    @Agent
    String ask(@V("rawMessage") String userMessage);

    default String askMap(Map<String, Object> input) {
        return ask(input.get("rawMessage").toString());
    }
}
