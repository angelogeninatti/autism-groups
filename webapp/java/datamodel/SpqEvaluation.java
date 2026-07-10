package datamodel;

public class SpqEvaluation {
    private final String visionScore;
    private final String hearingScore;
    private final String smellScore;
    private final String tasteScore;
    private final String touchScore;
    private final String visionDescriptionItalian;
    private final String hearingDescriptionItalian;
    private final String smellDescriptionItalian;
    private final String tasteDescriptionItalian;
    private final String touchDescriptionItalian;
    private final String visionDescriptionEnglish;
    private final String hearingDescriptionEnglish;
    private final String smellDescriptionEnglish;
    private final String tasteDescriptionEnglish;
    private final String touchDescriptionEnglish;


    public SpqEvaluation(String visionScore, String hearingScore, String smellScore, String tasteScore, String touchScore, String visionDescriptionItalian, String hearingDescriptionItalian, String smellDescriptionItalian, String tasteDescriptionItalian, String touchDescriptionItalian, String visionDescriptionEnglish, String hearingDescriptionEnglish, String smellDescriptionEnglish, String tasteDescriptionEnglish, String touchDescriptionEnglish) {
        this.visionScore = visionScore;
        this.hearingScore = hearingScore;
        this.smellScore = smellScore;
        this.tasteScore = tasteScore;
        this.touchScore = touchScore;
        this.visionDescriptionItalian = visionDescriptionItalian;
        this.hearingDescriptionItalian = hearingDescriptionItalian;
        this.smellDescriptionItalian = smellDescriptionItalian;
        this.tasteDescriptionItalian = tasteDescriptionItalian;
        this.touchDescriptionItalian = touchDescriptionItalian;
        this.visionDescriptionEnglish = visionDescriptionEnglish;
        this.hearingDescriptionEnglish = hearingDescriptionEnglish;
        this.smellDescriptionEnglish = smellDescriptionEnglish;
        this.tasteDescriptionEnglish = tasteDescriptionEnglish;
        this.touchDescriptionEnglish = touchDescriptionEnglish;
    }

    public String getVisionScore() {
        return visionScore;
    }

    public String getHearingScore() {
        return hearingScore;
    }

    public String getTasteScore() {
        return tasteScore;
    }

    public String getTouchScore() {
        return touchScore;
    }

    public String getSmellScore() {
        return smellScore;
    }

    public String getVisionDescriptionItalian() {
        return visionDescriptionItalian;
    }

    public String getHearingDescriptionItalian() {
        return hearingDescriptionItalian;
    }

    public String getTasteDescriptionItalian() {
        return tasteDescriptionItalian;
    }

    public String getTouchDescriptionItalian() {
        return touchDescriptionItalian;
    }

    public String getVisionDescriptionEnglish() {
        return visionDescriptionEnglish;
    }

    public String getHearingDescriptionEnglish() {
        return hearingDescriptionEnglish;
    }

    public String getSmellDescriptionEnglish() {
        return smellDescriptionEnglish;
    }

    public String getTasteDescriptionEnglish() {
        return tasteDescriptionEnglish;
    }

    public String getTouchDescriptionEnglish() {
        return touchDescriptionEnglish;
    }

    public String getSmellDescriptionItalian() {
        return smellDescriptionItalian;
    }
}