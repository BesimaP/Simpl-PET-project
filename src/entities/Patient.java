    package entities;

    import java.time.LocalDate;

    // Patienten — den centrale entitet (tabel patient). Hører til præcis én UserAccount.
    public class Patient {

        private int id;
        private int userAccountId;   // FK til user_account.id (1–1)
        private String name;
        private LocalDate dateOfBirth;

        public Patient(int id, int userAccountId, String name, LocalDate dateOfBirth) {
            this.id = id;
            this.userAccountId = userAccountId;
            this.name = name;
            this.dateOfBirth = dateOfBirth;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getUserAccountId() {
            return userAccountId;
        }

        public String getName() {
            return name;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        // Redigér profil (UC2 / US6b): navn og fødselsdato kan ændres
        public void setName(String name) {
            this.name = name;
        }
        public void setDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
    }