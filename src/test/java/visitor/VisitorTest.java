package visitor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VisitorTest {
    @Test
    public void testAbstractClassVisitorCreated() {
        Assertions.assertNotNull(Animal.AnimalVisitor.class);
    }

    @Test
    public void testCatAcceptor() {
        String meow = new Cat().accept(new Animal.AnimalVisitor<String>() {
            @Override
            public String visit(Cat cat) {
                return "Meow";
            }
        });

        Assertions.assertEquals("Meow", meow);
    }
}
