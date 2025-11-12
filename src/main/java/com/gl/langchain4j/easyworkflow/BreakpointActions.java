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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.gl.langchain4j.easyworkflow.EasyWorkflow.expandTemplate;

/**
 * Utility class providing common actions that can be executed at breakpoints within a workflow.
 * These actions are {@link BiConsumer} instances that operate on a {@link WorkflowDebugger.Breakpoint} and its context.
 */
public class BreakpointActions {
    /**
     * Creates a {@link BiConsumer} action for a breakpoint that logs a message using a prompt template. The template
     * can use workflow context variables with the `{{variable}}` notation.
     *
     * @param template The template string for the log message.
     * @return A {@link BiConsumer} that logs the formatted message when executed.
     */
    public static BiFunction<WorkflowDebugger.Breakpoint, Map<String, Object>, Object> log(String template) {
        return (b, ctx) -> {
            String text = ctx != null ? expandTemplate(template, ctx) : template;
            WorkflowDebugger.getLogger().info(text.replaceAll("\\n", "\\\\n"));
            return text;
        };
    }

    /**
     * Creates a {@link BiConsumer} action for a breakpoint that toggles the enabled state of other specified
     * breakpoints.
     *
     * @param enabled     {@code true} to enable the breakpoints, {@code false} to disable them.
     * @param breakpoints The breakpoints to toggle.
     * @return A {@link BiConsumer} that toggles the enabled state of the specified breakpoints when executed.
     */
    public static BiFunction<WorkflowDebugger.Breakpoint, Map<String, Object>, Object> toggleBreakpoints(boolean enabled, WorkflowDebugger.Breakpoint... breakpoints) {
        return (b, ctx) -> {
            Arrays.stream(breakpoints).forEach(toToggle -> toToggle.setEnabled(enabled));
            return null;
        };
    }

    /**
     * Creates a {@link BiConsumer} action for a breakpoint that generates an HTML file representing the workflow
     * execution.
     *
     * @param filePath The path to the file where the HTML should be written.
     * @return A {@link BiConsumer} that generates the HTML file when executed.
     */
    public static BiFunction<WorkflowDebugger.Breakpoint, Map<String, Object>, Object> toHtmlFile(String filePath) {
        return (b, ctx) -> {
            try {
                b.getWorkflowDebugger().toHtmlFile(filePath);
                return filePath;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}
