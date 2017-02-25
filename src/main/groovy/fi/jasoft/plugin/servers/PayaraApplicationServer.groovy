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
package fi.jasoft.plugin.servers

import fi.jasoft.plugin.configuration.ApplicationServerConfiguration
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Runs the project on a Payara server
 */
class PayaraApplicationServer extends ApplicationServer {

    public static final String NAME = 'payara'

    PayaraApplicationServer(Project project, List browserParameters, ApplicationServerConfiguration configuration) {
        super(project, browserParameters, configuration)
    }

    @Override
    String getServerRunner() {
        'fi.jasoft.plugin.PayaraServerRunner'
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
    def configureProcess(List<String> parameters) {
        super.configureProcess(parameters)

        // Override internal Payara classes. See https://payara.gitbooks.io/payara-server/content/documentation/
        // extended-documentation/classloading.html#41-globally-override-payara-included-libraries
        parameters.add("-Dfish.payara.classloading.delegate=false")
    }

    @Override
    def defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies) {
        Properties properties = new Properties()
        properties.load(PayaraApplicationServer.class.getResourceAsStream('/gradle.properties') as InputStream)
        def payaraWebProfile = projectDependencies.create(
                "fish.payara.extras:payara-embedded-web:${properties.getProperty('payara.version')}")
        dependencies.add(payaraWebProfile)
    }
}
