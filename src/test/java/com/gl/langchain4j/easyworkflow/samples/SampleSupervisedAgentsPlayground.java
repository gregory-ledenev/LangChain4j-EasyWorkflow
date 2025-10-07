package com.gl.langchain4j.easyworkflow.samples;

import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.HumanInTheLoopAgents;
import com.gl.langchain4j.easyworkflow.Playground;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.util.Map;
import java.util.prefs.Preferences;

public class SampleSupervisedAgentsPlayground {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
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

        EasyWorkflow.AgentWorkflowBuilder<SampleSupervisedAgents.SupervisorAgent> workflowBuilder = EasyWorkflow.builder(SampleSupervisedAgents.SupervisorAgent.class);
        SampleSupervisedAgents.SupervisorAgent supervisorAgent = workflowBuilder
                .chatModel(BASE_MODEL)
                .workflowDebugger(workflowDebugger)
                .doAsGroup()
                .agent(SampleSupervisedAgents.WithdrawAgent.class, builder -> builder.tools(bankTool))
                .agent(SampleSupervisedAgents.CreditAgent.class, builder -> builder.tools(bankTool))
                .agent(SampleSupervisedAgents.ExchangeAgent.class) // ExchangeTool provided via @AgentBuilderConfigurator annotation
                .agent(humanInTheLoop)
                .end()
                .build();

        try {
            workflowBuilder.toHtmlFile("workflow.html");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        playground.play(supervisorAgent, Map.of("request", "Transfer 100 EUR from Mario's account to Georgios' one"));
    }
}
