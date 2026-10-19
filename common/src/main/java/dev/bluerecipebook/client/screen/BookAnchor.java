package dev.bluerecipebook.client.screen;

public enum BookAnchor {
    INVENTORY(104, -22),
    CRAFTING(5, -49);

    private final int xFromLeft;
    private final int yFromCenter;

    BookAnchor(int xFromLeft, int yFromCenter) {
        this.xFromLeft = xFromLeft;
        this.yFromCenter = yFromCenter;
    }

    public int buttonX(int leftPos) {
        return leftPos + this.xFromLeft;
    }

    public int buttonY(int screenHeight) {
        return screenHeight / 2 + this.yFromCenter;
    }
}
