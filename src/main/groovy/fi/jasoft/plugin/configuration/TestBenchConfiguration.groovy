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

/**
 * Configuration options for Testbench
 */
@PluginConfiguration
class TestBenchConfiguration {

    /**
     * Should Testbench be enabled when running tests
     */
    boolean enabled = false

    /**
     * What version of testbench should be used
     */
    String version = "3.+"

    /**
     * Should the application be run before tests are run
     */
    boolean runApplication = true

    /**
     * The Testbench Hub configuration
     */
    final TestBenchHubConfiguration hub = new TestBenchHubConfiguration()

    /**
     * The Testbench node configuration
     */
    final TestBenchNodeConfiguration node = new TestBenchNodeConfiguration()
}
