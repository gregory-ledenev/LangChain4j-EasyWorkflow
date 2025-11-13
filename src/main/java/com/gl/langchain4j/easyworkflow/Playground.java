/*
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
 */

package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.V;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.BiFunction;

import static java.lang.System.out;

public interface Playground {

    /**
     * Plays with the agent with the given user message.
     *
     * @param agent The agent object.
     * @param userMessage The user's initial message.
     */
    void play(Object agent, Map<String, Object> userMessage);

    /**
     * Sets the human request. This method is typically used in interactive playgrounds
     * where the human's input needs to be explicitly set.
     *
     * @param request The human's request.
     */
    void setHumanRequest(HumanInTheLoop agent, String request);

    /**
     * Gets the human's response. This method is typically used in interactive playgrounds
     * to retrieve input from the human.
     *
     * @return The human's response.
     */
    String getHumanResponse(HumanInTheLoop agent);

    /**
     * Constant for the argument key "title".
     */
    String ARG_TITLE = "title";

    /**
     * Constant for the argument key "chatModels". A value for this key must be a {@code List<PlaygroundChatModel>}
     */
    String ARG_CHAT_MODELS = "chatModels";

    /**
     * Constant for the argument key "workflowDebugger". A value for this key must be a {@link WorkflowDebugger}
     * instance. This debugger is used to track and visualize the workflow execution.
     */
    String ARG_WORKFLOW_DEBUGGER = "workflowDebugger";

    /**
     * Constant for the argument key "use dialog". If the value for that argument is {@code true} - playground will be
     * presented as a dialog vs. frame by default.
     */
    String ARG_SHOW_DIALOG = "showDialog";

    /**
     * Constant for the argument key "ownerFrame". If the value for that argument is {@code Frame} and
     * {@code ARG_SHOW_DIALOG=true}- playground will be presented as a dialog with the specified owner frame.
     */
    String ARG_OWNER_FRAME = "ownerFrame";

    /**
     * Sets up the playground with the given arguments. Use {@code ARG_} constants to pass arguments to the playground.
     *
     * @param arguments A map of arguments for setting up the playground.
     */
    void setup(Map<String, Object> arguments);

    /**
     * Represents the type of playground.
     */
    enum Type {
        Console, GUI
    }

    /**
     * Creates a new Playground instance based on the specified type.
     *
     * @param agentClass The class of the agent to be used in the playground.
     * @param type       The type of playground to create (Console or GUI).
     * @return A new Playground instance.
     */
    static Playground createPlayground(Class<?> agentClass, Type type) {
        return createPlayground(agentClass, type, null);
    }

