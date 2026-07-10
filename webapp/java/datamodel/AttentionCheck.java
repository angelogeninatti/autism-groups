package datamodel;

public class AttentionCheck {
    private final int userAnswer;
    private final int correctAnswer;
    public AttentionCheck(int userAnswer, int correctAnswer) {
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
    }
    public boolean checkAttentionChecks() {
        return userAnswer == correctAnswer;
    }

    public String toString() {
        return "Attention checks: " + userAnswer + " - " + correctAnswer;
    }
}