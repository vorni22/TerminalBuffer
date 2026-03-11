# TerminalBuffer

A Java library for managing a terminal emulator's text buffer, supporting scrollback, screen, and advanced editing operations.

## Features

- **Merged Buffer Model:**  
  Maintains a single list of logical lines, with a logical split between scrollback and screen. Logical lines can straddle the boundary without being split.

- **Scrollback Management:**  
  Automatically manages scrollback size, removing the oldest lines or empty padding as needed.

- **User-Created Lines:**  
  Lines created by user actions (e.g., `pushScreenLine`) are preserved during rebalancing.

- **Cursor Tracking:**  
  Supports both logical and screen-space cursors, with robust handling during resizing and scrolling.

- **Comprehensive Unit Tests:**  
  Includes tests for all major operations, edge cases, and boundary conditions.

- **Debugging Support:**  
  Provides a `debugPrint()` method for visualizing buffer state during development and testing.

## Build & Run

This project uses Gradle. To build and run tests:

```bash
./gradlew cleanTest test
```

To see debug output from tests, ensure your `build.gradle` includes:

```groovy
test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
```

## Project Structure

- `src/main/java/terminalbuffer/`  
  Core source code (`TerminalBuffer.java`, `LogicalLine.java`, etc.)

- `src/test/java/terminalbuffer/`  
  Unit tests (`TerminalBufferTest.java`, `ModelTest.java`)

## Usage

See `TerminalBufferTest.java` for usage examples and expected behaviors.

## Development Process

- Incremental commits with clear, descriptive messages
- Separation of new features and refactorings
- Tests document expected behavior and edge cases

## Improvements

- Performance optimizations for very large buffers
- Additional API features (search, selection)
- More documentation and CI integration

## Resize Functionality

The buffer supports dynamic resizing of the terminal screen (both width and height):

- **Width Resize:**  
  When the screen width changes, logical lines are automatically reflowed to fit the new width. If a line becomes longer than the new width, it is wrapped into multiple screen rows. If a line becomes shorter, wrapped rows are merged.

- **Height Resize:**  
  When the screen height changes, the buffer rebalances the visible screen and scrollback. If the screen shrinks, content is pushed into scrollback (with a preference for removing empty lines first, if possible). If the screen grows, content is pulled from scrollback or new empty lines are added as needed.

- **Preservation of Content:**  
  The implementation ensures that meaningful content is preserved during resizing. Empty padding lines are removed before pushing non-empty lines into scrollback.

- **Cursor Handling:**  
  Cursor positions are adjusted as needed to remain valid after resizing.

See the unit tests in `TerminalBufferTest.java` for examples and expected behaviors when resizing the buffer.

---