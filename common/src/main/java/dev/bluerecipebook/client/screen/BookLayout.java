package dev.bluerecipebook.client.screen;

/** Layout numbers that match vanilla's recipe book and the screens that host it. */
public final class BookLayout {
    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    public static final int NARROW_SCREEN = 379;
    public static final int OFFSET_X = 86;
    public static final int RECIPES_PER_PAGE = 20;

    private BookLayout() {
    }

    public static int screenLeft(int screenWidth, int imageWidth, boolean bookOpen, boolean narrow) {
        if (bookOpen && !narrow) {
            return 177 + (screenWidth - imageWidth - 200) / 2;
        }
        return (screenWidth - imageWidth) / 2;
    }

    public static int bookX(int screenWidth, boolean narrow) {
        int offset = narrow ? 0 : OFFSET_X;
        return (screenWidth - IMAGE_WIDTH) / 2 - offset;
    }

    public static int bookY(int screenHeight) {
        return (screenHeight - IMAGE_HEIGHT) / 2;
    }
}
