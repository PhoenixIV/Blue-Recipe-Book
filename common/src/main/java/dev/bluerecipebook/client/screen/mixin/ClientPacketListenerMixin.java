package dev.bluerecipebook.client.screen.mixin;

import dev.bluerecipebook.client.book.NewArrivals;
import dev.bluerecipebook.client.discover.DiscoveryOrder;
import dev.bluerecipebook.client.strict.StrictDiscovery;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleAddOrRemoveRecipes(Lnet/minecraft/network/protocol/game/ClientboundRecipePacket;)V", at = @At("RETURN"))
    private void blueRecipeBook$recordDiscovery(ClientboundRecipePacket packet, CallbackInfo callback) {
        switch (packet.getState()) {
            case ADD -> {
                DiscoveryOrder.onUnlocked(packet.getRecipes());
                StrictDiscovery.onUnlocked();
            }
            case REMOVE -> {
                DiscoveryOrder.onRemoved(packet.getRecipes());
                StrictDiscovery.onRemoved(packet.getRecipes());
            }
            default -> {
                // INIT is the full known set and does not append, so a rejoin keeps the saved order.
            }
        }
    }

    @Inject(method = "handleAddOrRemoveRecipes(Lnet/minecraft/network/protocol/game/ClientboundRecipePacket;)V", at = @At("RETURN"))
    private void blueRecipeBook$initialArrivals(ClientboundRecipePacket packet, CallbackInfo callback) {
        if (packet.getState() == ClientboundRecipePacket.State.INIT) {
            NewArrivals.onInitialBook();
        }
    }
}
