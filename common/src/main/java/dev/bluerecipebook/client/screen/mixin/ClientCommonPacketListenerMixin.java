package dev.bluerecipebook.client.screen.mixin;

import dev.bluerecipebook.client.data.PlayerDataDisconnect;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the disconnect button when this mod leaves a singleplayer world.
 * The label is vanilla {@code gui.toTitle}.
 * If later Minecraft versions build this screen differently we can drop this mixin for simplicity. The connection disconnect stays.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerMixin {
    @Inject(method = "createDisconnectScreen(Lnet/minecraft/network/DisconnectionDetails;)Lnet/minecraft/client/gui/screens/Screen;", at = @At("HEAD"), cancellable = true)
    private void blueRecipeBook$title(DisconnectionDetails details, CallbackInfoReturnable<Screen> callback) {
        if (!PlayerDataDisconnect.consumeTitleReturn()) {
            return;
        }
        callback.setReturnValue(new DisconnectedScreen(
                new TitleScreen(),
                Component.translatable("disconnect.lost"),
                details.reason(),
                Component.translatable("gui.toTitle")));
    }
}
