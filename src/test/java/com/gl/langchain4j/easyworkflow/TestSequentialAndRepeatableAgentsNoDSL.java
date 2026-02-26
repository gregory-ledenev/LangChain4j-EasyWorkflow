package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

public class TestSequentialAndRepeatableAgentsNoDSL {
    public interface CreativeWriter {

        @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than
            3 sentences long around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
        @Agent("Generates a story based on the given topic")
        String generateStory(@V("topic") String topic);
    }

    public interface AudienceEditor {

        @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better align
        with the target audience of {{audience}}.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
        @Agent("Edits a story to better fit a given audience")
        String editStory(@V("story") String story, @V("audience") String audience);
    }

    public interface StyleEditor {

        @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
        @Agent("Edits a story to better fit a given style")
        String editStory(@V("story") String story, @V("style") String style);
    }

    public interface StyleScorer {

        @UserMessage("""
            You are a critical reviewer.
            Give a review score between 0.0 and 1.0 for the following
            story based on how well it aligns with the style '{{style}}'.
            Return only the score and nothing else.
            
            The story is: "{{story}}"
            """)
        @Agent("Scores a story based on how well it aligns with a given style")
        double scoreStyle(@V("story") String story, @V("style") String style);
    }

    public interface StyledWriter {

        @Agent
        String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
    }

    static final String GROQ_API_KEY = "groqApiKey";

    @Test
    void test() {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build();

        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(BASE_MODEL)
                .outputKey("score")
                .build();
        UntypedAgent styleReviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .testExitAtLoopEnd(true)
                .exitCondition( (agenticScope, loopCounter) -> {
                    double score = agenticScope.readState("score", 0.0);
                    return loopCounter <= 3 ? score >= 0.8 : score >= 0.6;
                })
                .build();

        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build();

        StyledWriter styledWriter = AgenticServices
                .sequenceBuilder(StyledWriter.class)
                .listener(new AgentListener() {
                    @Override
                    public void beforeAgentInvocation(AgentRequest agentRequest) {
                        System.out.println("STARTED: " + agentRequest.agentId());
                    }

                    @Override
                    public void afterAgentInvocation(AgentResponse agentResponse) {
                        System.out.println("FINISHED: " + agentResponse.agentId());
                    }

                    @Override
                    public void onAgentInvocationError(AgentInvocationError agentInvocationError) {
                        System.out.println(agentInvocationError);
                    }

                    @Override
                    public boolean inheritedBySubagents() {
                        return true;
                    }
                })
                .subAgents(creativeWriter, styleReviewLoop)
                .outputKey("story")
                .build();

        String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
        System.out.println(story);
    }
}
