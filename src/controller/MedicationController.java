package controller;

import io.javalin.Javalin;
import io.javalin.http.Context;
import service.MedicationService;

public class MedicationController {

    public static void registerRoutes(Javalin app){
        app.post("/medicin", MedicationController :: logDose);
    }

    public static void logDose(Context ctx){
    String medication = ctx.formParam("medication");
    String dose = ctx.formParam("dose");
    String unit = ctx.formParam("unit");
    String date = ctx.formParam("date");
    String time = ctx.formParam("time");
    boolean taken = ctx.formParam("taken") != null; // afkrydset checkbox sender 1, ellers null
    int patientId = 1;

        MedicationService.LogResult result = new MedicationService().logDose(patientId, medication, dose, unit, date, time, taken);

        switch (result){
            case SAVED -> ctx.redirect("/medicin.html");
            case INVALID_INPUT -> ctx.redirect("/medicin.html?fejl=felter");
            case NO_ACTIVE_ROUND -> ctx.redirect("/medicin.html?fejl=ingen-runde");
        }
    }

}
