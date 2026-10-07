package services;

import persistence.ConnectionPool;
import persistence.DocumentMapper;
import entities.Document;
import enums.DocumentType;
import enums.ServiceResult;
import exceptions.DatabaseException;
import exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

// Forretningslogik for dokumenter (US11). Kender IKKE Javalin.
// Regler: titel og type udfyldt, filtype PDF/JPG/PNG, maks 10 MB.
// Dokumentet ligger på patienten, så det kan uploades når som helst – også før første runde (fx en henvisning).
// Svarer med ServiceResult: OK · INVALID_INPUT
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private DocumentMapper documentMapper;

    public DocumentService(ConnectionPool connectionPool) {
        this.documentMapper = new DocumentMapper(connectionPool);
    }

    public ServiceResult uploadDocument(int patientId, String title, String type,
                                        String originalFileName, InputStream fileContent, long fileSize) {

        // 1. regel: titel, type og fil skal være udfyldt
        if (isBlank(title) || isBlank(type) || isBlank(originalFileName) || fileContent == null) {
            return ServiceResult.INVALID_INPUT;
        }
        // titlen skal kunne være i kolonnen (VARCHAR(100))
        if (isTooLong(title, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. typen skal matche en af dropdownens værdier
        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. regel: kun PDF, JPG og PNG
        String lower = originalFileName.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. regel: maks 10 MB
        if (fileSize > MAX_SIZE_BYTES) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. gem filen på disken under et unikt navn: UUID + endelsen (fx "3f2a….pdf").
        //    Det oprindelige filnavn bruges ikke – det kan være for langt til file_path (VARCHAR(255)) og indeholde mærkelige tegn.
        //    Titlen er det, patienten ser i listen
        String extension = lower.substring(lower.lastIndexOf('.'));   // ".pdf", ".jpg", ".jpeg" eller ".png" (tjekket i trin 3)
        Path target = Path.of(UPLOAD_DIR, UUID.randomUUID() + extension);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(fileContent, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Filen kunne ikke gemmes på disken", e);   // fanges af ExceptionConfig -> fejlsiden
        }

        // 6. gem rækken i databasen – kun stien til filen og dagens dato.
        //    Fejler databasen, slettes filen igen, så der ikke ligger en fil på disken uden en række
        try {
            documentMapper.save(new Document(0, patientId, LocalDate.now(), title, documentType, target.toString()));
        } catch (DatabaseException e) {
            deleteFile(target.toString());
            throw e;   // videre til ExceptionConfig -> fejlsiden
        }

        return ServiceResult.OK;
    }

    // Hent alle patientens dokumenter – til listen på siden
    public java.util.List<Document> getDocuments(int patientId) {
        return documentMapper.findByPatient(patientId);
    }

    // Finder ét dokument ud fra id – men kun blandt patientens egne, så man ikke kan åbne andres filer ved at gætte et id.
    // Returnerer null, hvis det ikke findes (controlleren svarer 404)
    public Document findDocument(int patientId, int documentId) {
        for (Document d : getDocuments(patientId)) {
            if (d.getId() == documentId) {
                return d;
            }
        }
        return null;
    }

    // Sletter ét dokument (US11): både rækken i databasen og selve filen på disken.
    // Kun patientens eget dokument – ellers NOT_FOUND (så man ikke kan slette andres ved at rette id'et)
    public ServiceResult deleteDocument(int patientId, int documentId) {
        Document document = findDocument(patientId, documentId);
        if (document == null) {
            return ServiceResult.NOT_FOUND;
        }
        // rækken først, så filen: fejler databasen, findes både række og fil stadig (ingen række, der peger på en slettet fil)
        documentMapper.delete(documentId, patientId);   // AND patient_id = ? i SQL'en – ekstra sikring
        deleteFile(document.getFilePath());
        return ServiceResult.OK;
    }

    // Sletter ALLE patientens filer på disken. Kaldes af ProfileService, når kontoen slettes:
    // ON DELETE CASCADE sletter kun rækkerne i databasen – filerne i uploads/ skal vi selv fjerne (US6b AC2)
    public void deleteAllFiles(int patientId) {
        deleteFiles(getDocuments(patientId));
    }

    // Sletter filerne bag en liste dokumenter. ProfileService henter listen FØR kontoen slettes og kalder denne bagefter –
    // så slettes filerne kun, hvis rækkerne i databasen faktisk er væk
    public void deleteFiles(java.util.List<Document> documents) {
        for (Document d : documents) {
            deleteFile(d.getFilePath());
        }
    }

    // hjælper: slet én fil. deleteIfExists = ingen fejl, hvis filen allerede er væk
    private void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            // filen kunne ikke slettes (fx låst) – rækken er allerede væk, så patienten ser den ikke mere
            System.err.println("Kunne ikke slette filen " + filePath + ": " + e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // lille hjælper: længere end kolonnen i databasen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    private boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
