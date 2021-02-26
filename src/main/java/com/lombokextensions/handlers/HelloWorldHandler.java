package com.lombokextensions.handlers;

import com.lombokextensions.HelloWorld;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.core.AnnotationValues;
import lombok.javac.Javac;
import lombok.javac.Javac8BasedLombokOptions;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;
import org.kohsuke.MetaInfServices;

import java.lang.reflect.Modifier;

import static lombok.javac.Javac.CTC_VOID;


@MetaInfServices(JavacAnnotationHandler.class)
public class HelloWorldHandler extends JavacAnnotationHandler<HelloWorld> {

    @Override
    public void handle(final AnnotationValues<HelloWorld> annotation,
                       final JCTree.JCAnnotation ast,
                       final JavacNode annotationNode) {

        Context context = annotationNode.getContext();

        Javac8BasedLombokOptions options = Javac8BasedLombokOptions.replaceWithDelombokOptions(context);
        options.deleteLombokAnnotations();

        // Remove Annotation
        JavacHandlerUtil.deleteAnnotationIfNeccessary(annotationNode, HelloWorld.class);

        // Remove import
//        JavacHandlerUtil.deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");

        JavacNode helloWorldClass = annotationNode.up();
        JCTree.JCMethodDecl method = createHelloWorldMethod(helloWorldClass);
        JavacHandlerUtil.injectMethod(helloWorldClass, method);
    }

    private JCTree.JCMethodDecl createHelloWorldMethod(JavacNode typeNode) {
        JavacTreeMaker treeMaker = typeNode.getTreeMaker();
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Modifier.PUBLIC);
        Name name = typeNode.toName("helloWorld");
        JCTree.JCExpression resType = treeMaker.Type(Javac.createVoidType(treeMaker, CTC_VOID));
        List<JCTree.JCTypeParameter> genericTypes = List.<JCTree.JCTypeParameter>nil();
        List<JCTree.JCVariableDecl> params = List.<JCTree.JCVariableDecl>nil();
        List<JCTree.JCExpression> thrown = List.<JCTree.JCExpression>nil();
        JCTree.JCBlock body = createHelloWorldMethodBody(typeNode);

        return treeMaker.MethodDef(modifiers, name, resType, genericTypes, params, thrown, body, null);
    }

    private JCTree.JCBlock createHelloWorldMethodBody(JavacNode typeNode) {
        JavacTreeMaker treeMaker = typeNode.getTreeMaker();
        JCTree.JCExpression printlnMethod = JavacHandlerUtil.chainDots(typeNode, "System", "out", "println");
        List<JCTree.JCExpression> printlnArgs = List.<JCTree.JCExpression>of(treeMaker.Literal("Hello World!"));
        JCTree.JCMethodInvocation invocation = treeMaker.Apply(List.<JCTree.JCExpression>nil(), printlnMethod, printlnArgs);

        return treeMaker.Block(0, List.<JCTree.JCStatement>of(treeMaker.Exec(invocation)));
    }

}
