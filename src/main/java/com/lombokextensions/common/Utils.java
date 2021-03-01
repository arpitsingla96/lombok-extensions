package com.lombokextensions.common;


import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import lombok.core.AST;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacHandlerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Utils {
    public static boolean isAbstractClass(JCTree.JCClassDecl classDecl) {
        return (classDecl.mods.flags & Flags.ABSTRACT) != 0L;
    }

    public static boolean isInterface(JCTree.JCClassDecl classDecl) {
        return (classDecl.mods.flags & Flags.INTERFACE) != 0L;
    }

    public static boolean isEnum(JCTree.JCClassDecl classDecl) {
        return Flags.isEnum(classDecl.sym);
    }

    public static boolean isMethodPresent(final JavacNode baseClassNode,
                                          final String methodName) {
        switch (JavacHandlerUtil.methodExists(methodName, baseClassNode, 1)) {
            case EXISTS_BY_LOMBOK:
                return true;
            case EXISTS_BY_USER:
                // TODO: Warnings don't work.
                baseClassNode.addError(
                        String.format("Not generating %s: A method with that name already exists.", methodName)
                );
                return true;
            case NOT_EXISTS:
            default:
                return false;
        }
    }

    public static HashSet<String> gatherUsedTypeNames(final JCTree.JCClassDecl classDecl) {
        java.util.HashSet<String> usedNames = new HashSet<String>();
        // 1. Add class name.
        usedNames.add(classDecl.name.toString());

        // 2. Add used type names.
        for (JCTree member : classDecl.getMembers()) {
            if (member.getKind() == com.sun.source.tree.Tree.Kind.VARIABLE && member instanceof JCTree.JCVariableDecl) {
                JCTree type = ((JCTree.JCVariableDecl) member).getType();
                if (type instanceof JCTree.JCIdent)
                    usedNames.add(((JCTree.JCIdent) type).getName().toString());
            }
        }

        return usedNames;
    }

    public static String generateNonClashingNameFor(final String classGenericName,
                                                    final java.util.HashSet<String> typeParamStrings) {
        if (!typeParamStrings.contains(classGenericName)) return classGenericName;
        int counter = 2;
        while (typeParamStrings.contains(classGenericName + counter)) counter++;
        return classGenericName + counter;
    }

    public static String generateNonClashingNameFor(final String classGenericName,
                                                    final JCTree.JCClassDecl classDecl) {
        return generateNonClashingNameFor(classGenericName, gatherUsedTypeNames(classDecl));
    }

    public static String titleCaseToCamelCase(final String titleCase) {
        if (titleCase == null || titleCase.isEmpty()) {
            return titleCase;
        }

        if (titleCase.charAt(0) >= 'A' && titleCase.charAt(0) <= 'Z') {
            String firstChar = String.valueOf((char) (titleCase.charAt(0) - 'A' + 'a'));
            return firstChar + titleCase.substring(1);
        }

        return titleCase;
    }

    public static String snakeCaseToCamelCase(final String camelCase) {
        StringBuilder builder = new StringBuilder();

        boolean capital = true;

        for (char c : camelCase.toCharArray()) {
            if (c == '_') {
                capital = true;
                continue;
            }

            if (capital) {
                builder.append(Character.toUpperCase(c));
                capital = false;
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }

        return builder.toString();
    }

    public static String variableNameForClass(final JavacNode classNode) {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) classNode.get();
        String className = classDecl.name.toString();
        String camelCaseClassName = titleCaseToCamelCase(className);

        if (!camelCaseClassName.equals(className)) {
            return camelCaseClassName;
        } else {
            return className + "0";
        }
    }

    public static List<JCTree.JCVariableDecl> enumConstants(final JCTree.JCClassDecl classDecl) {
        List<JCTree.JCVariableDecl> enumConstants = new ArrayList<>();
        for (JCTree tree : classDecl.getMembers()) {
            tree.accept(new JCTree.Visitor() {
                @Override
                public void visitVarDef(JCTree.JCVariableDecl that) {
                    if (JavacHandlerUtil.isEnumConstant(that)) {
                        enumConstants.add(that);
                    }
                }

                @Override
                public void visitTree(JCTree that) {
                }
            });
        }

        return enumConstants;
    }

    public static List<JavacNode> enumConstants(final JavacNode enumNode) {
        List<JavacNode> enumConstants = new ArrayList<>();

        for (JavacNode child : enumNode.down()) {
            if (child.getKind() != AST.Kind.FIELD) continue;
            JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) child.get();
            if (!JavacHandlerUtil.isEnumConstant(variableDecl)) continue;

            enumConstants.add(child);
        }

        return enumConstants;
    }

    public static JCTree.JCExpression copyType(JavacTreeMaker treeMaker, JCTree.JCVariableDecl fieldNode) {
        return fieldNode.type != null ? treeMaker.Type(fieldNode.type) : fieldNode.vartype;
    }
}
