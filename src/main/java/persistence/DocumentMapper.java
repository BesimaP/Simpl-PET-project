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

// SQL for tabellen document (US11). Selve filen ligger på disken, databasen gemmer kun stien.
public class DocumentMapper {
    private ConnectionPool connectionPool;

    public DocumentMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer dokumentet og returnerer det nye id
    public int save(Document document) {
        String sql = "INSERT INTO document (patient_id, upload_date, title, document_type_id, file_path) VALUES (?, ?, ?, (SELECT id FROM document_type WHERE name = ?), ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, document.getPatientId());
            statement.setObject(2, document.getUploadDate());
            statement.setString(3, document.getTitle());
            statement.setString(4, document.getDocumentType().name()); // id'et slås op i document_type
            statement.setString(5, document.getFilePath());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    document.setId(keys.getInt(1));
                }
            }
            return document.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save document", e);
        }
    }

    // nyeste først til "Mine dokumenter"
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
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    documents.add(mapRow(rs));
                }
                return documents;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find documents for patient " + patientId, e);
        }
    }

    // AND patient_id = ? så man ikke kan slette en andens dokument ved at ændre id'et.
    // Filen på disken slettes ikke her, det gør servicen
    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM document WHERE id = ? AND patient_id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 rækker = findes ikke eller ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete document " + id, e);
        }
    }

    private Document mapRow(ResultSet rs) throws SQLException {
        return new Document(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("upload_date", LocalDate.class),
                rs.getString("title"),
                DocumentType.valueOf(rs.getString("document_type")),
                rs.getString("file_path")
        );
    }
}
