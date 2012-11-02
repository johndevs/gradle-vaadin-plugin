# Introduction
The Vaadin Gradle plugin allows you to easily build Vaadin projects with Gradle. It helps with the most tedious tasks when building a Vaadin project like building the widgetset and running development mode. It also helps you to quickly get started by providing tasks for project, component and theme creation.


# Using the plugin
You do not need to compile the plugin from scratch if you want to use it. You only need to add the following line to your projects build.gradle to start using the plugin.

    apply from: 'http://plugins.jasoft.fi/vaadin.plugin'

# Plugin tasks
The following tasks are available in the plugin

* ``createVaadinComponent`` - Creates a new Vaadin Component.
* ``createVaadinProject`` - Creates a new Vaadin Project.
* ``createVaadinTheme`` - Creates a new Vaadin Theme
* ``devmode`` - Run Development Mode for easier debugging and development of client widgets.
* ``superdevmode`` - Run Super Development Mode for easier client widget development.
* ``themes`` - Compiles a Vaadin SASS theme into CSS
* ``vaadinRun`` - Runs the Vaadin application on an embedded Jetty Server
* ``widgetset`` - Compiles Vaadin Addons and components into Javascript.

# Plugin configurations
The following configuration options are available
* ``vaadin.version`` - Vaadin version (Vaadin 6 and 7 supported). Defaults to latest Vaadin 7
* ``vaadin.widgetset`` - The fully qualified name of the widgetset (eg. com.example.helloworld.MyWidgetset)
* ``vaadin.servletVersion`` - What server version is your application using. Default is 2.5
* ``vaadin.debugPort`` - On what port should the debugger listen. Default is 8000
* ``vaadin.gwt.style`` - Compilation style of the GWT compiler. Default is OBF.
* ``vaadin.gwt.optimize`` - Optimization level of the GWT compiler. Default is 0.
* ``vaadin.gwt.logLevel`` - The log level of the GWT compiler. Default is INFO.
* ``vaadin.devmode.noserver`` - Do not run the embedded Jetty server when running devmode. Default is false.
* ``vaadin.devmode.superDevMode`` - Add support for super devmode
