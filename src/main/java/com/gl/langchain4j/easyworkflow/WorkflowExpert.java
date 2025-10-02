package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.out;

/**
 * An AI expert interface that can answer questions about a workflow. Provides a factory method that allow getting an
 * expert for a {@code WorkflowDebugger} and for performing console chat.
 */
public interface WorkflowExpert {
    /**
     * Asks the workflow expert a question.
     *
     * @param userMessage The user's message or question.
     * @return The expert's response.
     */
    String ask(String userMessage);

    /**
     * Initializes and returns a WorkflowExpert instance. Uses RAG to supply a context to a chat, including workflow
     * structure and details about its execution.
     *
     * @param workflowDebugger The WorkflowDebugger instance to use for expert initialization.
     * @return A new WorkflowExpert instance.
     */
    static WorkflowExpert getWorkflowExpert(WorkflowDebugger workflowDebugger) {
        Objects.requireNonNull(workflowDebugger, "workflowDebugger cannot be null");

        Logger platformLogger = Logger.getLogger("ai.djl.util.Platform");
        platformLogger.setLevel(Level.WARNING);

        List<Document> documents = new ArrayList<>();
        documents.add(new DefaultDocument(workflowDebugger.toString(true)));
        documents.add(new DefaultDocument(workflowDebugger.getAgentWorkflowBuilder().toJson()));

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.builder()
                .embeddingModel(new AllMiniLmL6V2EmbeddingModel())
                .embeddingStore(embeddingStore)
                .build().ingest(documents);

        return AiServices.builder(WorkflowExpert.class)
                .chatModel(workflowDebugger.getAgentWorkflowBuilder().getChatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();
    }

    /**
     * Starts a console chat session with a WorkflowExpert, initializing it first.
     *
     * @param workflowDebugger The WorkflowDebugger instance to use for expert initialization.
     * @param userMessage The initial message from the user.
     * @return The expert's response to the initial message.
     */
    static String consoleChat(WorkflowDebugger workflowDebugger, String userMessage) {
        out.print("Please wait. Initializing chat");
        WorkflowExpert workflowExpert = WorkflowDebugger.simulateConsoleProgress(null, aO -> getWorkflowExpert(workflowDebugger));
        out.println();

        return consoleChat(workflowExpert, userMessage);
    }

    /**
     * Starts a console chat session with an existing WorkflowExpert.
     *
     * @param workflowExpert The initialized WorkflowExpert instance.
     * @param userMessage The user's message.
     * @return The expert's response.
     */
    static String consoleChat(WorkflowExpert workflowExpert, String userMessage) {
        return WorkflowDebugger.consoleChat(userMessage, workflowExpert::ask);
    }
}
