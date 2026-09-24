package enums;

// Status på et fertilitetsforløb. En patient har højst ét ACTIVE forløb ad gangen (håndhæves i service-laget).
public enum JourneyStatus {
    ACTIVE,
    COMPLETED
}
