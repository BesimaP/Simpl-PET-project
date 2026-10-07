package persistence;

import exceptions.DatabaseException;

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
public class AppointmentMapper {
    private ConnectionPool connectionPool;

    public AppointmentMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer én aftale og returnerer det id, databasen gav den
    public int save(Appointment appointment) {
        String sql = "INSERT INTO appointment (fertility_journey_id, date_time, appointment_type_id, location) VALUES (?, ?, (SELECT id FROM appointment_type WHERE name = ?), ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, appointment.getFertilityJourneyId());
            statement.setObject(2, appointment.getDateTime());          // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setString(3, appointment.getAppointmentType().name());       // enum -> "SCANNING" (matcher CHECK og option value i aftaler.html)
            statement.setString(4, appointment.getLocation());                     // fx "Vitanova"
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                appointment.setId(keys.getInt(1));
            }
            return appointment.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save appointment", e);
        }
    }

    // henter alle aftaler på ét forløb, tidligste først – siden deler dem selv i "kommende" og "tidligere"
    public List<Appointment> findByJourney(int fertilityJourneyId) {
        String sql = "SELECT appointment.*, appointment_type.name AS appointment_type "
                   + "FROM appointment "
                   + "JOIN appointment_type ON appointment_type.id = appointment.appointment_type_id "
                   + "WHERE appointment.fertility_journey_id = ? "
                   + "ORDER BY appointment.date_time";
        List<Appointment> appointments = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);){

            statement.setInt(1, fertilityJourneyId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                appointments.add(mapRow(rs));
            }
            return appointments;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find appointments for journey " + fertilityJourneyId, e);
        }
    }

    // sletter én aftale ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM appointment WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete appointment " + id, e);
        }
    }

    // én række fra databasen -> ét Appointment-objekt
    private Appointment mapRow(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("id"),
                rs.getInt("fertility_journey_id"),
                rs.getObject("date_time", LocalDateTime.class),
                AppointmentType.valueOf(rs.getString("appointment_type")), // "SCANNING" -> enum
                rs.getString("location")
        );
    }
}
