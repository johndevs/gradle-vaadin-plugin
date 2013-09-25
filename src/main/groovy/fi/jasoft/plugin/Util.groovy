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
package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet;

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
            classpath += project.files(it)
        }
        project.sourceSets.main.resources.srcDirs.each {
            classpath += project.files(it)
        }
        return classpath
    }

    public static SourceDirectorySet getMainSourceSet(Project project) {
        def sourceSet = project.vaadin.mainSourceSet
        if (sourceSet == null) {
            sourceSet = project.sourceSets.main.java
        }
        return sourceSet
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
        return !project.vaadin.version.startsWith('6') && !project.vaadin.version.startsWith('7.0')
    }

    public static void openBrowser(Project project, String url, int delay = 3000) {
        if (project.vaadin.plugin.openInBrowser && java.awt.Desktop.isDesktopSupported()) {
            Thread.startDaemon {
                sleep delay
                java.awt.Desktop.desktop.browse url.toURI()
            }
        }
    }

    public static boolean isIE10UserAgentSupported(Project project){
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



}