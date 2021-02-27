package visitor;

import lombok.core.PrintAST;

//@Visitor
@PrintAST
public interface Animal2 {
    <T> T accept(final Animal2Visitor<T> visitor);

    public interface Animal2Visitor<T> {
        T visit(final Cat2 cat);
    }
}