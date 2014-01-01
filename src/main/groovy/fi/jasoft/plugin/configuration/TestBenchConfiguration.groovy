/*
* Copyright 2013 John Ahlroos
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
    TestBenchHubConfiguration hub = new TestBenchHubConfiguration()

    /**
     * The Testbench node configuration
     */
    TestBenchNodeConfiguration node = new TestBenchNodeConfiguration()

    /**
     * @see TestBenchConfiguration#enabled
     *
     * @param enabled
     */
    void enabled(boolean enabled) {
        this.enabled = enabled
    }

    /**
     * @see TestBenchConfiguration#version
     *
     * @param version
     */
    void version(String version) {
        this.version = version
    }

    /**
     * @see TestBenchConfiguration#runApplication
     *
     * @param run
     */
    void runApplication(boolean run) {
        this.runApplication = run
    }

    /**
     * @see TestBenchConfiguration#hub
     *
     * @param closure
     * @return
     */
    TestBenchHubConfiguration hub(closure) {
        closure.delegate = hub
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /**
     * @see TestBenchConfiguration#node
     *
     * @param closure
     * @return
     */
    TestBenchNodeConfiguration node(closure) {
        closure.delegate = node
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
}
