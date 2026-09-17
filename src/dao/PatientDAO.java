    package dao;

    import entities.Patient;

    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;

    public class PatientDAO {
        private Connection connection;

        public PatientDAO (Connection connection){
            this.connection = connection;
        }

        // Gemmer en ny konto og returnerer det id, databasen gav den
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


    }
