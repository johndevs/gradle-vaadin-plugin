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
package com.devsoap.plugin.configuration

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.objectweb.asm.Opcodes

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Interface for Plugin configurations used as extensions.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(classes = [PluginConfigurationTransformation])
@interface PluginConfiguration { }

/**
 * Transforms the configuration class by adding necessary methods for setting a value
 * without using a equal('=') sign.
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class PluginConfigurationTransformation implements ASTTransformation {

    /**
     * Create a setter method that has a closure as a parameter
     *
     * @param field
     *      the field to make the setter for
     */
    static makeClosureSetterMethod(FieldNode field) {
        new AstBuilder().buildFromSpec {
            method(field.name, Opcodes.ACC_PUBLIC, Object) {
                parameters {
                    parameter 'closure':Object
                    Closure
                }
                exceptions { }
                block {
                    owner.expression.addAll new AstBuilder().buildFromString(
                        'Closure c = closure as Closure\n' +
                        "c.delegate = $field.name\n" +
                        'c.resolveStrategy = Closure.DELEGATE_FIRST\n' +
                        'c.call()'
                    )
                }
                annotations { }
            }
        }.get(0) as MethodNode
    }

    /**
     * Make a setter method that uses the field name e.g if field name is
     * "foo" then setter name is foo(value)
     *
     * @param field
     *      the field to make the setter for
     */
    static makeValueSetterMethod(FieldNode field) {
        new AstBuilder().buildFromSpec {
            method(field.name, Opcodes.ACC_PUBLIC, field.type.typeClass) {
                parameters {
                    parameter 'value':field.type.typeClass
                }
                exceptions { }
                block {
                    owner.expression.addAll new AstBuilder().buildFromString(
                            "this.$field.name = value"
                    )
                }
                annotations { }
            }
        }.get(0) as MethodNode
    }

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if ( !astNodes ) {
            return
        }

        ClassNode clazz = astNodes[1] as ClassNode

        for( FieldNode field:clazz.fields) {
            MethodNode method
            if ( field.final ) {
                method = makeClosureSetterMethod(field)
            } else {
                method = makeValueSetterMethod(field)
            }
            clazz.addMethod(method)
        }
    }
}
