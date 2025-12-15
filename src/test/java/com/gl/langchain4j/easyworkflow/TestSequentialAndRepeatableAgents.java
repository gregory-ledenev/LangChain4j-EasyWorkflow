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

package com.gl.langchain4j.easyworkflow;

import com.gl.langchain4j.easyworkflow.WorkflowDebugger.Breakpoint;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.BreakpointActions.toHtmlFile;
import static com.gl.langchain4j.easyworkflow.BreakpointActions.toggleBreakpoints;
import static com.gl.langchain4j.easyworkflow.EasyWorkflow.condition;
import static com.gl.langchain4j.easyworkflow.EasyWorkflow.expandTemplate;
import static com.gl.langchain4j.easyworkflow.WorkflowDebugger.AgentBreakpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
public class TestSequentialAndRepeatableAgents {
    static final String GROQ_API_KEY = "groqApiKey";
    static double score = 0.4;
    static int passCounter = 0;

    // use that to test chat
    public static void main(String[] args) {
        TestSequentialAndRepeatableAgents test = new TestSequentialAndRepeatableAgents();
        test.chat();
    }

    public void chat() {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        WorkflowDebugger workflowDebugger = new WorkflowDebugger();

        EasyWorkflow.AgentWorkflowBuilder<NovelCreator> builder = EasyWorkflow.builder(NovelCreator.class)
                .chatModel(BASE_MODEL)
                .workflowDebugger(workflowDebugger)
                .outputName("finalStory")
                .agent(new CreativeWriter())
                .agent(new AudienceEditor())
                .repeat(condition(agenticScope -> agenticScope.readState("score", 0.0) < 0.8, "score < 0.8"))
                    .agent(new StyleScorer())
                    .agent(new StyleEditor())
                .end()
                .output(OutputComposers.asBean(Novel.class))
                .agent(new QualityScorer());

        NovelCreator novelCreator = builder.build();

        Novel novel = novelCreator.createNovel("dragons and wizards", "infants", "fantasy");
        WorkflowExpertSupport.play(workflowDebugger, Novel.class.getSimpleName(), "describe results", Playground.Type.GUI);
    }

