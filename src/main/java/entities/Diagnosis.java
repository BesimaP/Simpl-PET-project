package entities;

// En diagnose, som patienten selv har registreret (tabel diagnosis, US7). En patient kan have flere.
public class Diagnosis {

    private int id;
    private int patientId;       // FK til patient.id
    private String name;
    private String description;  // må være null

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public Diagnosis(int id, int patientId, String name, String description) {
        this.id = id;
        this.patientId = patientId;
        this.name = name;
        this.description = description;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
