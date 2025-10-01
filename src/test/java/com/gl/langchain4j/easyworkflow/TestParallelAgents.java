package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.prefs.Preferences;

import static com.gl.langchain4j.easyworkflow.OutputComposers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParallelAgents {
    static final String GROQ_API_KEY = "groqApiKey";

    @Test
    public void test() {

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
                moviesAndMeals.add(new EveningPlan(meals.get(i), movies.get(i)));
            }
            return moviesAndMeals;
        };

        WorkflowDebugger workflowDebugger = new WorkflowDebugger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        EasyWorkflow.AgentWorkflowBuilder<EveningPlannerAgent> builder;
        try {
            // getting results in parallel and using a custom function to combine them
            builder = EasyWorkflow.builder(EveningPlannerAgent.class);
            EveningPlannerAgent eveningPlannerAgent = builder
                    .chatModel(BASE_MODEL)
                    .workflowDebugger(workflowDebugger)
                    .doParallel(resultFunction)
                    .outputName("plan")
                    .agent(new FoodExpert())
                    .agent(new MovieExpert())
                    .end()
                    .outputName("plan")
                    .build();

            assertEquals("[EveningPlan[meal=Chicken Fajitas, movie=The Princess Briden], EveningPlan[meal=Grilled Cheeseburgers, movie=Elf], EveningPlan[meal=Seafood Paella, movie=Amélie]]", eveningPlannerAgent.plan("happy").toString());

            System.out.println(builder.toJson());

            try {
                workflowDebugger.toHtmlFile("workflow.html", false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // getting results in parallel and mapping function to combine them
            GenericEveningPlannerAgent genericEveningPlannerAgent = EasyWorkflow.builder(GenericEveningPlannerAgent.class)
                    .chatModel(BASE_MODEL)
                    .doParallel(asMap("movies", "meals"))
                    .agent(new FoodExpert())
                    .agent(new MovieExpert())
                    .end()
                    .build();

            assertEquals("{movies=[The Princess Briden, Elf, Amélie], meals=[Chicken Fajitas, Grilled Cheeseburgers, Seafood Paella]}", genericEveningPlannerAgent.plan("happy").toString());

            // getting results in parallel and using a bean list function to combine them
            BeanListEveningPlannerAgent beanListEveningPlannerAgent = EasyWorkflow.builder(BeanListEveningPlannerAgent.class)
                    .chatModel(BASE_MODEL)
                    .doParallel(asBeanList(EveningPlan.class,
                            mappingOf("movies", "movie"),
                            mappingOf("meals", "meal")))
                    .outputName("result")
                    .agent(new FoodExpert())
                    .agent(new MovieExpert())
                    .end()
                    .outputName("result")
                    .build();
            assertEquals("[EveningPlan[meal=Chicken Fajitas, movie=The Princess Briden], EveningPlan[meal=Grilled Cheeseburgers, movie=Elf], EveningPlan[meal=Seafood Paella, movie=Amélie]]", beanListEveningPlannerAgent.plan("happy").toString());
        } finally {
            executor.shutdownNow();
            EasyWorkflow.closeSharedExecutorService();
        }
    }

    @SuppressWarnings("unused")
    public interface EveningPlannerAgent {
        @Agent
        List<EveningPlan> plan(@P("mood") String mood);
    }

    @SuppressWarnings("unused")
    public interface GenericEveningPlannerAgent {
        @Agent
        Map<String, List<String>> plan(@P("mood") String mood);
    }

    @SuppressWarnings("unused")
    public interface BeanListEveningPlannerAgent {
        @Agent
        List<EveningPlan> plan(@P("mood") String mood);
    }

    @SuppressWarnings("unused")
    public static class FoodExpert extends WorkflowDebuggerSupport.Impl {
        @Agent(outputName = "meals")
        public List<String> findMeal(@V("mood") String mood) {
            inputReceived(mood);
            List<String> result = List.of("Chicken Fajitas", "Grilled Cheeseburgers", "Seafood Paella");
            outputProduced(result);

            return result;
        }
    }

    @SuppressWarnings("unused")
    public static class MovieExpert extends WorkflowDebuggerSupport.Impl {
        @Agent(outputName = "movies")
        public List<String> findMovie(@V("mood") String mood) {
            inputReceived(mood);
            List<String> result = List.of("The Princess Briden", "Elf", "Amélie");
            outputProduced(result);

            return result;
        }
    }

    @SuppressWarnings("unused")
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