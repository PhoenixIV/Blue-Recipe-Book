package dev.bluerecipebook.client;

import dev.bluerecipebook.BlueRecipeBook;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class BookSettingsScreen {
    private BookSettingsScreen() {
    }

    public static Screen open(Screen parent) {
        return YetAnotherConfigLib.create(BookSettings.handler(), (defaults, config, builder) -> builder
                        .title(Component.literal(BlueRecipeBook.MOD_NAME))
                        .category(ConfigCategory.createBuilder()
                                .name(Component.translatable("gui.bluerecipebook.config.category.general"))
                                .option(toggle(
                                        "strictlyRequireEveryIngredient",
                                        false,
                                        defaults.strictlyRequireEveryIngredient,
                                        () -> config.strictlyRequireEveryIngredient,
                                        value -> config.strictlyRequireEveryIngredient = value))
                                .option(toggle(
                                        "revealAllRecipes",
                                        false,
                                        defaults.revealAllRecipes,
                                        () -> config.revealAllRecipes,
                                        value -> config.revealAllRecipes = value))
                                .option(toggle(
                                        "groupRecipes",
                                        true,
                                        defaults.groupRecipes,
                                        () -> config.groupRecipes,
                                        value -> config.groupRecipes = value))
                                .option(toggle(
                                        "scrollToMoveItems",
                                        defaults.scrollToMoveItems,
                                        () -> config.scrollToMoveItems,
                                        value -> config.scrollToMoveItems = value))
                                .build()))
                .generateScreen(parent);
    }

    /**
     * Builds an ON/OFF option whose value text tags the vanilla state.
     *
     * @param vanillaValue the state vanilla uses, which gets the "Vanilla" tag
     */
    private static Option<Boolean> toggle(String name, boolean vanillaValue, boolean defaultValue,
            Supplier<Boolean> read, Consumer<Boolean> write) {
        return toggle(name, defaultValue, read, write, on -> status(on, vanillaValue));
    }

    /** Builds an ON/OFF option. Neither state is vanilla, so the value text has no "Vanilla" tag. */
    private static Option<Boolean> toggle(String name, boolean defaultValue,
            Supplier<Boolean> read, Consumer<Boolean> write) {
        return toggle(name, defaultValue, read, write, on -> onOff(on));
    }

    private static Option<Boolean> toggle(String name, boolean defaultValue,
            Supplier<Boolean> read, Consumer<Boolean> write, ValueFormatter<Boolean> format) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("gui.bluerecipebook.config." + name))
                .description(OptionDescription.of(DescriptionSeparators.fit(
                        Component.translatable("gui.bluerecipebook.config." + name + ".description"))))
                .binding(defaultValue, read, write)
                .controller(option -> BooleanControllerBuilder.create(option)
                        .formatValue(format))
                .build();
    }

    /** Returns the ON or OFF word in green or red, with " (Vanilla)" after the vanilla state. */
    private static MutableComponent status(boolean on, boolean vanillaValue) {
        MutableComponent text = onOff(on);
        if (on == vanillaValue) {
            text.append(" (")
                    .append(Component.translatable("gui.bluerecipebook.vanilla"))
                    .append(")");
        }
        return text;
    }

    /** Returns the ON or OFF word in green or red. */
    private static MutableComponent onOff(boolean on) {
        return Component.translatable(on ? "options.on" : "options.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
