package visitor;

import com.lombokextensions.VisitorEnum;
import lombok.core.PrintAST;


@PrintAST
@VisitorEnum
public enum AnimalType {
    CAT;

    public static final String SHIT = "SHIT";
}
