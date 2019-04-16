package Administration;

public enum ExaminationType {
    KNEE("knee"),
    ELBOW("elbow"),
    HIP("hip");

    private final String type;

    ExaminationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
