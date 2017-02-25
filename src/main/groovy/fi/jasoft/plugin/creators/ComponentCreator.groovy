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
package fi.jasoft.plugin.creators

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import groovy.transform.Canonical
import org.apache.commons.lang.StringUtils

/**
 * Created by john on 9/12/16.
 */
@Canonical
class ComponentCreator implements Runnable {

    static final String DOT = '.'
    static final String SERVER_PACKAGE = 'server'
    static final String CLIENT_PACKAGE = 'client'

    String widgetset = Util.APP_WIDGETSET
    File javaDir
    String componentName

    @Override
    void run() {

        String widgetsetPackagePath
        String widgetsetPackage
        if ( widgetset.contains(DOT) ) {
            def widgetsetPackageFQN = widgetset.substring(0, widgetset.lastIndexOf(DOT))
            widgetsetPackagePath = TemplateUtil.convertFQNToFilePath(widgetsetPackageFQN)
            def widgetsetName = widgetset.tokenize(DOT).last()
            widgetsetPackage = widgetset.replaceAll("$DOT$widgetsetName", StringUtils.EMPTY)
        } else {
            widgetsetPackagePath = ''
            widgetsetPackage = null
        }

        File widgetsetDir = new File(javaDir, widgetsetPackagePath)
        File componentDir = new File(new File(widgetsetDir, SERVER_PACKAGE), componentName.toLowerCase())
        componentDir.mkdirs()

        File widgetDir = new File(new File(widgetsetDir, CLIENT_PACKAGE), componentName.toLowerCase())
        widgetDir.mkdirs()

        Map<String,String> substitutions = [:]
        substitutions['componentServerPackage'] = "${widgetsetPackage ? widgetsetPackage + DOT:StringUtils.EMPTY}" +
                "$SERVER_PACKAGE.${componentName.toLowerCase()}"
        substitutions['componentClientPackage'] = "${widgetsetPackage ? widgetsetPackage + DOT:StringUtils.EMPTY}" +
                "$CLIENT_PACKAGE.${componentName.toLowerCase()}"
        substitutions['componentName'] = componentName
        substitutions['componentStylename'] = componentName.toLowerCase()

        TemplateUtil.writeTemplate('MyComponent.java', componentDir,
                "${componentName}.java", substitutions)
        TemplateUtil.writeTemplate('MyComponentWidget.java', widgetDir,
                "${componentName}Widget.java", substitutions)
        TemplateUtil.writeTemplate('MyComponentConnector.java', widgetDir,
                "${componentName}Connector.java", substitutions)
    }
}
