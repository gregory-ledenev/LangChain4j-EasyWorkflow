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

package com.gl.appframework;

import org.slf4j.Logger;

import javax.swing.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A factory class for creating proxied {@link Logger} instances that can be intercepted
 * by a {@link LoggerAspect}.
 */
public abstract class LoggerFactory {
    private static LoggerAspect loggerAspect = null;

    /**
     * Retrieves the currently set {@link LoggerAspect}.
     *
     * @return The {@link LoggerAspect} instance, or {@code null} if none is set.
     */
    public static LoggerAspect getLoggerAspect() {
        return loggerAspect;
    }

    /**
     * Sets the {@link LoggerAspect} to be used for intercepting logger calls.
     *
     * @param aLoggerAspect The {@link LoggerAspect} instance to set.
     */
    public static void setLoggerAspect(LoggerAspect aLoggerAspect) {
        loggerAspect = aLoggerAspect;
    }

    /**
     * Retrieves a proxied {@link Logger} instance for the given class. If a {@link LoggerAspect} is set, it will
     * intercept calls to the logger methods.
     *
     * @param clazz The class for which to get the logger.
     * @return A proxied {@link Logger} instance.
     */
    public static Logger getLogger(Class<?> clazz) {
        Logger originalLogger = org.slf4j.LoggerFactory.getLogger(clazz);
        return (Logger) Proxy.newProxyInstance(
                Logger.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                (proxy, method, args) -> {
                    LoggerAspect aspect = getLoggerAspect();
                    return aspect != null ?
                            aspect.invoke(originalLogger, method, args) :
                            method.invoke(originalLogger, args);
                }
        );
    }

    /**
     * An interface for defining an aspect that can intercept logger calls.
     */
    public interface LoggerAspect {
        /**
         * Intercepts a logger method invocation.
         *
         * @param logger The original {@link Logger} instance.
         * @param method The method being invoked on the logger.
         * @param args   The arguments passed to the logger method.
         * @return The result of the invocation.
         * @throws Throwable If an error occurs during invocation.
         */
        Object invoke(Logger logger, Method method, Object[] args) throws Throwable;
    }

    /**
     * Creates and returns a {@link LoggerAspect} that intercepts log messages.
     * Specifically, it captures error messages and displays them as notifications.
     *
     * @return A new {@link LoggerAspect} instance.
     */
    public static LoggerFactory.LoggerAspect createNotificationLoggerAspect() {
        return (logger, method, args) -> {
            if (method.getName().equals("error")) {
                String text = args[0] != null ? args[0].toString() : "";

                if (args.length > 1 && args[1] instanceof Throwable ex) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    ex.printStackTrace(pw);
                    text += "\n" + sw;
                }

                String finalText = text;
                SwingUtilities.invokeLater(() -> NotificationCenter.getInstance().postNotification(
                        new NotificationCenter.Notification(NotificationCenter.NotificationType.ERROR, "Error", finalText, null)));
            }
            return method.invoke(logger, args);
        };
    }
}
