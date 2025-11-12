package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * An AI expert interface that can answer questions about a workflow.
 */
public interface WorkflowExpert {
    /**
     * Asks the workflow expert a question.
     *
     * @param question The user's message or question.
     * @return The expert's response.
     */
    @SystemMessage("You are a helpful assistant that can answer questions about a workflow and its execution results.")
    @UserMessage("{{question}}")
    @Agent
    String ask(@V("question") String question);

    default String askMap(Map<String, Object> input) {
        return ask(input.get("question").toString());
    }
}
