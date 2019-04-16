package Administration;

public interface TopicNames {
    String Info = "info";
    String ExaminationRequest = "examination";
    String ExaminationResponse = "result";

    static String setRequestTopic(ExaminationType e, String doctorName) {
        return ExaminationRequest + "." + e.getType() + "." + doctorName;
    }

    static String getAnyExamResponse(String doctorName) {
        return ExaminationResponse + ".*." + doctorName;
    }

    static String getExamResponse(String requestKey) {
        int lastDotIndex = requestKey.lastIndexOf(".");
        String doctorName = requestKey.substring(lastDotIndex + 1);
        String examinationType = requestKey.substring(requestKey.indexOf(".") + 1, lastDotIndex);
        return ExaminationResponse + "." + examinationType + "." + doctorName;
    }
}
