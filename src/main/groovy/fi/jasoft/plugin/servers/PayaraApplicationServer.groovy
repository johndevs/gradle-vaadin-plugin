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
package fi.jasoft.plugin.servers

import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Runs the project on a Payara server
 */
class PayaraApplicationServer extends ApplicationServer {

    public static final String NAME = 'payara'

    def payaraVersion = '4.1.153'

    PayaraApplicationServer(Object project, Object browserParameters) {
        super(project, browserParameters)
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
    def defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies) {
        def payaraWebProfile = projectDependencies.create("fish.payara.extras:payara-embedded-web:$payaraVersion")
        dependencies.add(payaraWebProfile)
    }
}