    /**
     * Creates a new Playground instance based on the specified type.
     *
     * @param agentClass       The class of the agent to be used in the playground.
     * @param type             The type of playground to create (Console or GUI).
     * @param workflowDebugger The workflowDebugger to work with. Pass it if you want a complete GUI Playground with
     *                         workflow summary, tracking execution results, Workflow Expert, etc.
     * @return A new Playground instance.
     */
    static Playground createPlayground(Class<?> agentClass, Type type, WorkflowDebugger workflowDebugger) {
        Playground result;
        switch (type) {
            case Console -> result = new ConsolePlayground(agentClass);
            case GUI -> {
                // intentionally done via reflection to not expose GUI classes here
                // as it may cause issues in headless environments
                try {
                    Class<?> playgroundClass = Class.forName("com.gl.langchain4j.easyworkflow.gui.GUIPlayground");
                    result = (Playground) playgroundClass.getConstructor(Class.class).newInstance(agentClass);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            default -> throw new IllegalStateException("Unexpected playground type: " + type);
        }

        if (workflowDebugger != null)
            result.setup(Map.of(ARG_WORKFLOW_DEBUGGER, workflowDebugger));

        return result;
    }

    abstract class BasicPlayground implements Playground, BiFunction<Object, Map<String, Object>, Object> {
        protected final Class<?> agentClass;
        protected final Method agentMethod;

        public BasicPlayground(Class<?> agentClass) {
            this.agentClass = agentClass;
            this.agentMethod = findAgentMethod();
        }

        @Override
        public void setup(Map<String, Object> arguments) {
        }

        @Override
        public Object apply(Object agent, Map<String, Object> request) {
            try {
                Object[] args = requestToArguments(request, agentMethod.getParameters());
                return agentMethod.invoke(agent, args);
            } catch (Exception ex) {
                throw  new RuntimeException(WorkflowDebugger.getFailureCauseException(ex));
            }
        }

        protected String getArgumentInstructions() {
            StringBuilder instructions = new StringBuilder();
            instructions.append("To enter specific arguments, delimit them with a semicolon.\n");
            instructions.append("Arguments:\n");
            for (int i = 0; i < agentMethod.getParameterCount(); i++) {
                Parameter parameter = agentMethod.getParameters()[i];
                String name = parameter.getName();
                V v = parameter.getAnnotation(V.class);
                if (v != null)
                    name = v.value();

                instructions.append(String.format("- %s (Type: %s)\n",
                        name,
                        agentMethod.getParameterTypes()[i].getSimpleName()));
            }
            return instructions.toString();
        }

        protected Method findAgentMethod() {
            List<Method> agentMethods = Arrays.stream(agentClass.getMethods())
                    .filter(method -> method.isAnnotationPresent(Agent.class))
                    .toList();

            if (agentMethods.isEmpty())
                throw new IllegalStateException(String.format("No public method annotated with @Agent found in class %s", agentClass.getName()));
            if (agentMethods.size() > 1)
                throw new IllegalStateException(String.format("Multiple public methods annotated with @Agent found in class %s. Please annotate only one.", agentClass.getName()));

            return agentMethods.get(0);
        }

        /**
         * Converts a request string into a map of argument names to their corresponding values.
         *
         * @param request The request string, where arguments are delimited by semicolons.
         * @param parameters An array of {@link Parameter} objects representing the expected arguments.
         * @return A {@link Map} where keys are parameter names and values are the converted argument values.
         */
        public static Map<String, Object> requestToArgumentsMap(String request, Parameter[] parameters) {
            Map<String, Object> result = new HashMap<>();
            Object[] arguments = requestToArguments(request, parameters);
            for (int i = 0; i < parameters.length; i++) {
                result.put(parameters[i].getName(), arguments[i]);
            }
            return result;
         }

        /**
         * Converts a map of request parameters to an array of argument values, matching the order of the given parameters.
         *
         * @param request A {@link Map} where keys are parameter names (or their {@link V} annotation values) and values are the argument values.
         * @param parameters An array of {@link Parameter} objects representing the expected arguments.
         * @return An array of {@link Object}s, where each element is an argument value in the order of the provided parameters.
         */
        public static Object[] requestToArguments(Map<String, Object> request, Parameter[] parameters) {
            Object[] arguments = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                String name = parameter.getName();
                V v = parameter.getAnnotation(V.class);
                if (v != null)
                    name = v.value();

                arguments[i] = request.get(name);
            }

            return arguments;
        }

        /**
         * Converts a semicolon-delimited request string into an array of argument values, converting them to the appropriate types.
         *
         * @param request The request string, where arguments are delimited by semicolons.
         * @param parameters An array of {@link Parameter} objects representing the expected arguments and their types.
         * @return An array of {@link Object}s, where each element is a converted argument value.
         */
        public static Object[] requestToArguments(String request, Parameter[] parameters) {
            if (request == null || request.isEmpty())
                return new Object[0];

            String[] requestParts = request.split(";");
            Object[] result = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                if (i < requestParts.length) {
                    result[i] = convert(requestParts[i], parameters[i].getType());
                } else {
                    result[i] = null;
                }
            }

            return result;
        }

        protected static Object convert(String value, Class<?> toClass) {
            if (toClass == String.class) {
                return value;
            } else if (toClass == Integer.class || toClass == int.class) {
                return Integer.parseInt(value);
            } else if (toClass == Long.class || toClass == long.class) {
                return Long.parseLong(value);
            } else if (toClass == Double.class || toClass == double.class) {
                return Double.parseDouble(value);
            } else if (toClass == Float.class || toClass == float.class) {
                return Float.parseFloat(value);
            } else if (toClass == Boolean.class || toClass == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (toClass == Short.class || toClass == short.class) {
                return Short.parseShort(value);
            } else if (toClass == Byte.class || toClass == byte.class) {
                return Byte.parseByte(value);
            }

            return value;
        }
    }

    class ConsolePlayground extends BasicPlayground {
        Scanner scanner;

        /**
         * Constructs a new {@code ConsolePlayground} with the specified agent class.
         *
         * @param agentClass The class of the agent to be used in the playground.
         */
        public ConsolePlayground(Class<?> agentClass) {
            super(agentClass);
        }

        /**
         * Plays with the agent in a console-based interactive loop.
         * @param agent The agent object.
         * @param userMessage The user's initial message.
         */
        @Override
        public void play(Object agent, Map<String, Object> userMessage) {
            String result;
            Map<String, Object> request = userMessage;
            out.println("PLAYGROUND (type your questions or arguments, or 'exit' to quit)\n");
            out.println(getArgumentInstructions());

            if (request != null && ! request.isEmpty())
                out.println("> " + request);

            scanner = new Scanner(System.in);
            try {
                while (true) {
                    if (request != null && !request.isEmpty()) {
                        out.print("[thinking...]\n");
                        result = apply(agent, request).toString();
                        if (result != null && !result.isEmpty())
                            out.println("Answer: " + result);
                    }
                    out.print("> ");

                    String line = scanner.nextLine();
                    if (line.equalsIgnoreCase("exit"))
                        break;

                    request = requestToArgumentsMap(line, agentMethod.getParameters());
                }
            } finally {
                scanner.close();
            }
        }

        /**
         * Sets the human request and prints it to the console.
         * @param request The human's request.
         */
        @Override
        public void setHumanRequest(HumanInTheLoop agent, String request) {
            out.println(request);
            out.print("> ");
        }

        /**
         * Reads the human's response from the console.
         * @return The human's response.
         */
        @Override
        public String getHumanResponse(HumanInTheLoop agent) {
            return scanner.nextLine();
        }
    }

    record PlaygroundChatModel(String name, ChatModel chatModel) {
        @Override
        public String toString() {
            return name;
        }
    }
}
