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
package fi.jasoft.plugin

import org.junit.Test
import static junit.framework.Assert.assertEquals

/**
 * Tests the utility methods in TemplateUtil
 */
class TemplateUtilTest extends PluginTestBase {

    @Test
    void 'write template with substitutions'() {

        def substitutions = [:]
        substitutions['color'] = 'brown'
        substitutions['obsticle'] = 'dog'

        TemplateUtil.writeTemplate("MyTestTemplate.java", testDir, "quick-fox.txt", substitutions)

        File resultFile = new File(testDir.canonicalPath + "/quick-fox.txt")

        assertEquals 'The quick brown fox jumps over the lazy dog', resultFile.text
    }

    @Test
    void 'search for files in public resource folder'() {

        generateFilesInPublicFolder(testDir.canonicalPath + '/src/main/resources/com/example/client/public' as File)

        // Get files
        def files = TemplateUtil.getFilesFromPublicFolder(project)
        assertEquals 3, files.size()

        assertEquals 'This is a css file',  files.find { it.name == 'file.css'}.text
        assertEquals 'This is a java file',  files.find { it.name == 'file.java'}.text
        assertEquals 'This is a text file',  files.find { it.name == 'file.txt'}.text

        // Get files with postfix
        files = TemplateUtil.getFilesFromPublicFolder(project, 'css')
        assertEquals 1, files.size()
        assertEquals 'This is a css file', files.find { it.name == 'file.css'}.text
    }
    
    @Test
    void 'search for files in multiple source folders'() {
        project.apply plugin: 'java'
        project.sourceSets.main.java.srcDir "src/extra/java"
        generateFilesInPublicFolder(testDir.canonicalPath + '/src/main/resources/com/example/client/public' as File)
        generateFilesInPublicFolder(testDir.canonicalPath + '/src/extra/java/com/example/client/public' as File)
    	
        def files = TemplateUtil.getFilesFromPublicFolder(project)
        assertEquals 6, files.size()
    }

    @Test
    void 'search for files in main source set, public folder'() {

        def publicFolder = testDir.canonicalPath + '/src/main/java/com/example/client/public' as File

        generateFilesInPublicFolder(publicFolder)

        // Get files
        def files = TemplateUtil.getFilesFromPublicFolder(project)
        assertEquals 3, files.size()

        assertEquals 'This is a css file',  files.find { it.name == 'file.css'}.text
        assertEquals 'This is a java file',  files.find { it.name == 'file.java'}.text
        assertEquals 'This is a text file',  files.find { it.name == 'file.txt'}.text

        // Get files with postfix
        files = TemplateUtil.getFilesFromPublicFolder(project, 'css')
        assertEquals 1, files.size()
        assertEquals 'This is a css file', files.find { it.name == 'file.css'}.text
    }

    private void generateFilesInPublicFolder(File publicFolder){

        // Create public folder if it does not exist
        publicFolder.mkdirs()

        // Create files
        def txtFile = (publicFolder.canonicalPath + '/file.txt') as File
        txtFile << 'This is a text file'

        def cssFile = (publicFolder.canonicalPath + '/file.css') as File
        cssFile << 'This is a css file'

        def javaFile = (publicFolder.canonicalPath + '/file.java') as File
        javaFile << 'This is a java file'
    }
}