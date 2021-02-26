package visitor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VisitorTest {
    @Test
    public void testAnimalImplAcceptor() {
        Assertions.assertDoesNotThrow(() -> {
            new Animal() {
                @Override
                public <T> T accept(T visitor) {
                    return visitor;
                }
            };
        });
    }

    @Test
    public void testAbstractClassVisitorCreated() {
        Assertions.assertNotNull(Animal.AnimalVisitor.class);
    }

    @Test
    public void testCatAcceptor() {
        Assertions.assertEquals("x", new Cat().accept("x"));
    }
}
