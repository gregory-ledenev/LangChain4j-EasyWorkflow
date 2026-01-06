package com.gl.langchain4j.easyworkflow.samples;

import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.HumanInTheLoopAgents;
import com.gl.langchain4j.easyworkflow.playground.Playground;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.agentic.workflow.impl.SequentialPlanner;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.*;

public class SampleSupervisedAgentsPlayground {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {
        OpenAiChatModel metaLlamaModel = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();
        OpenAiChatModel openAIModel = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("openai/gpt-oss-120b") // or another model
                .build();
        OpenAiChatModel metaLlamaModel1 = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("llama-3.3-70b-versatile") // or another model
                .build();
        OpenAiChatModel qwenModel = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("qwen/qwen3-32b") // or another model
                .build();

        SampleSupervisedAgents.BankTool bankTool = new SampleSupervisedAgents.BankTool();
        bankTool.createAccount("Mario", 1000.0);
        bankTool.createAccount("Georgios", 1000.0);

        Playground playground = Playground.createPlayground(SampleSupervisedAgents.SupervisorAgent.class, Playground.Type.GUI);

        HumanInTheLoop humanInTheLoop = HumanInTheLoopAgents.playgroundAgent(playground,
                "confirmation",
                """
                An agent that asks the user to confirm transactions.
                YES - to confirm; any other value - to decline""");

        WorkflowDebugger workflowDebugger = new WorkflowDebugger();
        workflowDebugger.addBreakpoint(Breakpoint.builder(Breakpoint.Type.TOOL_INPUT,
                (b, states) -> {
                    String result = "TOOL_INPUT: %s, %s".formatted(states.get(KEY_TOOL), states.get(KEY_TOOL_REQUEST));
                    System.out.println(result);
                    return result;
                }).build());
        workflowDebugger.addBreakpoint(Breakpoint.builder(Breakpoint.Type.TOOL_OUTPUT,
                (b, states) -> {
                    String result = "TOOL_OUTPUT: %s, %s -> %s".formatted(states.get(KEY_TOOL),
                            states.get(KEY_TOOL_REQUEST),
                            states.get(KEY_TOOL_RESPONSE));
                    System.out.println(result);
                    return result;
                }).build());

        EasyWorkflow.AgentWorkflowBuilder<SampleSupervisedAgents.SupervisorAgent> workflowBuilder = EasyWorkflow.builder(SampleSupervisedAgents.SupervisorAgent.class);
        SampleSupervisedAgents.SupervisorAgent supervisorAgent = workflowBuilder
                .chatModel(openAIModel)
                .workflowDebugger(workflowDebugger)
                .doAsPlannerGroup(EasyWorkflow.lambdaWithDescription(SequentialPlanner::new, "Sequential"))
                    .setState("a", "1")
                    .setState("b", "2")
                .end()
                .doAsGroup()
                    .agent(SampleSupervisedAgents.WithdrawAgent.class, builder -> builder.tools(bankTool))
                    .agent(SampleSupervisedAgents.CreditAgent.class, builder -> builder.tools(bankTool))
                    .agent(SampleSupervisedAgents.ExchangeAgent.class)
                    // ExchangeTool provided via @AgentBuilderConfigurator annotation
                    .agent(humanInTheLoop)
                .end()
                .build();

        try {
            workflowBuilder.toHtmlFile("workflow.html");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        playground.setup(Map.of(
                Playground.ARG_WORKFLOW_DEBUGGER, workflowDebugger,
                Playground.ARG_CHAT_MODELS, List.of(
                        new Playground.PlaygroundChatModel("meta-llama/llama-4-scout-17b-16e-instruct", metaLlamaModel),
                        new Playground.PlaygroundChatModel("openai/gpt-oss-120b", openAIModel),
                        new Playground.PlaygroundChatModel("llama-3.3-70b-versatile", metaLlamaModel1),
                        new Playground.PlaygroundChatModel("qwen/qwen3-32b", qwenModel)
                )));
        playground.play(supervisorAgent, Map.of("request", "Transfer 100 EUR from Mario's account to Georgios' one"));
    }
}
