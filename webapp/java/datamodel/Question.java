package datamodel;

public class Question {
    private final int id;
    private final String textItalian;
    private final String textEnglish;
    private final boolean inverted;

    public Question(int id, String textItalian, String textEnglish, boolean inverted) {
        this.id = id;
        this.textItalian = textItalian;
        this.textEnglish = textEnglish;
        this.inverted = inverted;
    }

    public int getId() {
        return id;
    }

    public String getTextItalian() {
        return textItalian;
    }

    public String getTextEnglish() {
        return textEnglish;
    }

    public boolean isInverted() {
        return inverted;
    }
}