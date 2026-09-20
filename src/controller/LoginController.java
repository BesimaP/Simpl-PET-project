package controller;

import dao.DatabaseConnection;
import dao.UserAccountDAO;
import entities.UserAccount;
import io.javalin.Javalin;

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

        // POST /opretprofil – når brugeren trykker Opret profil
        //   0. i opretprofil.html: <form action="/opretprofil" method="post">
        //   1. læs felterne: name, dateOfBirth, username, password
        //   2. tjek: findes brugernavnet allerede? (UserAccountDAO.findByUsername != null) -> tilbage til opretprofil.html
        //   3. byg et UserAccount(0, username, password) og kald UserAccountDAO.save -> giver accountId
        //      (TODO senere: hash kodeordet med BCrypt før det gemmes)
        //   4. byg en Patient(0, accountId, name, LocalDate.parse(dateOfBirth)) og kald PatientDAO.save
        //   5. send til login.html


    }
}
