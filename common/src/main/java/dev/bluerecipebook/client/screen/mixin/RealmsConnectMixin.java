package dev.bluerecipebook.client.screen.mixin;

import com.mojang.realmsclient.dto.RealmsServer;
import dev.bluerecipebook.client.discover.DiscoveryFiles;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.realms.RealmsConnect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records which realm is joined. The server entry vanilla builds for it has only the host. */
@Mixin(RealmsConnect.class)
public class RealmsConnectMixin {
    @Inject(method = "connect", at = @At("HEAD"))
    private void blueRecipeBook$rememberRealm(RealmsServer server, ServerAddress address, CallbackInfo callback) {
        DiscoveryFiles.joiningRealm(server.id, server.name);
    }
}
