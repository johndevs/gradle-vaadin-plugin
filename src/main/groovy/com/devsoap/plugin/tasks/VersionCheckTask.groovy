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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.Util
import groovy.transform.Memoized
import groovyx.net.http.HTTPBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

import java.util.concurrent.TimeUnit

/**
 * Checks the plugin version for a new version
 *
 *  @author John Ahlroos
 *  @since 1.2.0
 */
@ParallelizableTask
class VersionCheckTask extends DefaultTask {

    static final String URL = "https://plugins.gradle.org/plugin/$GradleVaadinPlugin.pluginId"

    static final String NAME = "vaadinPluginVersionCheck"

    static final long CACHE_TIME = TimeUnit.DAYS.toMillis(1)

    File versionCacheFile

    VersionCheckTask() {
        project.afterEvaluate {
            versionCacheFile = new File(project.buildDir, '.vaadin-gradle-plugin-version.check')
            boolean firstRun = false
            if(!versionCacheFile.exists()) {
                versionCacheFile.parentFile.mkdirs()
                versionCacheFile.createNewFile()
                firstRun = true
            }

            inputs.file(versionCacheFile)
            outputs.file(versionCacheFile)

            long cacheAge = System.currentTimeMillis() - versionCacheFile.lastModified()
            outputs.upToDateWhen { !firstRun && cacheAge < CACHE_TIME}
            onlyIf { firstRun || cacheAge > CACHE_TIME }
        }
    }

    @TaskAction
    void run() {
        VersionNumber pluginVersion = VersionNumber.parse(GradleVaadinPlugin.version)
        if(latestReleaseVersion > pluginVersion){
            project.logger.warn "!! A newer version of the Gradle Vaadin plugin is available, " +
                    "please upgrade to $latestReleaseVersion !!"
        }
        versionCacheFile.text = latestReleaseVersion.toString()
    }

    /**
     * Gets the latest released Gradle plugin version
     *
     * @return
     *      the latest released version number
     */
    @Memoized
    static VersionNumber getLatestReleaseVersion() {
        VersionNumber version = VersionNumber.UNKNOWN
        try {
            HTTPBuilder http = Util.configureHttpBuilder(new HTTPBuilder(URL))
            def html = http.get([:])
            def matcher = html =~ /Version (\S+)/
            if (matcher.find()) {
                version = VersionNumber.parse(matcher.group(1))
            }
        } catch (IOException | URISyntaxException e){
            version = VersionNumber.UNKNOWN
        }
        version
    }
}
