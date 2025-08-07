package kassuk.addon.blackout.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import kassuk.addon.blackout.modules.SoundModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinExplosion {
    @WrapOperation(
        method = "onExplosion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;playSoundClient(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"
        )
    )
    private void onExplosion(ClientWorld instance, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance, Operation<Void> original) {
        SoundModifier m = Modules.get().get(SoundModifier.class);

        if (m.isActive() && m.expSound.get()) {
            original.call(instance, x, y, z, sound, category, (float)(volume * m.explosionVolume.get()), (float)(pitch * m.explosionPitch.get()), useDistance);
        }
    }
}
