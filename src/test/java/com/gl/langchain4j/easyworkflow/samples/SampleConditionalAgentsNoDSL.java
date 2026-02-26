package com.gl.langchain4j.easyworkflow.samples;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.prefs.Preferences;

public class SampleConditionalAgentsNoDSL {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(BASE_MODEL)
                .outputKey("category")
                .build();

        MedicalExpert medicalExpert = AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(BASE_MODEL)
                .outputKey("response")
                .build();
        LegalExpert legalExpert = AgenticServices
                .agentBuilder(LegalExpert.class)
                .chatModel(BASE_MODEL)
                .outputKey("response")
                .build();
        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(BASE_MODEL)
                .outputKey("response")
                .build();

        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL,
                                        technicalExpert,
                                        new PrintCategoryAgent(),
                                        new PrintCategoryAgent(),
                                        new PrintCategoryAgent(),
                                        new PrintCategoryAgent())
                .build();

        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .listener(new AgentListener() {
                    @Override
                    public void beforeAgentInvocation(AgentRequest agentRequest) {
                        System.out.println("beforeAgentInvocation: " + agentRequest.agentId());
                    }

//                    @Override
//                    public void afterAgentInvocation(AgentResponse agentResponse) {
//                        System.out.println("afterAgentInvocation: " + agentResponse.agentId() + " RESULT: " + agentResponse.output());
//                    }
//
                    @Override
                    public boolean inheritedBySubagents() {
                        return true;
                    }
                })
                .build();

        String response = expertRouterAgent.ask("How to setup a VPN?");
    }

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public interface CategoryRouter {

        @UserMessage("""
                Analyze the following user request and categorize it as 'legal', 'medical' or 'technical'.
                In case the request doesn't belong to any of those categories categorize it as 'unknown'.
                Reply with only one of those words and nothing else.
                The user request is: '{{request}}'.
                """)
        @Agent(value = "Categorizes a user request", outputKey = "category")
        RequestCategory classify(@V("request") String request);
    }

    public interface MedicalExpert {

        @UserMessage("""
                You are a medical expert.
                Analyze the following user request under a medical point of view and provide the best possible answer.
                The user request is {{request}}.
                """)
        @Agent(value = "A medical expert", outputKey = "response")
        String medical(
                @V("request") String request);
    }

    public interface ExpertRouterAgent {

        @Agent
        String ask(@V("request") String request);
    }

    public interface LegalExpert {

        @UserMessage("""
                You are a legal expert.
                Analyze the following user request under a legal point of view and provide the best possible answer.
                The user request is {{request}}.
                """)
        @Agent(value = "A legal expert", outputKey = "response")
        String legal(
                @V("request") String request);
    }

    public interface TechnicalExpert {
        @UserMessage("""
                You are a technical expert.
                Analyze the following user request under a technical point of view and provide the best possible answer.
                The user request is {{request}}.
                """)
        @Agent(value = "A technical expert", outputKey = "response")
        String technical(
                @V("request") String request);
    }

    public static class PrintCategoryAgent {
        @Agent(value = "Print Category Agent", outputKey = "testOutput")
        public RequestCategory printCategory(@V("category") RequestCategory category) {
            System.out.println("Category: " + category);
            return category;
        }
    }
}