    @Test
    public void test() {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        List<String> breakpointOutput = new ArrayList<>();

        WorkflowDebugger workflowDebugger = new WorkflowDebugger();
        workflowDebugger.addBreakpoint(new Breakpoint(Breakpoint.Type.SESSION_STARTED, (aBreakpoint, ctx) ->
                breakpointOutput.add(expandTemplate("Args: {{topic}}, {{audience}}, {{style}}", workflowDebugger.getWorkflowInput())),
                null, true));
        workflowDebugger.addBreakpoint(new Breakpoint(Breakpoint.Type.SESSION_STOPPED, (aBreakpoint, ctx) ->
                breakpointOutput.add(expandTemplate("Result: {{story}}", ctx)),
                null, true));
        workflowDebugger.addBreakpoint(new AgentBreakpoint((aBreakpoint, ctx) ->
                breakpointOutput.add("Score: " + ctx.getOrDefault("score", 0.0)),
                Breakpoint.Type.AGENT_OUTPUT, null, new String[]{"score"}, null, true));

        workflowDebugger.addBreakpoint(Breakpoint.builder(Breakpoint.Type.AGENT_INPUT, toHtmlFile("workflow.html")).build());

        Breakpoint scoreBreakpoint = Breakpoint.
                builder(Breakpoint.Type.AGENT_OUTPUT, "SCORE for '{{$agentClass}}': {{score}}")
                .outputNames("score")
                .condition(ctx -> ((double) ctx.getOrDefault("score", 0.0)) >= 0.0)
                .agentClasses(StyleScorer.class)
                .enabled(false)
                .build();
        workflowDebugger.addBreakpoint(scoreBreakpoint);

        workflowDebugger.addBreakpoint(Breakpoint.
                builder(Breakpoint.Type.SESSION_STARTED, "SESSION STARTED")
                .build());

        workflowDebugger.addBreakpoint(Breakpoint.
                builder(Breakpoint.Type.SESSION_STARTED, toggleBreakpoints(true, scoreBreakpoint))
                .build());

        workflowDebugger.addBreakpoint(Breakpoint.
                builder(Breakpoint.Type.SESSION_STOPPED, "SESSION STOPPED")
                .build());

        workflowDebugger.addBreakpoint(Breakpoint.
                builder(Breakpoint.Type.AGENT_INPUT, "INPUT for '{{$agentClass}}': {{$input}}")
                .build());
        workflowDebugger.addBreakpoint(Breakpoint.
                builder(Breakpoint.Type.AGENT_OUTPUT, "OUTPUT for '{{$agentClass}}': {{$output}}")
                .build());

        EasyWorkflow.AgentWorkflowBuilder<NovelCreator> builder = EasyWorkflow.builder(NovelCreator.class)
                .chatModel(BASE_MODEL)
                .workflowDebugger(workflowDebugger)
                .outputName("finalStory")
                .agent(new CreativeWriter())
                .agent(new AudienceEditor())
                .repeat(condition(agenticScope -> agenticScope.readState("score", 0.0) < 0.8, "score < 0.8"))
                    .agent(new StyleScorer())
                    .breakpoint("SCORE (INLINE): {{score}}", ctx -> (double) ctx.getOrDefault("score", 0.0) >= 0.0)
                    .agent(new StyleEditor())
                .end()
                .output(OutputComposers.asBean(Novel.class))
                .agent(new QualityScorer());

        NovelCreator novelCreator = builder.build();

        Novel novel = novelCreator.createNovel("dragons and wizards", "infants", "fantasy");

        System.out.println(builder.toJson());

        try {
            workflowDebugger.toHtmlFile("workflow.html", false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println(novel);
        System.out.println(breakpointOutput);
        System.out.println(workflowDebugger.toString(true));
        assertEquals("[Args: dragons and wizards, infants, fantasy, Score: 0.6000000000000001, Score: 0.8, Result: 0In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!]", breakpointOutput.toString());
        assertEquals("""
                     ↓ IN > "audience": infants
                     ↓ IN > "topic": dragons and wizards
                     ↓ IN > "style": fantasy
                     -----------------------
                           ↓ IN: UserMessage { name = null, contents = [TextContent { text = "You are a creative writer. Generate a draft of a story no more than 3 sentences long around the\\ngiven topic.\\nReturn only the story and nothing else.\\nThe topic is dragons and wizards.\\n" }], attributes = {} }
                     1. ▷︎ CreativeWriter
                           ↓ OUT > "story": In the mystical realm of Aethoria, ancient dragons forged an unlikely alliance with powerful wizards, uniting against a dark sorcerer who threatened to destroy their world. The dragons, with their fiery breath and scales as black as coal, soared through the skies alongside the wizards, who wielded magical staffs that crackled with electric energy. Together, they clashed in a spectacular battle against the dark sorcerer's legion of shadow creatures, their combined might shaking the foundations of Aethoria.
                     -----------------------
                           ↓ IN: UserMessage { name = null, contents = [TextContent { text = "You are a professional editor. Analyze and rewrite the following story to better align with the\\ntarget audience of infants.\\nReturn only the story and nothing else.\\nThe story is "In the mystical realm of Aethoria, ancient dragons forged an unlikely alliance with powerful wizards, uniting against a dark sorcerer who threatened to destroy their world. The dragons, with their fiery breath and scales as black as coal, soared through the skies alongside the wizards, who wielded magical staffs that crackled with electric energy. Together, they clashed in a spectacular battle against the dark sorcerer's legion of shadow creatures, their combined might shaking the foundations of Aethoria.".\\n" }], attributes = {} }
                     2. ▷︎ AudienceEditor
                           ↓ OUT > "story": In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!
                     -----------------------
                           ↓ IN: UserMessage { name = null, contents = [TextContent { text = "You are a critical reviewer. Give a review score between 0.0 and 1.0 for the following story based\\non how well it aligns with the style 'fantasy'.\\nReturn only the score and nothing else.\\nThe story is: "In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!"\\n" }], attributes = {} }
                     3. ▷︎ StyleScorer
                           ↓ OUT > "score": 0.6000000000000001
                     -----------------------
                           ↓ IN: {score=0.6000000000000001, audience=infants, topic=dragons and wizards, style=fantasy, story=In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!}
                     4. ▷︎ LineBreakpointAgent
                           ↓ OUT > "": SCORE (INLINE): 0.6000000000000001
                     -----------------------
                           ↓ IN: UserMessage { name = null, contents = [TextContent { text = "You are a professional editor. Analyze and rewrite the following story to better fit and be more\\ncoherent with the fantasy style.\\nReturn only the story and nothing else.\\nThe story is "In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!".\\n" }], attributes = {} }
                     5. ▷︎ StyleEditor
                           ↓ OUT > "story": 0In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!
                     -----------------------
                           ↓ IN: UserMessage { name = null, contents = [TextContent { text = "You are a critical reviewer. Give a review score between 0.0 and 1.0 for the following story based\\non how well it aligns with the style 'fantasy'.\\nReturn only the score and nothing else.\\nThe story is: "0In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!"\\n" }], attributes = {} }
                     6. ▷︎ StyleScorer
                           ↓ OUT > "score": 0.8
                     -----------------------
                           ↓ IN: UserMessage { name = null, contents = [TextContent { text = "0In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!" }], attributes = {} }
                     7. ▷︎ QualityScorer
                           ↓ OUT > "quality": 0.74
                     -----------------------
                     ◼ RESULT: Novel[story=0In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!, score=0.8]
                     """, workflowDebugger.toString(true));
    }

    public interface NovelCreator {
        @Agent(outputKey = "story")
        Novel createNovel(@V("topic") String topic, @V("audience") String audience, @V("style") String style);
    }

    public static class CreativeWriter extends WorkflowDebuggerSupport.Impl {
        @UserMessage("""
                     You are a creative writer. Generate a draft of a story no more than 3 sentences long around the
                     given topic.
                     Return only the story and nothing else.
                     The topic is {{topic}}.
                     """)
        @Agent(value = "Generates a story based on the given topic", outputKey = "story")
        public String generateStory(@V("topic") String topic) {
            inputReceived(expandUserMessage(Map.of("topic", topic)));
            String result = """
                            In the mystical realm of Aethoria, ancient dragons forged an unlikely alliance with powerful wizards, uniting against a dark sorcerer who threatened to destroy their world. The dragons, with their fiery breath and scales as black as coal, soared through the skies alongside the wizards, who wielded magical staffs that crackled with electric energy. Together, they clashed in a spectacular battle against the dark sorcerer's legion of shadow creatures, their combined might shaking the foundations of Aethoria.""";
            outputProduced(result);
            return result;
        }
    }

    public static class AudienceEditor extends WorkflowDebuggerSupport.Impl {
        @UserMessage("""
                     You are a professional editor. Analyze and rewrite the following story to better align with the
                     target audience of {{audience}}.
                     Return only the story and nothing else.
                     The story is "{{story}}".
                     """)
        @Agent(value = "Edits a story to better fit a given audience", outputKey = "story")
        public String editStory(@V("story") String story, @V("audience") String audience) {
            inputReceived(expandUserMessage(Map.of(
                    "story", story,
                    "audience", audience)));
            String result = """
                            In a magical land, friendly dragons played with happy wizards. The dragons had shiny scales and could blow bubbles. The wizards had special sticks that made fun sparks. They all worked together to make the world a happy place. They chased the grumpy clouds away, and everyone was happy and played together!""";
            outputProduced(result);
            return result;
        }
    }

    public static class StyleEditor extends WorkflowDebuggerSupport.Impl {
        @UserMessage("""
                     You are a professional editor. Analyze and rewrite the following story to better fit and be more
                     coherent with the {{style}} style.
                     Return only the story and nothing else.
                     The story is "{{story}}".
                     """)
        @Agent(value = "Edits a story to better fit a given style", outputKey = "story")
        public String editStory(@V("story") String story, @V("style") String style) {
            inputReceived(expandUserMessage(Map.of(
                    "story", story,
                    "style", style)));
            String result = passCounter + story;
            outputProduced(result);
            return result;
        }
    }

    public static class StyleScorer extends WorkflowDebuggerSupport.Impl {
        @UserMessage("""
                     You are a critical reviewer. Give a review score between 0.0 and 1.0 for the following story based
                     on how well it aligns with the style '{{style}}'.
                     Return only the score and nothing else.
                     The story is: "{{story}}"
                     """)
        @Agent(value = "Scores a story based on how well it aligns with a given style", outputKey = "score")
        public double scoreStyle(@V("story") String story, @V("style") String style) {
            inputReceived(expandUserMessage(Map.of(
                    "story", story,
                    "style", style)));
            score += 0.2;
            double result = score;
            outputProduced(result);
            return result;
        }
    }

    public static class QualityScorer implements WorkflowDebuggerSupport {
        private WorkflowDebugger workflowDebugger;

        @Agent(outputKey = "quality")
        public double scoreStyle(@V("story") String story) {
            double result = 0.74;
            if (workflowDebugger != null) {
                inputReceived(story);
                outputProduced(result);
            }
            return result;
        }

        @Override
        public WorkflowDebugger getWorkflowDebugger() {
            return workflowDebugger;
        }

        @Override
        public void setWorkflowDebugger(WorkflowDebugger workflowDebugger) {
            this.workflowDebugger = workflowDebugger;
        }
    }

    public static final class Novel {
        private String story;
        private double score;

        public String getStory() {
            return story;
        }

        public void setStory(String aStory) {
            story = aStory;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double aScore) {
            score = aScore;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Novel) obj;
            return Objects.equals(this.story, that.story) &&
                    Double.doubleToLongBits(this.score) == Double.doubleToLongBits(that.score);
        }

        @Override
        public int hashCode() {
            return Objects.hash(story, score);
        }

        @Override
        public String toString() {
            return "Novel[" +
                    "story=" + story + ", " +
                    "score=" + score + ']';
        }

    }
}
