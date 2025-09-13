# LangChain4j EasyWorkflow

LangChain4j EasyWorkflow is a library that provides a fluent API for building complex agentic workflows using LangChain4j's Agentic framework. It allows defining sequences of agents, conditional execution, parallel execution, agent grouping, and loops.

## Features

*   **Fluent API:** A simple and intuitive DSL-style API for defining complex agent workflows.
*   **Sequential Execution:** Define a sequence of agents that will be executed one after another.
*   **Conditional Execution:** Execute agents based on a condition.
*   **Parallel Execution:** Execute agents in parallel and compose their outputs.
*   **Agent Grouping:** Group agents and supervise their execution.
*   **Loops:** Repeat a sequence of agents until a condition is met.

## Usage

To use LangChain4j EasyWorkflow, you need to add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.gl.langchain4j</groupId>
    <artifactId>easy-workflow</artifactId>
    <version>1.0.0</version>
</dependency>
```

## AgentWorkflowBuilder Methods

The `AgentWorkflowBuilder` provides the following methods for constructing a workflow:

### `outputName(String outputName)`
Sets the output name for the main agent of this workflow.

### `chatModel(ChatModel chatModel)`
Sets the `ChatModel` to be used by all agents in this workflow.

### `chatMemory(ChatMemory chatMemory)`
Sets the `ChatMemory` to be used by all agents in this workflow.

### `agent(Class<?> agentClass)`
Adds an agent to the workflow using its class.

### `agent(Class<?> agentClass, String outputName)`
Adds an agent to the workflow using its class and specifies an output name.

### `agent(Class<?> agentClass, Consumer<AgentBuilder<?>> configurator)`
Adds an agent to the workflow using its class and a configurator for its builder.

### `agent(Class<?> agentClass, String outputName, Consumer<AgentBuilder<?>> configurator)`
Adds an agent to the workflow using its class, an output name, and a configurator.

### `agent(Object agent)`
Adds an existing agent instance to the workflow.

### `agent(Object agent, String outputName)`
Adds an existing agent instance to the workflow and specifies an output name.

### `agent(Object agent, Consumer<AgentBuilder<?>> configurator)`
Adds an existing agent instance to the workflow and a configurator for its builder.

### `agent(Object agent, String outputName, Consumer<AgentBuilder<?>> configurator)`
Adds an existing agent instance to the workflow, an output name, and a configurator.

### `ifThen(Predicate<AgenticScope> condition)`
Starts an "if-then" conditional block.

### `doParallel(Function<AgenticScope, Object> outputComposer)`
Starts a "do parallel" block.

### `doParallel(String outputName, Function<AgenticScope, Object> outputComposer)`
Starts a "do parallel" block with a specified output name.

### `group()`
Starts a "group" block.

### `group(String outputName)`
Starts a "group" block with a specified output name.

### `repeat(Predicate<AgenticScope> condition)`
Starts a "repeat" block.

### `repeat(int maxIterations, Predicate<AgenticScope> condition)`
Starts a "repeat" block with a specified maximum number of iterations.

### `end()`
Ends the current nested statement.

### `build()`
Terminal operation that builds the EasyWorkflow and returns the main agent instance.

## Sequential and Repeatable Agents

The following example shows how to create a sequential workflow with a repeatable block of agents. You may check the [Sequential Workflow](https://docs.langchain4j.dev/tutorials/agents#sequential-workflow)
and [Loop Workflow](https://docs.langchain4j.dev/tutorials/agents#loop-workflow) for complete samples description or check the runnable test at [TestSequentialAndRepeatableAgents.java](/src/test/java/com/gl/langchain4j/easyworkflow/TestSequentialAndRepeatableAgents.java)

```java
NovelCreator novelCreator = EasyWorkflow.builder(NovelCreator.class)
        .chatModel(BASE_MODEL)
        .agent(CreativeWriter.class)
        .agent(AudienceEditor.class)
        .repeat(5, agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
            .agent(StyleScorer.class)
            .agent(StyleEditor.class)
        .end()
        .build();

String story = novelCreator.createNovel("dragons and wizards", "infants", "fantasy");
```

## Conditional Agents

The following example shows how to create a workflow with conditional execution of agents.

```java
ExpertRouterAgent expertRouterAgent = EasyWorkflow.builder(ExpertRouterAgent.class)
        .chatModel(BASE_MODEL)
        .chatMemory(chatMemory)
        .agent(CategoryRouter.class)
        .ifThen(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL)
            .agent(MedicalExpert.class).end()
        .ifThen(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL)
            .agent(LegalExpert.class).end()
        .ifThen(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL)
            .agent(TechnicalExpert.class).end()
        .build();

expertRouterAgent.ask("Should I sue my neighbor who caused this damage?");
```

## Parallel Agents

The following example shows how to create a workflow with parallel execution of agents.

```java
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

eveningPlannerAgent.plan("happy");
```

## License

This project is licensed under the MIT License.
