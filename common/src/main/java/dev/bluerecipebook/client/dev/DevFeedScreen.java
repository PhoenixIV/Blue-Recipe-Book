package dev.bluerecipebook.client.dev;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dev-only popup of test sequences. A test button starts its sequence and closes the screen,
 * and Escape closes it without a sequence.
 */
public final class DevFeedScreen extends Screen {
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW = 24;
    private static final int PANEL_PADDING = 12;

    private static final List<Test> TESTS = List.of(
            // One new recipe unlocks, so the page is not overwhelmed.
            new Test("Poppy", List.of("give @s minecraft:poppy 1")),
            // Tests group behavior: three plank types that share a group and unlock other groups.
            new Test("Planks", List.of(
                    "give @s minecraft:oak_planks 1",
                    "give @s minecraft:spruce_planks 1",
                    "give @s minecraft:birch_planks 1")),
            // Strict discovery: the fence stays hidden until planks and a stick are both held.
            new Test("Fence", List.of(
                    "give @s minecraft:oak_planks 1",
                    "give @s minecraft:stick 1")),
            // Strict discovery: the beacon stays hidden until glass, obsidian, and the nether star are all held.
            new Test("Beacon", List.of(
                    "give @s minecraft:nether_star 1",
                    "give @s minecraft:obsidian 1",
                    "give @s minecraft:glass 1")),
            // Observes a recipe taken away after it was unlocked.
            new Test("Give & Take plank", List.of(
                    "recipe give @s minecraft:oak_planks",
                    "recipe take @s minecraft:oak_planks")),
            // Observes removal while a group is popped open.
            new Test("Give & Take glass", List.of(
                    "recipe give @s minecraft:red_stained_glass_pane",
                    "recipe give @s minecraft:green_stained_glass_pane",
                    "recipe give @s minecraft:blue_stained_glass_pane",
                    "recipe take @s minecraft:red_stained_glass_pane",
                    "recipe take @s minecraft:green_stained_glass_pane",
                    "recipe take @s minecraft:blue_stained_glass_pane")));

    private final Screen parent;

    public DevFeedScreen(Screen parent) {
        super(Component.literal("Feed"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.left();
        int top = this.top();
        for (int i = 0; i < TESTS.size(); i++) {
            Test test = TESTS.get(i);
            this.addRenderableWidget(Button.builder(Component.literal(test.label()), button -> {
                DevFeed.start(test.commands());
                this.onClose();
            }).bounds(left, top + i * ROW, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        int left = this.left() - PANEL_PADDING;
        int top = this.top() - PANEL_PADDING - this.font.lineHeight - 4;
        int bottom = this.top() + TESTS.size() * ROW + PANEL_PADDING;
        graphics.fill(left, top, left + BUTTON_WIDTH + PANEL_PADDING * 2, bottom, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.top() - PANEL_PADDING - this.font.lineHeight, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private int left() {
        return this.width / 2 - BUTTON_WIDTH / 2;
    }

    private int top() {
        return this.height / 2 - TESTS.size() * ROW / 2;
    }

    private record Test(String label, List<String> commands) {
    }
}
