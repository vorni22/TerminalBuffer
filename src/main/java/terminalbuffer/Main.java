package terminalbuffer;

public class Main {
    public static void main(String[] args) {
        TerminalBuffer buffer = new TerminalBuffer(10, 4, 10);

        // Write some text
        buffer.write("HelloWorld");
        buffer.debugPrint();

        // Insert text at cursor
        buffer.setScreenCursorPos(0, 5);
        buffer.insert("123");
        buffer.debugPrint();

        // Push lines to scrollback
        buffer.pushScreenLine();
        buffer.pushScreenLine();
        buffer.debugPrint();

        // Fill a line
        buffer.setScreenCursorPos(2, 0);
        buffer.fillScreenLine('X');
        buffer.debugPrint();

        // Resize buffer
        buffer.resize(5, 6);
        buffer.debugPrint();

        // Resize buffer
        buffer.resize(10, 4);
        buffer.debugPrint();

        // Resize buffer
        buffer.resize(13, 4);
        buffer.debugPrint();

        // Resize buffer
        buffer.resize(15, 5);
        buffer.debugPrint();

        // Clear screen
        buffer.clearScreen();
        buffer.debugPrint();

        // Clear all
        buffer.clearAll();
        buffer.debugPrint();
    }
}