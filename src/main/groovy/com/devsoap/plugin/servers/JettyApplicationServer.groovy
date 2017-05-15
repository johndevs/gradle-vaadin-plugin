/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.servers

import com.devsoap.plugin.Util
import com.devsoap.plugin.configuration.ApplicationServerConfiguration
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Runs the project on a Jetty server
 */
class JettyApplicationServer extends ApplicationServer {

    public static final String NAME = 'jetty'
    public static final String JETTY_VERSION_PROPERTY = 'jetty.version'

    JettyApplicationServer(Project project, Map browserParameters, ApplicationServerConfiguration configuration) {
        super(project, browserParameters, configuration)
    }

    @Override
    String getServerRunner() {
        'com.devsoap.plugin.JettyServerRunner'
    }

    @Override
    String getServerName() {
        NAME
    }

    @Override
    String getSuccessfullyStartedLogToken() {
        'INFO: Jetty started'
    }

    @Override
    def defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies) {
        def jettyAll =  projectDependencies.create(
                "org.eclipse.jetty.aggregate:jetty-all:${Util.pluginProperties.getProperty(JETTY_VERSION_PROPERTY)}")
        dependencies.add(jettyAll)

        def jettyAnnotations = projectDependencies.create("org.eclipse.jetty:jetty-annotations:" +
                "${Util.pluginProperties.getProperty(JETTY_VERSION_PROPERTY)}")
        dependencies.add(jettyAnnotations)

        def jettyPlus = projectDependencies.create(
                "org.eclipse.jetty:jetty-plus:${Util.pluginProperties.getProperty(JETTY_VERSION_PROPERTY)}")
        dependencies.add(jettyPlus)

        def jettyDeploy = projectDependencies.create(
                "org.eclipse.jetty:jetty-deploy:${Util.pluginProperties.getProperty(JETTY_VERSION_PROPERTY)}")
        dependencies.add(jettyDeploy)

        def asm = projectDependencies.create('org.ow2.asm:asm:5.0.3')
        dependencies.add(asm)

        def asmCommons = projectDependencies.create('org.ow2.asm:asm-commons:5.0.3')
        dependencies.add(asmCommons)

        def jspApi = projectDependencies.create('javax.servlet.jsp:jsp-api:2.2')
        dependencies.add(jspApi)
    }
}
