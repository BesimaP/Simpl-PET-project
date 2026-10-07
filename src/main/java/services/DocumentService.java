package services;

import persistence.ConnectionPool;
import persistence.DocumentMapper;
import entities.Document;
import enums.DocumentType;
import enums.ServiceResult;

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

        // 5. gem filen på disken under et unikt navn
        String storedName = UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = Path.of(UPLOAD_DIR, storedName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(fileContent, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Filen kunne ikke gemmes på disken", e);   // fanges af ExceptionConfig -> fejlsiden
        }

        // 6. gem rækken i databasen – kun stien til filen og dagens dato
        documentMapper.save(new Document(0, patientId, LocalDate.now(), title, documentType, target.toString()));

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
        deleteFile(document);
        documentMapper.delete(documentId);
        return ServiceResult.OK;
    }

    // Sletter ALLE patientens filer på disken. Kaldes af ProfileService, når kontoen slettes:
    // ON DELETE CASCADE sletter kun rækkerne i databasen – filerne i uploads/ skal vi selv fjerne (US6b AC2)
    public void deleteAllFiles(int patientId) {
        for (Document d : getDocuments(patientId)) {
            deleteFile(d);
        }
    }

    // hjælper: slet filen bag ét dokument. deleteIfExists = ingen fejl, hvis filen allerede er væk
    private void deleteFile(Document document) {
        try {
            Files.deleteIfExists(Path.of(document.getFilePath()));
        } catch (IOException e) {
            // filen kunne ikke slettes (fx låst) – rækken slettes alligevel, så patienten ikke ser den mere
            System.err.println("Kunne ikke slette filen " + document.getFilePath() + ": " + e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}