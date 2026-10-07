package entities;

import java.time.LocalDateTime;

// En dagbogsnote på forløbet (tabel diary_entry, US4).
public class DiaryEntry {

    private int id;
    private int patientId;   // FK til patient.id – noter hører til patienten, ikke et forløb
    private LocalDateTime dateTime;
    private String title;
    private String content;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public DiaryEntry(int id, int patientId, LocalDateTime dateTime, String title, String content) {
        this.id = id;
        this.patientId = patientId;
        this.dateTime = dateTime;
        this.title = title;
        this.content = content;
    }

    // gettere: læs felterne. setId bruges af mapperen efter save; resten kan ikke ændres udefra
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getTitle() {
        return title;
    }
    public String getContent() {
        return content;
    }
}
