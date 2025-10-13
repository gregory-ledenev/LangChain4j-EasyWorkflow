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

import com.gl.langchain4j.easyworkflow.*;
import com.gl.langchain4j.easyworkflow.WorkflowDebugger.Breakpoint;
import com.gl.langchain4j.easyworkflow.gui.FormEditorType;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.internal.AgenticScopeOwner;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Objects;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.BreakpointActions.toggleBreakpoints;

/**
 * This class provides a sample for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#sequential-workflow">Sequential Workflow</a> and
 * <a href="https://docs.langchain4j.dev/tutorials/agents#loop-workflow">Loop Workflow</a> using EasyWorkflow DSL-style
 * workflow initialization.
 */
@SuppressWarnings("unused")
public class SampleSequentialAndRepeatableAgents {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        WorkflowDebugger workflowDebugger = new WorkflowDebugger();

        Breakpoint scoreBreakpoint = Breakpoint.
                builder(Breakpoint.Type.AGENT_OUTPUT, "SCORE for '{{$agentClass}}': {{score}}")
                .outputNames("score")
                .condition(ctx -> (double) ctx.getOrDefault("score", 0.0) >= 0.0)
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

        NovelCreator novelCreator = EasyWorkflow.builder(NovelCreator.class)
                .chatModel(BASE_MODEL)
                .workflowDebugger(workflowDebugger)
                .agent(CreativeWriter.class)
                .agent(AudienceEditor.class)
                .repeat(agenticScope -> agenticScope.readState("score", 0.0) < 0.8)
                    .agent(StyleScorer.class)
                    .breakpoint("SCORE (INLINE): {{score}}", ctx -> (double)ctx.getOrDefault("score", 0.0) >= 0.0)
                    .agent(StyleEditor.class)
                .end()
                .output(OutputComposers.asBean(Novel.class))
                .agent(new QualityScorer())
                .build();

        Novel novel = novelCreator.createNovel("dragons and wizards", "infants", "fantasy");
        System.out.println(novel);
        System.out.println("Agentic Scope: " + workflowDebugger.getAgenticScope().state());
        System.out.println(workflowDebugger.toString(true));
    }

    public interface CreativeWriter {
        @UserMessage("""
                     You are a creative writer. Generate a draft of a story no more than 3 sentences long around the
                     given topic.
                     Return only the story and nothing else.
                     The topic is {{topic}}.
                     """)
        @Agent(value = "Generates a story based on the given topic", outputName = "story")
        String generateStory(@V("topic") String topic);
    }

    public interface AudienceEditor {
        @UserMessage("""
                     You are a professional editor. Analyze and rewrite the following story to better align with the
                     target audience of {{audience}}.
                     Return only the story and nothing else.
                     The story is "{{story}}".
                     """)
        @Agent(value = "Edits a story to better fit a given audience", outputName = "story")
        String editStory(@V("story") String story, @V("audience") String audience);
    }

    public interface StyleEditor {
        @UserMessage("""
                     You are a professional editor. Analyze and rewrite the following story to better fit and be more
                     coherent with the {{style}} style.
                     Return only the story and nothing else.
                     The story is "{{story}}".
                     """)
        @Agent(value = "Edits a story to better fit a given style", outputName = "story")
        String editStory(@V("story") String story, @V("style") String style);
    }

    public interface NovelCreator extends AgenticScopeOwner {
        @Agent(outputName = "story")
        Novel createNovel(
                @PlaygroundParam(editorType = FormEditorType.Note, description = "Story topic")
                @V("topic")
                String topic,

                @PlaygroundParam(description = "Story audience", editorType = FormEditorType.EditableDropdown, editorChoices = {"infants", "children", "teenagers", "adults"})
                @V("audience")
                String audience,

                @PlaygroundParam(description = "Story style", editorType = FormEditorType.EditableDropdown, editorChoices = {"comedy", "horror", "fantasy", "romance"})
                @V("style")
                String style
        );
    }

    public interface StyleScorer {
        @UserMessage("""
                     You are a critical reviewer. Give a review score between 0.0 and 1.0 for the following story based
                     on how well it aligns with the style '{{style}}'.
                     Return only the score and nothing else.
                     The story is: "{{story}}"
                     """)
        @Agent(value = "Scores a story based on how well it aligns with a given style", outputName = "score")
        double scoreStyle(@V("story") String story, @V("style") String style);
    }

    public static class QualityScorer implements WorkflowDebuggerSupport {
        private WorkflowDebugger workflowDebugger;

        @Agent(outputName = "quality")
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

        public void setStory(String aStory) {
            story = aStory;
        }

        public void setScore(double aScore) {
            score = aScore;
        }

        public String getStory() {
            return story;
        }

        public double getScore() {
            return score;
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

    public static class LoggingOutputGuardrail implements OutputGuardrail {
        @Override
        public OutputGuardrailResult validate(AiMessage responseFromLLM) {
            return success();
        }
    }
}
