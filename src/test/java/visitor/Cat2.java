package visitor;

import lombok.core.PrintAST;


@PrintAST
//@Visitable
public class Cat2 implements Animal2 {
    @Override
    public <T> T accept(final Animal2Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
