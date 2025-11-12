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
import com.gl.langchain4j.easyworkflow.WorkflowDebugger;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.OutputComposers.*;

/**
 * This class provides a sample for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#parallel-workflow">Parallel Workflow</a>
 * using EasyWorkflow DSL-style
 * workflow initialization.
 */
@SuppressWarnings("unused")
public class SampleParallelAgents {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {

        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        // result function that combines the outputs of meals and movies agents running in parallel
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

        ExecutorService executor = Executors.newFixedThreadPool(2);
        EasyWorkflow.AgentWorkflowBuilder<EveningPlannerAgent> builder;
        try {
            // getting results in parallel and using a custom function to combine them
            builder = EasyWorkflow.builder(EveningPlannerAgent.class);
            EveningPlannerAgent eveningPlannerAgent = builder
                    .chatModel(BASE_MODEL)
                    .logOutput(true)
                    .executor(executor)
                    .doParallel(resultFunction)
                        .outputName("plan")
                        .agent(FoodExpert.class)
                        .agent(MovieExpert.class)
                    .end()
                    .outputName("plan")
                    .build();

            System.out.println(eveningPlannerAgent.plan("happy"));
            System.out.println(eveningPlannerAgent.plan("sad"));

            // getting results in parallel and mapping function to combine them
            GenericEveningPlannerAgent genericEveningPlannerAgent = EasyWorkflow.builder(GenericEveningPlannerAgent.class)
                    .chatModel(BASE_MODEL)
                    .doParallel(asMap("movies", "meals"))
                        .agent(FoodExpert.class)
                        .agent(MovieExpert.class)
                    .end()
                    .build();

            System.out.println(genericEveningPlannerAgent.plan("happy"));
            System.out.println(genericEveningPlannerAgent.plan("sad"));
            System.out.println("--------------------");

            // getting results in parallel and using a bean list function to combine them
            WorkflowDebugger workflowDebugger = new WorkflowDebugger();
            BeanListEveningPlannerAgent beanListEveningPlannerAgent = EasyWorkflow.builder(BeanListEveningPlannerAgent.class)
                    .chatModel(BASE_MODEL)
                    .workflowDebugger(workflowDebugger)
                    .logInput(true)
                    .logOutput(true)
                    .doParallel(asBeanList(EveningPlan.class,
                            mappingOf("movies", "movie"),
                            mappingOf("meals", "meal")))
                        .outputName("result")
                        .agent(FoodExpert.class)
                        .agent(MovieExpert.class)
                    .end()
                    .outputName("result")
                    .build();

            System.out.println(beanListEveningPlannerAgent.plan("happy"));
            System.out.println(genericEveningPlannerAgent.plan("sad"));
        } finally {
            executor.shutdownNow();
            EasyWorkflow.closeSharedExecutorService();
        }
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
        List<EveningPlan> plan(@V("mood") String mood);
    }

    public interface GenericEveningPlannerAgent {
        @Agent
        Map<String, List<String>> plan(@V("mood") String mood);
    }

    public interface BeanListEveningPlannerAgent {
        @Agent
        List<EveningPlan> plan(@V("mood") String mood);
    }

    public static final class EveningPlan {
        private String meal;
        private String movie;

        public EveningPlan(@P("meal") String meal, @P("movie") String movie) {
            this.meal = meal;
            this.movie = movie;
        }

        public EveningPlan() {
        }

        public String getMeal() {
            return meal;
        }

        public void setMeal(String aMeal) {
            meal = aMeal;
        }

        public String getMovie() {
            return movie;
        }

        public void setMovie(String aMovie) {
            movie = aMovie;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (EveningPlan) obj;
            return Objects.equals(this.meal, that.meal) &&
                    Objects.equals(this.movie, that.movie);
        }

        @Override
        public int hashCode() {
            return Objects.hash(meal, movie);
        }

        @Override
        public String toString() {
            return "EveningPlan[" +
                    "meal=" + meal + ", " +
                    "movie=" + movie + ']';
        }

        }
}
