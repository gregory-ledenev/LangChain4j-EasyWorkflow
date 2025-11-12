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
import com.gl.langchain4j.easyworkflow.Playground;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.condition;

/**
 * This class provides a sample GUI Playground for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#conditional-workflow">Conditional Workflow</a>
 * using EasyWorkflow DSL-style workflow initialization.
 */
@SuppressWarnings("unused")
public class SampleConditionalAgentsPlayground {
    static final String GROQ_API_KEY = "groqApiKey";
    public static void main(String[] args) {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        WorkflowDebugger workflowDebugger = new WorkflowDebugger();

        EasyWorkflow.AgentWorkflowBuilder<SampleConditionalAgents.ExpertRouterAgent> builder = EasyWorkflow.builder(SampleConditionalAgents.ExpertRouterAgent.class);
        SampleConditionalAgents.ExpertRouterAgent expertRouterAgent = builder
                .chatModel(BASE_MODEL)
                .chatMemory(chatMemory)
                .workflowDebugger(workflowDebugger)
                .setState("response", "")
                .agent(SampleConditionalAgents.CategoryRouter.class)
                .ifThen(condition(agenticScope -> agenticScope.readState("category", SampleConditionalAgents.RequestCategory.UNKNOWN) == SampleConditionalAgents.RequestCategory.MEDICAL, "category == RequestCategory.MEDICAL"))
                    .agent(SampleConditionalAgents.MedicalExpert.class)
                .end().elseIf()
                    .breakpoint("ELSEIF")
                .end()
                .ifThen(condition(agenticScope -> agenticScope.readState("category", SampleConditionalAgents.RequestCategory.UNKNOWN) == SampleConditionalAgents.RequestCategory.LEGAL, "category == RequestCategory.LEGAL"))
                    .agent(SampleConditionalAgents.LegalExpert.class)
                .end()
                .ifThen(condition(agenticScope -> agenticScope.readState("category", SampleConditionalAgents.RequestCategory.UNKNOWN) == SampleConditionalAgents.RequestCategory.TECHNICAL, "category == RequestCategory.TECHNICAL"))
                    .agent(SampleConditionalAgents.TechnicalExpert.class)
                .end()
                .agent(SampleConditionalAgents.SummaryAgent.class)
                .output(OutputComposers.asMap("response", "summary"))
                .build();

        Playground playground = Playground.createPlayground(SampleConditionalAgents.ExpertRouterAgent.class,
                Playground.Type.GUI,
                workflowDebugger);
        playground.play(expertRouterAgent, Map.of("request", "I broke my leg, what should I do?"));
    }
}
