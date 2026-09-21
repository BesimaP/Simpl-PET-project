package controller;

import dao.DatabaseConnection;
import dao.PatientDAO;
import dao.UserAccountDAO;
import entities.Patient;
import entities.UserAccount;
import io.javalin.Javalin;

import java.time.LocalDate;

// Koordinatoren for login.html. Modtager formularen, spørger DAO'en og beslutter, hvor brugeren sendes hen.
public class LoginController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: LoginController.registerRoutes(app)
    public static void registerRoutes(Javalin app) {

        // formularen på login.html sender hertil (action="/login" method="post")
        app.post("/login", ctx -> {
            // 1. åbn kuverten: læs felterne fra formularen (name="username" og name="password" i HTML)
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            // 2. lav arkivaren og bed den finde kortet – et almindeligt metodekald, ingen SQL her
            UserAccountDAO dao = new UserAccountDAO(DatabaseConnection.getConnection());
            UserAccount user = dao.findByUsername(username); // kortet, eller null hvis brugeren ikke findes

            // 3. beslut: findes kortet, og passer kodeordet? (senere: BCrypt.checkpw i stedet for equals)
            if (user == null || !password.equals(user.getPasswordHash())) {
                ctx.redirect("/login.html");      // nej -> tilbage til login
            } else {
                ctx.redirect("/dashboard.html");  // ja -> ind på dashboard
            }
        });



        // formularen på opretprofil.html sender hertil (action="/opretprofil" method="post")
        app.post("/opretprofil", ctx -> {
            // 1. åbn kuverten: læs felterne fra formularen
            String name = ctx.formParam("name");
            String dateOfBirth = ctx.formParam("dateOfBirth");
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            UserAccountDAO accountDao = new UserAccountDAO(DatabaseConnection.getConnection());

            // 2. findes brugernavnet allerede? så tilbage til siden med en fejl i adressen (js kan vise den)
            if (accountDao.findByUsername(username) != null) {
                ctx.redirect("/opretprofil.html?fejl=brugernavn");
                return; // stop her – resten skal ikke køre
            }

            // 3. gem kontoen – id'et fra databasen skal bruges til patienten lige efter
            //    (TODO senere: hash kodeordet med BCrypt før det gemmes)
            int accountId = accountDao.save(new UserAccount(0, username, password));

            // 4. gem patienten, knyttet til kontoen via accountId
            PatientDAO patientDao = new PatientDAO(DatabaseConnection.getConnection());
            patientDao.save(new Patient(0, accountId, name, LocalDate.parse(dateOfBirth)));

            // 5. ind på dashboard med det samme
            ctx.redirect("/dashboard.html");
        });
    }
}
