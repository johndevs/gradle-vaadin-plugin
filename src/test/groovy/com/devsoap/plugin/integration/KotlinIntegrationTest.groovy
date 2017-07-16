package com.devsoap.plugin.integration

import com.devsoap.plugin.extensions.VaadinPluginExtension

/**
 * Base class for testing building projects with Kotlin and Kotlin DSL
 */
class KotlinIntegrationTest extends IntegrationTest {

    final String kotlinVersion

    KotlinIntegrationTest(String kotlinVersion) {
        this.kotlinVersion = kotlinVersion
    }

    @Override
    void setup() {
        startTime = System.currentTimeMillis()
        println "Running test in $projectDir.root"
        buildFile = makeBuildFile(projectDir.root)
        settingsFile = projectDir.newFile("settings.gradle")
    }

    @Override
    protected File makeBuildFile(File projectDir, boolean applyPluginToFile=true) {
        File buildFile = new File(projectDir, 'build.gradle.kts')
        buildFile.createNewFile()

        // Imports
        applyImports(buildFile)

        // Apply plugin to project
        buildFile << "buildscript {\n"
        buildFile << "repositories {\n"
        applyBuildScriptRepositories(buildFile)
        buildFile << "}\n"
        buildFile << "dependencies {\n"
        applyBuildScriptClasspathDependencies(buildFile)
        buildFile << "}\n"
        buildFile << "}\n"

        // Apply custom plugins{} block
        applyThirdPartyPlugins(buildFile)

        if ( applyPluginToFile ) {
            applyRepositories(buildFile)
            applyPlugin(buildFile)
            buildFile << """
                configure<VaadinPluginExtension> {
                    logToConsole = true
                }
            """.stripIndent()
        }

        buildFile
    }

    @Override
    protected void applyRepositories(File buildFile) {
        String escapedDir = getPluginDir()
        buildFile << """
            repositories {
                flatDir {
                    dirs(file("$escapedDir"))
                } 
            }
        """.stripIndent()
    }

    @Override
    protected void applyPlugin(File buildFile) {
       buildFile << """
        apply {
            plugin("com.devsoap.plugin.vaadin")
        }
        """.stripIndent()
    }

    @Override
    protected void applyBuildScriptRepositories(File buildFile) {
        String escapedDir = getPluginDir()
        buildFile << "mavenLocal()\n"
        buildFile << "mavenCentral()\n"
        buildFile << """
            flatDir {
                dirs(file("$escapedDir"))
            }
         """.stripIndent()
    }

    @Override
    protected void applyThirdPartyPlugins(File buildFile) {
        if(!buildFile || !buildFile.exists()){
            throw new IllegalArgumentException("$buildFile does not exist or is null")
        }

        buildFile << """
           plugins {
                id("org.jetbrains.kotlin.jvm").version("$kotlinVersion")
           }

           dependencies {
                compile("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
           }
        """.stripIndent()
    }

    @Override
    protected void applyBuildScriptClasspathDependencies(File buildFile) {
        def projectVersion = System.getProperty('integrationTestProjectVersion')
        buildFile << """
             classpath("org.codehaus.groovy.modules.http-builder:http-builder:0.7.1")
             classpath("com.devsoap.plugin:gradle-vaadin-plugin:$projectVersion")
             classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        """.stripIndent()
    }

    protected void applyImports(File buildFile) {
        buildFile << """
            import $VaadinPluginExtension.canonicalName
        """.stripIndent()
    }
}
