package com.gl.langchain4j.easyworkflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gl.langchain4j.easyworkflow.Playground.ARG_TITLE;
import static java.lang.System.out;

/**
 * Provides a factory method that allow getting an expert for a {@code WorkflowDebugger} and for performing a playground
 * session.
 */
public class WorkflowExpertSupport {
    /**
     * Initializes and returns a WorkflowExpert instance. Uses RAG to supply a context to a chat, including workflow
     * structure and details about its execution.
     *
     * @param workflowDebugger The WorkflowDebugger instance to use for expert initialization.
     * @return A new WorkflowExpert instance.
     */
    public static WorkflowExpert getWorkflowExpert(WorkflowDebugger workflowDebugger) {
        Objects.requireNonNull(workflowDebugger, "workflowDebugger cannot be null");

        Logger platformLogger = Logger.getLogger("ai.djl.util.Platform");
        platformLogger.setLevel(Level.WARNING);

        List<Document> documents = new ArrayList<>();
        documents.add(new DefaultDocument(workflowDebugger.toString(true)));
        documents.add(new DefaultDocument(workflowDebugger.getAgentWorkflowBuilder().toJson()));

        if (workflowDebugger.getAgenticScope() != null) {
            try {
                String context = new ObjectMapper().writeValueAsString(workflowDebugger.getAgenticScope().state());
                documents.add(new DefaultDocument(context, new Metadata(Map.of("type", "workflow context",
                        "description", "Workflow context with state variables"))));
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }

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
     * Starts a playground session with a WorkflowExpert, initializing it first.
     *
     * @param workflowDebugger The WorkflowDebugger instance to use for expert initialization.
     * @param workflowTitle    The title of the workflow.
     * @param userMessage      The initial message from the user.
     * @param type             The type of playground to create.
     */
    public static void play(WorkflowDebugger workflowDebugger, String workflowTitle, String userMessage, Playground.Type type) {
        out.print("Please wait. Initializing chat");

        play(getWorkflowExpert(workflowDebugger), workflowTitle, userMessage, type);
    }

    /**
     * Starts a playground session with an existing WorkflowExpert.
     *
     * @param workflowExpert The initialized WorkflowExpert instance.
     * @param workflowTitle  The title of the workflow.
     * @param userMessage    The user's message.
     * @param type           The type of playground to create.
     */
    public static void play(WorkflowExpert workflowExpert, String workflowTitle, String userMessage, Playground.Type type) {
        Playground playground = Playground.createPlayground(WorkflowExpert.class, type);
        playground.setup(Map.of(ARG_TITLE, "Workflow Expert - %s".formatted(workflowTitle)));
        playground.play(workflowExpert, Map.of("userMessage", userMessage));
    }
}
