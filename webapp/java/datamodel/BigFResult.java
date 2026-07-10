package datamodel;

public class BigFResult {
    private final int total;
    private final int agreeableness;
    private final int conscientiousness;
    private final int emotionalStability;
    private final int extroversion;
    private final int openness;

    public BigFResult(int total, int agreeableness, int conscientiousness, int emotionalStability, int extroversion, int openness) {
        this.total = total;
        this.agreeableness = agreeableness;
        this.conscientiousness = conscientiousness;
        this.emotionalStability = emotionalStability;
        this.extroversion = extroversion;
        this.openness = openness;
    }

    public int getOpenness() {
        return openness;
    }

    public int getExtroversion() {
        return extroversion;
    }

    public int getEmotionalStability() {
        return emotionalStability;
    }

    public int getConscientiousness() {
        return conscientiousness;
    }

    public int getAgreeableness() {
        return agreeableness;
    }

    public int getTotal() {
        return total;
    }
}