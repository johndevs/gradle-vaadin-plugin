/*
* Copyright 2015 John Ahlroos
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

import fi.jasoft.plugin.tasks.BuildClassPathJar
import groovy.io.FileType
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.util.VersionNumber

import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.jar.Manifest

/**
 * Utility class for Vaadin tasks
 */
class Util {

    /**
     * Reads an input string from the console
     *
     * @param format
     *      The string format according to String.format
     *
     * @return
     *      The input from the console
     */
    static String readLine(String format) {
        readLine(format, null)
    }

    /**
     * Reads an input string from the console
     *
     * @param format
     *      The string format according to String.format
     * @param args
     *      The arguments provided to Console.readLine
     * @return
     *      The input from the console
     */
    static String readLine(String format, Object... args) {
        def console = System.console()
        try {
            if (console) {
                return console.readLine(format, args)
            }

            System.out.print(String.format(format, args));

            def reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();

        } catch (IOException ioe) {
            // Ignore
        }
        null
    }

    /**
     * Get the compile time classpath of a project
     *
     * @param project
     *      the project to get the classpath for
     *
     * @return
     *      the classpath as a collection of files
     */
    static FileCollection getCompileClassPath(Project project) {
        project.sourceSets.main.compileClasspath
    }

    /**
     * Returns the classpath used for embedded Jetty
     *
     * @param project
     *      the project to get the classpath for
     *
     * @return
     *      the classpath as a collection of files
     */
    static FileCollection getJettyClassPath(Project project) {
        FileCollection collection

        if(project.vaadin.plugin.useClassPathJar){
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            collection = project.files(pathJarTask.archivePath)
        } else {
            collection = project.configurations[DependencyListener.Configuration.JETTY9.caption]
            collection += getCompileClassPath(project)
        }

        collection += project.sourceSets.main.runtimeClasspath

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
    static isPushSupported(Project project) {
        String version = Util.getVaadinVersion(project)
        version == '+' || (version.startsWith('7') && !version.startsWith('7.0'))
    }

    /**
     * Is push supported and enabled in the project
     *
     * @param project
     *      The project to check
     * @return  true if push is supported and enabled
     */
    static isPushSupportedAndEnabled(Project project) {
        isPushSupported(project) && project.vaadin.push
    }

    /**
     * Does the project support addon SCSS styles
     *
     * @param project
     *      the project to check the support for
     * @return
     *      <code>true</code> if addon styles are supported
     */
    static boolean isAddonStylesSupported(Project project) {
        VersionNumber version = VersionNumber.parse(getVaadinVersion(project))
        version.minor > 0
    }

    /**
     * Opens a URL in the default system browser
     *
     * @param project
     *      the project to open the browser for
     * @param url
     *      the URL to open
     */
    static void openBrowser(Project project, String url) {
        if (project.vaadin.plugin.openInBrowser && java.awt.Desktop.isDesktopSupported()) {
            Thread.startDaemon {
                java.awt.Desktop.desktop.browse url.toURI()
            }
        }
    }

    /**
     * Is the IE10 GWT user agent supported by project
     *
     * @param project
     *      the project to check support for
     *
     * @return
     *      <code>true</code> if the IE10 user agent is supported
     */
    static boolean isIE10UserAgentSupported(Project project) {
        if (getVaadinVersion(project) == '+') {
            return true
        }
        VersionNumber version = VersionNumber.parse(getVaadinVersion(project))
        version.minor > 0
    }

    /**
     * Is the Opera GWT user agent supported by project
     *
     * @param project
     *      the project to check support for
     *
     * @return
     *      <code>true</code> if the Opera user agent is supported
     */
    static boolean isOperaUserAgentSupported(Project project) {
        VersionNumber version = VersionNumber.parse(getVaadinVersion(project))
        version.minor < 4
    }

    /**
     * Does the Vaadin project support the Servlet 3 specification.
     *
     * @param project
     *      the project to check support for
     * @return
     *      <code>true</code> if Servlet 3 is supported
     */
    static boolean isServlet3Project(Project project) {
        if (getVaadinVersion(project) == '+') {
            return true
        }
        VersionNumber version = VersionNumber.parse(getVaadinVersion(project))
        version.minor > 0
    }

    /**
     * Is the project the root project in a multimodule project
     *
     * @param project
     *      the project to check
     * @return
     *      <code>true</code> if the project is the root project
     */
    static boolean isRootProject(Project project) {

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
        true
    }

    /**
     * Does the project support SCSS compiling
     *
     * @param project
     *      the project to check support for
     * @return
     *      <code>true</code> if SCSS compiling is supported
     */
    static boolean isSassCompilerSupported(Project project) {
        VersionNumber version = VersionNumber.parse(getVaadinVersion(project))
        version.minor > 1
    }

    /**
     * Get a list of all addon themename.scss on the project classpath
     *
     * @param project
     *      the project to create the list for
     *
     * @return
     *      a list of paths to the scss files
     */
    static List findAddonSassStylesInProject(Project project) {
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

        paths
    }

    /**
     * Is the project a groovy Vaadin project
     *
     * @param project
     *      the project to check groovyness on
     *
     * @return
     *      <code>true</code> if project is a groovy project
     */
    static boolean isGroovyProject(Project project){
        project.plugins.findPlugin(fi.jasoft.plugin.GradleVaadinGroovyPlugin)
    }

    /**
     * Monitors a process for output in a separate thread and logs the output into either a file or to the console
     *
     * @param project
     *      the project to monitor
     * @param process
     *      the process to monitor
     * @param filename
     *      the log file to write to
     * @param monitor
     *      Optional additional monitor closure for processing output
     */
    static void logProcess(final Project project, final Process process, final String filename, Closure monitor={}) {
        if(project.vaadin.plugin.logToConsole){
            Thread.start 'Info logger', {
                try {
                    def errorOccurred = false
                    process.inputStream.eachLine { output ->
                        monitor.call(output)
                        if (output.contains("[WARN]")) {
                            project.logger.warn(output.replaceAll("\\[WARN\\]", '').trim())
                        } else if(output.contains('[ERROR]')){
                            errorOccurred = true
                        } else {
                            project.logger.info(output.trim())
                        }
                        if(errorOccurred){
                            // An error has occurred, dump everything to console
                            project.logger.error(output.replaceAll("\\[ERROR\\]",'').trim())
                        }
                    }
                } catch(IOException e){
                    // Stream might be closed
                }
            }

            Thread.start 'Error logger', {
                try {
                    process.errorStream.eachLine { String output ->
                        monitor.call(output)
                        project.logger.error(output.replaceAll("\\[ERROR\\]", '').trim())
                    }
                } catch(IOException e){
                    // Stream might be closed
                }
            }
        } else {
            File logDir = project.file('build/logs/')
            logDir.mkdirs()

            final File logFile = new File(logDir.canonicalPath + '/' + filename)
            Thread.start 'Info logger', {
                logFile.withWriterAppend { out ->
                    try {
                        def errorOccurred = false
                        process.inputStream.eachLine { output ->
                            monitor.call(output)
                            if (output.contains("[WARN]")) {
                                out.println "[WARN] " + output.replaceAll("\\[WARN\\]", '').trim()
                            } else if(output.contains('[ERROR]')){
                                errorOccurred = true
                                out.println "[ERROR] "+output.replaceAll("\\[ERROR\\]",'').trim()
                            } else {
                                out.println "[INFO] " + output.trim()
                            }
                            out.flush()
                            if(errorOccurred){
                                // An error has occurred, dump everything to console
                                project.logger.error(output.replaceAll("\\[ERROR\\]",'').trim())
                            }
                        }
                    } catch (IOException e) {
                        // Stream might be closed
                    }
                }
            }
            Thread.start 'Error logger', {
                logFile.withWriterAppend { out ->
                    try {
                        process.errorStream.eachLine { output ->
                            monitor.call(output)
                            project.logger.error(output.replaceAll("\\[ERROR\\]",'').trim())
                            out.println "[ERROR] "+output.replaceAll("\\[ERROR\\]",'').trim()
                            out.flush()
                        }
                    } catch (IOException e) {
                        // Stream might be closed
                    }
                }
            }
        }
    }

    /**
     * Recursively Watches a directory for changes in a separate thread
     *
     * @param project
     *      the project to watch
     * @param dir
     *      the directory to watch
     * @param closure
     *      the closure to call when a change in the directory occurs
     */
    static void watchDirectoryForChanges(Project project, File dir, Closure closure) {
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

    /**
     * Returns the themes directory
     *
     * @param project
     *      The project to get the themes directory for
     * @return
     *      The themes directory
     */
    def static File getThemesDirectory(Project project) {
        if(project.vaadin.plugin.themesDirectory){
            project.file(project.vaadin.plugin.themesDirectory)
        } else {
            File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
            project.file(webAppDir.canonicalPath + '/VAADIN/themes')
        }
    }

    /**
<<<<<<< HEAD
     * Returns the widgetset directory
     *
     * @param project
     *      The project to get the directory for
     * @return
     *      The widgetset directory
     */
    static File getWidgetsetDirectory(Project project) {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        project.file(webAppDir.canonicalPath +'/VAADIN/widgetsets')
    }

    /**
     * Returns the widgetset cache directory
     *
     * @param project
     *      The project to get the directory for
     * @return
     *      The widgetset directory
     */
    static File getWidgetsetCacheDirectory(Project project) {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        project.file(webAppDir.canonicalPath +'/VAADIN/gwt-unitCache')
    }

    /**
     * Returns the resolved Vaadin version.
     *
     * For example, if the version has been defined as 7.x and the real latest Vaadin 7
     * version that is releases is 7.3.10 then this method will return 7.3.10.     *
     *
     * @param project
     *      The project to get the Vadin version for
     * @return
     *      The resolved Vaadin version
     */
    static String getResolvedVaadinVersion(Project project) {
        def version = project.vaadin.version
        project.configurations[DependencyListener.Configuration.SERVER.caption].resolvedConfiguration.firstLevelModuleDependencies.each{ dependency ->
            if(dependency.moduleName == 'vaadin-server'){
               version = dependency.moduleVersion
           }
        }
        version
    }

    /**
     * Returns all addon jars in the proejct
     *
     * @param project
     *      The project to look in
     * @return
     *      a set of addon dependencies
     */
    static Set findAddonsInProject(Project project) {
        def addons = []
        def widgetsetAttribute = new Attributes.Name('Vaadin-Widgetsets')
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                conf.files(dependency).each { File file ->
                    file.withInputStream { InputStream stream ->
                        def jarStream = new JarInputStream(stream)
                        def mf = jarStream.getManifest()
                        def attributes = mf?.mainAttributes
                        if (attributes?.getValue(widgetsetAttribute)) {
                            if (!dependency.name.startsWith('vaadin-client')) {
                                addons << [
                                        groupId: dependency.group,
                                        artifactId: dependency.name,
                                        version: dependency.version
                                ]
                            }
                        }
                    }
                }
            }
        }
        addons
    }

    /**
     * Returns the defined Vaadin version or if no version is defined then it returns the default vaadin version.
     *
     * @param project
     *      The project to get the version for
     * @return
     *      version as a string
     */
    static String getVaadinVersion(Project project) {
        project.vaadin.version ?: '7.3.+'
    }
}