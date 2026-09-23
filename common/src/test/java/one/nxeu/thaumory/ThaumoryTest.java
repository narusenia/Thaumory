package one.nxeu.thaumory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThaumoryTest {
    @Test
    void modIdMatchesFabricMetadata() {
        assertEquals("thaumory", Thaumory.MOD_ID);
    }
}
