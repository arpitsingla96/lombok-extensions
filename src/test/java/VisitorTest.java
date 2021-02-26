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

    @Test
    public void testAbstractClassVisitorCreated() {
        Assertions.assertNotNull(VisitorHelper.VisitorHelperVisitor.class);
    }
}
