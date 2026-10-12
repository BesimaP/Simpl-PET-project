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

// Forretningslogik for dokumenter (US11). Kender IKKE Javalin.
// Regler: titel og type udfyldt, filtype PDF/JPG/PNG, maks 10 MB.
// Dokumentet ligger på patienten, så det kan uploades når som helst – også før første runde (fx en henvisning).
// Svarer med ServiceResult: OK · INVALID_INPUT
// Rækken i databasen klarer DocumentMapper, selve filen på disken klarer FileStorage – servicen har kun reglerne
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

        // 1. regel: titel, type og fil skal være udfyldt
        if (InputCheck.isBlank(title) || InputCheck.isBlank(type) || InputCheck.isBlank(originalFileName) || fileContent == null) {
            return ServiceResult.INVALID_INPUT;
        }
        // titlen skal kunne være i kolonnen (VARCHAR(100))
        if (InputCheck.isTooLong(title, 100)) {
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

        // 5. gem filen på disken – FileStorage giver den et unikt navn og svarer med stien. Titlen er det, patienten ser i listen
        String extension = lower.substring(lower.lastIndexOf('.'));   // ".pdf", ".jpg", ".jpeg" eller ".png" (tjekket i trin 3)
        String filePath = fileStorage.save(fileContent, extension);

        // 6. gem rækken i databasen – kun stien til filen og dagens dato.
        //    Fejler databasen, slettes filen igen, så der ikke ligger en fil på disken uden en række
        try {
            documentMapper.save(new Document(0, patientId, LocalDate.now(), title.trim(), documentType, filePath));
        } catch (DatabaseException e) {
            fileStorage.delete(filePath);
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
        fileStorage.delete(document.getFilePath());
        return ServiceResult.OK;
    }

    // Sletter filerne bag en liste dokumenter. ProfileService henter listen FØR kontoen slettes og kalder denne bagefter –
    // så slettes filerne kun, hvis rækkerne i databasen faktisk er væk
    public void deleteFiles(java.util.List<Document> documents) {
        for (Document d : documents) {
            fileStorage.delete(d.getFilePath());
        }
    }

    // Selve filen til "Åbn" – controlleren sender den til browseren. IOException = filen er væk
    public byte[] readFile(Document document) throws IOException {
        return fileStorage.read(document.getFilePath());
    }

    // PDF eller billede? Så browseren ved, hvordan filen skal vises
    public String contentType(Document document) {
        return fileStorage.contentType(document.getFilePath());
    }
}
