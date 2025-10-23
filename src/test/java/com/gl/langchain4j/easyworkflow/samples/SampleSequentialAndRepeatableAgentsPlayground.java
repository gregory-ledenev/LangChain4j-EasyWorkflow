package com.gl.langchain4j.easyworkflow.samples;

import com.gl.langchain4j.easyworkflow.EasyWorkflow;
import com.gl.langchain4j.easyworkflow.OutputComposers;
import com.gl.langchain4j.easyworkflow.Playground;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.condition;
import static com.gl.langchain4j.easyworkflow.Playground.ARG_SHOW_DIALOG;

import com.gl.langchain4j.easyworkflow.gui.inspector.*;

public class SampleSequentialAndRepeatableAgentsPlayground {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        WorkflowDebugger workflowDebugger = new WorkflowDebugger();
        EasyWorkflow.AgentWorkflowBuilder<SampleSequentialAndRepeatableAgents.NovelCreator> builder = EasyWorkflow.builder(SampleSequentialAndRepeatableAgents.NovelCreator.class)
                .chatModel(BASE_MODEL)
                .workflowDebugger(workflowDebugger)
                .agent(SampleSequentialAndRepeatableAgents.CreativeWriter.class)
                .agent(SampleSequentialAndRepeatableAgents.AudienceEditor.class)
                .repeat( condition(agenticScope -> agenticScope.readState("score", 0.0) < 0.8, "score < 0.8"))
                .agent(SampleSequentialAndRepeatableAgents.StyleScorer.class)
                .agent(SampleSequentialAndRepeatableAgents.StyleEditor.class)
                .end()
                .output(OutputComposers.asBean(SampleSequentialAndRepeatableAgents.Novel.class))
                .agent(new SampleSequentialAndRepeatableAgents.QualityScorer());
        SampleSequentialAndRepeatableAgents.NovelCreator novelCreator = builder
                .build();

//        System.out.println(builder.toJson());
//        WorkflowInspectorListPane.show(builder);
        Playground playground = Playground.createPlayground(SampleSequentialAndRepeatableAgents.NovelCreator.class, Playground.Type.GUI);
        playground.setup(Map.of(Playground.ARG_WORKFLOW_DEBUGGER, workflowDebugger));
        playground.play(novelCreator, Map.of(
                "topic", "dragons and wizards",
                "audience", "infants",
                "style", "fantasy"));
    }
}
