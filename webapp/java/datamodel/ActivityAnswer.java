package datamodel;

import com.google.gson.JsonArray;

import java.util.Arrays;

public class ActivityAnswer {
    private final int activityId;
    private final int[] answers;

    public ActivityAnswer (int activityId, int[] answers) {
        this.activityId = activityId;
        this.answers = answers;
    }

    public int getActivityId() {
        return this.activityId;
    }
    public int[] getAnswer() {
        return this.answers;
    }

    public String toString() {
        return this.activityId + " " + Arrays.toString(this.answers);
    }
}