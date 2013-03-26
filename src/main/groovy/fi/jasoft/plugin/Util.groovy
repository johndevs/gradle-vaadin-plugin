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
package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection;

class Util {


	public static String readLine(String format) {
		readLine(format, null)
	}

	public static String readLine(String format, Object... args) {
		try{
				if (System.console() != null) {
		       	 	return System.console().readLine(format, args);
		    	}
		   		
		   		System.out.print(String.format(format, args));
		    	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		    	return reader.readLine();

		}catch(IOException ioe){
			// Ignore
		}
    	return null;
	}

    public static FileCollection getClassPath(Project project){
        FileCollection classpath =
            project.configurations.providedCompile +
                    project.configurations.compile +
                    project.configurations.vaadinSources +
                    project.configurations.gwtSources +
                    project.sourceSets.main.runtimeClasspath +
                    project.sourceSets.main.compileClasspath

        project.sourceSets.main.java.srcDirs.each{
            classpath += project.files(it)
        }
        project.sourceSets.main.resources.srcDirs.each{
            classpath += project.files(it)
        }
        return classpath
    }
}