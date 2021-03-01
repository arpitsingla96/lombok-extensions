package visitor;

import com.lombokextensions.VisitorEnum;
import lombok.core.PrintAST;


@PrintAST
//@VisitorEnum
public enum Animal2Type {
    CAT {
        @Override
        protected <T> T accept(AnimalTypeVisitor<T> visitor) {
            return visitor.visitCat();
        }
    };

    protected abstract <T> T accept(AnimalTypeVisitor<T> visitor);

    public interface AnimalTypeVisitor<T> {
        T visitCat();
    }
}
