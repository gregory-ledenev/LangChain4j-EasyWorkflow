# EasyWorkflow for LangChain4j

EasyWorkflow for LangChain4j is a library that offers a fluent, DSL-style API for building complex agentic workflows on top of the LangChain4j Agentic framework. It removes boilerplate and makes it simple to express AI workflows in a clear, readable way.

With EasyWorkflow, you can define workflows that include sequences of agents, conditional branches, parallel execution, agent groups, and loops, combining flexibility with elegance.

## Features

*   **Fluent API:** A simple and intuitive DSL-style API for defining complex agent workflows.
*   **Sequential Execution:** Define a sequence of agents that will be executed one after another.
*   **Conditional Execution:** Execute agents based on a condition.
*   **Parallel Execution:** Execute agents in parallel and compose their outputs.
*   **Agent Grouping:** Group agents and supervise their execution.
*   **Loops:** Repeat a sequence of agents until a condition is met.

## Adding to Your Build

To add EasyWorkflow to your build system, you can use the following Maven dependency:

```xml
<dependency>
    <groupId>io.github.gregory-ledenev</groupId>
    <artifactId>langchain4j-easyworkflow</artifactId>
    <version>0.9.3</version>
</dependency>
```
to get JavaDoc for it:

```xml
<dependency>
    <groupId>io.github.gregory-ledenev</groupId>
    <artifactId>langchain4j-easyworkflow</artifactId>
    <version>0.9.3</version>
    <classifier>javadoc</classifier>
</dependency>
```

## How to use EasyWorkflow

The `EasyWorkflow` is the main entry point for creating workflows. Here’s how to use it for common tasks. To start you can use `EasyWorkflow.builder(Class<?> agentClass)` method to get a builder object and provide the main agentic interface. Then you can configure it, add agents and finally use the `build` as a terminal operation to create a configured workflow.

### 1. Basic Configuration

Before adding agents, you need to configure the workflow. At a minimum, you must provide a `ChatModel`. You can also provide a `ChatMemory` instance to maintain conversation history and specify some other properties.

```java
// Import your chat model, e.g., OpenAiChatModel
// Import a chat memory, e.g., MessageWindowChatMemory

ExpertRouterAgent expertRouterAgent = EasyWorkflow.builder(ExpertRouterAgent.class)
        .chatModel(chatModel) // Mandatory: The chat model for the agents
        .chatMemory(chatMemory) // Optional: Shared memory for the agents
        // ... add agents and control flow here
        .build();

```

It is possible to set up input and output logging by using `logInput(true)` and `logOutput(true)` methods.

Note: EasyWorkflows logging uses corresponding guard rails to intercept agents flow so if you use customizers to set up guard rails of your own — you should set up logging manually using `LoggingGuardrails.Input` and `LoggingGuardrails.Output` loggers.  

```java
BeanListEveningPlannerAgent agent = EasyWorkflow.builder(BeanListEveningPlannerAgent.class)
        .chatModel(BASE_MODEL)
        .logInput(true)
        .logOutput(true)
        .doParallel()
            .agent(FoodExpert.class)
            .agent(MovieExpert.class)
        .end()
        .build();
```
### 2. Adding Agents

You can add agents to the workflow to be executed sequentially. You can add an agent by its class or by providing an
already-created instance. An agent can be defined either by an interface or a class, having a single method, annotated
with an `@Agent` annotation. 

See more: [Agents in LangChain4j](https://docs.langchain4j.dev/tutorials/agents#agents-in-langchain4j)

```java
NovelCreator novelCreator = EasyWorkflow.builder(NovelCreator.class)
        .chatModel(BASE_MODEL)
        // Add agents by their class
        .agent(CreativeWriter.class)
        .agent(AudienceEditor.class)
        // You can also add a pre-configured agent instance
        // .agent(new MyCustomAgent())
        .build();

public interface CreativeWriter {
    @UserMessage("""
                 You are a creative writer. Generate a draft of a story no more than 3 sentences long around the
                 given topic. Return only the story and nothing else. The topic is {{topic}}.
                 """)
    @Agent(value = "Generates a story based on the given topic", outputName = "story")
    String generateStory(@V("topic") String topic);
}
```

If you need to access a complete set of properties allowed by agent's builder — create an agent, providing a customizer 
lambda function:

```java
SupervisorAgent supervisorAgent1 = EasyWorkflow.builder(SupervisorAgent.class)
    .chatModel(BASE_MODEL)
    .doAsGroup()
        .agent(WithdrawAgent.class, builder -> builder.agentBuilderCustomizer(bankTool))
        .agent(CreditAgent.class, builder -> builder.agentBuilderCustomizer(bankTool))
        .agent(ExchangeAgent.class, builder -> builder.agentBuilderCustomizer(new ExchangeTool()))
        .agent(humanInTheLoop)
    .end()
    .build();
```
### 3. Adding Control Flow

For more complex workflows, you can use control flow statements like `ifThen`, `repeat`, `doParallel`, and `group`. Each of these statements opens a block that must be closed with the `end()` method.

Here's an example combining a conditional and a loop:

```java
ExpertRouterAgent expertRouterAgent = EasyWorkflow.builder(ExpertRouterAgent.class)
        .chatModel(BASE_MODEL)
        .agent(CategoryRouter.class)
        // Execute a block of agents only if a condition is met
        .ifThen(scope -> scope.readState("category", "UNKNOWN").equals("LEGAL"))
            .agent(LegalExpert.class)
            // You can nest control flow
            .repeat(2, scope -> scope.readState("isClear", false) == true)
                .agent(LegalClarifier.class)
            .end() // Closes the 'repeat' block
        .end() // Closes the 'ifThen' block
        .build();
```

You can find more detailed examples in the following sections.

## Sample for Sequential and Repeatable Agents

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

## Sample for Conditional Agents

The following example shows how to create a workflow with conditional execution of agents. You may check the [Conditional Workflow](https://docs.langchain4j.dev/tutorials/agents#conditional-workflow) for complete samples description or check the runnable test at [TestConditionalAgents.java](/src/test/java/com/gl/langchain4j/easyworkflow/TestConditionalAgents.java)

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

## Sample for Parallel Agents

The following example shows how to create a workflow with parallel execution of agents. You may check the [Parallel Workflow](https://docs.langchain4j.dev/tutorials/agents#parallel-workflow) for complete samples description or check the runnable test at [TestParallelAgents.java](/src/test/java/com/gl/langchain4j/easyworkflow/TestParallelAgents.java)

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
## Sample for Pure Agentic AI

The following example shows how to create a workflow with a supervised group of agents that form a pure agentic AI.
You may check the [Parallel Workflow](https://docs.langchain4j.dev/tutorials/agents#pure-agentic-ai) for complete samples description or check the runnable test at [TestSupervisedAgents.java](/src/test/java/com/gl/langchain4j/easyworkflow/TestSupervisedAgents.java)

```java
SupervisorAgent supervisorAgent1 = EasyWorkflow.builder(SupervisorAgent.class)
        .chatModel(BASE_MODEL)
        .group()
            .agent(WithdrawAgent.class, builder -> builder.agentBuilderCustomizer(bankTool))
            .agent(CreditAgent.class, builder -> builder.agentBuilderCustomizer(bankTool))
            .agent(ExchangeAgent.class, builder -> builder.agentBuilderCustomizer(new ExchangeTool()))
            .agent(humanInTheLoop)
        .end()
        .build();
```
## License

The EasyWorkflow for LangChain4j is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
