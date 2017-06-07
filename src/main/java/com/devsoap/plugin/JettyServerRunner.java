/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class JettyServerRunner {

    public static final String SERVER_STARTING_TOKEN = "Jetty starting";
    public static final String SERVER_STARTED_TOKEN = "Jetty started";
    public static final String SERVER_STOPPING_TOKEN = "Jetty stopping";
    public static final String SERVER_STOPPED_TOKEN = "Jetty stopped";
    public static final String SERVER_FAILED_TOKEN = "Jetty error";

    // Usage: 'JettyServerRunner [port] [webbappdir] [classesdir] [resourcesdir] [LogLevel]'
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String webAppDir = args[1];
        String classesDir = args[2];
        String resourcesDir = args[3];
        //String logLevel = args[4];

        List<String> resources = new ArrayList<>();
        if (new File(webAppDir).exists()){
            resources.add(webAppDir);
        }

        // By default always log with info
        if(System.getProperty("org.eclipse.jetty.LEVEL") == null ||
                System.getProperty("org.eclipse.jetty.LEVEL").equals("")){
            System.setProperty("org.eclipse.jetty.LEVEL", "INFO");
        }

        Server server = new Server(port);

        // Static file handler
        WebAppContext handler = new WebAppContext();
        server.setHandler(handler);

        handler.setContextPath("/");
        handler.setBaseResource(Resource.newResource(webAppDir));
        handler.setParentLoaderPriority(true);
        handler.setExtraClasspath(classesDir+";"+resourcesDir);
        handler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/build/classes/.*");
        handler.setConfigurations(new Configuration[]{
                new WebXmlConfiguration(),
                new WebInfConfiguration(),
                new PlusConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new AnnotationConfiguration(),
                new JettyWebXmlConfiguration()
        });
        handler.setClassLoader(new WebAppClassLoader(JettyServerRunner.class.getClassLoader(), handler));

        server.addLifeCycleListener(new LifeCycle.Listener() {
            @Override
            public void lifeCycleStarting(LifeCycle event) {
                Logger.getLogger(JettyServerRunner.class.getName()).info(SERVER_STARTING_TOKEN);
            }

            @Override
            public void lifeCycleStarted(LifeCycle event) {
                Logger.getLogger(JettyServerRunner.class.getName()).info(SERVER_STARTED_TOKEN);
            }

            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) {
                Logger.getLogger(JettyServerRunner.class.getName()).info(SERVER_FAILED_TOKEN);
            }

            @Override
            public void lifeCycleStopping(LifeCycle event) {
                Logger.getLogger(JettyServerRunner.class.getName()).info(SERVER_STOPPING_TOKEN);
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                Logger.getLogger(JettyServerRunner.class.getName()).info(SERVER_STOPPED_TOKEN);
            }
        });

        server.start();
        server.join();
    }
}
