package com.lombokextensions.handlers.visitor;

import com.lombokextensions.Visitable;
import com.lombokextensions.common.Utils;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;
import org.kohsuke.MetaInfServices;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


@MetaInfServices(JavacAnnotationHandler.class)
@HandlerPriority(value = 1, subValue = 2)
public class VisitableHandler extends JavacAnnotationHandler<Visitable> {
    @Override
    public void handle(final AnnotationValues<Visitable> annotationValues,
                       final JCTree.JCAnnotation jcAnnotation,
                       final JavacNode annotationNode) {
        preProcessCleanup(annotationNode);
        final JavacNode childClassNode = annotationNode.up();

        try {
            validateUsage(annotationNode, childClassNode);
            List<VisitorClassGenerator> visitorClassGenerators = getVisitors(childClassNode);

            for (VisitorClassGenerator visitorClassGenerator : visitorClassGenerators) {
                visitorClassGenerator.addVisitMethod(childClassNode);
                addAcceptorOverride(childClassNode);
            }

        } catch (StopException e) {
            return;
        }

    }

    private void preProcessCleanup(final JavacNode annotationNode) {
        JavacHandlerUtil.deleteAnnotationIfNeccessary(annotationNode, Visitable.class);

        // Delete any imports here
        // deleteImportFromCompilationUnit()
    }

    private void validateUsage(final JavacNode annotationNode,
                               final JavacNode childClassNode) throws StopException {
        if (childClassNode == null) {
            annotationNode.addError("@Visitor doesn't have a parent.");
            throw new StopException();
        }

        if (childClassNode.getKind() != AST.Kind.TYPE) {
            annotationNode.addError("@Visitor only supported on a class.");
            throw new StopException();
        }
    }

    private List<VisitorClassGenerator> getVisitors(final JavacNode childClassNode) {
        JCTree.JCClassDecl childClassDecl = (JCTree.JCClassDecl) childClassNode.get();
        java.util.List<Type> closures = Types.instance(childClassNode.getAst().getContext()).closure(childClassDecl.sym.type);

        java.util.List<VisitorClassGenerator> visitorClassGenerators = new ArrayList<>();
        for (Type closure : closures) {
            if (VisitorHandler.visitorBaseClasses.containsKey(closure)) {
                visitorClassGenerators.add(VisitorHandler.visitorBaseClasses.get(closure));
            }
        }

        return visitorClassGenerators;
    }

    private void addAcceptorOverride(final JavacNode childClassNode) {
        String acceptorName = "accept";
        String returnTypeGeneric = "T";
        String paramDefaultName = "visitor";
        JavacTreeMaker treeMaker = childClassNode.getTreeMaker();
        JCTree.JCClassDecl childClassDecl = (JCTree.JCClassDecl) childClassNode.get();

        if (Utils.isMethodPresent(childClassNode, acceptorName)) {
            return;
        }

        long paramFlags = JavacHandlerUtil.addFinalIfNeeded(Flags.PARAMETER, childClassNode.getContext());

        JCTree.JCAnnotation overrideAnnotation = treeMaker.Annotation(JavacHandlerUtil.genJavaLangTypeRef(childClassNode, "Override"), com.sun.tools.javac.util.List.<JCTree.JCExpression>nil());

        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Modifier.PUBLIC, com.sun.tools.javac.util.List.of(overrideAnnotation));
        Name name = childClassNode.toName(acceptorName);
        String t = Utils.generateNonClashingNameFor(returnTypeGeneric, childClassDecl);
        JCTree.JCExpression tType = treeMaker.Ident(childClassNode.toName(t));
        Name paramName = childClassNode.toName(paramDefaultName);

        // TODO: This is buggy
        JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(paramFlags), paramName, tType, null);
        com.sun.tools.javac.util.List<JCTree.JCTypeParameter> generics = com.sun.tools.javac.util.List.of(treeMaker.TypeParameter(childClassNode.toName(t), com.sun.tools.javac.util.List.nil()));

        JCTree.JCMethodDecl acceptorMethod = treeMaker.MethodDef(
                modifiers,
                name,
                tType,
                generics,
                com.sun.tools.javac.util.List.of(param),
                com.sun.tools.javac.util.List.nil(),
                createHelloWorldMethodBody(childClassNode, param),
                null
                );

        JavacHandlerUtil.injectMethod(childClassNode, acceptorMethod);
    }

    private JCTree.JCBlock createHelloWorldMethodBody(JavacNode childClassNode, JCTree.JCVariableDecl param) {
        JavacTreeMaker treeMaker = childClassNode.getTreeMaker();

        JCTree.JCReturn returnStatement = treeMaker.Return(treeMaker.Ident(childClassNode.toName("visitor")));

        return treeMaker.Block(0, com.sun.tools.javac.util.List.of(returnStatement));
    }

}
