package com.lombokextensions.handlers.visitor;

import com.lombokextensions.common.Utils;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;

import java.lang.reflect.Modifier;

public class AcceptorMethodGenerator {
    private static final String ACCEPTOR_DEFAULT_NAME = "accept";
    private static final String VISITOR_PARAM_DEFAULT_NAME = "visitor";
    private static final String RETURN_TYPE_GENERIC_DEFAULT_NAME = "T";

    private final JavacNode visitorClassNode;

    public AcceptorMethodGenerator(final JavacNode visitorClassNode) {
        this.visitorClassNode = visitorClassNode;
    }

    public void generateDeclInBase(JavacNode baseClassNode) {
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();
        Name genericName = genericName(baseClassNode);
        JCTree.JCVariableDecl visitorParam = visitorParam(baseClassNode, genericName);

        JCTree.JCMethodDecl acceptorMethod;
        try {
            acceptorMethod = treeMaker.MethodDef(
                    modifiersForBaseDecl(baseClassNode),
                    methodName(baseClassNode),
                    returnType(baseClassNode, genericName),
                    generics(baseClassNode, genericName),
                    List.of(visitorParam),
                    List.nil(),
                    null,
                    null
            );
        } catch (StopException e) {
            return;
        }

        JavacHandlerUtil.injectMethod(baseClassNode, acceptorMethod);
    }

    public void generateImplInChild(final JavacNode childClassNode) {
        JavacTreeMaker treeMaker = childClassNode.getTreeMaker();
        Name genericName = genericName(childClassNode);
        JCTree.JCVariableDecl visitorParam = visitorParam(childClassNode, genericName);

        JCTree.JCMethodDecl acceptorMethod;
        acceptorMethod = treeMaker.MethodDef(
                modifiersForChildImpl(childClassNode),
                methodName(childClassNode),
                returnType(childClassNode, genericName),
                generics(childClassNode, genericName),
                List.of(visitorParam),
                List.nil(),
                childImplBody(childClassNode, visitorParam),
                null
        );

        JavacHandlerUtil.injectMethod(childClassNode, acceptorMethod);
    }

    private JCTree.JCModifiers modifiersForBaseDecl(JavacNode baseClassNode) throws StopException {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) baseClassNode.get();
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();

        JCTree.JCModifiers modifiers;
        if (Utils.isAbstractClass(classDecl) || Utils.isEnum(classDecl)) {
            modifiers = treeMaker.Modifiers(Modifier.PROTECTED | Modifier.ABSTRACT);
        } else if (Utils.isInterface(classDecl)) {
            modifiers = treeMaker.Modifiers(0L);
        } else {
            baseClassNode.addError("@Visitor not supported on this type.");
            throw new StopException();
        }

        return modifiers;
    }

    private JCTree.JCModifiers modifiersForChildImpl(final JavacNode childClassNode) {
        JavacTreeMaker treeMaker = childClassNode.getTreeMaker();
        JCTree.JCAnnotation overrideAnnotation = treeMaker.Annotation(
                JavacHandlerUtil.genJavaLangTypeRef(childClassNode, "Override"),
                List.nil()
        );
        return treeMaker.Modifiers(Modifier.PUBLIC, List.of(overrideAnnotation));
    }

    private Name methodName(JavacNode node) {
        return node.toName(ACCEPTOR_DEFAULT_NAME);
    }

    private Name genericName(JavacNode classNode) {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) classNode.get();
        String t = Utils.generateNonClashingNameFor(RETURN_TYPE_GENERIC_DEFAULT_NAME, classDecl);
        return classNode.toName(t);
    }

    private JCTree.JCExpression returnType(JavacNode classNode, Name genericName) {
        JavacTreeMaker treeMaker = classNode.getTreeMaker();
        return treeMaker.Ident(genericName);
    }

    private List<JCTree.JCTypeParameter> generics(JavacNode classNode, Name genericName) {
        JavacTreeMaker treeMaker = classNode.getTreeMaker();
        return List.of(treeMaker.TypeParameter(genericName, List.nil()));
    }

    private JCTree.JCVariableDecl visitorParam(JavacNode classNode, Name genericName) {
        JavacTreeMaker treeMaker = classNode.getTreeMaker();
        Name paramName = classNode.toName(VISITOR_PARAM_DEFAULT_NAME);
        long paramFlags = JavacHandlerUtil.addFinalIfNeeded(Flags.PARAMETER, classNode.getContext());

        return treeMaker.VarDef(
                treeMaker.Modifiers(paramFlags),
                paramName,
                treeMaker.Ident(genericName),
                null);
    }

    private JCTree.JCBlock childImplBody(JavacNode classNode, JCTree.JCVariableDecl visitorParam) {
        JavacTreeMaker treeMaker = classNode.getTreeMaker();
        JCTree.JCReturn returnStatement = treeMaker.Return(treeMaker.Ident(visitorParam.name));
        return treeMaker.Block(0, List.of(returnStatement));
    }

}
