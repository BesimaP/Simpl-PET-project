package services;

import persistence.ConnectionPool;
import persistence.DocumentMapper;
import persistence.FileStorage;
import entities.Document;
import enums.DocumentType;
import enums.ServiceResult;
import exceptions.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

// Logik for dokumenter (US11). DocumentMapper gemmer rækken, FileStorage gemmer filen på disken.
// Dokumenter ligger på patienten, så de kan uploades før første runde (fx en henvisning).
public class DocumentService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private DocumentMapper documentMapper;
    private FileStorage fileStorage;

    public DocumentService(ConnectionPool connectionPool) {
        this.documentMapper = new DocumentMapper(connectionPool);
        this.fileStorage = new FileStorage();
    }

    public ServiceResult uploadDocument(int patientId, String title, String type,
                                        String originalFileName, InputStream fileContent, long fileSize) {

        // titel, type og fil skal være udfyldt
        if (InputCheck.isBlank(title) || InputCheck.isBlank(type) || InputCheck.isBlank(originalFileName) || fileContent == null) {
            return ServiceResult.INVALID_INPUT;
        }
        // titlen skal kunne være i kolonnen (VARCHAR(100))
        if (InputCheck.isTooLong(title, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // typen skal være en af dropdownens værdier
        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // kun PDF, JPG og PNG
        String lower = originalFileName.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            return ServiceResult.INVALID_INPUT;
        }

        if (fileSize > MAX_SIZE_BYTES) {
            return ServiceResult.INVALID_INPUT;
        }

        // FileStorage giver filen et unikt navn og returnerer stien
        String extension = lower.substring(lower.lastIndexOf('.'));
        String filePath = fileStorage.save(fileContent, extension);

        // fejler databasen, sletter vi filen igen, så der ikke ligger en fil uden en række
        try {
            documentMapper.save(new Document(0, patientId, LocalDate.now(), title.trim(), documentType, filePath));
        } catch (DatabaseException e) {
            fileStorage.delete(filePath);
            throw e;
        }

        return ServiceResult.OK;
    }

    public java.util.List<Document> getDocuments(int patientId) {
        return documentMapper.findByPatient(patientId);
    }

    // Leder kun blandt patientens egne dokumenter, så man ikke kan åbne andres filer ved at gætte et id. null = findes ikke
    public Document findDocument(int patientId, int documentId) {
        for (Document d : getDocuments(patientId)) {
            if (d.getId() == documentId) {
                return d;
            }
        }
        return null;
    }

    // Sletter både rækken og filen – kun patientens eget dokument, ellers NOT_FOUND
    public ServiceResult deleteDocument(int patientId, int documentId) {
        Document document = findDocument(patientId, documentId);
        if (document == null) {
            return ServiceResult.NOT_FOUND;
        }
        // rækken først: fejler databasen, er filen der stadig, så ingen række peger på en slettet fil
        documentMapper.delete(documentId, patientId);
        fileStorage.delete(document.getFilePath());
        return ServiceResult.OK;
    }

    // Bruges af ProfileService, når kontoen slettes – kaldes først, når rækkerne er væk
    public void deleteFiles(java.util.List<Document> documents) {
        for (Document d : documents) {
            fileStorage.delete(d.getFilePath());
        }
    }

    // Filen til "Åbn". IOException betyder, at filen er væk fra disken
    public byte[] readFile(Document document) throws IOException {
        return fileStorage.read(document.getFilePath());
    }

    // så browseren ved, om det er en PDF eller et billede
    public String contentType(Document document) {
        return fileStorage.contentType(document.getFilePath());
    }
}
