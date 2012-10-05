package fi.jasoft.plugin;

class VaadinPluginExtension{
	String widgetset										// Widgetset, leave empty for serverside application
	String version = "7+" 									// Using the latest vaadin 7 build
	String applicationPackage = "com.example" 			// Default package to create the application in
	String applicationName = "MyApplication"	

	String gwtStyle = "OBF"
	String gwtOptimize = 0
	String gwtLogLevel = "INFO"

	String startupUrl = 'http://localhost:8080'
}
