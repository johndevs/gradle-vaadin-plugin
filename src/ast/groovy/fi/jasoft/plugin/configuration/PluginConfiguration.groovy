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
package fi.jasoft.plugin.configuration

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

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(classes = [PluginConfigurationTransformation.class])
public @interface PluginConfiguration { }

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class PluginConfigurationTransformation implements ASTTransformation {

    def createClosureSetterMethod(FieldNode field) {
        new AstBuilder().buildFromSpec {
            method(field.name, Opcodes.ACC_PUBLIC, Object, {
                parameters {
                    parameter 'closure': Object.class
                    Closure
                }
                exceptions {}
                block {
                    owner.expression.addAll new AstBuilder().buildFromString(
                        "def c = closure as Closure\n" +
                        "c.delegate = "+field.name+"\n" +
                        "c.resolveStrategy = Closure.DELEGATE_FIRST\n" +
                        "c.call()"
                    )
                }
                annotations {}
            })
        }[0] as MethodNode
    }

    def createValueSetterMethod(FieldNode field) {
        new AstBuilder().buildFromSpec {
            method(field.name, Opcodes.ACC_PUBLIC, field.type.typeClass, {
                parameters {
                    parameter 'value': field.type.typeClass
                }
                exceptions {}
                block {
                    owner.expression.addAll new AstBuilder().buildFromString(
                            "this."+field.name+" = value"
                    )
                }
                annotations {}
            })
        }[0] as MethodNode
    }

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if(!astNodes) return

        def clazz = astNodes[1] as ClassNode

        for(FieldNode field : clazz.fields){
            def MethodNode method
            if(field.final) {
                method = createClosureSetterMethod(field)
            } else {
                method = createValueSetterMethod(field)
            }
            clazz.addMethod(method)
        }
    }
}