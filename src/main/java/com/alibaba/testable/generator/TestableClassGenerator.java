package com.alibaba.testable.generator;

import com.alibaba.testable.generator.model.Statement;
import com.alibaba.testable.generator.statement.CallSuperMethodStatementGenerator;
import com.alibaba.testable.model.TestableContext;
import com.alibaba.testable.translator.EnableTestableInjectTranslator;
import com.alibaba.testable.util.ConstPool;
import com.squareup.javapoet.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generate testable class code
 *
 * @author flin
 */
public class TestableClassGenerator {

    private final TestableContext cx;

    public TestableClassGenerator(TestableContext cx) {
        this.cx = cx;
    }

    public String fetch(Symbol.ClassSymbol clazz, String packageName, String className) {
        JCTree tree = cx.trees.getTree(clazz);
        EnableTestableInjectTranslator translator = new EnableTestableInjectTranslator(cx);
        tree.accept(translator);

        List<MethodSpec> methodSpecs = new ArrayList<>();
        for (JCMethodDecl method : translator.getMethods()) {
            if (isNoncallableMethod(method)) {
                continue;
            }
            if (isConstructorMethod(method)) {
                methodSpecs.add(buildConstructorMethod(clazz, method));
            } else {
                methodSpecs.add(buildMemberMethod(clazz, method));
            }
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(clazz.asType());
        for (MethodSpec m : methodSpecs) {
            builder.addMethod(m);
        }
        TypeSpec testableClass = builder.build();
        JavaFile javaFile = JavaFile.builder(packageName, testableClass).build();
        return javaFile.toString();
    }

    private MethodSpec buildMemberMethod(Element classElement, JCMethodDecl method) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name.toString())
            .addModifiers(toPublicFlags(method.getModifiers()))
            .returns(TypeName.get(((Type.MethodType)method.sym.type).restype));
        for (JCVariableDecl p : method.getParameters()) {
            builder.addParameter(getParameterSpec(p));
        }
        if (method.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
            builder.addException(Exception.class);
        } else {
            builder.addAnnotation(Override.class);
            for (JCExpression exception : method.getThrows()) {
                builder.addException(TypeName.get(exception.type));
            }
        }
        addCallSuperStatements(builder, classElement, method);
        return builder.build();
    }

    private MethodSpec buildConstructorMethod(Element classElement, JCMethodDecl method) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        for (JCVariableDecl p : method.getParameters()) {
            builder.addParameter(getParameterSpec(p));
        }
        addCallSuperStatements(builder, classElement, method);
        return builder.build();
    }

    private void addCallSuperStatements(MethodSpec.Builder builder, Element classElement, JCMethodDecl method) {
        String className = classElement.getSimpleName().toString();
        Statement[] statements = new CallSuperMethodStatementGenerator().fetch(className, method);
        for (Statement s : statements) {
            builder.addStatement(s.getLine(), s.getParams());
        }
    }

    private boolean isConstructorMethod(JCMethodDecl method) {
        return method.name.toString().equals(ConstPool.CONSTRUCTOR_NAME);
    }

    private boolean isNoncallableMethod(JCMethodDecl method) {
        return method.getModifiers().getFlags().contains(Modifier.ABSTRACT);
    }

    private Set<Modifier> toPublicFlags(JCModifiers modifiers) {
        Set<Modifier> flags = new HashSet<>(modifiers.getFlags());
        flags.remove(Modifier.PRIVATE);
        flags.remove(Modifier.PROTECTED);
        flags.add(Modifier.PUBLIC);
        return flags;
    }

    private ParameterSpec getParameterSpec(JCVariableDecl type) {
        return ParameterSpec.builder(TypeName.get(type.sym.type), type.name.toString()).build();
    }

}
