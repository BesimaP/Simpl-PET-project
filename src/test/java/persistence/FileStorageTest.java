package persistence;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Test af FileStorage: gemmer, læser og sletter rigtige filer i mappen uploads/ – ingen database.
// Hver test rydder op efter sig selv, så der ikke ligger testfiler tilbage
class FileStorageTest {

    private final FileStorage fileStorage = new FileStorage();

    @Test
    void saveWritesFileThatCanBeReadAgain() throws IOException {
        String path = fileStorage.save(new ByteArrayInputStream("hej".getBytes()), ".pdf");
        try {
            assertTrue(path.endsWith(".pdf"));
            assertArrayEquals("hej".getBytes(), fileStorage.read(path));
        } finally {
            fileStorage.delete(path);
        }
    }

    @Test
    void saveGivesEachFileItsOwnName() {
        String first = fileStorage.save(new ByteArrayInputStream("a".getBytes()), ".png");
        String second = fileStorage.save(new ByteArrayInputStream("b".getBytes()), ".png");
        try {
            assertNotEquals(first, second);   // UUID: to uploads overskriver ikke hinanden
        } finally {
            fileStorage.delete(first);
            fileStorage.delete(second);
        }
    }

    @Test
    void deleteRemovesFile() {
        String path = fileStorage.save(new ByteArrayInputStream("x".getBytes()), ".jpg");
        fileStorage.delete(path);
        assertFalse(Files.exists(Path.of(path)));
    }

    @Test
    void deleteMissingFileDoesNotThrow() {
        assertDoesNotThrow(() -> fileStorage.delete("uploads/findes-ikke.pdf"));
    }

    @Test
    void readMissingFileThrowsIOException() {
        assertThrows(IOException.class, () -> fileStorage.read("uploads/findes-ikke.pdf"));
    }

    @Test
    void contentTypeFromExtension() {
        assertEquals("application/pdf", fileStorage.contentType("uploads/fil.pdf"));
        assertEquals("image/png", fileStorage.contentType("uploads/fil.png"));
    }
}
