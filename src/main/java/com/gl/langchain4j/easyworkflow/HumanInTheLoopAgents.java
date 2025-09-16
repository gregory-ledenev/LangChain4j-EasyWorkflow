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

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;

import java.util.Scanner;

/**
 * Utility class for creating various types of Human-in-the-Loop agents. These agents allow for human intervention and
 * decision-making within automated workflows.
 */
public class HumanInTheLoopAgents {
    /**
     * Creates a {@link HumanInTheLoop} agent that interacts with the user via the console.
     *
     * @param outputName  The name to assign to the output of this human-in-the-loop agent.
     * @param description A description of the human's role or the prompt to display to the human.
     * @return A new {@link HumanInTheLoop} instance configured for console interaction.
     */
    public static HumanInTheLoop consoleAgent(String outputName, String description) {
        return AgenticServices
                .humanInTheLoopBuilder()
                .description(description)
                .outputName(outputName)
                .requestWriter(request -> {
                    System.out.println(request);
                    System.out.print("> ");
                })
                .responseReader(() -> {
                    try (Scanner scanner = new Scanner(System.in)) {
                        String confirmation = scanner.nextLine();
                        System.out.println("Proceeding...");
                        return confirmation;
                    }
                })
                .build();
    }
}
