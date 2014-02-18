/*
* Copyright 2014 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.jasoft.plugin.configuration

/**
 * Configuration options for the GWT compiler
 */
class GWTConfiguration {

    /**
     * Compilation style
     */
    String style = "OBF"

    /**
     * Should the compilation result be optimized
     */
    int optimize = 0

    /**
     * Should logging be enabled
     */
    boolean logging = true

    /**
     * The log level. Possible levels NONE,DEBUG,TRACE,INFO
     */
    String logLevel = "INFO"

    /**
     * Amount of local workers used when compiling. By default the amount of processors.
     */
    int localWorkers = Runtime.getRuntime().availableProcessors()

    /**
     * Should draft compile be used
     */
    boolean draftCompile = false

    /**
     * Should strict compiling be used
     */
    boolean strict = false

    /**
     * What user agents (browsers should be used. By defining null all user agents are used.
     */
    String userAgent = null

    /**
     * Extra jvm arguments passed the JVM running the compiler
     */
    String[] jvmArgs = null

    /**
     * The version of GWT used when compiling. Only used with Vaadin 6.
     */
    String version = "2.3.0"

    /**
     * Extra arguments passed to the compiler
     */
    String extraArgs

    /**
     * Source paths where the compiler will look for source files
     */
    String[] sourcePaths = ['client', 'shared']

    /**
     * Should the compiler permutations be collapsed to save time
     */
    boolean collapsePermutations = false

    /**
     * Extra module inherits
     */
    String[] extraInherits

    /**
     * @see GWTConfiguration#style
     *
     * @param style
     *      The compilation style
     */
    void style(String style) {
        this.style = style
    }

    /**
     * @see GWTConfiguration#optimize
     *
     * @param optimize
     *      The level of optimizations. Should be betweeen 0-9
     */
    void optimize(int optimize) {
        this.optimize = optimize
    }

    /**
     * @see GWTConfiguration#logLevel
     *
     * @param logLevel
     *      The logging level. By default INFO.
     */
    void logLevel(String logLevel) {
        this.logLevel = logLevel
    }

    /**
     * @see GWTConfiguration#localWorkers
     *
     * @param localWorkers
     *      The amount of workers. By default the amount of processors
     */
    void localWorkers(int localWorkers) {
        this.localWorkers = localWorkers
    }

    /**
     * @see GWTConfiguration#draftCompile
     *
     * @param draftCompile
     *      <code>true</code> if draft compiler should be used.
     */
    void draftCompile(boolean draftCompile) {
        this.draftCompile = draftCompile
    }

    /**
     * @see GWTConfiguration#strict
     *
     * @param strict
     */
    void strict(boolean strict) {
        this.strict = strict
    }

    /**
     * @see GWTConfiguration#userAgent
     *
     * @param userAgent
     */
    void userAgent(String userAgent) {
        this.userAgent = userAgent
    }

    /**
     * @see GWTConfiguration#jvmArgs
     *
     * @param jvmArgs
     */
    void jvmArgs(String[] jvmArgs) {
        this.jvmArgs = jvmArgs
    }

    /**
     * @see GWTConfiguration#version
     *
     * @param version
     */
    @Deprecated
    void version(String version) {
        this.version = version
    }

    /**
     * @see GWTConfiguration#extraArgs
     *
     * @param extraArgs
     */
    void extraArgs(String extraArgs) {
        this.extraArgs = extraArgs
    }

    /**
     * @see GWTConfiguration#extraInherits
     *
     * @param inherits
     */
    void extraInherits(String[] inherits) {
        this.extraInherits = inherits
    }

    /**
     * @see GWTConfiguration#sourcePaths
     *
     * @param sourcePaths
     */
    void sourcePaths(String[] sourcePaths) {
        this.sourcePaths = sourcePaths
    }

    /**
     * @see GWTConfiguration#collapsePermutations
     *
     * @param collapse
     */
    void collapsePermutations(boolean collapse) {
        this.collapsePermutations = collapse
    }

    /**
     * @see GWTConfiguration#logging
     *
     * @param logging
     */
    void logging(boolean logging) {
        this.logging = logging
    }
}
