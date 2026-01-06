package com.gl.langchain4j.easyworkflow.playground;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.gui.platform.FormEditorType;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.service.V;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Stream;

public interface PlaygroundMetadata {
    String PROPERTY_SYSTEM_MESSAGE = "systemMessage";
    String PROPERTY_USER_MESSAGE = "userMessage";

    record Type(String name) {
    }

    enum Category {
        Agent, NonAiAgent, HumanInTheLoop
    }

    record Model(String id, String name) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Model model = (Model) o;
            return Objects.equals(id, model.id);
        }

        @Override
        public @NonNull String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }

    class Agent {
        private final Type type;
        private final String name;
        private final String agentId;
        private final String description;
        private final Type outputType;
        private final String outputKey;
        private final List<Argument> arguments;
        @JsonBackReference
        private final Agent parent;
        @JsonManagedReference
        private final List<Agent> subagents;
        private final AgenticSystemTopology topology;
        private final Category category;
        private final Map<String, Object> customProperties;

        public Category getCategory() {
            return category;
        }

        public Agent(AgentInstance agentInstance, Agent parent) {

            this.type = new Type(agentInstance.type().getName());
            this.name = agentInstance.name();
            this.agentId = agentInstance.agentId();
            this.description = agentInstance.description();
            this.outputType = new Type(agentInstance.outputType().getTypeName());
            this.outputKey = agentInstance.outputKey();
            this.arguments = computeArguments(agentInstance);
            this.parent = parent;
            this.topology = agentInstance.topology();
            this.customProperties = computeCustomProperties(agentInstance);
            this.subagents = agentInstance.subagents().stream()
                    .map(subAgentInstance -> new Agent(subAgentInstance, this))
                    .toList();
            this.category = computeCategory(agentInstance);
        }

        private Category computeCategory(AgentInstance agentInstance) {
            Category result = Category.Agent;

            if (HumanInTheLoop.class.isAssignableFrom(agentInstance.type()))
                result = Category.HumanInTheLoop;
            else if (! (agentInstance instanceof AgentExecutor && ((AgentExecutor) agentInstance).agent() instanceof Proxy))
                result = Category.NonAiAgent;

            return result;
        }

        private static List<Argument> computeArguments(AgentInstance agentInstance) {
            Method agentMethod = EasyWorkflow.getAgentMethod(agentInstance.type());
            return agentMethod == null ?
                    List.of() :
                    Stream.of(agentMethod.getParameters())
                            .map(parameter -> {
                                String name = parameter.isAnnotationPresent(V.class) ?
                                        parameter.getAnnotation(V.class).value() :
                                        parameter.getName();
                                return new Argument(new Type(parameter.getType().getName()), name, null, parameter.getAnnotation(com.gl.langchain4j.easyworkflow.playground.PlaygroundParam.class));
                            })
                            .toList();
        }

        public Map<String, Object> getCustomProperties() {
            return customProperties;
        }

        private Map<String, Object> computeCustomProperties(AgentInstance agentInstance) {
            Map<String, Object> customProperties = new HashMap<>();

            String systemMessage = EasyWorkflow.getSystemMessageTemplate(agentInstance.type());
            if (systemMessage != null)
                customProperties.put(PROPERTY_SYSTEM_MESSAGE, systemMessage);


            String userMessage = EasyWorkflow.getUserMessageTemplate(agentInstance.type());
            if (userMessage != null)
                customProperties.put(PROPERTY_USER_MESSAGE, userMessage);

            return Collections.unmodifiableMap(customProperties);
        }

        public AgenticSystemTopology getTopology() {
            return topology;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getAgentId() {
            return agentId;
        }

        public String getDescription() {
            return description;
        }

        public Type getOutputType() {
            return outputType;
        }

        public String getOutputKey() {
            return outputKey;
        }

        public List<Argument> getArguments() {
            return arguments;
        }

        public Agent getParent() {
            return parent;
        }

        public List<Agent> getSubagents() {
            return subagents;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Agent that = (Agent) o;
            return Objects.equals(agentId, that.agentId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(agentId);
        }

        @Override
        public String toString() {
            return "AgentMetadata[" +
                    "type=" + getType() + ", " +
                    "name=" + getName() + ", " +
                    "agentId=" + getAgentId() + ", " +
                    "description=" + getDescription() + ", " +
                    "outputType=" + getOutputType() + ", " +
                    "outputKey=" + getOutputKey() + ", " +
                    "arguments=" + getArguments() + ", " +
                    "subagents=" + getSubagents() + ']';
        }
    }

    public record Argument(Type type,
                           String name,
                           Object defaultValue,
                           String label,
                           String description,
                           FormEditorType editorType,
                           String[] editorChoices) {

        public Argument(Type type, String name, Object defaultValue, PlaygroundParam playgroundParam) {
            this(type, name, defaultValue,
                    playgroundParam != null ? playgroundParam.label() : "",
                    playgroundParam != null ? playgroundParam.description() : "",
                    playgroundParam == null ? FormEditorType.Default : playgroundParam.editorType(),
                    playgroundParam == null ? new String[0] : playgroundParam.editorChoices());
        }
    }
}
