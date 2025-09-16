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
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * This class provides a sample for
 * <a href="https://docs.langchain4j.dev/tutorials/agents#pure-agentic-ai">Pure Agentic AI</a>
 * using EasyWorkflow DSL-style workflow initialization.
 */
public class TestSupervisedAgents {
    static final String GROQ_API_KEY = "groqApiKey";

    public static void main(String[] args) {
        OpenAiChatModel BASE_MODEL = new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.groq.com/openai/v1/") // replace it if you use another service
                .apiKey(Preferences.userRoot().get(GROQ_API_KEY, null)) // replace it with your API key
                .modelName("meta-llama/llama-4-scout-17b-16e-instruct") // or another model
                .build();

        BankTool bankTool = new BankTool();
        bankTool.createAccount("Mario", 1000.0);
        bankTool.createAccount("Georgios", 1000.0);

        HumanInTheLoop humanInTheLoop = HumanInTheLoopAgents.consoleAgent("confirmation",
                "An agent that asks the user to confirm transactions. YES - to confirm; any other value - to decline");

        SupervisorAgent supervisorAgent1 = EasyWorkflow.builder(SupervisorAgent.class)
                .chatModel(BASE_MODEL)
                .doAsGroup()
                    .agent(WithdrawAgent.class, builder -> builder.tools(bankTool))
                    .agent(CreditAgent.class, builder -> builder.tools(bankTool))
                    .agent(ExchangeAgent.class) // ExchangeTool provided via @AgentBuilderConfigurator annotation
                    .agent(humanInTheLoop)
                .end()
                .build();

        System.out.println(supervisorAgent1.makeTransaction("Transfer 100 EUR from Mario's account to Georgios' one"));
        System.out.println(bankTool.getBalance("Mario"));
        System.out.println(bankTool.getBalance("Georgios"));
    }

    public interface WithdrawAgent {

        @SystemMessage("""
                       You are a banker that can only withdraw US dollars (USD) from a user account,
                       """)
        @UserMessage("""
                     Withdraw {{amount}} USD from {{user}}'s account and return the new balance.
                     """)
        @Agent("A banker that withdraw USD from an account")
        String withdraw(@V("user") String user, @V("amount") Double amount);
    }

    public interface CreditAgent {
        @SystemMessage("""
                       You are a banker that can only credit US dollars (USD) to a user account,
                       """)
        @UserMessage("""
                     Credit {{amount}} USD to {{user}}'s account and return the new balance.
                     """)
        @Agent("A banker that credit USD to an account")
        String credit(@V("user") String user, @V("amount") Double amount);
    }

    public interface ExchangeAgent {
        @UserMessage("""
                     You are an operator exchanging money in different currencies.
                     Use the tool to exchange {{amount}} {{originalCurrency}} into {{targetCurrency}}
                     returning only the final amount provided by the tool as it is and nothing else.
                     IMPORTANT: All amounts are Double numerics (not Strings)
                     """)
        @Agent("A money exchanger that converts a given amount of money from the original to the target currency")
        Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);

        @AgentBuilderConfigurator
        static AgentBuilder<?> configure(AgentBuilder<?> builder) {
            builder.tools(new ExchangeTool());
            return builder;
        }
    }

    public interface SupervisorAgent {
        @Agent(outputName = "response")
        String makeTransaction(@V("request") String request);
    }

    static class BankTool {

        private final Map<String, Double> accounts = new HashMap<>();

        void createAccount(String user, Double initialBalance) {
            if (accounts.containsKey(user)) {
                throw new RuntimeException("Account for user " + user + " already exists");
            }
            accounts.put(user, initialBalance);
        }

        double getBalance(String user) {
            Double balance = accounts.get(user);
            if (balance == null) {
                throw new RuntimeException("No balance found for user " + user);
            }
            return balance;
        }

        @Tool("Credit the given user with the given amount (as a double) and return the new balance")
        Double credit(@P("user name") String user, @P("amount") Double amount) {
            Double balance = accounts.get(user);
            if (balance == null) {
                throw new RuntimeException("No balance found for user " + user);
            }
            System.out.println("crediting: " + amount);
            Double newBalance = balance + amount;
            accounts.put(user, newBalance);
            return newBalance;
        }

        @Tool("Withdraw the given amount (as a double) with the given user and return the new balance")
        Double withdraw(@P("user name") String user, @P("amount") Double amount) {
            Double balance = accounts.get(user);
            if (balance == null) {
                throw new RuntimeException("No balance found for user " + user);
            }
            System.out.println("withdrawing: " + amount);
            Double newBalance = balance - amount;
            accounts.put(user, newBalance);
            return newBalance;
        }
    }

    public static class ExchangeTool {
        public ExchangeTool() {
            System.out.println("ExchangeTool created");
        }

        @Tool("""
              Exchange the given amount of money from the original to the target currency.
              All values are Double numerics""")
        Double exchange(@P("originalCurrency") String originalCurrency, @P("amount") Double amount, @P("targetCurrency") String targetCurrency) {
            return amount * 1.15;
        }
    }
}
