package datamodel;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Activity {
    private final int id;
    private final String titleItalian;
    private final String titleEnglish;
    private final String descriptionItalian;
    private final String descriptionEnglish;
    private final String categoryItalian;
    private final String categoryEnglish;
    private final String city;
    private final SpqEvaluation spqEvaluation;
    private final int like;

    public Activity(int id, String titleItalian, String titleEnglish, String descriptionItalian, String descriptionEnglish, String categoryItalian, String categoryEnglish, String city, SpqEvaluation spqEvaluation, int like) {
        this.id = id;
        this.titleItalian = titleItalian;
        this.titleEnglish = titleEnglish;
        this.descriptionItalian = descriptionItalian;
        this.descriptionEnglish = descriptionEnglish;
        this.categoryItalian = categoryItalian;
        this.categoryEnglish = categoryEnglish;
        this.city = city;
        this.spqEvaluation = spqEvaluation;
        this.like = like;
    }

    public String getTitleItalian() {
        return titleItalian;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public String getDescriptionItalian() {
        return descriptionItalian;
    }

    public String getDescriptionEnglish() {
        return descriptionEnglish;
    }

    public String getCategoryItalian() {
        return categoryItalian;
    }

    public String getCategoryEnglish() {
        return categoryEnglish;
    }

    public String getCity() {
        return city;
    }

    public SpqEvaluation getSpqEvaluation() {
        return spqEvaluation;
    }

    public int getId() {
        return id;
    }

    public int getLike() {
        return like;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id +
                ", titleItalian='" + titleItalian + '\'' +
                ", titleEnglish='" + titleEnglish + '\'' +
                ", descriptionItalian='" + descriptionItalian + '\'' +
                ", descriptionEnglish='" + descriptionEnglish + '\'' +
                ", categoryItalian='" + categoryItalian + '\'' +
                ", categoryEnglish='" + categoryEnglish + '\'' +
                ", city='" + city + '\'' +
                ", spqEvaluation=" + spqEvaluation +
                ", like=" + like +
                '}';
    }
}