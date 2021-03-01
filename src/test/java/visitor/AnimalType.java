package visitor;

import com.lombokextensions.EnumConstants;
import com.lombokextensions.VisitorEnum;
import lombok.core.PrintAST;


@PrintAST
@VisitorEnum
@EnumConstants
public enum AnimalType {
    CAT;
}
