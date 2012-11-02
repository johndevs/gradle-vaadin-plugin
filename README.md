# Using the plugin

You will not need to compile the plugin from scratch if you want to use it. You can just include it in your gradle project by adding the following line into your build.gradle.

apply from: 'http://plugins.jasoft.fi/vaadin.plugin'

# Plugin tasks
The following tasks are available in the plugin

* createVaadinComponent - Creates a new Vaadin Component.
* createVaadinProject - Creates a new Vaadin Project.
* createVaadinTheme - Creates a new Vaadin Theme
* devmode - Run Development Mode for easier debugging and development of client widgets.
* superdevmode - Run Super Development Mode for easier client widget development.
* themes - Compiles a Vaadin SASS theme into CSS
* vaadinRun - Runs the Vaadin application on an embedded Jetty Server
* widgetset - Compiles Vaadin Addons and components into Javascript.

# Plugin configurations
The following configuration options are available
* vaadin.version - Vaadin version (Vaadin 6 and 7 supported). Defaults to latest Vaadin 7
* vaadin.widgetset - The fully qualified name of the widgetset (eg. com.example.helloworld.MyWidgetset)
* vaadin.servletVersion - What server version is your application using. Default is 2.5
* vaadin.debugPort - On what port should the debugger listen. Default is 8000
* vaadin.gwt.style - Compilation style of the GWT compiler. Default is OBF.
* vaadin.gwt.optimize - Optimization level of the GWT compiler. Default is 0.
* vaadin.gwt.logLevel - The log level of the GWT compiler. Default is INFO.
* vaadin.devmode.noserver - Do not run the embedded Jetty server when running devmode. Default is false.
* vaadin.devmode.superDevMode - Add support for super devmode

