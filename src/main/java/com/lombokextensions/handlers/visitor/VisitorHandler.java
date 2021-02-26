package com.lombokextensions.handlers.visitor;

import com.lombokextensions.Visitor;
import com.lombokextensions.common.Utils;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
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
import java.util.HashMap;


@MetaInfServices(JavacAnnotationHandler.class)
@HandlerPriority(value = 1, subValue = 1)
public class VisitorHandler extends JavacAnnotationHandler<Visitor> {
    private static final String ACCEPTOR_DEFAULT_NAME = "accept";
    private static final String VISITOR_PARAM_DEFAULT_NAME = "visitor";
    private static final String RETURN_TYPE_GENERIC_DEFAULT_NAME = "T";

    public static HashMap<Type, VisitorClassGenerator> visitorBaseClasses = new HashMap<>();

    @Override
    public void handle(final AnnotationValues<Visitor> annotationValues,
                       final JCTree.JCAnnotation jcAnnotation,
                       final JavacNode annotationNode) {
        preProcessCleanup(annotationNode);

        final JavacNode baseClassNode = annotationNode.up();

        try {
            validateUsage(annotationNode, baseClassNode);
            VisitorClassGenerator visitorClassGenerator = new VisitorClassGenerator(baseClassNode);
            visitorClassGenerator.generateEmptyClass();
            addAcceptDeclInBaseClass(baseClassNode);
            visitorBaseClasses.put(((JCTree.JCClassDecl)baseClassNode.get()).sym.type, visitorClassGenerator);
        } catch (StopException e) {
            return;
        }
    }

    private void addAcceptDeclInBaseClass(final JavacNode baseClassNode) throws StopException {
        String acceptorName = ACCEPTOR_DEFAULT_NAME;
        if (Utils.isMethodPresent(baseClassNode, acceptorName)) {
            return;
        }

        long paramFlags = JavacHandlerUtil.addFinalIfNeeded(Flags.PARAMETER, baseClassNode.getContext());

        JCTree.JCClassDecl baseClassDecl = (JCTree.JCClassDecl) baseClassNode.get();
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();
        JCTree.JCModifiers modifiers = modifiersForAcceptorDeclInBaseClass(baseClassNode);
        String t = Utils.generateNonClashingNameFor(RETURN_TYPE_GENERIC_DEFAULT_NAME, baseClassDecl);
        JCTree.JCExpression tType = treeMaker.Ident(baseClassNode.toName(t));
        Name paramName = baseClassNode.toName(VISITOR_PARAM_DEFAULT_NAME);
        JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(paramFlags), paramName, tType, null);
        List<JCTree.JCTypeParameter> generics = List.of(treeMaker.TypeParameter(baseClassNode.toName(t), List.nil()));

        JCTree.JCMethodDecl acceptorMethod = treeMaker.MethodDef(
                modifiers,
                baseClassNode.toName(acceptorName),
                tType,
                generics,
                List.of(param),
                List.nil(),
                null,
                null);

        JavacHandlerUtil.injectMethod(baseClassNode, acceptorMethod);
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

    private JCTree.JCModifiers modifiersForAcceptorDeclInBaseClass(final JavacNode baseClassNode) throws StopException {
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

}