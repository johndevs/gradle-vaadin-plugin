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
package fi.jasoft.plugin

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * Created by john on 11/26/14.
 */
class DependencyListenerTest extends PluginTestBase {

    @Test
    void testCreateConfiguration() {

        DependencyListener.createConfiguration(
                project,
                DependencyListener.Configuration.SERVER,
                ['com.example:example-test:3.2.1']
        )

        def conf = project.configurations.getByName(DependencyListener.Configuration.SERVER.caption)
        assertNotNull 'Configuration could not be found', conf
        assertEquals 'Dependency was not added to configuration', 1, conf.dependencies.size()

        DependencyListener.createConfiguration(
                project,
                DependencyListener.Configuration.CLIENT,
                ['com.example:example-test:3.2.1'],
                [project.configurations.runtime, project.configurations.compile]
        )

        conf = project.configurations.getByName(DependencyListener.Configuration.CLIENT.caption)
        assertNotNull 'Configuration could not be found', conf
        assertEquals 'Dependency was not added to configuration', 1, conf.dependencies.size()
        assertEquals 'Configuration did not extend other configurations', 2, conf.extendsFrom.size()

    }
}
