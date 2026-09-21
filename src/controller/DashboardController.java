package controller;

import io.javalin.Javalin;

public class DashboardController {

        public static void registerroutes(Javalin app) {
            // POST /start-runde – når brugeren trykker "Start runde" på start-runde.html
            app.post("/opret-forløb", ctx -> {
                
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
