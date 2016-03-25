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

import fi.jasoft.plugin.configuration.VaadinPluginExtension
import fi.jasoft.plugin.tasks.BuildClassPathJar
import groovy.io.FileType
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

import org.gradle.api.artifacts.ProjectDependency
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

/**
 * Utility class for Vaadin tasks
 */
class Util {

    private static final String PLUS = '+'
    private static final String SPACE = ' '
    private static final String VAADIN_PROPERTY = 'vaadin'
    private static final String WARNING_LOG_MARKER = '[WARN]'
    private static final String ERROR_LOG_MARKER = '[ERROR]'
    private static final String INFO_LOG_MARKER = '[INFO]'
    private static final String INFO_LOGGER = 'Info logger'
    private static final String ERROR_LOGGER = 'Error logger'
    private static final String STREAM_CLOSED_LOG_MESSAGE = 'Stream was closed'
    private static final String VAADIN = 'VAADIN'
    private static final String GRADLE_HOME = 'org.gradle.java.home'
    private static final String JAVA_HOME = 'JAVA_HOME'
    private static final String JAVA_BIN_NAME = 'java'
    private static final String GWT_MODULE_POSTFIX = '.gwt.xml'

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
     * Get the compile time classpath of the project or the classpath jar if enabled
     *
     * @param project
     *      the project to get the classpath for
     * @return
     *      the classpath as a collection of files
     */
    static FileCollection getCompileClassPathOrJar(Project project) {
        def vaadin = project.vaadin as VaadinPluginExtension
        FileCollection classpath
        if(vaadin.plugin.useClassPathJar) {
            // Add dependencies using the classpath jar
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            classpath = project.files(pathJarTask.archivePath)
        } else {
            classpath = getCompileClassPath(project)
        }
        classpath
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

        if(project.vaadinCompile.gwtSdkFirstInClasspath){
            collection = moveGwtSdkFirstInClasspath(project, collection)
        }

        collection
    }

