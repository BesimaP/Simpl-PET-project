package entities;

// En diagnose registreret af patienten selv (tabel diagnosis, US7). En patient kan have flere.
public class Diagnosis {

    private int id;
    private int patientId;       // FK til patient.id
    private String name;
    private String description;  // må være null

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Diagnosis(int id, int patientId, String name, String description) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.description = description;
    }

    // gettere: læs felterne. setId bruges af DAO'en efter save; resten kan ikke ændres udefra
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
