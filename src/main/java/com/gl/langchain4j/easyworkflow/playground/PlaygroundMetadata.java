package com.gl.langchain4j.easyworkflow.playground;

import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.gui.platform.FormEditorType;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.MethodAgentInvoker;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.service.V;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Stream;

public interface PlaygroundMetadata {
    public static final String PROPERTY_IS_AI_AGENT = "isAiAgent";
    public static final String PROPERTY_IS_HUMAN_IN_THE_LOOP_AGENT = "isHumanInTheLoopAgent";
    public static final String PROPERTY_SYSTEM_MESSAGE = "systemMessage";
    public static final String PROPERTY_USER_MESSAGE = "userMessage";

    record Type(String name) {
    }

    class Agent {
        private final Type type;
        private final String name;
        private final String agentId;
        private final String description;
        private final Type outputType;
        private final String outputKey;
        private final List<Argument> arguments;
        private final Agent parent;
        private final List<Agent> subagents;
        private final AgenticSystemTopology topology;
        private final Map<String, Object> customProperties;

        public Agent(AgentInstance agentInstance, Agent parent) {

            this.type = new Type(agentInstance.type().getName());
            this.name = agentInstance.name();
            this.agentId = agentInstance.agentId();
            this.description = agentInstance.description();
            this.outputType = new Type(agentInstance.outputType().getTypeName());
            this.outputKey = agentInstance.outputKey();
            this.arguments = getArguments(agentInstance);
            this.parent = parent;
            this.topology = agentInstance.topology();
            this.customProperties = getCustomProperties(agentInstance);
            this.subagents = agentInstance.subagents().stream()
                    .map(subAgentInstance -> new Agent(subAgentInstance, this))
                    .toList();
        }

        private static List<Argument> getArguments(AgentInstance agentInstance) {
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

        private Map<String, Object> getCustomProperties(AgentInstance agentInstance) {
            Map<String, Object> customProperties = new HashMap<>();
            boolean isAIAgent = false;
            customProperties.put(PROPERTY_IS_AI_AGENT, agentInstance instanceof AgentExecutor && ((AgentExecutor) agentInstance).agent() instanceof Proxy);
            customProperties.put(PROPERTY_IS_HUMAN_IN_THE_LOOP_AGENT, HumanInTheLoop.class.isAssignableFrom(agentInstance.type()));

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
