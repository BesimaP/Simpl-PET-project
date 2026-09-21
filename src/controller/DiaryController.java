package controller;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class DiaryController {

    public static void registerRoutes(Javalin app){
        app.post("/dagbog", ctx -> DiaryController.saveEntry(ctx));
    }

    public static void saveEntry(Context ctx){

    }
}
