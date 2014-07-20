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

import groovy.io.FileType
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.WarPluginConvention

class Util {


    public static String readLine(String format) {
        readLine(format, null)
    }

    public static String readLine(String format, Object... args) {
        try {
            if (System.console() != null) {
                return System.console().readLine(format, args);
            }

            System.out.print(String.format(format, args));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();

        } catch (IOException ioe) {
            // Ignore
        }
        return null;
    }

    public static FileCollection getClassPath(Project project) {
        FileCollection classpath =
            project.configurations.providedCompile +
                    project.configurations.compile +
                    project.sourceSets.main.runtimeClasspath +
                    project.sourceSets.main.compileClasspath

        Util.getMainSourceSet(project).srcDirs.each {
            classpath += [project.files(it)]
        }
        project.sourceSets.main.resources.srcDirs.each {
            classpath += [project.files(it)]
        }
        return classpath
    }

    /**
     * Returns the main source set where project source files are stored
     *
     * @param project
     *      The project to get the source set from
     *
     * @return
     *      The source set
     */
    static SourceDirectorySet getMainSourceSet(Project project) {
        if(project.vaadin.mainSourceSet){
            project.vaadin.mainSourceSet
        } else {
            // fallback to java source set
            project.sourceSets.main.java
        }
    }

    public static SourceDirectorySet getTestSourceSet(Project project) {
        return project.sourceSets.test.java
    }

    public static boolean isPushSupportedAndEnabled(Project project) {
        if (!project.vaadin.push) {
            return false
        }

        String version = project.vaadin.version
        if (version == '+') {
            return true
        }
        if (version.startsWith('7') && !version.startsWith('7.0')) {
            return true
        }
        return false
    }

    public static boolean isAddonStylesSupported(Project project) {
        return !project.vaadin.version.startsWith('7.0')
    }

    public static void openBrowser(Project project, String url, int delay = 3000) {
        if (project.vaadin.plugin.openInBrowser && java.awt.Desktop.isDesktopSupported()) {
            Thread.startDaemon {
                sleep delay
                java.awt.Desktop.desktop.browse url.toURI()
            }
        }
    }

    public static boolean isIE10UserAgentSupported(Project project) {
        String version = project.vaadin.version
        if (version == '+') {
            return true
        }
        if (version.startsWith('7') && !version.startsWith('7.0')) {
            return true
        }
    }

    public static boolean isServlet3Project(Project project) {
        String version = project.vaadin.version
        if (version == '+') {
            return true
        }
        if (version.startsWith('7') && !version.startsWith('7.0')) {
            return true
        }
    }

    public static boolean isRootProject(Project project) {

        // Check if project is the root project
        if (project.hasProperty('vaadin') && project.equals(project.getRootProject())) {
            return true
        }

        // If not traverse upwards and see if there are any other vaadin projects in the hierarchy
        while (!project.equals(project.getRootProject())) {
            project = project.getRootProject()
            if (project.hasProperty('vaadin')) {
                return false
            }
        }

        // no other vaadin projects found upwards, this is the root project
        return true
    }

    public static boolean isSassCompilerSupported(Project project) {

        // Sass compiler is supported 7.2+
        String version = project.vaadin.version
        if(version.startsWith("6") || version.startsWith("7.0") || version.startsWith("7.1") ){
            return false
        }
        return true
    }

    public static List findAddonSassStylesInProject(Project project) {
        File resourceDir = project.sourceSets.main.resources.srcDirs.iterator().next()
        File addonsDir = project.file(resourceDir.canonicalPath+'/VAADIN/addons')

        def paths = []

        if(addonsDir.exists()){
            addonsDir.traverse(type: FileType.DIRECTORIES) {
                def themeName = it.getName()
                def fileNameRegExp = ~/$themeName\.s?css/
                it.traverse(type: FileType.FILES, nameFilter : fileNameRegExp) {
                    paths += ["/VAADIN/addons/$themeName/"+it.getName()]
                }
            }
        }

        return paths
    }
}