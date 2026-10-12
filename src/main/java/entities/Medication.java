package entities;

// Et lægemiddel (tabel medication). Hører ikke til en bestemt patient, men bruges af alle doser.
public class Medication {

    private int id;
    private String name;          // UNIQUE i databasen
    private String description;   // må være null
    private String unit;          // fast pr. præparat: "IU", "mg" eller "µg" (svar fra klinikken)

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public Medication(int id, String name, String description, String unit) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unit = unit;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }
}
