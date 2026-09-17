    package dao;

    import entities.Round;

    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;

    public class RoundDAO {
        private Connection connection;

        public RoundDAO (Connection connection){
             this.connection = connection;
        }

        public int save(Round round) {
            String sql = "INSERT INTO round (fertility_journey_id, round_number, treatment_type, start_date, status) VALUES (?, ?, ?, ?, ?)";
            try {
                PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                statement.setInt(1, round.getFertilityJourneyId());
                statement.setInt(2, round.getRoundNumber());
                statement.setString(3, round.getTreatmentType().name()); // enum -> "IVF"
                statement.setString(4, round.getStartDate().toString()); // LocalDate -> "2026-08-28"
                statement.setString(5, round.getStatus().name());        // enum -> "IN_PROGRESS"
                statement.executeUpdate();

                ResultSet keys = statement.getGeneratedKeys();
                if (keys.next()) {
                    round.setId(keys.getInt(1)); // objektet får rækkens id
                }
                return round.getId();

            } catch (SQLException e) {
                throw new RuntimeException("Could not save round", e);
            }
        }


    }
