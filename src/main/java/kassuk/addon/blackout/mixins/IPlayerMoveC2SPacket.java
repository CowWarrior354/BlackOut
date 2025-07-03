package kassuk.addon.blackout.mixins;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface IPlayerMoveC2SPacket {
    @Accessor("x")
    double blackout$getX();

    @Accessor("y")
    double blackout$getY();

    @Accessor("z")
    double blackout$getZ();
}
