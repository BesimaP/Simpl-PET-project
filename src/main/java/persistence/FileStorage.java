package persistence;

import exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// Gemmer, læser og sletter uploadede filer i mappen uploads/. Svarer til en mapper, bare for disken:
// DocumentMapper tager sig af rækken i document, FileStorage af selve filen.
public class FileStorage {

    private static final String UPLOAD_DIR = "uploads";

    // Vi bruger UUID + endelse som filnavn i stedet for det oprindelige navn, som kan være for langt
    // til file_path (VARCHAR(255)) eller have mærkelige tegn. Returnerer stien, der gemmes i databasen
    public String save(InputStream content, String extension) {
        Path target = Path.of(UPLOAD_DIR, UUID.randomUUID() + extension);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Filen kunne ikke gemmes på disken", e);   // ExceptionConfig viser fejlsiden
        }
        return target.toString();
    }

    // bruges af "Åbn" på dokumentsiden. Hvis filen er væk, svarer controlleren 404
    public byte[] read(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }

    // så browseren ved, om det er PDF eller billede.
    // probeContentType kan give null på nogle maskiner, så falder vi tilbage på endelsen
    public String contentType(String filePath) {
        String contentType = null;
        try {
            contentType = Files.probeContentType(Path.of(filePath));
        } catch (IOException e) {
            // så bruger vi endelsen nedenfor
        }
        if (contentType != null) {
            return contentType;
        }
        String name = filePath.toLowerCase();
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        } else if (name.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";   // .jpg/.jpeg er de eneste andre typer, vi tillader
    }

    // deleteIfExists, så det ikke giver fejl, hvis filen allerede er væk
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            // rækken er allerede slettet, så patienten ser ikke filen – vi logger bare fejlen
            System.err.println("Kunne ikke slette filen " + filePath + ": " + e.getMessage());
        }
    }
}
