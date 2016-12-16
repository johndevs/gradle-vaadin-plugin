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
package fi.jasoft.plugin.unit

import fi.jasoft.plugin.Util
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests for Util utility methods
 */
class UtilTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder()

    @Before
    void setup() {
        testFolder.newFolder('public')
        testFolder.newFile('public' + File.separator + 'foo.css')

        testFolder.newFolder('public', 'baz')
        testFolder.newFile('public' + File.separator + 'baz' + File.separator + 'AAA.css')
    }

    @Test void 'Test relative file path conversion'() {
        def path = Util.getRelativePathForFile('public',
                new File(testFolder.root, 'public' + File.separator + 'foo.css'))
        Assert.assertEquals('foo.css', path)

        path = Util.getRelativePathForFile('public',
                new File(testFolder.root, 'public' + File.separator + 'baz' + File.separator + 'AAA.css'))
        Assert.assertEquals('baz' + File.separator + 'AAA.css', path)
    }

    @Test void 'Test replacing file extension'() {
        def file = new File(testFolder.root, 'public' + File.separator + 'foo.css')
        def path = Util.replaceExtension(file.canonicalPath, 'css', 'scss')
        Assert.assertTrue(path.endsWith('.scss'))
    }
}
