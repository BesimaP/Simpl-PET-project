    package entities;

    import java.time.LocalDateTime;

    // En dagbogsnote på forløbet (tabel diary_entry, US4).
    public class DiaryEntry {

        private int id;
        private int fertilityJourneyId;   // FK til fertility_journey.id
        private LocalDateTime dateTime;
        private String title;
        private String content;

        public DiaryEntry(int id, int fertilityJourneyId, LocalDateTime dateTime, String title, String content) {
            this.id = id;
            this.fertilityJourneyId = fertilityJourneyId;
            this.dateTime = dateTime;
            this.title = title;
            this.content = content;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getFertilityJourneyId() {
            return fertilityJourneyId;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public String getTitle() {
            return title;
        }
        public String getContent() {
            return content;
        }
    }
