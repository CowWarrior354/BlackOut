package kassuk.addon.blackout.mixins;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.screen.sync.ComponentChangesHash;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface ComponentHasherNetworkHandlerAccessor {
    @Accessor("componentHasher")
    ComponentChangesHash.ComponentHasher getComponentHasher();
}
