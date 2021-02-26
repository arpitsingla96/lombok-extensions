package com.lombokextensions.handlers.visitor;


import com.lombokextensions.common.Utils;
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

    private final JavacNode baseClassNode;

    public VisitorClassGenerator(final JavacNode baseClassNode) {
        this.baseClassNode = baseClassNode;
    }

    public void generateEmptyClass() {
        JavacTreeMaker treeMaker = baseClassNode.getTreeMaker();

        JCTree.JCClassDecl visitorClass = treeMaker.ClassDef(
                visitorClassModifiers(),
                visitorClassName(),
                visitorClassGenerics(),
                null,
                List.nil(),
                List.nil()
        );

        JavacHandlerUtil.injectType(baseClassNode, visitorClass);
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

}
