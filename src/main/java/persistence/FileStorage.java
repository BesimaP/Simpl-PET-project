package persistence;

import exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// Gemmer, læser og sletter de uploadede filer på disken (mappen uploads/ i projektmappen).
// Samme rolle som en mapper – bare for disken i stedet for databasen. DocumentService har reglerne,
// DocumentMapper har rækken i tabellen document, og FileStorage har selve filen (SRP: én klasse, ét job)
public class FileStorage {

    private static final String UPLOAD_DIR = "uploads";

    // Gemmer filen under et unikt navn: UUID + endelsen (fx "3f2a….pdf") og returnerer stien, som gemmes i databasen.
    // Det oprindelige filnavn bruges ikke – det kan være for langt til file_path (VARCHAR(255)) og indeholde mærkelige tegn
    public String save(InputStream content, String extension) {
        Path target = Path.of(UPLOAD_DIR, UUID.randomUUID() + extension);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Filen kunne ikke gemmes på disken", e);   // fanges af ExceptionConfig -> fejlsiden
        }
        return target.toString();
    }

    // Læser hele filen – til "Åbn" på dokumentsiden. IOException = filen er væk (controlleren svarer 404)
    public byte[] read(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }

    // Filtypen, så browseren ved, om det er PDF eller billede.
    // probeContentType kan give null (computeren kender ikke filtypen) – så bruges en standardtype ud fra endelsen
    public String contentType(String filePath) {
        String contentType = null;
        try {
            contentType = Files.probeContentType(Path.of(filePath));
        } catch (IOException e) {
            // typen kunne ikke læses – endelsen bruges nedenfor
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
        return "image/jpeg";   // .jpg / .jpeg – de eneste andre typer, upload tillader
    }

    // Sletter én fil. deleteIfExists = ingen fejl, hvis filen allerede er væk
    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            // filen kunne ikke slettes (fx låst) – rækken er allerede væk, så patienten ser den ikke mere
            System.err.println("Kunne ikke slette filen " + filePath + ": " + e.getMessage());
        }
    }
}
