package com.lombokextensions.handlers.visitor;


import com.lombokextensions.common.Utils;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;

import java.lang.reflect.Modifier;

public class VisitorClassGenerator {
    private static final String DEFAULT_VISITOR_CLASS_PREFIX = "Visitor";
    private static final String DEFAULT_VISITOR_CLASS_GENERIC = "T";
    private static final String DEFAULT_VISIT_METHOD_NAME = "visit";

    private final JavacNode baseClassNode;
    private final VisitMethodNameProvider visitMethodNameProvider;
    private JavacNode visitorClassNode;

    public VisitorClassGenerator(final JavacNode baseClassNode,
                                 final VisitMethodNameProvider visitMethodNameProvider) {
        this.baseClassNode = baseClassNode;
        this.visitMethodNameProvider = visitMethodNameProvider;
    }

    public JavacNode generateEmptyClass() {
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();

        JCTree.JCClassDecl visitorClass = treeMaker.ClassDef(
                visitorClassModifiers(),
                visitorClassName(),
                visitorClassGenerics(),
                null,
                List.nil(),
                List.nil()
        );

        visitorClassNode = JavacHandlerUtil.injectType(baseClassNode, visitorClass);

        return visitorClassNode;
    }

    public String addVisitMethod(final JavacNode childClassNode) {
        String visitMethodName = DEFAULT_VISIT_METHOD_NAME;
        JavacTreeMaker treeMaker = visitorClassNode.getTreeMaker();
        JCTree.JCClassDecl childClassDecl = (JCTree.JCClassDecl) childClassNode.get();

        long paramFlags = JavacHandlerUtil.addFinalIfNeeded(Flags.PARAMETER, visitorClassNode.getContext());
        JCTree.JCExpression paramType = treeMaker.Ident(childClassDecl.name);
        String paramNameString = Utils.variableNameForClass(childClassNode);
        Name paramName = visitorClassNode.toName(paramNameString);
        JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(paramFlags), paramName, paramType, null);

        JCTree.JCMethodDecl visitMethod = generateVisitMethodDef(List.of(param), visitMethodName);

        JavacHandlerUtil.injectMethod(visitorClassNode, visitMethod);

        return visitMethodName;
    }

    public String addVisitMethodForEnum(final JavacNode enumConstant) {
        String visitMethodName = DEFAULT_VISIT_METHOD_NAME + Utils.snakeCaseToCamelCase(enumConstant.getName());

        JCTree.JCMethodDecl visitMethod = generateVisitMethodDef(List.nil(), visitMethodName);
        JavacHandlerUtil.injectMethod(visitorClassNode, visitMethod);

        return visitMethodName;
    }

    private JCTree.JCMethodDecl generateVisitMethodDef(final List<JCTree.JCVariableDecl> params, final String methodName) {
        JavacTreeMaker treeMaker = visitorClassNode.getTreeMaker();
        JCTree.JCClassDecl visitorClassDecl = (JCTree.JCClassDecl) visitorClassNode.get();

        JCTree.JCModifiers modifiers = treeMaker.Modifiers(0L);
        JCTree.JCExpression returnType = treeMaker.Ident(visitorClassDecl.getTypeParameters().get(0).name);

        Name name = visitorClassNode.toName(methodName);

        return treeMaker.MethodDef(
                modifiers,
                name,
                returnType,
                List.nil(),
                params,
                List.nil(),
                null,
                null
        );

    }

    private JCTree.JCModifiers visitorClassModifiers() {
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();
        return treeMaker.Modifiers(Modifier.PUBLIC | Modifier.INTERFACE);
    }

    private Name visitorClassName() {
        String name = String.format("%s%s", baseClassNode.getName(), DEFAULT_VISITOR_CLASS_PREFIX);
        return baseClassNode.toName(name);
    }

    private List<JCTree.JCTypeParameter> visitorClassGenerics() {
        JCTree.JCClassDecl baseClassDecl = (JCTree.JCClassDecl) baseClassNode.get();
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();

        String t = Utils.generateNonClashingNameFor(DEFAULT_VISITOR_CLASS_GENERIC, baseClassDecl);

        return List.of(treeMaker.TypeParameter(baseClassNode.toName(t), List.nil()));
    }

    public interface VisitMethodNameProvider {
        public String provide(String defaultName);
    }
}
