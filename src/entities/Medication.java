    package entities;

    // Stamdata for et lægemiddel (tabel medication). Tilhører ikke en patient — genbruges på tværs af registreringer.
    public class Medication {

        private int id;
        private String name;          // UNIQUE i databasen
        private String description;   // må være null

        public Medication(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

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
    }
