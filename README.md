# Introduction
The Vaadin Gradle plugin allows you to easily build Vaadin projects with Gradle. It helps with the most tedious tasks when building a Vaadin project like building the widgetset and running development mode. It also helps you to quickly get started by providing tasks for project, component and theme creation.

Build automatically tested on Travis CI
[![Build Status](https://travis-ci.org/johndevs/gradle-vaadin-plugin.png?branch=master)](https://travis-ci.org/johndevs/gradle-vaadin-plugin)


# Using the plugin
You do not need to compile the plugin from scratch if you want to use it. You only need to add the following line to your projects build.gradle to start using the plugin.

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin'

or to use a specific version of the plugin

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin?version=x.x.x'

If you are behind a proxy and cannot use the plugin url directly you then you can download the plugin jar from http://vaadin.com/addon/vaadin-plugin-for-gradle and include it in your build.gradle like so:
```
apply plugin: 'vaadin'

buildscript {
    repositories {
        flatDir dirs: '<Directory where the plugin jar can be found>'
    }

    dependencies {
        classpath group: 'fi.jasoft.plugin', name: 'gradle-vaadin-plugin', version: '0.6'
    }
}

repositories {
        flatDir dirs: '<Directory where the plugin jar can be found>'
}
```


# Plugin tasks
The following tasks are available in the plugin

* ``vaadinCreateComponent`` - Creates a new Vaadin Component.
* ``vaadinCreateComposite`` - Creates a new Vaadin Composite for use with VisualDesigner.
* ``vaadinCreateProject`` - Creates a new Vaadin Project.
* ``vaadinCreateTheme`` - Creates a new Vaadin Theme
* ``vaadinCreateAddonTheme`` - Creates a new Vaadin addon theme
* ``vaadinCreateWidgetsetGenerator`` - Creates a new widgetset generator for optimizing the widgetset
* ``vaadinCreateTestbenchTest`` - Creates a new testbench JUnit test. (Requires Testbench enabled).
* ``vaadinDevMode`` - Run Development Mode for easier debugging and development of client widgets.
* ``vaadinSuperDevMode`` - Run Super Development Mode for easier client widget development.
* ``vaadinCompileThemes`` - Compiles a Vaadin SASS theme into CSS
* ``vaadinCompileWidgetset`` - Compiles Vaadin Addons and components into Javascript.
* ``vaadinRun`` - Runs the Vaadin application on an embedded Jetty Server
* ``vaadinAddons`` - Search for addons in the Vaadin Directory. Optional parameters: -Psearch=<term> -Psort=[name|description|date|rating] -Pverbose=[true|false]
* ``vaadinAddonZip`` - Create Vaadin Directory compatible Addon zip archive of the project. Metadata can be configurated with the vaadin.addon.* properties.
* ``vaadinJavadocJar`` - Generate javadoc from project and package it as a jar.
* ``vaadinSourcesJar`` - Packages all sources a a jar.

Not provided by the Vaadin plugin directly but inherited from other dependent plugins.
* ``jar`` - Create Vaadin Directory compatible Addon jar out of the project. Metadata can be configurated with the vaadin.addon.* properties.
* ``war``- Create a WAR archive of the project which can run on any application server.

# Plugin configurations
The following configuration options are available.

For a better example of an actual working build.gradle using these options see https://gist.github.com/johndevs/11184881 .

## Vaadin Project configurations
* ``vaadin.version`` - Vaadin version (Vaadin 6 and 7 supported). Defaults to latest Vaadin 7
* ``vaadin.widgetset`` - The fully qualified name of the widgetset (eg. com.example.helloworld.MyWidgetset)
* ``vaadin.widgetsetGenerator`` - The fully qualified name of the widgetset generator.
* ``vaadin.debugPort`` - On what port should the debugger listen. Default is 8000
* ``vaadin.manageWidgetset`` - Should the plugin manage the widgetset for you. Default is true.
* ``vaadin.manageDependencies`` - Should the plugin manage the Vaadin depencies for you. Default is true.
* ``vaadin.manageRepositories`` - Should the plugin add repositories such as maven central and vaadin addons to the project.  Default is true.
* ``vaadin.serverPort`` - The port the embedded server listens to. Default is 8080.
* ``vaadin.jvmArgs`` - Additional JVM arguments passed to the vaadinRun task. Default is ''.
* ``vaadin.addon.author`` - The author of the Vaadin addon.
* ``vaadin.addon.license`` - The licence of the Vaadin addon.
* ``vaadin.addon.title`` - The title for the addon as seen in the Vaadin Directory.
* ``vaadin.addon.styles`` - An array of paths relative to webroot (eg. '/VAADIN/addons/myaddon/myaddon.scss') where CSS and SCSS files for an addon can be found.
* ``vaadin.mainSourceSet`` - Defines the main source set where all source files will be generated.
* ``vaadin.push`` - Should vaadin push be enabled for the application. Default is false.
* ``vaadin.debug`` - Should the application be run in debug mode. Default is true.
* ``vaadin.profiler`` - Should the vaadin client side profiler be enabled. Default is false.

## Vaadin GWT configurations
* ``vaadin.gwt.style`` - Compilation style of the GWT compiler. Default is OBF.
* ``vaadin.gwt.optimize`` - Optimization level of the GWT compiler. Default is 0.
* ``vaadin.gwt.logLevel`` - The log level of the GWT compiler. Default is INFO.
* ``vaadin.gwt.localWorkers`` - The amount of threads the GWT compiler should use. Default is the amount of CPU's available.
* ``vaadin.gwt.draftCompile`` - Should GWT draft compile be used. Default is false.
* ``vaadin.gwt.strict`` - Should the GWT Compiler be run in strict mode. Default is false.
* ``vaadin.gwt.userAgent`` - The browsers you want to support. All browser are supported by default.
* ``vaadin.gwt.jvmArgs`` - Additional JVM arguments passed to the widgetset compiler. Default is ''. Example:
```
gwt.jvmArgs = ['-Xmx500M', '-XX:MaxPermSize=256M']
```
* ``vaadin.gwt.extraArgs`` - Extra compiler arguments that should be passed to the widgetset compiler.
* ``vaadin.gwt.sourcePaths`` - Source folders where GWT code that should be compiled to JS is found. Default is 'client' and 'shared'.
* ``vaadin.gwt.collapsePermutations`` - Should all permutations be compiled into a single js file for faster compilation time (but larger file size).

## Vaadin Devmode configurations
* ``vaadin.devmode.noserver`` - Do not run the embedded Jetty server when running devmode. Default is false.
* ``vaadin.devmode.superDevMode`` - Add support for super devmode. Default is false.
* ``vaadin.devmode.bindAddress`` - The address the DevMode server should be bound to. Default is 127.0.0.1.
* ``vaadin.devmode.codeServerPort`` - The port the DevMode server should be bound to. Default is 9997.

## Vaadin Tooling configurations
All Vaadin Tooling are free to try for 30 days but then requires a license. See https://vaadin.com/tools-and-services for more information.

### Vaadin JRebel
(Licence no longer available through Vaadin, contact http://zeroturnaround.com/ for licence)

* ``vaadin.jrebel.enabled`` - Should JRebel be used when running the project. Default is false
* ``vaadin.jrebel.location`` - Absolute path of jrebel.jar (required if ```jrebel.enabled``` is set to true)

### Vaadin Testbench
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

## Plugin configurations
* ``vaadin.plugin.terminateOnEnter`` - Should the vaadinRun and devmode tasks be terminated on enter-key. Default true.
* ``vaadin.plugin.logToConsole``- Should server logs be logged to the console or to a log file. Default is logging to file.
* ``vaadin.plugin.openInBrowser`` - Should the application be opened in a browser tab after the application is launched. Default true.
