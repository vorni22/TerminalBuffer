package terminalbuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TerminalBufferTest {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 24;
    private static final int MAX_SCROLLBACK = 100;

    private TerminalBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
    }

    @Nested
    @DisplayName("Simple tests")
    class ConstructionTests {
        @Test
        @DisplayName("Sample test 00")
        void reportsConfiguredDimensions() {
            
        }
    }

}
