/*
* Copyright 2013 John Ahlroos
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
	String widgetset
	String widgetsetGenerator = null										
	String version = "7+" 									
	String servletVersion = "2.5"
	String debugPort = 8000
	boolean manageWidgetset = true
	boolean manageDependencies = true
	int serverPort = 8080
	String[] jvmArgs = null
	
	// GWT Compiler and DevMode
	GWT gwt = new GWT()
	class GWT{
		String style = "OBF"
		String optimize = 0
		String logLevel = "INFO"
		int localWorkers = Runtime.getRuntime().availableProcessors()
		boolean draftCompile = false
		boolean strict = false
		String userAgent = "ie8,ie9,gecko1_8,safari,opera"
		String[] jvmArgs = null
		String version = "2.3.0"
		String extraArgs
	}

	// DevMode
	DevMode devmode = new DevMode()
	class DevMode {
		boolean noserver = false
		boolean superDevMode = false
		String bindAddress = '127.0.0.1'
		int codeServerPort = 9997
	}


}
