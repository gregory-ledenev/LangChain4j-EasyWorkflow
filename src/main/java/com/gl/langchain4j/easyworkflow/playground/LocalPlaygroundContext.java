package com.gl.langchain4j.easyworkflow.playground;

import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.V;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class LocalPlaygroundContext implements PlaygroundContext {
    private PlaygroundMetadata.Agent agentMetadata;
    private Object agent;
    private Method agentMethod;
    private final EasyWorkflow.AgentWorkflowBuilder<?> builder;
    private Playground.PlaygroundChatModel chatModel;
    private final List<Playground.PlaygroundChatModel> chatModels;
    private List<PlaygroundMetadata.Model> metadataChatModels;

    public LocalPlaygroundContext(Object agent, EasyWorkflow.AgentWorkflowBuilder<?> builder,
                                  Playground.PlaygroundChatModel chatModel,
                                  List<Playground.PlaygroundChatModel> chatModels) {
        this.agent = Objects.requireNonNull(agent);
        this.builder = builder;
        this.chatModel = chatModel;
        this.chatModels = chatModels;
        if (chatModels != null)
            metadataChatModels = chatModels
                    .stream()
                    .map(playgroundChatModel -> new PlaygroundMetadata.Model(playgroundChatModel.id(), playgroundChatModel.name()))
                    .toList();
        if (agent instanceof AgentInstance agentInstance) {
            this.agentMethod = Objects.requireNonNull(EasyWorkflow.getAgentMethod(agentInstance.type()));
            this.agentMetadata = new PlaygroundMetadata.Agent(agentInstance, null);
        }
    }

    public static LocalPlaygroundContext createLocalPlaygroundContext(Object agent, EasyWorkflow.AgentWorkflowBuilder<?> builder,
                                                                      ChatModel model,
                                                                      List<ChatModel> models) {
        return new LocalPlaygroundContext(agent, builder,
                new Playground.PlaygroundChatModel(model),
                models.stream()
                        .map(Playground.PlaygroundChatModel::new)
                        .toList());
    }

    public PlaygroundMetadata.Agent getAgentMetadata() {
        return agentMetadata;
    }

    @Override
    public Object sendMessage(Map<String, Object> message) {
        try {
            Object[] args = requestToArguments(message, agentMethod.getParameters());
            return agentMethod.invoke(agent, args);
        } catch (Exception ex) {
            throw  new RuntimeException(WorkflowDebugger.getFailureCauseException(ex));
        }
    }

    @Override
    public PlaygroundMetadata.Model getChatModel() {
        return chatModel != null ? new PlaygroundMetadata.Model(chatModel.id(), chatModel.name()) : null;
    }

    @Override
    public void setChatModel(PlaygroundMetadata.Model chatModel) {
        if (getChatModel().equals(chatModel))
            return;

        Objects.requireNonNull(chatModel);

        Optional<Playground.PlaygroundChatModel> first = chatModels.stream().filter(playgroundChatModel -> playgroundChatModel.id().equals(chatModel.id())).findFirst();
        first.ifPresent(playgroundChatModel -> {
            this.chatModel = playgroundChatModel;
            builder.chatModel(playgroundChatModel.chatModel());
            agent = builder.build();
            this.agentMetadata = new PlaygroundMetadata.Agent((AgentInstance) agent, null);
        });
    }

    @Override
    public List<PlaygroundMetadata.Model> getChatModels() {
        return metadataChatModels;
    }

    private static Object[] requestToArguments(Map<String, Object> request, Parameter[] parameters) {
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.getName();
            V v = parameter.getAnnotation(V.class);
            if (v != null)
                name = v.value();

            arguments[i] = request.get(name);
        }

        return arguments;
    }
}
