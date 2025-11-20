/*
 *
 * Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * /
 */

package com.gl.langchain4j.easyworkflow.samples;

import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.OutputComposers;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.io.IOException;
import java.util.Map;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.condition;
import static java.lang.System.out;

/**
 * This class provides a sample for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#conditional-workflow">Conditional Workflow</a>
 * using EasyWorkflow DSL-style workflow initialization.
 */
@SuppressWarnings("unused")
public class SampleConditionalAgents {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        WorkflowDebugger workflowDebugger = new WorkflowDebugger();

        EasyWorkflow.AgentWorkflowBuilder<ExpertRouterAgent> builder = EasyWorkflow.builder(ExpertRouterAgent.class);
        ExpertRouterAgent expertRouterAgent = builder
                .chatModel(BASE_MODEL)
                .chatMemory(chatMemory)
                .workflowDebugger(workflowDebugger)
                .setState("response", "")
                .agent(CategoryRouter.class)
                .ifThen(condition(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, "category == RequestCategory.MEDICAL"))
                .agent(MedicalExpert.class)
                .end().elseIf()
                .breakpoint("ELSEIF")
                .end()
                .ifThen(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL)
                .agent(LegalExpert.class)
                .end()
                .ifThen(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL)
                .agent(TechnicalExpert.class)
                .end()
                .agent(SummaryAgent.class)
                .output(OutputComposers.asMap("response", "summary"))
                .build();

        System.out.println(builder.generateAISummary());

        try {
            builder.toHtmlFile("workflow.html");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        out.println(expertRouterAgent.ask("I broke my leg, what should I do?"));
        out.println(workflowDebugger.toString(true));
        out.println(expertRouterAgent.ask("Should I sue my neighbor who caused this damage?"));
        out.println(expertRouterAgent.ask("How to configure a VPN on Windows 10?"));
        out.println(expertRouterAgent.ask("What is the meaning of life?"));
    }

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public interface SummaryAgent {

        @UserMessage("""
                     Prepare a short 1-2 sentences summary for provided information.
                     The information is: '{{response}}'.
                     """)
        @Agent(value = "Categorizes a user request", outputKey = "summary")
        String summary(@V("response") String response);
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
                @MemoryId String memoryId,
                @V("request") String request);
    }

    public interface LegalExpert {

        @UserMessage("""
                     You are a legal expert.
                     Analyze the following user request under a legal point of view and provide the best possible answer.
                     The user request is {{request}}.
                     """)
        @Agent(value = "A legal expert", outputKey = "response")
        String legal(
                @MemoryId String memoryId,
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
                @MemoryId String memoryId,
                @V("request") String request);
    }

    public interface ExpertRouterAgent {

        @Agent(outputKey = "response")
        Map<String, String> ask(@V("request") String request);
    }

    public interface ContextSummarizer {

        @UserMessage("""
                     Create a very short summary, 2 sentences at most, of the
                     following conversation between an AI agent and a user.
                     
                     The user conversation is: '{{it}}'.
                     """)
        @Agent
        String summarize(String conversation);
    }
}
