package dev.bluerecipebook.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Returns the description with each separator line fitted to the settings column.
 * A separator is a line of one symbol repeated at least three times, such as
 * {@code ───} drawn as {@code ────────────────}, with any formatting codes at its start and end kept.
 * The length is measured again whenever the text is drawn, and a line that cannot fit stays as written.
 */
public final class DescriptionSeparators {
    private static final int MINIMUM = 3;
    private static final char SECTION = '\u00a7';

    private DescriptionSeparators() {
    }

    public static Component fit(Component text) {
        return new Fitted(text);
    }

    private static Component apply(Component text) {
        int column = columnWidth();
        if (column <= 0) {
            return text;
        }
        Font font = Minecraft.getInstance().font;
        String[] lines = text.getString().split("\n", -1);
        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            int body = bodyStart(lines[i]);
            int end = bodyEnd(lines[i]);
            if (!isSeparator(lines[i], body, end)) {
                continue;
            }
            String fitted = fitted(font, column, lines[i].substring(0, body), lines[i].charAt(body),
                    lines[i].substring(end));
            if (fitted == null) {
                continue;
            }
            lines[i] = fitted;
            changed = true;
        }
        if (!changed) {
            return text;
        }
        return Component.literal(String.join("\n", lines));
    }

    // A formatting code is the section sign plus the next character, and it has no width.
    private static int bodyStart(String line) {
        int index = 0;
        while (index + 1 < line.length() && line.charAt(index) == SECTION) {
            index += 2;
        }
        return index;
    }

    private static int bodyEnd(String line) {
        int index = line.length();
        while (index - 2 >= 0 && line.charAt(index - 2) == SECTION) {
            index -= 2;
        }
        return index;
    }

    private static boolean isSeparator(String line, int body, int end) {
        if (end - body < MINIMUM) {
            return false;
        }
        char symbol = line.charAt(body);
        if (Character.isWhitespace(symbol)) {
            return false;
        }
        for (int i = body + 1; i < end; i++) {
            if (line.charAt(i) != symbol) {
                return false;
            }
        }
        return true;
    }

    private static String fitted(Font font, int columnWidth, String prefix, char symbol, String suffix) {
        StringBuilder line = new StringBuilder(prefix).append(symbol);
        int width = font.width(line.toString());
        if (width > columnWidth) {
            return null;
        }
        while (true) {
            line.append(symbol);
            int grown = font.width(line.toString());
            if (grown <= width || grown > columnWidth) {
                line.setLength(line.length() - 1);
                return line.append(suffix).toString();
            }
            width = grown;
        }
    }

    // YetAnotherConfigLib's description column. A longer line wraps onto the next row.
    private static int columnWidth() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int columnWidth = screenWidth / 3;
        int padding = columnWidth / 20;
        columnWidth = Math.min(columnWidth, 400);
        return columnWidth - padding * 2;
    }

    private static final class Fitted implements Component {
        private final Component source;

        private Fitted(Component source) {
            this.source = source;
        }

        @Override
        public Style getStyle() {
            return Style.EMPTY;
        }

        @Override
        public ComponentContents getContents() {
            return apply(this.source).getContents();
        }

        @Override
        public List<Component> getSiblings() {
            return List.of();
        }

        @Override
        public FormattedCharSequence getVisualOrderText() {
            return Language.getInstance().getVisualOrder(this);
        }
    }
}
