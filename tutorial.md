# Building AI Workflows with EasyWorkflow for LangChain4j

EasyWorkflow for LangChain4j is a library that introduces a fluent, DSL-style API for designing complex agentic workflows on top of the LangChain4j Agentic framework. It eliminates boilerplate code and makes AI workflow definitions concise, clear, and highly readable.

This article uses [Agents and Agentic AI tutorial](https://docs.langchain4j.dev/tutorials/agents/#loop-workflow) as the base but adopts it for use with EasyWorkflow. You may check the [source code for this article](https://github.com/gregory-ledenev/LangChain4j-EasyWorkflow/blob/main/src/test/java/com/gl/langchain4j/easyworkflow/samples/SampleSequentialAndRepeatableAgents.java) at GitHub.

With EasyWorkflow, you can easily compose workflows that include sequences of agents, conditional branches, parallel execution, loops, and even groups of agents. This gives you both flexibility and elegance when building intelligent applications.

## First Step: A Storytelling Workflow
Let’s start with a simple example: creating a workflow that writes a short story or novel snippet based on a topic, target audience, and style.

You begin by defining a `NovelCreator` interface, which acts as the entry point to the workflow:

```java
public interface NovelCreator {
    @Agent(outputName = "story")
    String createNovel(@V("topic") String topic, @V("audience") String audience, @V("style") String style);
}
```

Next, define a `CreativeWriter` agent that drafts the story around the given topic. To define an agent, you have to create an interface, having just one method and annotated with an `@Agent`, `@UserMessage` annotations:

```java
public interface CreativeWriter {
    @UserMessage("""
                You are a creative writer. Generate a draft of a
                story no more than 3 sentences
                long around the given topic.
                Return only the story and nothing else.
                The topic is {{topic}}.
                """)
    @Agent(value = "Generates a story based on the given topic", outputName = "story")
    String generateStory(@V("topic") String topic);
}
```
Now you can build and run your first workflow, having a single CreativeWriter agent:

```java
NovelCreator novelCreator = EasyWorkflow.builder(NovelCreator.class)
    .chatModel(BASE_MODEL)
        .agent(CreativeWriter.class)
    .build();

String story = novelCreator.createNovel("dragons and wizards", "infants", "fantasy");
System.out.println(story);
```

Run the workflow and get a creative story draft (about 2–3 sentences) around the topic “dragons and wizards”. No editing or refinement yet, just a raw imaginative draft.

## Adding Editing Agents
To refine the draft, you can add two editing agents: `AudienceEditor` - one for aligning the story with the target audience, and `StyleEditor` - for fine tuning the style.

```java
public interface AudienceEditor {
@UserMessage("""
            You are a professional editor. Rewrite the
            following story to better suit
            the target audience {{audience}}.
            Return only the story and nothing else.
            The story is: "{{story}}".
            """)
    @Agent(value = "Edits a story to better fit a given audience", outputName = "story")
    String editStory(@V("story") String story, @V("audience") String audience);
}

public interface StyleEditor {
    @UserMessage("""
                You are a professional editor. Rewrite the
                following story so it better
                matches the {{style}} style.
                Return only the story and nothing else.
                The story is: "{{story}}".
                """)
    @Agent(value = "Edits a story to better fit a given style", outputName = "story")
    String editStory(@V("story") String story, @V("style") String style);
}
```

Add the `AudienceEditor` and  `StyleEditor` agents to your workflow:

```java
NovelCreator novelCreator = EasyWorkflow.builder(NovelCreator.class)
    .chatModel(BASE_MODEL)
        .agent(CreativeWriter.class)
        .agent(AudienceEditor.class)
        .agent(StyleEditor.class)
    .build();
String story = novelCreator.createNovel("dragons and wizards", "adults", "fantasy");
System.out.println(story);
```

Run the workflow and get a polished story draft that’s rewritten for adults as the audience and styled as a fantasy narrative. You see how multiple agents can refine the initial draft step by step.

## Refinement with Loops and Scoring
Since AI agents can be inconsistent, you may need to loop through refinements until you reach a certain quality threshold. To do this, define a `StyleScorer` agent that evaluates how well the story matches the required style:

```java
public interface StyleScorer {
    @UserMessage("""
                You are a critical reviewer. Rate the following
                story between 0.0 and 1.0
                based on how well it matches the '{{style}}' style.
                Return only the score and nothing else.
                The story is: "{{story}}"
                """)
    @Agent(value = "Scores a story based on style alignment", outputName = "score")
    double scoreStyle(@V("story") String story, @V("style") String style);
}
```
You can now modify the workflow to loop through the scorer and editor until the score exceeds 0.8:

```java
NovelCreator novelCreator = EasyWorkflow.builder(NovelCreator.class)
    .chatModel(BASE_MODEL)
        .agent(CreativeWriter.class)
        .agent(AudienceEditor.class)
        .repeat(agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
            .agent(StyleScorer.class)
            .agent(StyleEditor.class)
        .end()
    .build();

String story = novelCreator.createNovel("dragons and wizards", "adults", "fantasy");
System.out.println(story);
```

Run the workflow and get a story that goes through multiple style refinement iterations until scoring at least 0.8 for alignment with the fantasy style. The output is more consistently on-theme and better polished than previous runs.


## What’s Next?
That’s all it takes to build adaptive, multi-step AI-powered workflows with EasyWorkflow. From here, you can explore even more complex patterns:

* Parallel execution of multiple agents 
* Conditional branching based on context
* Supervised groups of agents collaborating

You can use the Workflow Debugger that provides debugging functionality for agentic workflows. It allows you to:

* Examine the workflow context, including the results of agent executions.
* Define and handle breakpoints for events like agent input and output.

EasyWorkflow offers functionality to generate visual flow chart diagrams of the agentic workflow as HTML files. These diagrams serve as valuable tools for debugging as well as for illustration and documentation purposes.

EasyWorkflow lets you scale simple prototypes into sophisticated, easy writable and easy readable, production-ready AI workflows.