package datamodel;

public class SPQResult {
    private final int total;
    private final int touch;
    private final int smell;
    private final int vision;
    private final int hearing;
    private final int taste;

    public SPQResult(int total, int touch, int smell, int vision, int hearing, int taste) {
        this.total = total;
        this.touch = touch;
        this.smell = smell;
        this.vision = vision;
        this.hearing = hearing;
        this.taste = taste;
    }

    public int getVision() {
        return vision;
    }

    public int getHearing() {
        return hearing;
    }

    public int getTaste() {
        return taste;
    }

    public int getSmell() {
        return smell;
    }

    public int getTouch() {
        return touch;
    }

    public int getTotal() {
        return total;
    }
}