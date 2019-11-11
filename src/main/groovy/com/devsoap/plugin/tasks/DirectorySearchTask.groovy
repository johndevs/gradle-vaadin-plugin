/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.tasks

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * Searches for an addon in the Vaadin Directory
 *
 * @author John Ahlroos
 * @since 1.0
 */
class DirectorySearchTask extends DefaultTask {

    static final String NAME = 'vaadinAddons'
    static final String SPACE = ' '

    private final String directoryUrl = 'https://vaadin.com/Directory/resource/addon/all?detailed=true'

    private final File cachedAddonResponse = project.file('build/cache/addons.json')

    private final int maxCacheAge = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)

    /**
     * Searches for addons using the given search pattern
     */
    @Input
    @Option(option = 'search', description ='String to search for in addons')
    String searchPattern

    /**
     * Sorts the result with the given options
     */
    @Input
    @Option(option = 'sort', description = 'Sort criteria (options:name,description,date,rating)')
    String sortOption

    /**
     * If enabled prints more information in the search results
     */
    @Input
    @Option(option = 'verbose', description = 'Should verbose descriptions be shown')
    Boolean verbose

    DirectorySearchTask() {
        description = "Lists addons in the Vaadin Directory"
    }

    /**
     * Runs the search
     */
    @TaskAction
    void run() {

        String fetchingAddonLog = 'Fetching addon listing from vaadin.com...'
        if ( !cachedAddonResponse.exists() ) {
            project.logger.info(fetchingAddonLog)
            cachedAddonResponse.parentFile.mkdirs()
            cachedAddonResponse.createNewFile()
            cachedAddonResponse.write(directoryUrl.toURL().text)

        } else if ( new Date(cachedAddonResponse.lastModified() ).before(
                new Date(System.currentTimeMillis() - maxCacheAge))) {
            project.logger.info(fetchingAddonLog)
            cachedAddonResponse.write(directoryUrl.toURL().text)
        } else {
            project.logger.info('Reading addon listing from local cache...')
        }

        def args = project.properties
        String search = searchPattern ?: args.get('search', null)
        String sort = sortOption ?: args.get('sort', null)
        Boolean verbose = verbose ?: Boolean.parseBoolean(args.get('verbose', 'false'))
        listAddons(search, sort, verbose)
    }

    private void listAddons(String search, String sort, boolean verbose) {
        def json = new JsonSlurper().parseText(cachedAddonResponse.text)
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        println SPACE

        json.addon.findAll {
            search == null || it.name.toLowerCase().contains(search) || it.summary.toLowerCase().contains(search)

        }.sort {
            switch (sort) {
                case 'name':return it.name
                case 'description':return it.summary
                case 'date':return dateFormat.parse(it.oldestRelease.toString())
                case 'rating':return Double.parseDouble(it.avgRating)
                default:return null
            }

        }.each {
            if ( verbose ) {
                println 'Name: ' + it.name
                println 'Description: ' + it.summary
                println 'Url: ' + it.linkUrl
                println 'Rating: ' + it.avgRating
                if ( it.artifactId != null && it.groupId != null && it.version != null ) {
                    println("Dependency: \"${it.groupId}:${it.artifactId}:${it.version}\"")
                }

            } else {
                print DirectorySearchTask.truncate(it.name.toString(), 29).padRight(30)
                print DirectorySearchTask.truncate(it.summary.toString(), 49).padRight(50)
                if ( it.artifactId != null && it.groupId != null && it.version != null ) {
                    print(" \"${it.groupId}:${it.artifactId}:${it.version}\"")
                }
            }
            println SPACE
        }
    }

    private static String truncate(String str, Integer maxLength) {
        if ( str == null ) {
            return ''
        }
        if ( str.length() > maxLength ) {
            return str[0..(maxLength - 2)] + "\u2026"
        }
        return str
    }
}
