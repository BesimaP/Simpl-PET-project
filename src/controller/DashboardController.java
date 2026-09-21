package controller;

import io.javalin.Javalin;

public class DashboardController {

        public static void registerRoutes(Javalin app) {
            // POST /opret-forloeb – når brugeren trykker "Start dit forløb" på dashboardtom.html (US1)
            app.post("/opret-forloeb", ctx -> {
                // 1. åbn kuverten: startdatoen fra formularen
                String startDate = ctx.formParam("startDate");
                int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

                // 2. tom dato? (feltet har required, men serveren stoler aldrig blindt på browseren) -> tilbage til siden
                if (startDate == null || startDate.isBlank()) {
                    ctx.redirect("/dashboardtom.html?fejl=dato");
                    return;
                }

                FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());

                // 3. regel fra US1: kun ét aktivt forløb ad gangen – opret kun, hvis der ikke allerede er et
                if (journeyDao.findActiveByPatient(patientId) == null) {
                    journeyDao.save(new FertilityJourney(0, patientId, LocalDate.parse(startDate), JourneyStatus.ACTIVE));
                }

                // 4. ind på dashboard – nu findes der et forløb (med eller uden runde)
                ctx.redirect("/dashboard.html");
                
            });

            app.post("/start-runde", ctx -> {
                // 1. åbn kuverten: behandlingstype og startdato
                String type = ctx.formParam("type");
                String startDate = ctx.formParam("startDate");
                int patientId = 1;

                // 2. find patientens aktive forløb – uden forløb er der ingen skuffe at lægge runden i
                FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());
                RoundDAO roundDAO = new RoundDAO(DatabaseConnection.getConnection());

                FertilityJourney journey = journeyDao.findActiveByPatient(patientId);
                if(journey == null){
                    ctx.redirect("/dashboardtom.html");
                    return;
                }

                //3. er der allerede en runde i gang? Så må der ikke startes en ny(kun en ad gangen)
                if(roundDAO.findActiveByJourney(journey.getId()) != null){
                    ctx.redirect("/dashboard.html?fejl= runde-i-gang");
                    return;
                }

                //4. rundenummer = antal runder i forløbt + 1 (første runde bliver nr.1)
                int roundNumber = roundDAO.findActiveByJourney(journey.getId()).size() + 1;

                //5. Byg kortet og gem. end_date og result er null, til runden afsluttes
                Round round = new Round(0, journey.getId(), roundNumber, TreatmentType.valueOf(type),
                        LocalDate.parse(startDate), null, RoundStatus.IN_PROGRESS, null);
                roundDAO.save(round);

                //6.ind på dashboard
                ctx.redirect("/dashboard.html");

            });

        }
        //   0. start-runde.html: <form action="/start-runde" method="post">
        //   1. læs felterne: type (IVF/ICSI/IUI/FET), startDate
        //   2. int patientId = 1;  // TODO: fra session
        //   3. FertilityJourney journey = journeyDao.findActiveByPatient(patientId)
        //   4. hvis journey == null: lav et nyt
        //        int journeyId = journeyDao.save(new FertilityJourney(0, patientId, LocalDate.parse(startDate), JourneyStatus.ACTIVE))
        //      ellers: journeyId = journey.getId()
        //   5. gem runden: roundDao.save(new Round(0, journeyId, 1, TreatmentType.valueOf(type), LocalDate.parse(startDate), null, RoundStatus.IN_PROGRESS, null))
        //      (rundenummer 1 indtil videre – TODO: tæl eksisterende runder + 1)
        //   6. redirect til dashboard.html
    }
