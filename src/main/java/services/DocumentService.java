package services;

import dao.DatabaseConnection;
import dao.DocumentDAO;
import entities.Document;
import entities.FertilityJourney;
import entities.Round;
import enums.DocumentType;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// Forretningslogik for dokumenter (US11). Kender IKKE Javalin.
// Regler: titel og type udfyldt, filtype PDF/JPG/PNG, maks 10 MB, aktiv runde skal findes.
// Svarer med ServiceResult: OK · INVALID_INPUT · NO_ACTIVE_JOURNEY · NO_ACTIVE_ROUND
public class DocumentService {

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    public ServiceResult uploadDocument(int patientId, String title, String type,
                                        String originalFileName, InputStream fileContent, long fileSize) {

        // 1. regel: titel, type og fil skal være udfyldt
        if (isBlank(title) || isBlank(type) || isBlank(originalFileName) || fileContent == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. typen skal matche en af dropdownens værdier
        DocumentType documentType;
        try {
            documentType = DocumentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. regel: kun PDF, JPG og PNG
        String lower = originalFileName.toLowerCase();
        if (!(lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png"))) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. regel: maks 10 MB
        if (fileSize > MAX_SIZE_BYTES) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. find den aktive runde – et dokument hører til en runde
        Round round;
        try {
            round = new RoundService().findActiveRound(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        } catch (NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // 6. gem filen på disken under et unikt navn
        String storedName = UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = Path.of(UPLOAD_DIR, storedName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(fileContent, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not save uploaded file", e);
        }

        // 7. gem rækken i databasen – kun stien til filen
        new DocumentDAO(DatabaseConnection.getConnection())
                .save(new Document(0, round.getId(), title, documentType, target.toString()));

        return ServiceResult.OK;
    }

    // Hent alle dokumenter i den runde, der er i gang – til listen på siden
    public java.util.List<Document> getDocuments(int patientId) {
        try {
            Round round = new RoundService().findActiveRound(patientId);
            return new DocumentDAO(DatabaseConnection.getConnection()).findByRound(round.getId());
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return new java.util.ArrayList<>();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}