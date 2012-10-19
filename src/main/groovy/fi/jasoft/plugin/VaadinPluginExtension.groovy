package fi.jasoft.plugin;

class VaadinPluginExtension{
	String widgetset										// Widgetset, leave empty for serverside application
	String version = "7+" 									// Using the latest vaadin 7 build
	
	// GWT Compiler and DevMode
	String gwtStyle = "OBF"
	String gwtOptimize = 0
	String gwtLogLevel = "INFO"

	// DevMode
	String devModeStartupUrl = 'http://localhost:8888'
	String devModeDebugPort = 8000
	boolean superDevModeEnabled = false
	String servletVersion = "2.5"
}
