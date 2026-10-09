# User stories med acceptkriterier

*Hvert acceptkriterie følger Givet/Når/Så-formatet (Gherkin), med en kort forklaring af, hvad det konkret tester. User stories matcher domænemodellen (Patient → FertilityJourney → Round, med Diagnosis, Document og Notification): hver kasse og attribut i modellen kan spores til en story herunder.*

### User story 1
Som patient vil jeg kunne oprette et nyt fertilitetsforløb, så jeg kan begynde at følge min behandling fra start.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en patient er logget ind, når patienten opretter et nyt fertilitetsforløb, så vises det nye forløb på patientens oversigt" → tester at selve oprettelsen virker og bliver synlig (en patient kan godt have flere forløb over tid)
- Acceptkriterie 2: "Givet at patienten opretter et forløb, når formularen vises, så er startdatoen udfyldt med dags dato og kan ændres" → tester at man ikke behøver at indtaste datoen, men kan vælge en tidligere dato, hvis forløbet allerede er startet (fx med konsultationer og blodprøver før første runde)
- Acceptkriterie 3: "Givet patienten allerede har et forløb med status ACTIVE, når hun forsøger at oprette et nyt, så oprettes der ikke noget nyt forløb, og dashboardet viser det eksisterende forløb" → tester at en patient højst kan have ét aktivt forløb ad gangen

### User story 2
Som patient vil jeg kunne se en tidslinje over hændelser i en runde, så jeg ved, hvor langt jeg er nået, uden at skulle spørge min klinik.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en runde med registrerede hændelser, når patienten åbner tidslinjen, så vises hændelserne sorteret efter dato" → tester at sorteringen er korrekt
- Acceptkriterie 2: "Givet en ny hændelse registreres, når den gemmes, så vises hændelsen på tidslinjen i korrekt kronologisk rækkefølge, næste gang tidslinjen åbnes" → tester at systemet selv tilføjer hændelsen, uden at patienten skal skrive den ind

### User story 3
Som patient vil jeg kunne se mine kommende aftaler, så jeg ikke overser vigtige tider i mit forløb.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet flere aftaler registreret, når aftaleoversigten åbnes, så vises de sorteret efter dato, nærmeste først" → tester sorteringslogikken
- Acceptkriterie 2: "Givet en oprettet aftale, når den vises i listen, så fremgår dato, type og lokation" → tester at de rigtige felter faktisk er synlige
- Acceptkriterie 3: "Givet flere forløb, når aftalerne vises, så knyttes hver aftale til det korrekte forløb" → tester at data ikke blandes sammen mellem forløb
- Acceptkriterie 4: "Givet en logget-ind patient, når en aftale oprettes med dato, type og lokation, så gemmes den på det aktive forløb og vises i oversigten" → tester at patienten selv kan oprette aftaler

### User story 4
Som patient vil jeg kunne skrive dagbogsnoter, så jeg har et bedre overblik over mine tanker og følelser lige i momentet – også før og efter et forløb.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en fritekst-note skrives og gemmes, så gemmes den med dato og titel" → tester at gem-funktionen virker med de rigtige felter
- Acceptkriterie 2: "Givet en note gemmes, når den vises igen, så er den tilknyttet patienten og vises, også selvom patienten ikke har et aktivt forløb" → tester korrekt tilknytning (noter ligger på patienten, ændret uge 40)

### User story 5
Som patient vil jeg kunne logge ind, så jeg kan få adgang til mit eget, private forløb.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en bruger uden profil forsøger at logge ind, når login fejler pga. manglende profil, så vises en mulighed for at oprette en ny profil (jf. User story 6)" → tester at nye brugere korrekt henvises videre til oprettelse
- Acceptkriterie 2: "Givet korrekt brugernavn og adgangskode, når de indtastes, så logges brugeren ind og får adgang til eget forløb" → tester den centrale sikkerhedsfunktion
- Acceptkriterie 3: "Givet forkert brugernavn eller adgangskode, når der klikkes Log ind, så vises en fejlbesked, og brugeren logges ikke ind" → tester at uautoriseret adgang afvises

### User story 6a
Som patient vil jeg kunne oprette en profil, så jeg kan blive registreret som bruger af systemet.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en ny bruger uden profil, når navn, fødselsdato, brugernavn og adgangskode oprettes, så oprettes en ny patientprofil" → tester at oprettelsen lykkes med alle felter
- Acceptkriterie 2: "Givet oplysninger er indtastet forkert eller brugernavn taget, så kommer fejlmeddelelse om at prøve igen" → tester fejlhåndtering ved ugyldig oprettelse

### User story 6b
Som patient vil jeg kunne redigere min profil og slette min konto, så mine oplysninger er korrekte, og jeg selv bestemmer over mine data.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en logget-ind patient, når profilen redigeres, så kan navn og fødselsdato opdateres" → tester redigeringsfunktionen
- Acceptkriterie 2: "Givet en logget-ind patient, når hun sletter sin konto og bekræfter, så slettes kontoen og alle tilknyttede data (forløb, runder, logs, dokumenter, notifikationer)" → tester at patienten ejer sine data og kan fjerne dem helt

