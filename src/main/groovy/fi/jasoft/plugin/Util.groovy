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
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.WarPluginConvention

import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes

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

    public static FileCollection getCompileClassPath(Project project) {
        project.sourceSets.main.compileClasspath
    }

    /**
     * Returns the classpath used for embedded Jetty
     *
     * @param project
     *      The project
     *
     * @return
     */
    static FileCollection getJettyClassPath(Project project) {
        FileCollection collection = project.configurations[DependencyListener.Configuration.JETTY9.caption]
        collection += project.sourceSets.main.runtimeClasspath
        collection += getCompileClassPath(project)
        collection
    }

    /**
     * Gets the classpath used for compiling client side code
     *
     * @param project
     *      The project to compile
     * @return
     *      The classpath for the GWT compiler
     */
    static FileCollection getClientCompilerClassPath(Project project) {
        FileCollection collection = project.sourceSets.main.runtimeClasspath
        collection += project.sourceSets.main.compileClasspath

        getMainSourceSet(project).srcDirs.each {
            collection += project.files(it)
        }

        project.sourceSets.main.java.srcDirs.each { File dir ->
            collection += project.files(dir)
        }

        if(project.vaadin.gwt.gwtSdkFirstInClasspath){
            FileCollection gwtCompilerClasspath = project.configurations[DependencyListener.Configuration.CLIENT.caption];
            collection = gwtCompilerClasspath + collection.minus(gwtCompilerClasspath);
        }

        collection
    }

    /**
     * Returns the main source set where project source files are stored
     *
     * @param project
     *      The project to get the source set from
     * @param forceDefaultJavaSourceset
     *      Should the Java source set be preferred
     * @return
     *      The source set
     */
    static SourceDirectorySet getMainSourceSet(Project project, forceDefaultJavaSourceset=false) {
        if(project.vaadin.mainSourceSet) {
            project.vaadin.mainSourceSet
        } else if(isGroovyProject(project) && !forceDefaultJavaSourceset) {
            project.sourceSets.main.groovy
        } else {
            project.sourceSets.main.java
        }
    }

    /**
     * Returns the sources set where project test sources files are located
     *
     * @param project
     *      The project
     * @param forceDefaultJavaSourceset
     *      Should the Java source set be preferred
     * @return
     *      The source set
     */
    static SourceDirectorySet getMainTestSourceSet(Project project, forceDefaultJavaSourceset=false) {
        if(project.vaadin.mainTestSourceSet) {
            project.vaadin.mainTestSourceSet
        } else if(isGroovyProject(project) && !forceDefaultJavaSourceset) {
            project.sourceSets.test.groovy
        } else {
            project.sourceSets.test.java
        }
    }

    /**
     * Does the selected Vaadin version support push
     *
     * @param project
     *      The project to check
     * @return true if push is supported
     */
    def static isPushSupported(Project project) {
        String version = project.vaadin.version
        version == '+' || (version.startsWith('7') && !version.startsWith('7.0'))
    }

    /**
     * Is push supported and enabled in the project
     *
     * @param project
     *      The project to check
     * @return  true if push is supported and enabled
     */
    def static isPushSupportedAndEnabled(Project project) {
        isPushSupported(project) && project.vaadin.push
    }

    public static boolean isAddonStylesSupported(Project project) {
        return !project.vaadin.version.startsWith('7.0')
    }

    public static void openBrowser(Project project, String url) {
        if (project.vaadin.plugin.openInBrowser && java.awt.Desktop.isDesktopSupported()) {
            Thread.startDaemon {
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
                    paths += ["VAADIN/addons/$themeName/"+it.getName()]
                }
            }
        }

        return paths
    }

    def static boolean isGroovyProject(Project project){
        project.plugins.findPlugin(fi.jasoft.plugin.GradleVaadinGroovyPlugin)
    }

    def static logProcess(final Project project, final Process process, final String filename, Closure monitor={}) {
        if(project.vaadin.plugin.logToConsole){
            Thread.start 'Info logger', {
                process.getInputStream().eachLine { output ->
                    monitor.call(output)
                    if(output.contains("[WARN]")){
                        project.logger.warn(output.replaceAll("\\[WARN\\]",'').trim())
                    } else {
                        project.logger.info(output.trim())
                    }
                }
            }
            Thread.start 'Error logger', {
                process.getErrorStream().eachLine { String output ->
                    monitor.call(output)
                    project.logger.error(output.replaceAll("\\[ERROR\\]",'').trim())
                }
            }
        } else {
            File logDir = project.file('build/logs/')
            logDir.mkdirs()

            final File logFile = new File(logDir.canonicalPath + '/' + filename)
            Thread.start 'Info logger', {
                logFile.withWriterAppend { out ->
                    process.getInputStream().eachLine { output ->
                        monitor.call(output)
                        if(output.contains("[WARN]")){
                            out.println "[WARN] "+output.replaceAll("\\[WARN\\]",'').trim()
                        } else {
                            out.println "[INFO] "+output.trim()
                        }
                        out.flush()
                    }
                }
            }
            Thread.start 'Error logger', {
                logFile.withWriterAppend { out ->
                    process.getErrorStream().eachLine { output ->
                        monitor.call(output)
                        out.println "[ERROR] "+output.replaceAll("\\[ERROR\\]",'').trim()
                        out.flush()
                    }
                }
            }
        }
    }

    def static watchDirectoryForChanges(Project project, File dir, Closure closure) {
        def path = Paths.get(dir.canonicalPath)
        def watchService = FileSystems.getDefault().newWatchService()

        Files.walkFileTree path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path p, BasicFileAttributes attrs){
                p.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY)
                FileVisitResult.CONTINUE
            }
        }

        project.logger.info "Watching directory $dir for changes..."

        def stop = false
        while(true) {
            def key = watchService.take()

            // Cancel out multiple same events by sleeping for a moment..
            sleep(1000)

            key.pollEvents().each { WatchEvent event ->
                if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
                    stop = closure.call(key, event)
                }
            }
            if(!key.reset() || stop) break
        }

        project.logger.info "Stopped watching directory"
    }
}