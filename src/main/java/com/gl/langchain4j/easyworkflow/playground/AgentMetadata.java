package com.gl.langchain4j.easyworkflow.playground;

import dev.langchain4j.agentic.planner.AgentInstance;

import java.util.List;

public record AgentMetadata(MetadataType type,
                            String name,
                            String agentId,
                            String description,
                            MetadataType outputType,
                            String outputKey,
                            List<AgentArgumentMetadata> arguments,
                            AgentMetadata parent,
                            List<AgentMetadata> subagents) { // TODO: fix it to properly setup parent
    public AgentMetadata(AgentInstance agentInstance, AgentMetadata parent) {
        this(
                new MetadataType(agentInstance.type().getName()),
                agentInstance.name(),
                agentInstance.agentId(),
                agentInstance.description(),
                new MetadataType(agentInstance.outputType().getTypeName()),
                agentInstance.outputKey(),
                agentInstance.arguments().stream()
                        .map(agentArgument -> new AgentArgumentMetadata(new MetadataType(agentArgument.type().getTypeName())))
                        .toList(),
                parent,
                null
        );
//        subagents = agentInstance.subagents().stream()
//                .map(subAgentInstance -> new AgentMetadata(subAgentInstance, this))
//                .toList();

    }
}
