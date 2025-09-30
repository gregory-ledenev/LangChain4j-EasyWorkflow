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
 */

package com.gl.langchain4j.easyworkflow;

import dev.langchain4j.agentic.scope.AgenticScope;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * Utility class for composing results from an {@link AgenticScope} into various formats, such as Maps or lists of
 * beans. It usually can be used to combine results produced by several agents working in parallel.
 */
public class OutputComposers {
    /**
     * Creates a function that composes the results from specified output names in the {@link AgenticScope} into a Map.
     *
     * @param outputNames The names of the outputs to retrieve from the {@link AgenticScope}.
     * @return A {@link Function} that takes an {@link AgenticScope} and returns a {@link Map} of the specified outputs.
     */
    public static Function<AgenticScope, Object> asMap(String... outputNames) {
        if (outputNames.length == 0)
            return agenticScope -> null;

        return agenticScope -> {
            Map<String, Object> result = new HashMap<>();
            for (String outputName : outputNames) {
                result.put(outputName, agenticScope.readState(outputName));
            }
            return result;
        };
    }

    /**
     * Creates a {@link Mapping} object that associates an output name from the {@link AgenticScope} with a property
     * name in a target bean. The property name will be the same as the output name.
     *
     * @param outputName The name of the output in the {@link AgenticScope}.
     * @return A new {@link Mapping} instance.
     */
    @SuppressWarnings("unused")
    public static Mapping mappingOf(String outputName) {
        return mappingOf(outputName, outputName);
    }

    /**
     * Creates a {@link Mapping} object that associates an output name from the {@link AgenticScope} with a property
     * name in a target bean.
     *
     * @param outputName   The name of the output in the {@link AgenticScope}.
     * @param propertyName The name of the property in the target bean.
     * @return A new {@link Mapping} instance.
     */
    public static Mapping mappingOf(String outputName, String propertyName) {
        return new Mapping(outputName, propertyName);
    }

    /**
     * Creates a function that composes the results from specified output names in the {@link AgenticScope} into a List
     * of beans. Each bean in the list will be an instance of the provided {@code beanClass}. The values from the
     * {@link AgenticScope} are mapped to the bean's properties using the provided {@link Mapping}s. If an output in the
     * {@link AgenticScope} is a List, it will be iterated to populate multiple beans.
     *
     * @param beanClass The class of the bean to create.
     * @param mappings  An array of {@link Mapping} objects defining how {@link AgenticScope} outputs map to bean
     *                  properties.
     * @param <T>       The type of the bean.
     * @return A {@link Function} that takes an {@link AgenticScope} and returns a {@link List} of beans.
     */
    public static <T> Function<AgenticScope, Object> asBeanList(Class<T> beanClass,
                                                                Mapping... mappings) {
        Objects.requireNonNull(beanClass, "Bean class can't be null");
        if (mappings.length == 0)
            return agenticScope -> null;

        return agenticScope -> {
            Map<String, List<Object>> states = new HashMap<>();
            int maxStateSize = cacheAgenticScopeStates(mappings, agenticScope, states);

            List<T> result = new ArrayList<>();
            for (int i = 0; i < maxStateSize; i++) {
                try {
                    result.add(createAgenticScopeStateBean(beanClass, mappings, states, i));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create or populate bean of type " + beanClass.getName(), e);
                }
            }

            return result;
        };
    }

    /**
     * Creates a function that composes all results from the {@link AgenticScope} into a single bean. The keys in the
     * {@link AgenticScope}'s state are used to find corresponding setter methods in the bean. For example, a state key
     * "userName" will attempt to call `setUserName(String value)` on the bean. Non-matching state keys or missing
     * setters will be ignored.
     *
     * @param beanClass The class of the bean to create and populate.
     * @param <T>       The type of the bean.
     * @return A {@link Function} that takes an {@link AgenticScope} and returns a single populated bean.
     */
    public static <T> Function<AgenticScope, Object> asBean(Class<T> beanClass) {
        Objects.requireNonNull(beanClass, "Bean class can't be null");

        return agenticScope -> {
            try {
                T bean = beanClass.getConstructor().newInstance();
                for (Map.Entry<String, Object> entry : agenticScope.state().entrySet()) {
                    String stateKey = entry.getKey();
                    Object stateValue = entry.getValue();
                    if (stateValue != null) {
                        String setterName = "set" + Character.toUpperCase(stateKey.charAt(0)) + stateKey.substring(1);
                        Method setter = getMethod(beanClass, setterName, stateValue);
                        if (setter != null)
                            setter.invoke(bean, stateValue);
                    }
                }
                return bean;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create or populate bean of type " + beanClass.getName(), e);
            }
        };
    }

    private static <T> Method getMethod(Class<T> beanClass, String setterName, Object stateValue) {
        Method setter = null;
        try {
            setter = beanClass.getMethod(setterName, stateValue.getClass());
        } catch (NoSuchMethodException e) {
            // Ignore if setter not found for this state key
            // Try primitive types for Number wrappers
            if (stateValue instanceof Number) {
                Class<?> primitiveType = getPrimitiveType(stateValue.getClass());
                if (primitiveType != null) {
                    try {
                        setter = beanClass.getMethod(setterName, primitiveType);
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
        }
        return setter;
    }

    private static <T> T createAgenticScopeStateBean(Class<T> beanClass, Mapping[] mappings,
                                                     Map<String, List<Object>> states, int index)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        T bean = beanClass.getDeclaredConstructor().newInstance();
        for (Mapping mapping : mappings) {
            List<Object> stateList = states.get(mapping.outputName());
            if (stateList != null && index < stateList.size()) {
                Object value = stateList.get(index);
                String setterName = beanClass.isRecord() ?
                        mapping.propertyName :
                        "set" + Character.toUpperCase(mapping.propertyName.charAt(0)) + mapping.propertyName.substring(1);
                Method setter = beanClass.getDeclaredMethod(setterName, value.getClass());
                setter.invoke(bean, value);
            }
        }
        return bean;
    }

    @SuppressWarnings("unchecked")
    private static int cacheAgenticScopeStates(Mapping[] mappings, AgenticScope agenticScope, Map<String, List<Object>> states) {
        int maxStateSize = 0;
        for (Mapping mapping : mappings) {
            Object state = agenticScope.readState(mapping.outputName());
            if (state instanceof List) {
                List<Object> stateList = (List<Object>) state;
                states.put(mapping.outputName(), stateList);
                maxStateSize = Math.max(maxStateSize, stateList.size());
            } else {
                states.put(mapping.outputName(), List.of(state));
                maxStateSize = Math.max(maxStateSize, 1);
            }
        }
        return maxStateSize;
    }

    private static Class<?> getPrimitiveType(Class<?> wrapperType) {
        if (wrapperType == Integer.class) return int.class;
        if (wrapperType == Long.class) return long.class;
        if (wrapperType == Double.class) return double.class;
        if (wrapperType == Float.class) return float.class;
        if (wrapperType == Boolean.class) return boolean.class;
        if (wrapperType == Byte.class) return byte.class;
        if (wrapperType == Short.class) return short.class;
        if (wrapperType == Character.class) return char.class;
        return null;
    }

    /**
     * Represents a mapping between an output name from the {@link AgenticScope} and a property name in a target bean.
     *
     * @param outputName   The name of the output in the {@link AgenticScope}.
     * @param propertyName The name of the property in the target bean.
     */
    public record Mapping(String outputName, String propertyName) {
        @SuppressWarnings("unused")
        public Mapping(String outputName) {
            this(outputName, outputName);
        }
    }
}
