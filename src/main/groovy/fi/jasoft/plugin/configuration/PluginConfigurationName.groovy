package fi.jasoft.plugin.configuration

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Interface for plugin configurations
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface PluginConfigurationName {
    String value() default ""
}
