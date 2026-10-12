package entities;

import enums.DocumentType;

import java.time.LocalDate;

// Et dokument på patienten (tabel document, US11), fx en henvisning. Ligger på patienten, så det kan uploades før første runde.
// Selve filen ligger på disken – vi gemmer kun stien.
public class Document {

    private int id;
    private int patientId;
    private LocalDate uploadDate;   // så dokumentet kan vises under den rigtige runde
    private String title;
    private DocumentType documentType;
    private String filePath;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public Document(int id, int patientId, LocalDate uploadDate, String title, DocumentType documentType, String filePath) {
        this.id = id;
        this.patientId = patientId;
        this.uploadDate = uploadDate;
        this.title = title;
        this.documentType = documentType;
        this.filePath = filePath;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public LocalDate getUploadDate() {
        return uploadDate;
    }

    public String getTitle() {
        return title;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getFilePath() {
        return filePath;
    }
}
