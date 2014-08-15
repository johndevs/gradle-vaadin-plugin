/*
* Copyright 2014 John Ahlroos
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

import groovy.util.logging.Log
import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.jmx.MBeanContainer
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.*
import java.lang.management.ManagementFactory

class ApplicationServerRunner {

    // Usage: 'ApplicationServerRunner [port] [webbappdir] [classesdir]'
    def static main(args) {
        def port = args[0]
        def webAppDir = args[1]
        def classesDir = args[2]

        def resources = []
        if((webAppDir as File).exists()){
            resources << webAppDir
        }

        // For debugging
        //System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");

        def server = new Server(port as int);
    
        // Setup JMX
        def mbContainer = [ManagementFactory.getPlatformMBeanServer()] as MBeanContainer;
        server.addBean(mbContainer);

        // Static file handler
        server.handler = new WebAppContext();
        server.handler.contextPath = '/'
        server.handler.baseResource = new ResourceCollection(resources as String[])
        server.handler.parentLoaderPriority = true
        server.handler.extraClasspath = classesDir
        server.handler.attributes.setAttribute('org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern', '.*/build/classes/.*')
        server.handler.configurations = [
                new WebXmlConfiguration(),
                new WebInfConfiguration(),
                new PlusConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new AnnotationConfiguration(),
                new JettyWebXmlConfiguration(),
        ]
        server.handler.classLoader = [
                ApplicationServerRunner.getClass().classLoader,
                server.handler as WebAppContext
        ] as WebAppClassLoader

        server.start();
        server.join();
    }
}
