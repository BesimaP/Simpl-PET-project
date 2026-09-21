package controller;

import enums.ServiceResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.AppointmentService;

public class AppointmentController{

    public static void registerRoutes(Javalin app){
        app.post("/aftaler", AppointmentController :: addAppointment);
    }

    private static void addAppointment(Context ctx){
        String type = ctx.formParam("type");
        String location = ctx.formParam("location");
        String date = ctx.formParam("date");
        String time = ctx.formParam("time");
        int patientId = 1;

        ServiceResult result = new AppointmentService().addAppointment(patientId, type, location, date, time);

        switch (result){
            case OK -> ctx.redirect("/aftaler.html");
            case INVALID_INPUT -> ctx.redirect("/aftaler.html?fejl=felter");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/aftaler.html?fejl=ingen-forloeb");
            default -> ctx.redirect("/aftaler.html?fejl=ukendt");
        }
    }

}
