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
 * Configuration options for addon projects
 */
class AddonConfiguration {

    /**
     * the author of the addon
     */
    String author = ''

    /**
     * The licence for the addon
     */
    String license = ''

    /**
     * The title of the addon as it should appear in the directory
     */
    String title = ''

    /**
     * @see AddonConfiguration#author
     *
     * @param author
     */
    void author(String author) {
        this.author = author
    }

    /**
     * @see AddonConfiguration#license
     *
     * @param license
     */
    void license(String license) {
        this.license = license
    }

    /**
     * @see AddonConfiguration#title
     *
     * @param title
     */
    void title(String title) {
        this.title = title
    }
}
