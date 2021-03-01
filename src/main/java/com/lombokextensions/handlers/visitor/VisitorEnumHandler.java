package com.lombokextensions.handlers.visitor;

import com.lombokextensions.VisitorEnum;
import com.lombokextensions.common.Utils;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.tree.JCTree;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;
import org.kohsuke.MetaInfServices;

import java.util.List;


@MetaInfServices(JavacAnnotationHandler.class)
public class VisitorEnumHandler extends JavacAnnotationHandler<VisitorEnum> {
    @Override
    public void handle(final AnnotationValues<VisitorEnum> annotation,
                       final JCTree.JCAnnotation jcAnnotation,
                       final JavacNode annotationNode) {
        preProcessCleanup(annotationNode);

        final JavacNode enumNode = annotationNode.up();
        try {
            validateUsage(annotationNode, enumNode);

            VisitorClassGenerator visitorClassGenerator = new VisitorClassGenerator(enumNode, new VisitorClassGenerator.VisitMethodNameProvider() {
                @Override
                public String provide(String defaultName) {
                    return defaultName;
                }
            });
            JavacNode visitorClassNode = visitorClassGenerator.generateEmptyClass();

            AcceptorMethodGenerator acceptorMethodGenerator = new AcceptorMethodGenerator(visitorClassNode);
            acceptorMethodGenerator.generateDeclInBase(enumNode);

            for (JavacNode enumConstant : Utils.enumConstants(enumNode)) {
                String visitMethodName = visitorClassGenerator.addVisitMethodForEnum(enumConstant);
                acceptorMethodGenerator.generateImplInEnumConstant(enumConstant, visitMethodName, enumNode);
            }

        } catch (StopException e) {
            return;
        }
    }

    private void preProcessCleanup(final JavacNode annotationNode) {
        JavacHandlerUtil.deleteAnnotationIfNeccessary(annotationNode, VisitorEnum.class);

        // Delete any imports here
        // deleteImportFromCompilationUnit()
    }

    private void validateUsage(final JavacNode annotationNode,
                               final JavacNode baseClassNode) throws StopException {
        if (baseClassNode == null) {
            annotationNode.addError("@Visitor doesn't have a parent.");
            throw new StopException();
        }

        if (baseClassNode.getKind() != AST.Kind.TYPE) {
            annotationNode.addError("@Visitor only supported on a class.");
            throw new StopException();
        }
    }

}
