package fi.jasoft.plugin.creators

import fi.jasoft.plugin.TemplateUtil
import groovy.transform.Canonical

/**
 * Created by john on 9/15/16.
 */
@Canonical
class AddonThemeCreator implements Runnable {

    private File resourceDir

    private String themeName

    private String templateDir

    @Override
    void run() {
        File vaadinDir = new File(resourceDir, 'VAADIN')
        File addonsDir = new File(vaadinDir, 'addons')
        File themeDir = new File(addonsDir, themeName)
        themeDir.mkdirs()

        Map<String, String> substitutions = [:]
        substitutions['themeName'] = themeName
        substitutions['theme'] = themeName.toLowerCase()

        if ( templateDir ) {
            TemplateUtil.writeTemplate("${templateDir}/myaddon.scss", themeDir, "${themeName}.scss", substitutions)
        } else {
            TemplateUtil.writeTemplate('myaddon.scss', themeDir, "${themeName}.scss", substitutions)
        }
    }
}