*US6 er splittet i 6a og 6b efter INVEST-vurdering (Small): oprettelse og redigering/sletning er to selvstændige flows.*

### User story 7
Som patient vil jeg kunne registrere mine diagnoser, så min behandler og jeg selv har overblik over min sygdomshistorik.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en logget-ind patient, når en ny diagnose med navn og beskrivelse registreres, så gemmes diagnosen på patientens profil" → tester at oprettelsen lykkes
- Acceptkriterie 2: "Givet flere registrerede diagnoser, når profilen ses, så vises alle patientens diagnoser" → tester at en patient kan have flere diagnoser samtidig
- Acceptkriterie 3: "Givet en registreret diagnose, når patienten sletter den og bekræfter, så fjernes den fra listen" → tester at en forkert diagnose kan fjernes igen

### User story 8
Som patient vil jeg kunne registrere mit medicinindtag, så jeg kan holde styr på, om jeg har taget min medicin som planlagt.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en aktiv runde, når en registrering oprettes med en valgt medicin, dosis og tidspunkt, så gemmes registreringen tilknyttet den korrekte medicin" → tester at registreringen refererer korrekt til medicin-stamdata
- Acceptkriterie 2: "Givet en gemt registrering, når medicinlisten ses, så vises medicinnavn, dosis og tidspunkt korrekt" → tester at data også vises korrekt bagefter
- Acceptkriterie 3: "Givet en planlagt dosis, når patienten markerer den som taget, så vises den som taget i medicinlisten" → tester at medicinloggen fungerer som daglig tjekliste

### User story 9
Som patient vil jeg kunne registrere mine hormonværdier, så jeg kan følge udviklingen i min behandling.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en aktiv runde, når hormontype, værdi, enhed og dato registreres, så gemmes målingen tilknyttet runden" → tester at registreringen lykkes med alle felter
- Acceptkriterie 2: "Givet flere målinger, når seneste værdi tjekkes, så vises den nyeste måling korrekt" → tester at "seneste" beregnes rigtigt

### User story 10a
Som patient vil jeg kunne starte og afslutte en runde, så mit forløb afspejler, hvor jeg er i behandlingen.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet et fertilitetsforløb, når en ny runde startes med behandlingstype og startdato, så oprettes runden tilknyttet forløbet med næste rundenummer" → tester at "start ny runde" opretter en selvstændig runde, og at systemet selv tæller rundenummeret op
- Acceptkriterie 2: "Givet en ny runde startes, når behandlingstype vælges, så kan der vælges mellem IVF, ICSI, IUI og FET" → tester at behandlingstyperne er faste værdier og matcher ordlisten
- Acceptkriterie 3: "Givet en runde i gang, når den afsluttes med et resultat, så gemmes resultatet på runden" → tester at afslutning og resultat hænger sammen på det korrekte niveau

### User story 10b
Som patient vil jeg kunne se min rundehistorik, så jeg kan følge, hvordan mine tidligere forsøg er gået.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet tidligere gennemførte runder, når rundehistorikken åbnes, så vises alle runder grupperet pr. forløb – også fra afsluttede forløb" → tester at historikken viser alle runder, ikke kun den seneste eller kun det aktive forløb
- Acceptkriterie 2: "Givet en runde vælges i historikken, når den åbnes, så vises rundenummer, behandlingstype, start- og slutdato, status og resultat" → tester at detaljerne vises korrekt

*US10 er splittet i 10a og 10b efter INVEST-vurdering (Small): at starte/afslutte en runde og at se historik er to selvstændige features.*

### User story 11
Som patient vil jeg kunne tilføje og se mine dokumenter, så jeg har adgang til henvisning, blodprøvesvar og behandlingsplan ét sted – også før første runde.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en patient med tilknyttede dokumenter, når dokumentlisten åbnes, så vises alle dokumenter med titel, type og upload-dato" → tester at dokumenter vises korrekt
- Acceptkriterie 2: "Givet et dokument vælges, når det åbnes, så vises filens indhold via den gemte filPath" → tester at det faktiske dokument kan tilgås
- Acceptkriterie 3: "Givet et dokument tilføjes med titel, dokumenttype og fil, når dokumenttype vælges, så kan der vælges mellem Henvisning, Blodprøvesvar, Behandlingsplan og Andet" → tester at patienten selv kan tilføje dokumenter, og at dokumenttyperne er faste værdier, der matcher ordlisten

### User story 12
Som patient vil jeg kunne modtage notifikationer, så jeg ikke overser vigtige påmindelser om medicin eller aftaler.

**Acceptkriterier:**
- Acceptkriterie 1: "Givet en patient har en planlagt medicindosis i dag, når appen åbnes, så oprettes en notifikation af typen MEDICATION_REMINDER for dosen" → tester at systemet selv genererer relevante påmindelser
- Acceptkriterie 2: "Givet ulæste notifikationer, når patienten åbner listen, så vises de med titel og besked, og det fremgår tydeligt, hvilke der er læst, og hvilke der ikke er"