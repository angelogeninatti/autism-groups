package datamodel;

public class SPQQuestion extends Question {
    private final String mainCategory;
    public SPQQuestion(int id, String textItalian, String textEnglish, boolean inverted, String mainCategory) {
        super(id, textItalian, textEnglish, inverted);
        this.mainCategory = mainCategory;
    }

    public String getMainCategory() {
        return mainCategory;
    }
}