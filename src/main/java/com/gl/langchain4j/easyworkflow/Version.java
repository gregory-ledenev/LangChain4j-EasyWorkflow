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

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Properties;

/**
 * Provides access to the application's version, build number, and build date.
 */
public class Version {
    private static final String VERSION_FILE = "version.properties";
    /**
     * Singleton instance of the Version class.
     */
    private static final Version VERSION = new Version();
    private String projectName;
    private String projectVersion;
    private int buildNumber;
    private Instant buildDate;

    /**
     * Constructs a new Version object. It attempts to load version information from a 'build.properties' file. If the
     * file is not found or an error occurs during loading, the version will be set to "Unknown".
     */
    private Version() {
        try (InputStream input = getClass().getResourceAsStream(VERSION_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            projectName = prop.getProperty("project.name");
            projectVersion = prop.getProperty("project.version");
            buildNumber = Integer.parseInt(prop.getProperty("build.number"));
            buildDate = Instant.parse(prop.getProperty("build.date"));
        } catch (IOException ex) {
            ex.printStackTrace();
            this.projectVersion = "Unknown";
        }
    }

    /**
     * Returns the singleton instance of the Version class.
     *
     * @return The singleton instance of Version.
     */
    public static Version getInstance() {
        return VERSION;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the version string.
     *
     * @return The version string.
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * Returns the build date of the application.
     *
     * @return The build date as an {@link Instant}.
     */
    public Instant getBuildDate() {
        return buildDate;
    }

    /**
     * Returns the build number of the application.
     *
     * @return The build number.
     */
    public int getBuildNumber() {
        return buildNumber;
    }

    /**
     * Returns a string representation of the Version object.
     *
     * @return A string containing the version, build number, and build date.
     */
    @Override
    public String toString() {
        return "Version{" +
                "version='" + projectVersion + '\'' +
                ", buildNumber=" + buildNumber +
                ", buildDate=" + buildDate +
                '}';
    }
}
