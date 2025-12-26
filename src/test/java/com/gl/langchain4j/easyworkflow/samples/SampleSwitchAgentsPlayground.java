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
import com.gl.langchain4j.easyworkflow.playground.Playground;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;
import java.util.prefs.Preferences;

import static java.lang.System.out;

/**
 * This class provides a sample playground for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#conditional-workflow">Conditional Workflow</a>
 * using EasyWorkflow DSL-style workflow initialization.
 */
public class SampleSwitchAgentsPlayground {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        WorkflowDebugger workflowDebugger = new WorkflowDebugger();
        workflowDebugger.addBreakpoint(new WorkflowDebugger.AgentBreakpoint((aBreakpoint, aAgenticScope) -> {
            out.println();
            return null;
        }, WorkflowDebugger.Breakpoint.Type.AGENT_OUTPUT, null, null, null, true));

        EasyWorkflow.AgentWorkflowBuilder<SampleSwitchAgents.ExpertRouterAgent> builder = EasyWorkflow.builder(SampleSwitchAgents.ExpertRouterAgent.class);
        SampleSwitchAgents.ExpertRouterAgent expertRouterAgent = builder
                .chatModel(BASE_MODEL)
                .chatMemory(chatMemory)
                .outputName("responseFinal")
                .workflowDebugger(workflowDebugger)
                .setState("response", "")
                .agent(SampleSwitchAgents.CategoryRouter.class)
                .doWhen("category", SampleSwitchAgents.RequestCategory.UNKNOWN)
                    .match(SampleSwitchAgents.RequestCategory.MEDICAL)
                        .agent(SampleSwitchAgents.MedicalExpert.class)
                    .end()
                    .match(SampleSwitchAgents.RequestCategory.LEGAL)
                        .agent(SampleSwitchAgents.LegalExpert.class)
                    .end()
                    .match(SampleSwitchAgents.RequestCategory.TECHNICAL)
                        .agent(SampleSwitchAgents.TechnicalExpert.class)
                        .setState("response", "")
                    .end()
                .end()
                .agent(SampleSwitchAgents.SummaryAgent.class)
                .output(OutputComposers.asMap("response", "summary"))
                .build();

        Playground playground = Playground.createPlayground(SampleSwitchAgents.ExpertRouterAgent.class,
                Playground.Type.GUI,
                workflowDebugger);
        playground.play(expertRouterAgent, Map.of("request", "I broke my leg, what should I do?"));
    }
}
