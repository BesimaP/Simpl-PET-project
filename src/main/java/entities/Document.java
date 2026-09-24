package entities;

import enums.DocumentType;

// Et dokument på en runde (tabel document, US11). Selve filen ligger på disken; kun stien gemmes.
public class Document {

    private int id;
    private int roundId;
    private String title;
    private DocumentType documentType;
    private String filePath;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Document(int id, int roundId, String title, DocumentType documentType, String filePath) {
        this.id = id;
        this.roundId = roundId;
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

    public int getRoundId() {
        return roundId;
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
