import com.lombokextensions.Visitor;
import lombok.Data;
import lombok.core.PrintAST;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VisitorTest {
    @Test
    public void testAbstractClassAcceptor() {
        Assertions.assertDoesNotThrow(() -> {
            new VisitorHelper() {
                @Override
                public <T> T accept(T visitor) {
                    return visitor;
                }
            };
        });
    }

    @Visitor
    private abstract class VisitorHelper {
    }


}
