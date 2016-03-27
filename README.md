# Introduction
The Vaadin Gradle plugin allows you to easily build Vaadin projects with Gradle. It helps with the most tedious tasks when building a Vaadin project like building the widgetset and running development mode. It also helps you to quickly get started by providing tasks for project, component and theme creation.


[![Build Status](https://travis-ci.org/johndevs/gradle-vaadin-plugin.png?branch=0.11)](https://travis-ci.org/johndevs/gradle-vaadin-plugin) [ ![Download](https://api.bintray.com/packages/johndevs/maven/gradle-vaadin-plugin/images/download.png) ](https://bintray.com/johndevs/maven/gradle-vaadin-plugin/_latestVersion)


# Using the plugin 

You do not need to compile the plugin from scratch if you want to use it. You only need to add the following line to your projects build.gradle to start using the plugin.

## Java projects

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin'

or to use a specific version of the plugin

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin?version=x.x.x'
    
## Groovy projects    
    
    apply from: 'http://plugins.jasoft.fi/vaadin-groovy.plugin'

or to use a specific version of the plugin

    apply from: 'http://plugins.jasoft.fi/vaadin-groovy.plugin?version=x.x.x'

## With new Gradle plugin mechanism (Gradle 2.1+)
(Note that using this method only works for Java projects.)

    plugins {
        id "fi.jasoft.plugin.vaadin" version "0.9.8"
    }

## Snapshot builds

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin?version=0.11-SNAPSHOT'
    
or

    apply from: 'http://plugins.jasoft.fi/vaadin-groovy.plugin?version=0.11-SNAPSHOT'

## Manually applying the plugin

If you are behind a proxy and cannot use the plugin url directly you then you can download the latest plugin jar from [here](https://bintray.com/johndevs/maven/gradle-vaadin-plugin/_latestVersion) and include it in your build.gradle like so:
```
buildscript {
    repositories {
        flatDir dirs: '<Directory where the plugin jar can be found>'
    }

    dependencies {
        classpath group: 'fi.jasoft.plugin', name: 'gradle-vaadin-plugin', version: '+'
    }
}

repositories {
        flatDir dirs: '<Directory where the plugin jar can be found>'
}

apply plugin: fi.jasoft.plugin.GradleVaadinPlugin

```


# Versions

|        | Gradle |  Vaadin  |  Java  | Jetty  | Payara | Servlet | Major features    |
|:------:|:------:|:--------:|:------:|:------:|:------:|:-------:|:-----------------:|
| 0.6.x  |   1.x  |    6,7   |   6,7  |  8.1   |   -    | 2.5,3.0 | Servlet 3 support |
| 0.7.x  |   1.x  |    6,7   |   6,7  |  8.1   |   -    | 2.5,3.0 | Testbench 3, Directory addon zip, Directory Browser, Source & Javadoc jars | 
| 0.8.x  |   1.x  |     7    |    7   |  8.1   |   -    | 3.0 | Idea support, Addon SCSS themes, GWT first in classpath  | 
| 0.9.x  |   2.x  |     7    |   7,8  |  9.2   |   -    | 3.0 | Groovy support, Jetty 9, Jetty autorestart |
| 0.10.x |   2.x  |     7    |   7,8  |  9.3   |   -    | 3.0 | Widgetset CDN support, Classpath JAR on Win as default | 
| 0.11.x |   3.x  |     7    |    8   |  9.3   |  4.1*  | 3.1 | Payara as web server, dependencies as defaultDependencies, Support for parallel execution of tasks, Compass SASS compiler support |

\* Payara becomes the default server for vaadinRun.


# Project configurations
* ``vaadin.version`` - Vaadin version (Vaadin 6 and 7 supported). Defaults to latest Vaadin 7
* ``vaadin.manageDependencies`` - Should the plugin manage the Vaadin depencies for you. Default is true.
* ``vaadin.manageRepositories`` - Should the plugin add repositories such as maven central and vaadin addons to the project.  Default is true.
* ``vaadin.mainSourceSet`` - Defines the main source set where all source files will be generated.
* ``vaadin.push`` - Should vaadin push be enabled for the application. Default is false.

# Addon configurations
* ``vaadin.addon.author`` - The author of the Vaadin addon.
* ``vaadin.addon.license`` - The licence of the Vaadin addon.
* ``vaadin.addon.title`` - The title for the addon as seen in the Vaadin Directory.
* ``vaadin.addon.styles`` - An array of paths relative to webroot (eg. '/VAADIN/addons/myaddon/myaddon.scss') where CSS and SCSS files for an addon can be found.

# Plugin configurations
* ``vaadin.plugin.logToConsole``- Should server logs be logged to the console or to a log file. Default is logging to file.
* ``vaadin.plugin.openInBrowser`` - Should the application be opened in a browser tab after the application is launched. Default true.
* ``vaadin.plugin.eclipseOutputDir`` - The directory where Eclipse will output its compiled classes. Default is project.sourceSets.main.output.classesDir.
* ~~``vaadin.plugin.jettyAutoRefresh`` - Should jetty automatically restart when a class is changed while jetty is running.~~
* ``vaadin.plugin.serverRestart`` - Should the server automatically restart when a class is changed.
* ``vaadin.plugin.useClassPathJar`` - Use a single jar to define the classpath (if the classpath is too long)

# JRebel configurations
* ``vaadin.jrebel.enabled`` - Should JRebel be used when running the project. Default is false
* ``vaadin.jrebel.location`` - Absolute path of jrebel.jar (required if ```jrebel.enabled``` is set to true)

# Vaadin Testbench configurations
* ``vaadin.testbench.enabled`` - Should Testbench be used for UI testing?. Default is false.
* ``vaadin.testbench.version`` - Version of testbench to use. By default the latest release of the 3.x series.
* ``vaadin.testbench.runApplication`` - Should the application be run on embedded Jetty before the tests are run. Default true.
* ``vaadin.testbench.hub.enabled`` - Should a testbench hub be started when running tests. Default false.
* ``vaadin.testbench.hub.host`` - The hostname of the hub
* ``vaadin.testbench.hub.port`` - The port of the hub
* ``vaadin.testbench.node.enabled`` - Should a testbench node be started when running tests. Default false.
* ``vaadin.testbench.node.host`` - The hostname of the node
* ``vaadin.testbench.node.port`` - The port of the node
* ``vaadin.testbench.node.hub`` - The URL of the hub where the node should connect to. By default http://localhost:4444/grid/register'.
* ``vaadin.testbench.node.browsers`` - A list of supported browsers by the hub. e.g.
```groovy
vaadin.testbench.node.browsers = [
    [ browserName: 'firefox', version: 3.6, maxInstances: 5, platform: 'LINUX' ],
    [ browserName: 'chrome', version: 22, maxInstances: 1, platform: 'WINDOWS' ]
]
```

# Plugin tasks

## vaadinCreateComponent

Creates a new Vaadin Component

### Parameters 
*  ``name`` - The class name of the component. By default *MyComponent*.

## vaadinCreateComposite

Creates a new Vaadin Composite.

### Parameters
* ``name`` -  The class name of the composite. By default *MyComposite*.
* ``package`` - The package where the composite should be placed. By default *com.example.\<component name\>*. 

## vaadinCreateProject

Creates a new Vaadin Project.

### Parameters
* ``name`` -  The application name. By default the project name.
* ``package`` - The package where the application classes will be placed. 
* ``widgetset`` - The widgetset name, if applicable.
By default, if a widgetset is defined, that package is used, otherwise *com.example.\<project name\>* is used.

## vaadinCreateTheme

Creates a new Vaadin Theme.

### Parameters
* ``name`` -  The name of the theme. By default the project name.

## vaadinCreateAddonTheme

Creates a new theme for a Vaadin Addon project.

### Parameters
* ``name`` -  The name of the theme. By default, if the addon title is used to generated the theme name, 
otherwise *MyAddonTheme* is used.

## vaadinCreateWidgetsetGenerator

Creates a new widgetset generator for optimizing the widgetset

## vaadinCreateTestbenchTest

Creates a new Testbench test.

## Parameters
* ``name`` -  The test class name.
* ``package`` - The test class package.

## vaadinDevMode (Deprecated)

Run Development Mode for easier debugging and development of client widgets.

Deprecated in favor of **vaadinSuperDevMode**

### Configurations
* ``noserver`` - Should the internal server be used. Default *false*.
* ``bindAddress`` - To what host or ip should development mode bind itself to. By default *localhost*.
* ``codeServerPort`` - To what port should development mode bind itself to. By default *9997*.
* ``extraArgs`` - Extra arguments passed to the code server. By default *none*.
* ``logLevel`` - The log level. Possible levels NONE,DEBUG,TRACE,INFO. By default *INFO*.
* ``server`` - Application server to use. Possible application servers are 'payara', 'jetty'. Default is *payara*.
* ``debug`` - Should application be run in debug mode. When running in production set this to true. Default is *true*.
* ``debugPort`` - The port the debugger listens to. Default is 8000.
* ``jvmArgs`` - Extra jvm args passed to the JVM running the Vaadin application.
* ``serverRestart`` - Should the server restart after every change. By default *true*.
* ``serverPort`` - The port the vaadin application should run on. By default 8080.
* ``themeAutoRecompile`` -  Should theme be recompiled when SCSS file is changes. Default is *true*.
* ``openInBrowser`` - Should the application be opened in a browser when it has been launched. By defailt *true*.

## vaadinSuperDevMode

Run Super Development Mode for easier client widget development.

### Configurations
* ``noserver`` - Should the internal server be used. Default *false*.
* ``bindAddress`` - To what host or ip should development mode bind itself to. By default *localhost*.
* ``codeServerPort`` - To what port should development mode bind itself to. By default *9997*.
* ``extraArgs`` - Extra arguments passed to the code server. By default *none*.
* ``logLevel`` - The log level. Possible levels NONE,DEBUG,TRACE,INFO. By default *INFO*.
* ``server`` - Application server to use. Possible application servers are 'payara', 'jetty'. Default is *payara*.
* ``debug`` - Should application be run in debug mode. When running in production set this to true. Default is *true*.
* ``debugPort`` - The port the debugger listens to. Default is 8000.
* ``jvmArgs`` - Extra jvm args passed to the JVM running the Vaadin application.
* ``serverRestart`` - Should the server restart after every change. By default *true*.
* ``serverPort`` - The port the vaadin application should run on. By default 8080.
* ``themeAutoRecompile`` -  Should theme be recompiled when SCSS file is changes. Default is *true*.
* ``openInBrowser`` - Should the application be opened in a browser when it has been launched. By defailt *true*.

## vaadinCompile

Compiles Vaadin Addons and components into Javascript.

### Configurations
* ``style`` - Compilation style. By default *OBF*.
* ``optimize`` - Should the compilation result be optimized. By default *0*.
* ``logging`` - Should logging be enabled. By default *true*.
* ``logLevel`` - The log level. Possible levels NONE,DEBUG,TRACE,INFO. By default *INFO*.
* ``localWorkers`` - Amount of local workers used when compiling. By default the amount of processors.
* ``draftCompile`` - Should draft compile be used. By default *true*.
* ``strict`` - Should strict compiling be used. By default *true*.
* ``userAgent`` - What user agents (browsers should be used. By defining null all user agents are used.)
* ``jvmArgs`` - Extra jvm arguments passed the JVM running the compiler
* ``extraArgs`` - Extra arguments passed to the compiler
* ``sourcePaths`` - Source paths where the compiler will look for source files. By default *['client', 'shared']*.
* ``collapsePermutations`` - Should the compiler permutations be collapsed. By default *true*.
* ``extraInherits`` - Extra module inherits.
* ``gwtSdkFirstInClasspath`` - Should GWT be placed first in the classpath when compiling the widgetset. By default *true*.
* ``outputDirectory`` - (Optional) root directory, for generated files; default is the web-app dir from the WAR plugin
* ``widgetsetCDN`` - Use external Vaadin hosted CDN for compiling the widgetset. By default *false*.
* ``profiler`` - Should the Vaadin client side profiler be used. By defailt *false*.
* ``manageWidgetset`` - Should the plugin manage the widgetset (gwt.xml file). By default *true*.
* ``widgetset`` - The widgetset to use for the project. Leave empty for a pure server side project, or to autodetect widgetset.
* ``widgetsetGenerator`` - The widgetset generator to use.

## vaadinThemeCompile

Compiles a Vaadin SASS theme into CSS.

### Configurations
* ``compiler`` - The SASS compiler to use. *vaadin* and *compass* are available. *vaadin* is default.
* ``themesDirectory`` - Root directory for themes. By default *src/main/webapp/VAADIN/themes*

## vaadinRun

Runs the Vaadin application on the application server.

### Parameters
* ``stopAfterStart`` - Should the server stop after starting. By default *false*.
* ``nobrowser`` - Do not open browser after server has started. By default *true*.

### Configurations
* ``server`` - Application server to use. Possible application servers are 'payara', 'jetty'. Default is *payara*.
* ``debug`` - Should application be run in debug mode. When running in production set this to true. Default is *true*.
* ``debugPort`` - The port the debugger listens to. Default is 8000.
* ``jvmArgs`` - Extra jvm args passed to the JVM running the Vaadin application.
* ``serverRestart`` - Should the server restart after every change. By default *true*.
* ``serverPort`` - The port the vaadin application should run on. By default 8080.
* ``themeAutoRecompile`` -  Should theme be recompiled when SCSS file is changes. Default is *true*.
* ``openInBrowser`` - Should the application be opened in a browser when it has been launched. By defailt *true*.

## vaadinAddons

Search for addons in the Vaadin Directory.

### Parameters
* ``search`` - String to search for in addons. By default empty string returning all addons in the directory.
* ``sort`` - Sort criteria (options: name,description,date,rating). By default *unsorted*.
* ``verbose`` - Should verbose descriptions be shown. By default *false*.

## vaadinAddonZip

Create Vaadin Directory compatible Addon zip archive of the project. Metadata can be configurated with the vaadin.addon.* properties

## vaadinJavadocJar

Generate javadoc from project and package it as a jar.

## vaadinSourcesJar
 
Packages all sources a jar.

## vaadinClassPathJar

Builds a classpath jar that is used for operating systems with limited command lengths (Windows).

## vaadinUpdateAddonStyles

Updates the addon.scss file listing with addons styles found in the classpath.

## vaadinUpdateWidgetset

Updates the widgetset (gwt.xml) file with inherits found from the project classpath.

