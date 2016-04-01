/*
* Copyright 2016 John Ahlroos
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

import fi.jasoft.plugin.MessageLogger
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * General configuration options for the plugin itself
 */
@PluginConfiguration
class VaadinPluginConfiguration {

    /**
     * Should all logs output by the task be redirected to the console (if false output is redirected to file)
     */
    boolean logToConsole = false

    /**
     * The directory where Eclipse will output its compiled classes.
     */
    String eclipseOutputDir = null

    /**
     * Should a classpath Jar be used to shorten the classpath.
     */
    boolean useClassPathJar = Os.isFamily(Os.FAMILY_WINDOWS)

    @Deprecated
    transient Project project

    @Deprecated
    VaadinPluginConfiguration(Project project){
        this.project = project
    }

    /**
     * Should the application be opened in a browser when it has been launched
     */
    @Deprecated
    void openInBrowser(boolean open){
        project.vaadinRun.openInBrowser = open
        isOpenInBrowser()
    }
    @Deprecated
    void setOpenInBrowser(boolean open){
        project.vaadinRun.openInBrowser = open
        isOpenInBrowser()
    }
    @Deprecated
    boolean isOpenInBrowser(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.plugin.openInBrowser',
                'This property has been replaced by vaadinRun.openInBrowser.')
        project.vaadinRun.openInBrowser
    }

    /**
     * Should theme be recompiled when SCSS file is changes.
     */
    @Deprecated
    void themeAutoRecompile(boolean recompile){
        project.vaadinRun.themeAutoRecompile = recompile
        isThemeAutoRecompile()
    }
    @Deprecated
    void setThemeAutoRecompile(boolean recompile){
        project.vaadinRun.themeAutoRecompile = recompile
        isThemeAutoRecompile()
    }
    @Deprecated
    boolean isThemeAutoRecompile(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.plugin.themeAutoRecompile',
                'This property has been replaced by vaadinRun.themeAutoRecompile.')
        project.vaadinRun.themeAutoRecompile
    }

    /**
     * Should jetty restart when a class is changed in the build directory.
     *
     * @deprecated
     */
    @Deprecated
    void jettyAutoRefresh(boolean refresh){
        project.vaadinRun.serverRestart = refresh
        isJettyAutoRefresh()
    }
    @Deprecated
    void setJettyAutoRefresh(boolean refresh){
        project.vaadinRun.serverRestart = refresh
        isJettyAutoRefresh()
    }
    @Deprecated
    boolean isJettyAutoRefresh(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.plugin.jettyAutoRefresh',
                'This property has been replaced by vaadinRun.serverRestart.')
        project.vaadinRun.serverRestart
    }

    /**
     * Custom directory where themes can be found
     *
     * @deprecated
     */
    @Deprecated
    void themesDirectory(String directory){
        project.vaadinThemeCompile.themesDirectory = directory
        getThemesDirectory()
    }
    @Deprecated
    void setThemesDirectory(String directory){
        project.vaadinThemeCompile.themesDirectory = directory
        getThemesDirectory()
    }
    @Deprecated
    String getThemesDirectory(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.plugin.themesDirectory',
                'This property has been replaced by vaadinThemeCompile.themesDirectory.')
        project.vaadinThemeCompile.themesDirectory
    }

    /**
     * Theme compiler to use
     * <p>
     *     Available options are
     *     <ul>
     *         <li>vaadin - Vaadin's SASS Compiler</li>
     *         <li>compass - Compass's SASS Compiler</li>
     *     </ul>
     */
    @Deprecated
    void themeCompiler(String directory){
        project.vaadinThemeCompile.themeCompiler = directory
        getThemeCompiler()
    }
    @Deprecated
    void setThemeCompiler(String directory){
        project.vaadinThemeCompile.themeCompiler = directory
        getThemeCompiler()
    }
    @Deprecated
    String getThemeCompiler(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.plugin.themeCompiler',
                'This property has been replaced by vaadinThemeCompile.compiler.')
        project.vaadinThemeCompile.themeCompiler
    }
}
