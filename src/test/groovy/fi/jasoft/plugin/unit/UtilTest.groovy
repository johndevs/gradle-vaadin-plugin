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
package fi.jasoft.plugin.unit

import fi.jasoft.plugin.Util
import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.WarPluginConvention
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths

/**
 * Tests for Util utility methods
 */
class UtilTest extends PluginTestBase {

    @Before
    void setup() {
        testDir.newFolder('public')
        testDir.newFile('public' + File.separator + 'foo.css')

        testDir.newFolder('public', 'baz')
        testDir.newFile('public' + File.separator + 'baz' + File.separator + 'AAA.css')
    }

    @Test void 'Test relative file path conversion'() {
        def path = Util.getRelativePathForFile('public',
                new File(testDir.root, 'public' + File.separator + 'foo.css'))
        Assert.assertEquals('foo.css', path)

        path = Util.getRelativePathForFile('public',
                new File(testDir.root, 'public' + File.separator + 'baz' + File.separator + 'AAA.css'))
        Assert.assertEquals('baz' + File.separator + 'AAA.css', path)
    }

    @Test void 'Test replacing file extension'() {
        def file = new File(testDir.root, 'public' + File.separator + 'foo.css')
        def path = Util.replaceExtension(file.canonicalPath, 'css', 'scss')
        Assert.assertTrue(path.endsWith('.scss'))
    }

    @Test void 'Get default widgetset directory'() {
        File widgetsetDir = Util.getWidgetsetDirectory(project)
        File defaultDir = project.file('src/main/webapp/VAADIN/widgetsets')
        Assert.assertEquals('Widgetset did not match', defaultDir.canonicalPath, widgetsetDir.canonicalPath)
    }

    @Test void 'Get custom widgetset directory'() {
        project.vaadinCompile.outputDirectory = 'build'
        File widgetsetDir = Util.getWidgetsetDirectory(project)
        File customDir = project.file('build/VAADIN/widgetsets')
        Assert.assertEquals('Widgetset did not match', customDir.canonicalPath, widgetsetDir.canonicalPath)
    }

    @Test void 'Get custom widgetset directory outside of project'() {
        project.vaadinCompile.outputDirectory = testDir.root.parentFile.canonicalPath
        File widgetsetDir = Util.getWidgetsetDirectory(project)
        File customDir = new File(testDir.root.parentFile, 'VAADIN'+File.separator+'widgetsets')
        Assert.assertEquals('Widgetset did not match', customDir.canonicalPath, widgetsetDir.canonicalPath)
    }

    @Test void 'Get default widgetset cache directory'() {
        File widgetsetDir = Util.getWidgetsetCacheDirectory(project)
        File defaultDir = project.file('src/main/webapp/VAADIN/gwt-unitCache')
        Assert.assertEquals('Widgetset cache dir did not match', defaultDir.canonicalPath, widgetsetDir.canonicalPath)
    }

    @Test void 'Get custom widgetset cache directory'() {
        project.vaadinCompile.outputDirectory = 'build'
        File widgetsetDir = Util.getWidgetsetCacheDirectory(project)
        File customDir = project.file('build/VAADIN/gwt-unitCache')
        Assert.assertEquals('Widgetset cache dir  did not match', customDir.canonicalPath, widgetsetDir.canonicalPath)
    }

    @Test void 'Get custom widgetset cache directory outside of project'() {
        project.vaadinCompile.outputDirectory = testDir.root.parentFile.canonicalPath
        File widgetsetDir = Util.getWidgetsetCacheDirectory(project)
        File customDir = new File(testDir.root.parentFile, 'VAADIN'+File.separator+'gwt-unitCache')
        Assert.assertEquals('Widgetset cache dir  did not match', customDir.canonicalPath, widgetsetDir.canonicalPath)
    }
}
