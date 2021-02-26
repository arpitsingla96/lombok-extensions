package com.lombokextensions.handlers.visitor;

import com.lombokextensions.Visitable;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;
import org.kohsuke.MetaInfServices;

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

    private List<VisitorClassGenerator> getVisitors(JavacNode childClassNode) {
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
}
