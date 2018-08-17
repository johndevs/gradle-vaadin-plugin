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
package com.devsoap.plugin

import com.devsoap.plugin.extensions.VaadinPluginExtension
import com.devsoap.plugin.tasks.BuildClassPathJar
import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.UpdateWidgetsetTask
import groovy.io.FileType
import groovy.transform.Memoized
import groovyx.net.http.HTTPBuilder
import org.apache.commons.lang.StringUtils
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.util.VersionNumber

import java.awt.*
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.List
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.regex.Pattern

/**
 * Utility class for Vaadin tasks
 */
class Util {

    private static final String PLUS = '+'
    private static final String SPACE = ' '
    private static final String WARNING_LOG_MARKER = '[WARN]'
    private static final String ERROR_LOG_MARKER = '[ERROR]'
    private static final String INFO_LOG_MARKER = '[INFO]'
    private static final String STREAM_CLOSED_LOG_MESSAGE = 'Stream was closed'
    private static final String VAADIN = 'VAADIN'
    private static final String GRADLE_HOME = 'org.gradle.java.home'
    private static final String JAVA_HOME = 'java.home'
    private static final String JAVA_BIN_NAME = 'java'
    private static final String GWT_MODULE_POSTFIX = '.gwt.xml'
    private static final String CLIENT_PACKAGE_NAME = 'client'
    private static final String VAADIN_CLIENT_DENDENCY = 'vaadin-client'
    private static final String VAADIN_SHARED_DEPENDENCY = 'vaadin-shared'
    private static final String VAADIN_COMP_SERVER_DEPENDENCY = 'vaadin-compatibility-server'
    private static final String VAADIN_COMP_CLIENT_DEPENDENCY = 'vaadin-compatibility-client'
    private static final String VAADIN_COMP_SHARED_DEPENDENCY = 'vaadin-compatibility-shared'
    private static final String VALIDATION_API_DEPENDENCY = 'validation-api-1.0'
    private static final String JAR_EXTENSION = '.jar'

    static final String APP_WIDGETSET = 'AppWidgetset'
    static final int VAADIN_SEVEN_MAJOR_VERSION = 7
    static final String VAADIN_SERVER_DEPENDENCY = 'vaadin-server'


