/*
* Copyright 2012 John Ahlroos
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
package fi.jasoft.plugin;

class VaadinPluginExtension{
	String widgetset										// Widgetset, leave empty for serverside application
	String version = "7+" 									// Using the latest vaadin 7 build
	
	// GWT Compiler and DevMode
	String gwtStyle = "OBF"
	String gwtOptimize = 0
	String gwtLogLevel = "INFO"

	// DevMode
	String devModeDebugPort = 8000
	boolean superDevModeEnabled = false
	String servletVersion = "2.5"
}
