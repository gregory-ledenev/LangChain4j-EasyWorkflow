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

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.prefs.Preferences;

/**
 * This class provides a sample for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#parallel-workflow">Parallel Workflow</a>
 * using EasyWorkflow DSL-style
 * workflow initialization.
 */
public class TestParallelAgents {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your Groq API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another Groq BASE_MODEL name
                .build();

        // result function, that composes outputs of meals and movies agents running in parallel
        Function<AgenticScope, Object> resultFunction = agenticScope -> {
            List<String> movies = agenticScope.readState("movies", List.of());
            List<String> meals = agenticScope.readState("meals", List.of());

            List<EveningPlan> moviesAndMeals = new ArrayList<>();
            for (int i = 0; i < movies.size(); i++) {
                if (i >= meals.size()) {
                    break;
                }
                moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
            }
            return moviesAndMeals;
        };

        EveningPlannerAgent eveningPlannerAgent = EasyWorkflow.builder(EveningPlannerAgent.class)
                .chatModel(BASE_MODEL)
                .doParallel(resultFunction)
                    .agent(FoodExpert.class)
                    .agent(MovieExpert.class)
                .end()
                .build();

        System.out.println(eveningPlannerAgent.plan("happy"));
        System.out.println(eveningPlannerAgent.plan("sad"));
        EasyWorkflow.closeSharedExecutorService();
    }

    public interface FoodExpert {

        @UserMessage("""
                     You are a great evening planner.
                     Propose a list of 3 meals matching the given mood.
                     The mood is {{mood}}.
                     For each meal, just give the name of the meal.
                     Provide a list with the 3 items and nothing else.
                     """)
        @Agent(outputName = "meals")
        List<String> findMeal(@V("mood") String mood);
    }

    public interface MovieExpert {

        @UserMessage("""
                     You are a great evening planner.
                     Propose a list of 3 movies matching the given mood.
                     The mood is {{mood}}.
                     Provide a list with the 3 items and nothing else.
                     """)
        @Agent(outputName = "movies")
        List<String> findMovie(@V("mood") String mood);
    }

    public interface EveningPlannerAgent {
        @Agent
        public List<EveningPlan> plan(@P("mood") String mood);
    }

    public record EveningPlan(String meal, String movie) {
    }

    public static class EveningPlannerAgentImpl {
        @Agent(outputName = "plan1213")
        public List<EveningPlan> plan(@P("plan") List<EveningPlan> plan) {
            return plan;
        }
    }
}
