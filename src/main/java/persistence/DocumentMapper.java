package persistence;

import exceptions.DatabaseException;

import enums.DocumentType;
import entities.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen document (US11). Selve filen ligger på disken – databasen kender kun stien.
public class DocumentMapper {
    private ConnectionPool connectionPool; // nøgleringen

    public DocumentMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer ét dokument (titel, type og sti til filen) og returnerer det id, databasen finder selv id'et
    public int save(Document document) {
        String sql = "INSERT INTO document (patient_id, upload_date, title, document_type_id, file_path) VALUES (?, ?, ?, (SELECT id FROM document_type WHERE name = ?), ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, document.getPatientId());
            statement.setObject(2, document.getUploadDate()); // LocalDate direkte
            statement.setString(3, document.getTitle());
            statement.setString(4, document.getDocumentType().name()); // ordet, fx "BLOOD_TEST_RESULT" -> databasen finder selv id'et
            statement.setString(5, document.getFilePath());            // fx "uploads/blodprove-sep.pdf"
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                document.setId(keys.getInt(1));
            }
            return document.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save document", e);
        }
    }

    // henter alle patientens dokumenter, nyeste først – til listen "Mine dokumenter"
    public List<Document> findByPatient(int patientId) {
        String sql = "SELECT document.*, document_type.name AS document_type "
                   + "FROM document "
                   + "JOIN document_type ON document_type.id = document.document_type_id "
                   + "WHERE document.patient_id = ? "
                   + "ORDER BY document.upload_date DESC, document.title";
        List<Document> documents = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                documents.add(mapRow(rs));
            }
            return documents;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find documents for patient " + patientId, e);
        }
    }

    // sletter rækken i databasen. OBS: selve filen på disken skal slettes et andet sted (i service/controller)
    public void delete(int id) {
        String sql = "DELETE FROM document WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete document " + id, e);
        }
    }

    // én række fra databasen -> ét Document-objekt
    private Document mapRow(ResultSet rs) throws SQLException {
        return new Document(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("upload_date", LocalDate.class),               // PostgreSQL giver selv en LocalDate
                rs.getString("title"),
                DocumentType.valueOf(rs.getString("document_type")), // "OTHER" -> enum
                rs.getString("file_path")
        );
    }
}