    /**
     * Get the compile time classpath of a project
     *
     * @param project
     *      the project to get the classpath for
     *
     * @return
     *      the classpath as a collection of files
     */
    @Memoized
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
    @Memoized
    static FileCollection getCompileClassPathOrJar(Project project) {
        VaadinPluginExtension pluginExtension = project.extensions.getByType(VaadinPluginExtension)

        FileCollection classpath
        if ( pluginExtension.useClassPathJar ) {
            // Add dependencies using the classpath jar
            BuildClassPathJar pathJarTask = project.tasks.getByName(BuildClassPathJar.NAME)
            if(!pathJarTask.archivePath.exists()) {
                throw new IllegalStateException("Classpath jar has not been created in project $pathJarTask.project")
            }
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
    @Memoized
    static FileCollection getClientCompilerClassPath(Project project) {
        FileCollection collection = project.sourceSets.main.runtimeClasspath
        collection += project.sourceSets.main.compileClasspath

        getMainSourceSet(project).srcDirs.each {
            collection += project.files(it)
        }

        project.sourceSets.main.java.srcDirs.each { File dir ->
            collection += project.files(dir)
        }

        // Filter out needed dependencies
        collection = collection.filter { File file ->
            if ( file.name.endsWith(JAR_EXTENSION) ) {

                // Add GWT compiler + deps
                if (file.name.startsWith(VAADIN_SERVER_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_CLIENT_DENDENCY) ||
                        file.name.startsWith(VAADIN_SHARED_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_COMP_SERVER_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_COMP_CLIENT_DEPENDENCY) ||
                        file.name.startsWith(VAADIN_COMP_SHARED_DEPENDENCY) ||
                        file.name.startsWith(VALIDATION_API_DEPENDENCY)) {
                    return true
                }

                // Addons with client side widgetset
                JarFile jar = new JarFile(file.absolutePath)
                if ( !jar.manifest ) {
                    return false
                }

                Attributes attributes = jar.manifest.mainAttributes
                return attributes.getValue('Vaadin-Widgetsets')
            }
            true
        }

        // Add additional dependencies directly to classpath
        collection += project.configurations[GradleVaadinPlugin.CONFIGURATION_CLIENT_COMPILE]

        // Ensure gwt sdk libs are in the correct order
        if (  project.vaadinCompile.gwtSdkFirstInClasspath ) {
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
    @Memoized
    static FileCollection moveGwtSdkFirstInClasspath(Project project , FileCollection collection) {
        if ( project.vaadin.manageDependencies ) {
            FileCollection gwtCompilerClasspath = project.configurations[GradleVaadinPlugin.CONFIGURATION_CLIENT]
            return gwtCompilerClasspath + (collection - gwtCompilerClasspath)
        } else if ( project.vaadinCompile.gwtSdkFirstInClasspath ) {
            project.logger.log(LogLevel.WARN, 'Cannot move GWT SDK first in classpath since plugin does not manage ' +
                    'dependencies. You can set vaadinCompile.gwtSdkFirstInClasspath=false and ' +
                    'arrange the dependencies yourself if you need to.')
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
    @Memoized
    static SourceDirectorySet getMainSourceSet(Project project, boolean forceDefaultJavaSourceset=false) {
        if ( project.vaadin.mainSourceSet ) {
            project.vaadin.mainSourceSet
        } else if(forceDefaultJavaSourceset) {
            project.sourceSets.main.java
        } else {
            switch (getProjectType(project)) {
                case ProjectType.GROOVY:
                    return project.sourceSets.main.groovy
                case ProjectType.KOTLIN:
                    return project.sourceSets.main.kotlin
                case ProjectType.JAVA:
                    return project.sourceSets.main.java
            }
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
    @Memoized
    static SourceDirectorySet getMainTestSourceSet(Project project, forceDefaultJavaSourceset=false) {
        if ( project.vaadin.mainTestSourceSet ) {
            project.vaadin.mainTestSourceSet
        } else if(forceDefaultJavaSourceset) {
            project.sourceSets.test.java
        } else {
            switch (getProjectType(project)) {
                case ProjectType.GROOVY:
                    return project.sourceSets.test.groovy
                case ProjectType.KOTLIN:
                    return project.sourceSets.test.kotlin
                case ProjectType.JAVA:
                    return project.sourceSets.test.java
            }
        }
    }

    /**
     * Is push supported and enabled in the project
     *
     * @param project
     *      The project to check
     * @return  true if push is supported and enabled
     */
    @Memoized
    static isPushEnabled(Project project) {
        VaadinPluginExtension vaadin = project.extensions[VaadinPluginExtension.NAME]
        vaadin.push
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
        if ( project.vaadinRun.openInBrowser
                && Desktop.isDesktopSupported()
                && Desktop.desktop.isSupported(Desktop.Action.BROWSE)) {
            GradleVaadinPlugin.THREAD_POOL.submit {
                Desktop.desktop.browse url.toURI()
            }
        } else if ( project.vaadinRun.openInBrowser ) {
            project.logger.info('Failed to open browser, AWT Desktop or Desktop.browse() is not supported on ' +
                    'current platform.')
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
    @Memoized
    static boolean isIE10UserAgentSupported(Project project) {
        if ( getVaadinVersion(project) == PLUS ) {
            return true
        }
        VersionNumber version = VersionNumber.parse(getVaadinVersion(project))
        version.major > VAADIN_SEVEN_MAJOR_VERSION ||
                (version.major == VAADIN_SEVEN_MAJOR_VERSION && version.minor > 0)
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
    @Memoized
    static boolean isOperaUserAgentSupported(Project project) {
        VersionNumber version = VersionNumber.parse(getResolvedVaadinVersion(project))
        version.major == VAADIN_SEVEN_MAJOR_VERSION && version.minor < 4
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
    @Memoized
    static List findAddonSassStylesInProject(Project project) {
        File resourceDir = project.sourceSets.main.resources.srcDirs.iterator().next()
        File addonsDir = project.file(resourceDir.canonicalPath + '/VAADIN/addons')

        List paths = []

        if ( addonsDir.exists() ) {
            addonsDir.traverse(type:FileType.DIRECTORIES) { themeDir ->
                String themeName = themeDir.name
                Pattern fileNameRegExp = ~/$themeName\.s?css/
                themeDir.traverse(type:FileType.FILES, nameFilter:fileNameRegExp) { cssFile ->
                    paths += ["VAADIN/addons/$themeName/$cssFile.name"]
                }
            }
        }

        paths
    }

    @Memoized
    static ProjectType getProjectType(Project project) {
        if(project.plugins.findPlugin('groovy')) {
            ProjectType.GROOVY
        } else if (project.plugins.findPlugin('org.jetbrains.kotlin.jvm')) {
            ProjectType.KOTLIN
        } else {
            ProjectType.JAVA
        }
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
    static void logProcess(final Project project, final Process process, final String filename, Closure monitor) {
        if ( project.vaadin.logToConsole ) {
            logProcessToConsole(project, process, monitor)
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
                                 Closure monitor={ }) {
        File logDir = project.file("$project.buildDir/logs/")
        logDir.mkdirs()

        final File LOGFILE = new File(logDir, filename)
        project.logger.info("Logging to file $LOGFILE")

        GradleVaadinPlugin.THREAD_POOL.submit {
            LOGFILE.withWriterAppend { out ->
                try {
                    boolean errorOccurred = false

                    process.inputStream.eachLine { output ->
                        monitor.call(output)
                        if ( output.contains(WARNING_LOG_MARKER) ) {
                            out.println WARNING_LOG_MARKER + SPACE + output.replace(WARNING_LOG_MARKER, '').trim()
                        } else if ( output.contains(ERROR_LOG_MARKER) ) {
                            errorOccurred = true
                            out.println ERROR_LOG_MARKER + SPACE + output.replace(ERROR_LOG_MARKER,'').trim()
                        } else {
                            out.println INFO_LOG_MARKER + SPACE + output.trim()
                        }
                        out.flush()
                        if ( errorOccurred ) {
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

        GradleVaadinPlugin.THREAD_POOL.submit {
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

        GradleVaadinPlugin.THREAD_POOL.submit {
            try {
                boolean errorOccurred = false
                process.inputStream.eachLine { output ->
                    monitor.call(output)
                    if ( output.contains(WARNING_LOG_MARKER) ) {
                        project.logger.warn(output.replace(WARNING_LOG_MARKER, '').trim())
                    } else if ( output.contains(ERROR_LOG_MARKER) ) {
                        errorOccurred = true
                    } else {
                        project.logger.info(output.trim())
                    }
                    if ( errorOccurred ) {
                        // An error has occurred, dump everything to console
                        project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                    }
                }
            } catch (IOException e) {
                // Stream might be closed
                project.logger.debug(STREAM_CLOSED_LOG_MESSAGE, e)
            }
        }

        GradleVaadinPlugin.THREAD_POOL.submit {
            try {
                process.errorStream.eachLine { String output ->
                    monitor.call(output)
                    project.logger.error(output.replace(ERROR_LOG_MARKER, '').trim())
                }
            } catch (IOException e) {
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
        Path path = Paths.get(dir.canonicalPath)
        WatchService watchService = FileSystems.getDefault().newWatchService()

        Files.walkFileTree path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path p, BasicFileAttributes attrs) {
                if ( p.toFile().exists() ) {
                    p.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY)
                }
                FileVisitResult.CONTINUE
            }
        }

        project.logger.info "Watching directory $dir for changes..."

        boolean stop = false
        while(true) {
            WatchKey key = watchService.take()

            // Cancel out multiple same events by sleeping for a moment..
            sleep(1000)

            key.pollEvents().each { WatchEvent event ->
                if ( !stop && event.kind() != StandardWatchEventKinds.OVERFLOW ) {
                    stop = !closure.call(key, event)
                }
            }
            if ( !key.reset() || stop ) {
                break
            }
        }

        project.logger.info "Stopped watching directory $dir"
    }

    /**
     * Returns the themes directory
     *
     * @param project
     *      The project to get the themes directory for
     * @return
     *      The themes directory
     */
    @Memoized
    static File getThemesDirectory(Project project) {
        File themesDir
        CompileThemeTask compileThemeTask = project.tasks.getByName(CompileThemeTask.NAME)
        if ( compileThemeTask.themesDirectory ) {
            String customDir = compileThemeTask.themesDirectory
            themesDir = new File(customDir)
            if ( !themesDir.absolute ) {
                themesDir = project.file(project.rootDir.canonicalPath + File.separator + customDir)
            }
        } else {
            File webAppDir = getWebAppDirectory(project)
            File vaadinDir = new File(webAppDir, VAADIN)
            themesDir = new File(vaadinDir, 'themes')
        }
        themesDir
    }

    /**
     * Returns the widgetset directory
     *
     * @param project
     *      The project to get the directory for
     * @return
     *      The widgetset directory
     */
    @Memoized
    static File getWidgetsetDirectory(Project project) {
        File webAppDir = getWebAppDirectory(project)
        File vaadinDir = new File(webAppDir, VAADIN)
        File widgetsetsDir = new File(vaadinDir, 'widgetsets')
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
    @Memoized
    static File getWidgetsetCacheDirectory(Project project) {
        File webAppDir = getWebAppDirectory(project)
        File vaadinDir = new File(webAppDir, VAADIN)
        File unitCacheDir = new File(vaadinDir, 'gwt-unitCache')
        unitCacheDir
    }

    /**
     * Returns the web-app directory
     *
     * @param project
     *      the project
     * @return
     *      the directory
     */
    @Memoized
    static File getWebAppDirectory(Project project) {
        String outputDir = project.vaadinCompile.outputDirectory
        if(outputDir){
            project.file(outputDir)
        } else if(project.convention.findPlugin(WarPluginConvention)){
            project.convention.getPlugin(WarPluginConvention).webAppDir
        } else {
            project.file('src/main/webapp')
        }
    }

    /**
     * Returns the resolved Vaadin version.
     *
     * For example, if the version has been defined as 7.x and the real latest Vaadin 7
     * version that is releases is 7.3.10 then this method will return 7.3.10.
     *
     * Note: This will resolve the CONFIGURATION_SERVER configuration
     *
     * @param project
     *      The project to get the Vaadin version for
     * @return
     *      The resolved Vaadin version
     */
    @Memoized
    static String getResolvedVaadinVersion(Project project) {
        getResolvedArtifactVersion(project,
                project.configurations[GradleVaadinPlugin.CONFIGURATION_SERVER],
                VAADIN_SERVER_DEPENDENCY,
                getVaadinVersion(project))
    }

    /**
     * Resolves the real artifact version from a meta version (for example 1.2.+). Note that this will try to resolve
     * the configuration to get the artifact.
     *
     * @param project
     *      the projecct
     * @param conf
     *      the configuration to resolve
     * @param artifactName
     *      the artifact name
     * @param defaultVersion
     *      the fallback version if the artifact cannot be found
     * @return
     *      returns the resolved version or if the artifact could not be found, the default version
     */
    @Memoized
    static String getResolvedArtifactVersion(Project project, Configuration conf, String artifactName,
                                             String defaultVersion=null) {
        String version = defaultVersion
        if(isResolvable(project, conf)){
            // Calling resolvedConfiguration triggers resolution, be aware
            version = conf.resolvedConfiguration.resolvedArtifacts
                    .find { it.name == artifactName }?.moduleVersion?.id?.version
        } else {
            project.logger.warn("Failed to get artifact version from non-resolvable configuration $conf")
        }
        version
    }

    /**
     * Is the project a legacy vaadin 8 project (the project has the legacy jars on its classpath)
     *
     * @param project
     *      the project
     * @return
     *      true if it legacy
     */
    @Memoized
    static boolean isLegacyVaadin8Project(Project project) {
        boolean isLegacyProject = false
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if(dependency.group == 'com.vaadin' && dependency.name == VAADIN_COMP_CLIENT_DEPENDENCY){
                    isLegacyProject = true
                }
            }
        }
        isLegacyProject
    }

    /**
     * Returns all addon jars in the project
     *
     * @param project
     *      The project to look in
     * @return
     *      a set of addon dependencies
     */
    @Memoized
    static Set findAddonsInProject(Project project,
                                   String byAttribute='Vaadin-Widgetsets',
                                   Boolean includeFile=false,
                                   List<Project> scannedProjects = []) {
        Set addons = []
        scannedProjects << project
        Attributes.Name attribute = new Attributes.Name(byAttribute)

        project.configurations.all.each { Configuration conf ->
            if(conf.name != GradleVaadinPlugin.CONFIGURATION_CLIENT){
                conf.allDependencies.each { Dependency dependency ->
                    if (dependency in ProjectDependency) {
                        Project dependentProject = ((ProjectDependency) dependency).dependencyProject
                        if (!(dependentProject in scannedProjects)) {
                            addons.addAll(findAddonsInProject(dependentProject, byAttribute, includeFile,
                                    scannedProjects))
                        }
                    } else if (isResolvable(project, conf)){
                        conf.files(dependency).each { File file ->
                            if (file.file && file.name.endsWith(JAR_EXTENSION)) {
                                file.withInputStream { InputStream stream ->
                                    JarInputStream jarStream = new JarInputStream(stream)
                                    Manifest mf = jarStream.getManifest()
                                    Attributes attributes = mf?.mainAttributes
                                    if (attributes?.getValue(attribute)) {
                                        if (!dependency.name.startsWith(VAADIN_CLIENT_DENDENCY)) {
                                            if (includeFile) {
                                                addons << [
                                                        groupId   : dependency.group,
                                                        artifactId: dependency.name,
                                                        version   : getResolvedArtifactVersion(project, conf,
                                                                dependency.name, dependency.version),
                                                        file      : file
                                                ]
                                            } else {
                                                addons << [
                                                        groupId   : dependency.group,
                                                        artifactId: dependency.name,
                                                        version   : getResolvedArtifactVersion(project, conf,
                                                                dependency.name, dependency.version)
                                                ]
                                            }
                                        }
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
     * Returns the client side package if one exists in the main source set
     *
     * @param project
     *      the project to search in
     * @return
     *
     */
    @Memoized
    static String getClientPackage(Project project) {
        String clientPackage
        getMainSourceSet(project).srcDirs.each { File srcDir ->
             project.fileTree(srcDir).visit { FileVisitDetails details ->
                if ( details.name == CLIENT_PACKAGE_NAME && details.directory ) {
                    details.stopVisiting()
                    clientPackage = details.file.canonicalPath - srcDir.canonicalPath
                    project.logger.info "Found client package $clientPackage"
                }
             }
        }
        clientPackage
    }

    /**
     * Returns the defined Vaadin version or if no version is defined then it returns the default vaadin version.
     *
     * @param project
     *      The project to get the version for
     * @return
     *      version as a string
     */
    @Memoized
    static String getVaadinVersion(Project project) {
        project.vaadin.version ?: pluginProperties.getProperty('vaadin.defaultVersion')
    }

    /**
     * Returns the classpath of the WAR used by the vaadinRun task
     *
     * @param project
     *      the project to use
     * @return
     *      classpath of WAR
     */
    @Memoized
    static FileCollection getWarClasspath(Project project) {

        // Include project classes and resources
        JavaPluginConvention java = project.convention.getPlugin(JavaPluginConvention)
        SourceSet mainSourceSet = java.sourceSets.getByName('main')
        FileCollection classpath =  mainSourceSet.output.classesDirs
        classpath += project.files(mainSourceSet.output.resourcesDir)

        // Include server dependencies
        classpath += project.configurations[GradleVaadinPlugin.CONFIGURATION_SERVER]

        // Exclude pre-compiled widgetset if compiled widgetset exists
        if ( project.vaadinCompile.widgetsetCDN || getWidgetset(project) ) {
            classpath = classpath.filter { file ->
                !(file.name.startsWith('vaadin') && file.name.contains('client-compiled'))}
        }

        // Include addons
        classpath += project.configurations[GradleVaadinPlugin.CONFIGURATION_CLIENT_COMPILE]

        // Include runtime dependencies
        classpath += project.configurations.runtime

        // Include push dependencies if enabled
        if ( isPushEnabled(project) ) {
            classpath += project.configurations[GradleVaadinPlugin.CONFIGURATION_PUSH]
        }

        // Remove provided dependencies
        if(project.configurations.findByName('providedCompile')){
            classpath -= project.configurations.providedCompile
        }

        if(project.configurations.findByName('providedRuntime')){
            classpath -= project.configurations.providedRuntime
        }

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
    @Memoized
    static String getRelativePathForFile(String parentFolderName, File file) {
        File parentFolder = file.parentFile
        while(parentFolder.name != parentFolderName) {
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
    @Memoized
    static String getJavaBinary(Project project) {
        String javaHome
        if ( project.hasProperty(GRADLE_HOME) ) {
            javaHome = project.properties[GRADLE_HOME]
        } else if ( System.getProperty(JAVA_HOME) ) {
            javaHome = System.getProperty(JAVA_HOME)
        }

        if ( javaHome ) {
            File javaBin = new File(javaHome, 'bin')
            File java = new File(javaBin, JAVA_BIN_NAME)
            return java.canonicalPath
        }

        // Fallback to Java on PATH with a warning
        project.logger.warn('Could not determine where the Java JRE is located, is JAVA_HOME set?')
        JAVA_BIN_NAME
    }

    /**
     * Resolves the first available widgetset from Project
     */
    @Memoized
    static String getWidgetset(Project project) {
        if ( project.vaadinCompile.widgetsetCDN ) {
            throw new GradleException("Cannot retrieve widgetset name from a project that is using the widgetset CDN.")
        }

        if ( project.vaadinCompile.widgetset ) {
            return project.vaadinCompile.widgetset
        }

        // Search for widgetset
        File widgetsetFile = resolveWidgetsetFile(project)
        if ( widgetsetFile ) {
            def sourceDirs = project.sourceSets.main.allSource
            File rootDir = sourceDirs.srcDirs.find { File directory ->
                project.fileTree(directory.absolutePath).contains(widgetsetFile)
            }
            if ( rootDir ) {
                File relativePath= new File( rootDir.toURI().relativize( widgetsetFile.toURI() ).toString() )
                String widgetset = TemplateUtil.convertFilePathToFQN(relativePath.path, GWT_MODULE_POSTFIX)
                project.logger.info "Detected widgetset $widgetset from project"
                widgetset
            }
        }
    }

    /**
     * Resolves the widgetset file automatically from sources
     */
    @Memoized
    static File resolveWidgetsetFile(Project project) {

        // Search for module XML in sources
        def sourceDirs = project.sourceSets.main.allSource
        List modules = []
        sourceDirs.srcDirs.each {
            modules.addAll(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
        }
        if ( !modules.isEmpty() ) {
            return modules.first()
        }

        // WidgetsetFile has been defined but not created, create it
        String widgetset = project.vaadinCompile.widgetset

        // If client side classes exists in project use client side package to determine widgetset
        if ( !widgetset ) {
            String clientPackage = getClientPackage(project)
            if ( clientPackage ) {
                String widgetsetPath = StringUtils.removeEnd(clientPackage, File.separator + CLIENT_PACKAGE_NAME)
                if ( widgetsetPath.size() > 0 ) {
                    widgetsetPath = TemplateUtil.convertFilePathToFQN(widgetsetPath, '') + '.'
                }
                widgetset =  widgetsetPath + APP_WIDGETSET
            }
        }

        // If addons exists in project but widgetset is not defined, use default one
        if ( !widgetset && findAddonsInProject(project).size() > 0 ) {
            widgetset = APP_WIDGETSET
        }

        // If dependent projects have widgetsets, use default one
        if ( !widgetset && UpdateWidgetsetTask.findInheritsInDependencies(project).size() > 0 ) {
            widgetset = APP_WIDGETSET
        }

        if ( widgetset && !project.vaadinCompile.widgetsetCDN ) {
            // No widgetset file detected, create one
            File resourceDir = project.sourceSets.main.resources.srcDirs.first()
            File widgetsetFile = new File(resourceDir,
                    TemplateUtil.convertFQNToFilePath(widgetset, GWT_MODULE_POSTFIX))
            widgetsetFile.parentFile.mkdirs()
            widgetsetFile.createNewFile()
            return widgetsetFile
        }

        null
    }

    /**
     * Ensures that the string can be used as a Java Class Name
     */
    @Memoized
    static String makeStringJavaCompatible(String string) {
        if (!string) {
            return null
        }
        List<Character> collector = []
        List<String> collector2 = []
        string.chars.collect(collector) { ch ->
            if (collector.empty) {
                Character.isJavaIdentifierStart(ch) ? ch : ''
            } else {
                Character.isJavaIdentifierPart(ch) ? ch : ' '
            }
        }.join('').tokenize().collect(collector2) { t ->
            collector2.empty ? t : t.capitalize()
        }.join('')
    }

    /**
     * Indicates if project can have non-resolvable configurations
     *
     * https://docs.gradle.org/3.4/release-notes.html#configurations-can-be-unresolvable
     *
     * @param project
     *      the project to check
     * @return
     *      true if project can have non-resolvable dependencies
     */
    @SuppressWarnings("DuplicateNumberLiteral")
    @Memoized
    static boolean hasNonResolvableConfigurations(Project project) {
        VersionNumber gradleVersion = VersionNumber.parse(project.gradle.gradleVersion)
        VersionNumber gradleVersionWithUnresolvableDeps = new VersionNumber(3, 3, 0, null)
        gradleVersion >= gradleVersionWithUnresolvableDeps
    }

    /**
     * Is the project and configuration resolvable
     *
     * https://docs.gradle.org/3.4/release-notes.html#configurations-can-be-unresolvable
     *
     * @param project
     *      the project to check
     * @param configuration
     *      the configuration to check
     * @return
     *      true if configuration can be resolved
     */
    @Memoized
    static boolean isResolvable(Project project, Configuration configuration) {
        hasNonResolvableConfigurations(project) ? configuration.canBeResolved: true
    }

    /**
     * Gets the plugin properties defined in gradle.properties
     */
    @Memoized
    static Properties getPluginProperties() {
        Properties properties = new Properties()
        properties.load(Util.class.getResourceAsStream('/gradle.properties') as InputStream)
        properties
    }

    /**
     * Configures a HTTPBuilder with timeout and proxy settings
     * @param http
     *      the http builder
     * @param timeout
     *      the timeout in milliseconds for the request
     * @param retryCount
     *      the retry count of the request
     * @return
     *      the configured builder
     */
    static HTTPBuilder configureHttpBuilder(HTTPBuilder http, int timeout=5000, int retryCount=1) {
        // SSL
        http.ignoreSSLIssues()

        // Timeout
        http.client.params.setIntParameter('http.socket.timeout', timeout)
        http.client.params.setIntParameter('http.connection.timeout', timeout)
        http.client.params.setParameter('http.method.retry-handler',
                new DefaultHttpRequestRetryHandler(retryCount, true))

        // Proxy
        int proxyPort = Integer.parseInt(System.getProperty('http.proxyPort', '-1'))
        if (proxyPort > 0) {
            String proxyScheme = System.getProperty('http.proxyScheme', 'http')
            String proxyHost = System.getProperty('http.proxyHost', 'localhost')
            http.setProxy(proxyHost, proxyPort, proxyScheme)
            String proxyUser = System.getProperty('http.proxyUser', null)
            if (proxyUser) {
                String proxyPass = System.getProperty('http.proxyPassword', null)
                http.auth.basic(proxyUser, proxyPass)
            }
        }

        http
    }
}
