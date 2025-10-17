package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.condition;
import static org.junit.jupiter.api.Assertions.*;

public class TestConditionalAgents {
    static final String GROQ_API_KEY = "groqApiKey";
    static final String RESPONSE_MEDICAL = "Some medical response";
    static final String RESPONSE_LEGAL = "Some legal response";
    static final String RESPONSE_TECHNICAL = "Some technical response";

    @Test
    public void testSwitch() {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        WorkflowDebugger debugger = new WorkflowDebugger();
        EasyWorkflow.AgentWorkflowBuilder<ExpertRouterAgent> builder = EasyWorkflow.builder(ExpertRouterAgent.class);
        ExpertRouterAgent expertRouterAgent = builder
                .chatModel(BASE_MODEL)
                .workflowDebugger(debugger)
                .setState("response", "")
                .agent(new CategoryRouter())
                .doWhen("category", RequestCategory.UNKNOWN)
                .match(RequestCategory.MEDICAL)
                .agent(new MedicalExpert())
                .end()
                .match(RequestCategory.LEGAL)
                .agent(new LegalExpert())
                .end()
                .match(RequestCategory.TECHNICAL)
                .agent(new TechnicalExpert())
                .end()
                .end()
                .agent(new SummaryAgent())
                .output(OutputComposers.asMap("response", "summary"))
                .build();

        System.out.println(builder.toJson());

        try {
            debugger.toHtmlFile("workflow.html", false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        assertEquals("{summary=Summary: Some medical response, response=Some medical response}", expertRouterAgent.ask("I broke my leg, what should I do?").toString());
        assertEquals("{summary=Summary: Some legal response, response=Some legal response}", expertRouterAgent.ask("Should I sue my neighbor who caused this damage?").toString());
        assertEquals("{summary=Summary: Some technical response, response=Some technical response}", expertRouterAgent.ask("How to configure a VPN on Windows 10?").toString());
        assertEquals("{summary=Summary: , response=}", expertRouterAgent.ask("What is the meaning of life?").toString());
    }

    @Test
    public void testIf() {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        final AtomicBoolean breakpointHit = new AtomicBoolean(false);

        WorkflowDebugger debugger = new WorkflowDebugger();

        EasyWorkflow.AgentWorkflowBuilder<ExpertRouterAgent> builder = EasyWorkflow.builder(ExpertRouterAgent.class);
        ExpertRouterAgent expertRouterAgent = builder
                .chatModel(BASE_MODEL)
                .workflowDebugger(debugger)
                .setState("response", "")
                .agent(new CategoryRouter())
                    .ifThen(condition(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, "category == MEDICAL"))
                        .agent(new MedicalExpert())
                    .end().elseIf()
                        .breakpoint((aBreakpoint, aAgenticScope) -> breakpointHit.set(true))
                    .end()
                    .ifThen(condition(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, "category == LEGAL"))
                        .agent(new LegalExpert())
                    .end()
                    .ifThen(condition(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, "category == TECHNICAL"))
                .agent(new TechnicalExpert())
                .end()
                .agent(new SummaryAgent())
                .output(OutputComposers.asMap("response", "summary"))
                .build();

        assertEquals("{summary=Summary: Some medical response, response=Some medical response}", expertRouterAgent.ask("I broke my leg, what should I do?").toString());
        assertFalse(breakpointHit.get());

        System.out.println(builder.toJson());

        try {
            debugger.toHtmlFile("workflow.html", false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        assertEquals("{summary=Summary: Some legal response, response=Some legal response}", expertRouterAgent.ask("Should I sue my neighbor who caused this damage?").toString());
        assertTrue(breakpointHit.get());
        breakpointHit.set(false);

        assertEquals("{summary=Summary: Some technical response, response=Some technical response}", expertRouterAgent.ask("How to configure a VPN on Windows 10?").toString());
        assertTrue(breakpointHit.get());
        breakpointHit.set(false);
        try {
            debugger.toHtmlFile("workflow.html", false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        assertEquals("{summary=Summary: , response=}", expertRouterAgent.ask("What is the meaning of life?").toString());
        assertTrue(breakpointHit.get());
        breakpointHit.set(false);
    }

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public interface ExpertRouterAgent {

        @Agent(outputName = "response")
        Map<String, String> ask(@V("request") String request);
    }

    @SuppressWarnings("unused")
    public static class SummaryAgent extends WorkflowDebuggerSupport.Impl {
        @Agent(value = "Summarises a user request", outputName = "summary")
        public String summary(@V("response") String response) {
            inputReceived(response != null && !response.isEmpty() ? response : "None");
            String result = "Summary: " + response;
            outputProduced(result);
            return result;
        }
    }

    @SuppressWarnings("unused")
    public static class CategoryRouter extends WorkflowDebuggerSupport.Impl {
        @Agent(value = "Categorizes a user request", outputName = "category")
        public RequestCategory classify(@V("request") String request) {
            inputReceived(request);

            RequestCategory result = RequestCategory.UNKNOWN;

            if (request.contains("broke my leg"))
                result = RequestCategory.MEDICAL;
            else if (request.contains("sue my neighbor"))
                result = RequestCategory.LEGAL;
            else if (request.contains("configure a VPN"))
                result = RequestCategory.TECHNICAL;

            outputProduced(result);

            return result;
        }
    }

    @SuppressWarnings("unused")
    public static class MedicalExpert extends WorkflowDebuggerSupport.Impl {
        @Agent(value = "A medical expert", outputName = "response")
        public String medical(@MemoryId String memoryId, @V("request") String request) {
            inputReceived(request);
            outputProduced(RESPONSE_MEDICAL);
            return RESPONSE_MEDICAL;
        }
    }

    @SuppressWarnings("unused")
    public static class LegalExpert extends WorkflowDebuggerSupport.Impl {
        @Agent(value = "A legal expert", outputName = "response")
        public String legal(@MemoryId String memoryId, @V("request") String request) {
            inputReceived(request);
            outputProduced(RESPONSE_LEGAL);
            return RESPONSE_LEGAL;
        }
    }

    @SuppressWarnings("unused")
    public static class TechnicalExpert extends WorkflowDebuggerSupport.Impl {
        @Agent(value = "A technical expert", outputName = "response")
        public String technical(@MemoryId String memoryId, @V("request") String request) {
            inputReceived(request);
            outputProduced(RESPONSE_TECHNICAL);
            return RESPONSE_TECHNICAL;
        }
    }
}