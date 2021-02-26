package com.lombokextensions.common;


import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;

import java.util.HashSet;

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
                JCTree type = ((JCTree.JCVariableDecl)member).getType();
                if (type instanceof JCTree.JCIdent)
                    usedNames.add(((JCTree.JCIdent)type).getName().toString());
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

}
