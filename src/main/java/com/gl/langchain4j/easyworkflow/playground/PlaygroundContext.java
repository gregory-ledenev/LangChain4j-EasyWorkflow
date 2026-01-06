package com.gl.langchain4j.easyworkflow.playground;

import java.util.List;
import java.util.Map;

public interface PlaygroundContext {
    PlaygroundMetadata.Agent getAgentMetadata();
    Object sendMessage(Map<String, Object> message);

    PlaygroundMetadata.Model getChatModel();
    void setChatModel(PlaygroundMetadata.Model chatModel);

    List<PlaygroundMetadata.Model> getChatModels();
}
