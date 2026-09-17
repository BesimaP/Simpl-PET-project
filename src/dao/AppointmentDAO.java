package dao;

import enums.AppointmentType;
import entities.Appointment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen appointment (US3). Aftaler hænger på forløbet, fordi første konsultation sker før nogen runde.
public class AppointmentDAO {
    private Connection connection;

    public AppointmentDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer én aftale og returnerer det id, databasen gav den
    public int save(Appointment appointment) {
        String sql = "INSERT INTO appointment (fertility_journey_id, date_time, appointment_type, location) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, appointment.getFertilityJourneyId());
            statement.setString(2, appointment.getDateTime().toString());          // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setString(3, appointment.getAppointmentType().name());       // enum -> "SCANNING" (matcher CHECK og option value i aftaler.html)
            statement.setString(4, appointment.getLocation());                     // fx "Vitanova"
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                appointment.setId(keys.getInt(1));
            }
            return appointment.getId();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save appointment", e);
        }
    }

    // henter alle aftaler på ét forløb, tidligste først – siden deler dem selv i "kommende" og "tidligere"
    public List<Appointment> findByJourney(int fertilityJourneyId) {
        String sql = "SELECT * FROM appointment WHERE fertility_journey_id = ? ORDER BY date_time";
        List<Appointment> appointments = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, fertilityJourneyId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                appointments.add(mapRow(rs));
            }
            return appointments;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find appointments for journey " + fertilityJourneyId, e);
        }
    }

    // sletter én aftale ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM appointment WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete appointment " + id, e);
        }
    }

    // én række fra databasen -> ét Appointment-objekt
    private Appointment mapRow(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("id"),
                rs.getInt("fertility_journey_id"),
                LocalDateTime.parse(rs.getString("date_time")),
                AppointmentType.valueOf(rs.getString("appointment_type")), // "SCANNING" -> enum
                rs.getString("location")
        );
    }
}
