# Introduction
The Vaadin Gradle plugin allows you to easily build Vaadin projects with Gradle. It helps with the most tedious tasks when building a Vaadin project like building the widgetset and running development mode. It also helps you to quickly get started by providing tasks for project, component and theme creation.


# Using the plugin
You do not need to compile the plugin from scratch if you want to use it. You only need to add the following line to your projects build.gradle to start using the plugin.

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin'

or to use a specific version of the plugin

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin?version=x.x.x'

# Plugin tasks
The following tasks are available in the plugin

* ``createVaadinComponent`` - Creates a new Vaadin Component.
* ``createVaadinComposite`` - Creates a new Vaadin Composite for use with VisualDesigner.
* ``createVaadinProject`` - Creates a new Vaadin Project.
* ``createVaadinTheme`` - Creates a new Vaadin Theme
* ``createVaadinWidgetsetGenerator`` - Creates a new widgetset generator for optimizing the widgetset
* ``devmode`` - Run Development Mode for easier debugging and development of client widgets.
* ``superdevmode`` - Run Super Development Mode for easier client widget development.
* ``themes`` - Compiles a Vaadin SASS theme into CSS
* ``vaadinRun`` - Runs the Vaadin application on an embedded Jetty Server
* ``widgetset`` - Compiles Vaadin Addons and components into Javascript.

# Plugin configurations
The following configuration options are available

## Vaadin Project configurations
* ``vaadin.version`` - Vaadin version (Vaadin 6 and 7 supported). Defaults to latest Vaadin 7
* ``vaadin.widgetset`` - The fully qualified name of the widgetset (eg. com.example.helloworld.MyWidgetset)
* ``vaadin.widgetsetGenerator`` - The fully qualified name of the widgetset generator.
* ``vaadin.servletVersion`` - What server version is your application using. Default is 2.5
* ``vaadin.debugPort`` - On what port should the debugger listen. Default is 8000
* ``vaadin.manageWidgetset`` - Should the plugin manage the widgetset for you. Default is true.
* ``vaadin.manageDependencies`` - Should the plugin manage the Vaadin depencies for you. Default is true.
* ``vaadin.serverPort`` - The port the embedded server listens to. Default is 8080.
* ``vaadin.jvmArgs`` - Additional JVM arguments passed to the vaadinRun task. Default is ''.

## Vaadin GWT configurations
* ``vaadin.gwt.version`` - GWT version used with Vaadin 6. Defaults to 2.3.0. 
* ``vaadin.gwt.style`` - Compilation style of the GWT compiler. Default is OBF.
* ``vaadin.gwt.optimize`` - Optimization level of the GWT compiler. Default is 0.
* ``vaadin.gwt.logLevel`` - The log level of the GWT compiler. Default is INFO.
* ``vaadin.gwt.localWorkers`` - The amount of threads the GWT compiler should use. Default is the amount of CPU's available.
* ``vaadin.gwt.draftCompile`` - Should GWT draft compile be used. Default is false.
* ``vaadin.gwt.strict`` - Should the GWT Compiler be run in strict mode. Default is false.
* ``vaadin.gwt.userAgent`` - The browsers you want to support. All browser are supported by default.
* ``vaadin.gwt.jvmArgs`` - Additional JVM arguments passed to the widgetset compiler. Default is ''.

## Vaadin Devmode configurations
* ``vaadin.devmode.noserver`` - Do not run the embedded Jetty server when running devmode. Default is false.
* ``vaadin.devmode.superDevMode`` - Add support for super devmode. Default is false.
* ``vaadin.devmode.bindAddress`` - The address the DevMode server should be bound to. Default is 127.0.0.1. 
* ``vaadin.devmode.codeServerPort`` - The port the DevMode server should be bound to. Default is 9997.