    /**
     * Moves the GWT SDK libs first in the classpath to ensure the GWT compiler
     * gets the correct versions of its dependencies.
     *
     * @param project
     *      the project
     * @param collection
     *      the collection with the classpath files
     * @return
     *      a new collection with the GWT SDK libs listed first
     */
    static FileCollection moveGwtSdkFirstInClasspath(Project project , FileCollection collection){
        FileCollection gwtCompilerClasspath = project.configurations[GradleVaadinPlugin.CONFIGURATION_CLIENT];
        return gwtCompilerClasspath + (collection - gwtCompilerClasspath);
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
    static SourceDirectorySet getMainSourceSet(Project project, boolean forceDefaultJavaSourceset=false) {
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
        version == PLUS || (version.startsWith('7') && !version.startsWith('7.0'))
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
        if (project.vaadinRun.openInBrowser && java.awt.Desktop.isDesktopSupported()) {
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
        if (getVaadinVersion(project) == PLUS) {
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
        if (getVaadinVersion(project) == PLUS) {
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
        if (project.hasProperty(VAADIN_PROPERTY) && project == project.rootProject) {
            return true
        }

        // If not traverse upwards and see if there are any other vaadin projects in the hierarchy
        while (project != project.rootProject) {
            project = project.rootProject
            if (project.hasProperty(VAADIN_PROPERTY)) {
                return false
            }
        }

        // no other vaadin projects found upwards, this is the root project
        true
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
            logProcessToConsole(project,process,monitor)
        } else {
            logProcessToFile(project, process, filename, monitor)
        }
    }

    /**
     * Logs a process to a file
     *
     * @param project
     *      the project
     * @param process
     *      the process
     * @param filename
     *      thefilename where to output the logs
     * @param monitor
     *      the monitor for monitoring log output
     */
    static void logProcessToFile(final Project project, final Process process, final String filename,
                                 Closure monitor={}) {
        File logDir = project.file("$project.buildDir/logs/")
        logDir.mkdirs()

        final File LOGFILE = new File(logDir, filename)
        project.logger.info("Logging to file $LOGFILE")

        Thread.start INFO_LOGGER, {
            LOGFILE.withWriterAppend { out ->
                try {
                    def errorOccurred = false

                    process.inputStream.eachLine { output ->
                        monitor.call(output)
                        if (output.contains(WARNING_LOG_MARKER)) {
                            out.println WARNING_LOG_MARKER + SPACE + output.replace(WARNING_LOG_MARKER, '').trim()
                        } else if(output.contains(ERROR_LOG_MARKER)){
                            errorOccurred = true
                            out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER,'').trim()
                        } else {
                            out.println INFO_LOG_MARKER + SPACE + output.trim()
                        }
                        out.flush()
                        if(errorOccurred){
                            // An error has occurred, dump everything to console
                            project.logger.error(output.replace(ERROR_LOG_MARKER,'').trim())
                        }
                    }
                } catch (IOException e) {
                    // Stream might be closed
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }

        Thread.start ERROR_LOGGER, {
            LOGFILE.withWriterAppend { out ->
                try {
                    process.errorStream.eachLine { output ->
                        monitor.call(output)
                        out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER,'').trim()
                        out.flush()
                    }
                } catch (IOException e) {
                    // Stream might be closed
                    project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
                }
            }
        }
    }

    /**
     * Logs process output to the console
     *
     * @param project
     *      the project
     * @param process
     *      the process to log output from
     * @param monitor
     *      the monitor to monitory output with
     */
    static void logProcessToConsole(final Project project, final Process process, Closure monitor={}) {
        project.logger.info("Logging to console")

        Thread.start INFO_LOGGER, {
            try {
                def errorOccurred = false
                process.inputStream.eachLine { output ->
                    monitor.call(output)
                    if (output.contains(WARNING_LOG_MARKER)) {
                        project.logger.warn(output.replace(WARNING_LOG_MARKER, '').trim())
                    } else if(output.contains(ERROR_LOG_MARKER)){
                        errorOccurred = true
                    } else {
                        project.logger.info(output.trim())
                    }
                    if(errorOccurred){
                        // An error has occurred, dump everything to console
                        project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                    }
                }
            } catch(IOException e){
                // Stream might be closed
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }

        Thread.start ERROR_LOGGER, {
            try {
                process.errorStream.eachLine { String output ->
                    monitor.call(output)
                    project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                }
            } catch(IOException e){
                // Stream might be closed
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
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
                if(p.toFile().exists()){
                    p.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY)
                }
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
                if (!stop && event.kind() != StandardWatchEventKinds.OVERFLOW) {
                    stop = !closure.call(key, event)
                }
            }
            if(!key.reset() || stop){
                break
            }
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
    static File getThemesDirectory(Project project) {
        if(project.vaadin.plugin.themesDirectory){
            project.file(project.vaadin.plugin.themesDirectory)
        } else {
            def webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
            def vaadinDir = new File(webAppDir, VAADIN)
            def themesDir = new File(vaadinDir, 'themes')
            themesDir
        }
    }

    /**
     * Returns the widgetset directory
     *
     * @param project
     *      The project to get the directory for
     * @return
     *      The widgetset directory
     */
    static File getWidgetsetDirectory(Project project) {
        def webAppDir = project.vaadinCompile.outputDirectory ?:
                project.convention.getPlugin(WarPluginConvention).webAppDir
        def vaadinDir = new File(webAppDir, VAADIN)
        def widgetsetsDir = new File(vaadinDir, 'widgetsets')
        widgetsetsDir
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
        def webAppDir = project.vaadinCompile.outputDirectory ?:
                project.convention.getPlugin(WarPluginConvention).webAppDir
        def vaadinDir = new File(webAppDir, VAADIN)
        def unitCacheDir = new File(vaadinDir, 'gwt-unitCache')
        unitCacheDir
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
        project.configurations[GradleVaadinPlugin.CONFIGURATION_SERVER].
                resolvedConfiguration.firstLevelModuleDependencies.each{ dependency ->
            if(dependency.moduleName == 'vaadin-server'){
               version = dependency.moduleVersion
           }
        }
        version
    }

    /**
     * Returns all addon jars in the project
     *
     * @param project
     *      The project to look in
     * @return
     *      a set of addon dependencies
     */
    static Set findAddonsInProject(Project project,
                                   String byAttribute='Vaadin-Widgetsets',
                                   Boolean includeFile=false) {
        def addons = []
        def attribute = new Attributes.Name(byAttribute)
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if(!(dependency in ProjectDependency)){
                    conf.files(dependency).each { File file ->
                        file.withInputStream { InputStream stream ->
                            def jarStream = new JarInputStream(stream)
                            def mf = jarStream.getManifest()
                            def attributes = mf?.mainAttributes
                            if (attributes?.getValue(attribute)) {
                                if (!dependency.name.startsWith('vaadin-client')) {
                                    if(includeFile){
                                        addons << [
                                                groupId: dependency.group,
                                                artifactId: dependency.name,
                                                version: dependency.version,
                                                file: file
                                        ]
                                    } else {
                                        addons << [
                                                groupId: dependency.group,
                                                artifactId: dependency.name,
                                                version: dependency.version,
                                        ]
                                    }
                                }
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
        project.vaadin.version ?: '7.6.+'
    }

    /**
     * Returns the classpath of the WAR used by the vaadinRun task
     *
     * @param project
     *      the project to use
     * @return
     *      classpath of WAR
     */
    static FileCollection getWarClasspath(Project project) {

        // Include project classes and resources
        FileCollection classpath = project.files(
                project.sourceSets.main.output.classesDir,
                project.sourceSets.main.output.resourcesDir
        )

        // Include server dependencies
        classpath += project.configurations[GradleVaadinPlugin.CONFIGURATION_SERVER]

        // Include client if no widgetset to provide pre-compiled widgetset
        if(!getWidgetset(project)){
            classpath += project.configurations[GradleVaadinPlugin.CONFIGURATION_CLIENT]
        }

        // Include runtime dependencies
        classpath += project.configurations.runtime

        // Include push dependencies if enabled
        if(isPushSupportedAndEnabled(project)) {
            classpath += project.configurations[GradleVaadinPlugin.CONFIGURATION_PUSH]
        }

        // Remove provided dependencies
        classpath -= project.configurations.providedCompile
        classpath -= project.configurations.providedRuntime

        // Ensure no duplicates
        classpath = project.files(classpath.files)

        classpath
    }

    /**
     * Gets a relative path to a parent path
     *
     * For example:
     *
     * We want to convert /foo/bar/baz/myfile.css to be relative to /foo/bar
     *
     * We select parent folder name to 'bar' and the path becomes baz/myfile.css
     *
     * @param parentFolderName
     *      the name of the parent folder.
     * @param file
     *      the file to convert
     * @return
     *      the relative path
     */
    static String getRelativePathForFile(String parentFolderName, File file){
        def parentFolder = file.parentFile
        while(parentFolder.name != parentFolderName){
            parentFolder = parentFolder.parentFile
        }
        file.canonicalPath.substring(parentFolder.canonicalPath.length() + 1)
    }

    /**
     * Replaces a file extension with another file extension
     *
     * @param filePath
     *      the path of the file
     * @param oldExtension
     *      the old extension
     * @param newExtension
     *      the new extension
     * @return
     *      the new file path with the new extension
     */
    static String replaceExtension(String filePath, String oldExtension, String newExtension) {
        filePath.substring(0, filePath.length() - oldExtension.length()) + newExtension
    }

    /**
     * Returns the path to the java binary
     *
     * @return
     */
    static String getJavaBinary(Project project){
        String javaHome
        if(project.hasProperty(GRADLE_HOME)){
            javaHome = project.properties[GRADLE_HOME]
        } else if(System.getProperty(JAVA_HOME)){
            javaHome = System.getProperty(JAVA_HOME)
        }

        if(javaHome){
            def javaBin =  new File(javaHome, 'bin')
            def java = new File(javaBin, JAVA_BIN_NAME)
            return java.canonicalPath
        }else {
            // Fallback to Java on PATH
            return JAVA_BIN_NAME
        }
    }

    /**
     * Resolves the first available widgetset from Project
     */
    static String getWidgetset(Project project) {
        if(project.vaadinCompile.widgetset){
            return project.vaadinCompile.widgetset
        }

        // Search for widgetset
        def widgetsetFile = resolveWidgetsetFile(project)
        if(widgetsetFile){
            def sourceDirs = project.sourceSets.main.allSource
            def File rootDir = sourceDirs.srcDirs.find { File directory ->
                project.fileTree(directory.absolutePath).contains(widgetsetFile)
            }
            if(rootDir){
                def relativePath= new File( rootDir.toURI().relativize( widgetsetFile.toURI() ).toString() )
                def widgetset = TemplateUtil.convertFilePathToFQN(relativePath.path, GWT_MODULE_POSTFIX)
                project.logger.info "Detected widgetset $widgetset from project"
                widgetset
            }
        }
    }

    /**
     * Resolves the widgetset file automatically from sources
     */
    static File resolveWidgetsetFile(Project project) {
        def sourceDirs = project.sourceSets.main.allSource
        def modules = []
        sourceDirs.srcDirs.each {
            modules.addAll(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
        }
        if(!modules.isEmpty()){
            return modules.first()
        }

        String widgetset = project.vaadinCompile.widgetset
        if(widgetset){
            // No widgetset file detected, create one
            File resourceDir = project.sourceSets.main.resources.srcDirs.first()
            def widgetsetFile = new File(resourceDir,
                    TemplateUtil.convertFQNToFilePath(widgetset, GWT_MODULE_POSTFIX))
            widgetsetFile.parentFile.mkdirs()
            widgetsetFile.createNewFile()
            return widgetsetFile
        }

        null
    }
}