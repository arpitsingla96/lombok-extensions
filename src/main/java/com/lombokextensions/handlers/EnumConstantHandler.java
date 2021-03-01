package com.lombokextensions.handlers;

import com.lombokextensions.EnumConstants;
import com.lombokextensions.Visitor;
import com.lombokextensions.common.Utils;
import com.lombokextensions.exception.StopException;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;
import org.kohsuke.MetaInfServices;

import java.lang.reflect.Modifier;

import static lombok.javac.handlers.JavacHandlerUtil.injectField;


@MetaInfServices(JavacAnnotationHandler.class)
public class EnumConstantHandler extends JavacAnnotationHandler<EnumConstants> {

    public static final String DEFAULT_CONSTANTS_INTERFACE_NAME = "Constants";

    @Override
    public void handle(final AnnotationValues<EnumConstants> annotation,
                       final JCTree.JCAnnotation ast,
                       final JavacNode annotationNode) {
        preProcessCleanup(annotationNode);

        final JavacNode enumNode = annotationNode.up();
        try {
            validateUsage(annotationNode, enumNode);
            JavacNode constantsClassNode = generateConstantsClass(enumNode);
            for (JavacNode enumConstant : Utils.enumConstants(enumNode)) {
                addConstantToClass(constantsClassNode, enumConstant);
            }

        } catch (StopException e) {
            return;
        }
    }

    private JavacNode addConstantToClass(final JavacNode constantsClassNode, final JavacNode enumConstant) {
        JavacTreeMaker treeMaker = constantsClassNode.getTreeMaker();
        Name fName = ((JCTree.JCVariableDecl) enumConstant.get()).name;
        JCTree.JCModifiers constantValueMods = treeMaker.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.FINAL);
        JCTree.JCExpression fieldType = JavacHandlerUtil.genJavaLangTypeRef(enumConstant, "String");
        JCTree.JCExpression init = treeMaker.Literal(fName.toString());

        JCTree.JCVariableDecl constantField = treeMaker.VarDef(constantValueMods, fName, fieldType, init);
        JavacNode fieldNode = injectField(constantsClassNode, constantField);

        return fieldNode;
    }

    private void preProcessCleanup(final JavacNode annotationNode) {
        JavacHandlerUtil.deleteAnnotationIfNeccessary(annotationNode, Visitor.class);

        // Delete any imports here
        // deleteImportFromCompilationUnit()
    }


    private void validateUsage(final JavacNode annotationNode,
                               final JavacNode enumNode) throws StopException {
        if (enumNode == null) {
            annotationNode.addError("@Visitor doesn't have a parent.");
            throw new StopException();
        }

        if (enumNode.getKind() != AST.Kind.TYPE || !Utils.isEnum((JCTree.JCClassDecl) enumNode.get())) {
            annotationNode.addError("@Visitor only supported on enums.");
            throw new StopException();
        }
    }

    private JavacNode generateConstantsClass(final JavacNode enumNode) {
        JavacTreeMaker treeMaker = enumNode.getTreeMaker();

        JCTree.JCClassDecl visitorClass = treeMaker.ClassDef(
                constantsClassModifiers(enumNode),
                constantsClassName(enumNode),
                List.nil(),
                null,
                List.nil(),
                List.nil()
        );

        return JavacHandlerUtil.injectType(enumNode, visitorClass);
    }

    private JCTree.JCModifiers constantsClassModifiers(final JavacNode enumNode) {
        JavacTreeMaker treeMaker = enumNode.getTreeMaker();
        return treeMaker.Modifiers(Modifier.PUBLIC | Modifier.INTERFACE);
    }

    private Name constantsClassName(final JavacNode enumNode) {
        return enumNode.toName(DEFAULT_CONSTANTS_INTERFACE_NAME);
    }

}
