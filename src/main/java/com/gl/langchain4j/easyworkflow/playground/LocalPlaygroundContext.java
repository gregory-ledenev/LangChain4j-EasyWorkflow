package com.gl.langchain4j.easyworkflow.playground;

import dev.langchain4j.agentic.planner.AgentInstance;

public class LocalPlaygroundContext implements PlaygroundContext {
    private final PlaygroundMetadata.Agent agentMetadata;

    public LocalPlaygroundContext(Object agent) {
        this.agentMetadata = new PlaygroundMetadata.Agent((AgentInstance) agent, null);
    }

    public PlaygroundMetadata.Agent getAgentMetadata() {
        return agentMetadata;
    }
}
