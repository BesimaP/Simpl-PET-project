    package dao;

    import entities.Round;
    import entities.UserAccount;
    import enums.Result;
    import enums.RoundStatus;
    import enums.TreatmentType;

    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.time.LocalDate;

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

        public Round findActiveByJourney(int fertilityJouneyID){
            String sql = "SELECT * FROM round WHERE fertility_Journey_ID = ? AND status = ?";

            try {
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setInt(1, fertilityJouneyID);
                statement.setString(2, RoundStatus.IN_PROGRESS.name());

                // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
                ResultSet rs = statement.executeQuery();

                // if, ikke while: der kan højst være én række, fordi round er UNIQUE
                if (rs.next()) {
                    // rækken -> et round-objekt (kortet), som controlleren kan kigge på
                    return new Round(
                            rs.getInt("id"),
                            rs.getInt("fertility_journey_id"),
                            rs.getInt("round_number"),
                            TreatmentType.valueOf(rs.getString("treatment_type")),
                            LocalDate.parse(rs.getString("start_date")),
                            LocalDate.parse(rs.getString("end_date")),
                            RoundStatus.valueOf(rs.getString("status")),
                            Result.valueOf(rs.getString("result")));
                }
                return null; // ingen række = runden findes ikke
            } catch (SQLException e) {
                throw new RuntimeException("Could not find round " + fertilityJouneyID, e);
            }
        }


    }
