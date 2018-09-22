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
package com.devsoap.plugin.extensions

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Configuration options for addon projects
 *
 * @author John Ahlroos
 * @since 1.2
 */
class AddonExtension {

    static final String NAME = 'vaadinAddon'

    private final Property<String> author
    private final Property<String> license
    private final Property<String> title
    private final Property<List<String>> styles

    AddonExtension(Project project) {
        author = project.objects.property(String)
        license = project.objects.property(String)
        title = project.objects.property(String)
        styles = project.objects.property(List)

        author.set('')
        license.set('')
        title.set('')
        styles.set([])
    }

    /**
     * the author of the addon
     */
    String getAuthor() {
        author.get()
    }

    /**
     * the author of the addon
     */
    void setAuthor(String author) {
       this.author.set(author)
    }

    /**
     * The licence for the addon
     */
    String getLicense() {
        license.get()
    }

    /**
     * The licence for the addon
     */
    void setLicense(String license) {
        this.license.set(license)
    }

    /**
     * The title of the addon as it should appear in the directory
     */
    String getTitle() {
        title.get()
    }

    /**
     * The title of the addon as it should appear in the directory
     */
    Provider<String> getTitleProvider() {
        title
    }

    /**
     * The title of the addon as it should appear in the directory
     */
    void setTitle(String title) {
        this.title.set(title)
    }

    /**
     * Array of paths (eg. /VAADIN/addons/myaddon/myaddon.scss) to stylesheets packaged with the addon
     */
    String[] getStyles() {
        return styles.present ? styles.get().toArray(new String[styles.get().size()]) : null
    }

    /**
     * Array of paths (eg. /VAADIN/addons/myaddon/myaddon.scss) to stylesheets packaged with the addon
     */
    void setStyles(String... styles) {
        this.styles.set(Arrays.asList(styles))
    }
}
