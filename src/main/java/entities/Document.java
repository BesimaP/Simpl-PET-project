package entities;

import enums.DocumentType;

import java.time.LocalDate;

// Et dokument på patienten (tabel document, US11) – fx henvisning eller blodprøvesvar, også før første runde. Selve filen ligger på disken; kun stien gemmes.
public class Document {

    private int id;
    private int patientId;
    private LocalDate uploadDate;   // gør, at dokumentet kan vises under den rigtige runde (via dato)
    private String title;
    private DocumentType documentType;
    private String filePath;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Document(int id, int patientId, LocalDate uploadDate, String title, DocumentType documentType, String filePath) {
        this.id = id;
        this.patientId = patientId;
        this.uploadDate = uploadDate;
        this.title = title;
        this.documentType = documentType;
        this.filePath = filePath;
    }

    // gettere: læs felterne. setId bruges af DAO'en efter save; resten kan ikke ændres udefra
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
