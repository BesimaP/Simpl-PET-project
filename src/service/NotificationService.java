package service;

import dao.DatabaseConnection;
import dao.NotificationDAO;
import entities.Notification;
import enums.ServiceResult;

import java.util.List;

// Forretningslogik for påmindelser (US12). Kender IKKE Javalin.
// Påmindelser hænger direkte på patienten – ingen forløb/runde at slå op.
public class NotificationService {

    // Henter alle patientens påmindelser, nyeste først – til notifikationer.html
    public List<Notification> getNotifications(int patientId) {
        return new NotificationDAO(DatabaseConnection.getConnection()).findByPatient(patientId);
    }

    // Tæller ulæste – til den røde prik på klokken i topbaren
    public int countUnread(int patientId) {
        return new NotificationDAO(DatabaseConnection.getConnection()).countUnread(patientId);
    }

    // Markér én påmindelse som læst (UC7) – bruges af POST /notifikationer/laest, når templates er på
    public ServiceResult markRead(int notificationId) {
        new NotificationDAO(DatabaseConnection.getConnection()).markRead(notificationId);
        return ServiceResult.OK;
    }
}
