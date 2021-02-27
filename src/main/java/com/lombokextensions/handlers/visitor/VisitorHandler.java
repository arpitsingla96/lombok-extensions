package com.lombokextensions.handlers.visitor;

import com.lombokextensions.Visitor;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;
import org.kohsuke.MetaInfServices;

import java.util.HashMap;


@MetaInfServices(JavacAnnotationHandler.class)
@HandlerPriority(value = 1, subValue = 1)
public class VisitorHandler extends JavacAnnotationHandler<Visitor> {
    public static HashMap<Type, VisitorClassGenerator> visitorGeneratorMap = new HashMap<>();
    public static HashMap<Type, AcceptorMethodGenerator> acceptorGeneratorMap = new HashMap<>();

    @Override
    public void handle(final AnnotationValues<Visitor> annotationValues,
                       final JCTree.JCAnnotation jcAnnotation,
                       final JavacNode annotationNode) {
        preProcessCleanup(annotationNode);

        final JavacNode baseClassNode = annotationNode.up();

        try {
            validateUsage(annotationNode, baseClassNode);

            VisitorClassGenerator visitorClassGenerator = new VisitorClassGenerator(baseClassNode);
            JavacNode visitorClassNode = visitorClassGenerator.generateEmptyClass();

            AcceptorMethodGenerator acceptorMethodGenerator = new AcceptorMethodGenerator(visitorClassNode);
            acceptorMethodGenerator.generateDeclInBase(baseClassNode);
            visitorGeneratorMap.put(((JCTree.JCClassDecl) baseClassNode.get()).sym.type, visitorClassGenerator);
            acceptorGeneratorMap.put(((JCTree.JCClassDecl) baseClassNode.get()).sym.type, acceptorMethodGenerator);
        } catch (StopException e) {
            return;
        }
    }

    private void preProcessCleanup(final JavacNode annotationNode) {
        JavacHandlerUtil.deleteAnnotationIfNeccessary(annotationNode, Visitor.class);

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