    package dao;

    import entities.Patient;

    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.time.LocalDate;

    public class PatientDAO {
        private Connection connection;

        public PatientDAO (Connection connection){
            this.connection = connection;
        }

        // Gemmer en ny patient og returnerer det id, databasen gav den
        public int save(Patient patient) {
            String sql = "INSERT INTO patient (user_account_id, name, date_of_birth) VALUES (?, ?, ?)";
            try {
                PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                statement.setInt(1, patient.getUserAccountId());
                statement.setString(2, patient.getName());
                statement.setString(3, patient.getDateOfBirth().toString());
                statement.executeUpdate();

                ResultSet keys = statement.getGeneratedKeys();
                if (keys.next()) {
                    patient.setId(keys.getInt(1));
                }
                return patient.getId();

            } catch (SQLException e) {
                throw new RuntimeException("Could not save patient", e);
            }
        }

        // Finder patienten bag en konto – returnerer null, hvis kontoen ingen patient har (bruges efter login)
        public Patient findByUserAccount(int userAccountId) {
            // ? = pladsholder for konto-id'et, som sættes nedenfor (aldrig lim tal ind i SQL-strengen selv)
            String sql = "SELECT * FROM patient WHERE user_account_id = ?";

            try {
                // gør SQL'en klar til at køre
                PreparedStatement statement = connection.prepareStatement(sql);
                // fyld ? ud – setInt, fordi user_account_id er et tal
                statement.setInt(1, userAccountId);

                // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
                ResultSet rs = statement.executeQuery();

                // if, ikke while: der kan højst være én række, fordi user_account_id er UNIQUE (én konto = én patient)
                if (rs.next()) {
                    // rækken -> et Patient-objekt (kortet). date_of_birth er gemt som tekst, derfor LocalDate.parse
                    return new Patient(rs.getInt("id"), rs.getInt("user_account_id"), rs.getString("name"), LocalDate.parse(rs.getString("date_of_birth")));
                }
                return null; // ingen række = kontoen har ingen patient

            } catch (SQLException e) {
                throw new RuntimeException("Could not find patient for account " + userAccountId, e);
            }
        }

        // Retter patientens navn (min-profil). UPDATE ændrer en række, der findes
        public void updateName(int id, String name) {
            // SET = hvad der ændres, WHERE = hvilken række. Uden WHERE ville ALLE patienter få det nye navn!
            String sql = "UPDATE patient SET name = ? WHERE id = ?";

            try {
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setInt(2, id);
                statement.setString(1, name); // første ? = det nye navn
                statement.executeUpdate();            // executeUpdate = INSERT/UPDATE/DELETE (ingen rækker tilbage)

            } catch (SQLException e) {
                throw new RuntimeException("Could not update name for patient " + id,e);
            }
        }
    }
