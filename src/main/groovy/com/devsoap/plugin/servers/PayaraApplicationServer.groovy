/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.servers

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.Util
import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.CompressCssTask
import com.devsoap.plugin.tasks.RunTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.util.GFileUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths


/**
 * Runs the project on a Payara server
 *
 * @author John Ahlroos
 * @since 1.1
 */
class PayaraApplicationServer extends ApplicationServer {

    public static final String NAME = 'payara'

    PayaraApplicationServer(Project project, Map browserParameters) {
        super(project, browserParameters)
    }

    @Override
    String getServerRunner() {
        'com.devsoap.plugin.PayaraServerRunner'
    }

    @Override
    String getServerName() {
        NAME
    }

    @Override
    String getSuccessfullyStartedLogToken() {
        'was successfully deployed'
    }

    @Override
    void configureProcess(List<String> parameters) {
        super.configureProcess(parameters)

        // Override internal Payara classes. See https://payara.gitbooks.io/payara-server/content/documentation/
        // extended-documentation/classloading.html#41-globally-override-payara-included-libraries
        parameters.add("-Dfish.payara.classloading.delegate=false")
    }

    @Override
    protected boolean executeServer(List appServerProcess) {

        // Use most-up-to-date resources
        updateExplodedWar()

        project.logger.debug("Running server with the command: $appServerProcess")
        process = appServerProcess.execute([], project.buildDir)

        if ( !process.alive ) {
            // Something is avery, warn user and return
            throw new GradleException("Server failed to start. Exited with exit code ${process.exitValue()}")
        }

        /*
         * This is needed to keep the payara exploded war directory and the project directories in-sync
         */
        def self = this
        GradleVaadinPlugin.THREAD_POOL.submit {
            watchClassDirectoryForChanges(self) {
                updateExplodedWar()
            }
        }

        /**
         * Ensure theme is compiled and compressed before copying
         */
        RunTask runTask = project.tasks.getByName(RunTask.NAME)
        if(runTask.themeAutoRecompile) {
            GradleVaadinPlugin.THREAD_POOL.submit {
                watchThemeDirectoryForChanges(self) {

                    // Recompile theme
                    CompileThemeTask.compile(project, true)

                    // Recompress theme
                    CompileThemeTask compileThemeTask = project.tasks.getByName(CompileThemeTask.NAME)
                    if(compileThemeTask.compress){
                        CompressCssTask.compress(project, true)
                    }

                    updateExplodedWar()
                }
            }
        }
        true
    }

    @Override
    void defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies) {
        Dependency payaraWebProfile = projectDependencies.create(
                "fish.payara.extras:payara-micro:${Util.pluginProperties.getProperty('payara.version')}")
        dependencies.add(payaraWebProfile)
    }

    /**
     * Copies the resource directories into the correct folder structure for Payara to be able to serve the web
     * application directly from a directory without needing a ScatteredArchive. This is needed so Spring Loaded
     * can work properly.
     */
    void updateExplodedWar() {
        File buildDir = new File(project.buildDir, serverName)
        File warDir = new File(buildDir, 'war')
        if(warDir.exists()){
            warDir.delete()
        }
        warDir.mkdirs()

        if(webAppDir.exists()){
            GFileUtils.copyDirectory(webAppDir, warDir)
        }

        File webInf = new File(warDir, 'WEB-INF')
        webInf.mkdirs()

        File classes = new File(webInf, 'classes')
        classes.mkdirs()
        classesDirs.each {
            GFileUtils.copyDirectory(it, classes)
        }

        if(resourcesDir.exists()){
            GFileUtils.copyDirectory(resourcesDir, classes)
        }

        File lib = new File(webInf, 'lib')
        lib.mkdirs()

        String[] dependencies = new String(Files.readAllBytes(Paths.get("$buildDir/classpath.txt")),
                StandardCharsets.UTF_8).split(";")
        dependencies.each {
            File jar = new File(it)
            if(jar.exists()){
                GFileUtils.copyFile(jar, new File(lib, jar.name))
            }
        }
    }
}
