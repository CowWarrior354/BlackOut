package kassuk.addon.blackout.hud;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author OLEPOSSU
 */

public class ArmorHudPlus extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Scale")
        .description("Scale to render at.")
        .defaultValue(1)
        .range(0.1, 5)
        .sliderRange(0.1, 5)
        .build()
    );
    private final Setting<Integer> rounding = sgGeneral.add(new IntSetting.Builder()
        .name("Rounding")
        .description("How rounded should the background be.")
        .defaultValue(50)
        .range(0, 100)
        .sliderRange(0, 100)
        .visible(() -> false) // trick to keep configs while rounding is temporarily disabled
        .build()
    );
    private final Setting<Boolean> bg = sgGeneral.add(new BoolSetting.Builder()
        .name("Background")
        .description("Renders a background behind armor pieces.")
        .defaultValue(false)
        .build()
    );
    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Background Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(0, 0, 0, 150))
        .build()
    );
    private final Setting<SettingColor> durColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Durability Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<DurMode> durMode = sgGeneral.add(new EnumSetting.Builder<DurMode>()
        .name("Durability Mode")
        .description("Where should durability be rendered at.")
        .defaultValue(DurMode.Bottom)
        .build()
    );

    public static final HudElementInfo<ArmorHudPlus> INFO = new HudElementInfo<>(BlackOut.HUD_BLACKOUT, "ArmorHud+", "A target hud the fuck you thinkin bruv.", ArmorHudPlus::new);

    public ArmorHudPlus() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.player == null) {return;}

        setSize(100 * scale.get() * 2, 28 * scale.get() * 2);
        MatrixStack stack = new MatrixStack();

        stack.translate(x, y, 0);
        stack.scale((float)(scale.get() * 2), (float)(scale.get() * 2), 1);

        if (bg.get()) {
            renderer.quad(
                x,
                y,
                100 * scale.get() * 2,
                28 * scale.get() * 2,
                bgColor.get()
            );
            /*
            RoundedQuadOld.render(
                renderer,
                x,
                y,
                100 * scale.get() * 2,
                28 * scale.get() * 2,
                (rounding.get() * 0.14f) * scale.get() * 2,
                bgColor.get()
            );
             */
        }

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack itemStack = mc.player.getEquippedStack(slot);

            if (itemStack.isEmpty()) continue;

            String text = String.valueOf(Math.round(100 - (float) itemStack.getDamage() / itemStack.getMaxDamage() * 100f));
            centeredText(renderer, text, x + (slot.getIndex() * 40 + 40) * scale.get(), y + (durMode.get() == DurMode.Top ? 6 : 34) * scale.get(), 40 * scale.get(), durColor.get(), scale.get());
        }

        renderer.post(() -> {
            Matrix3x2f drawStack = renderer.drawContext.getMatrices();
            drawStack.push();

            drawStack.translate(x, y, 0);
            drawStack.scale((float)(scale.get() * 2), (float)(scale.get() * 2), 1);

            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack itemStack = mc.player.getEquippedStack(slot);
                renderer.drawContext.drawItem(itemStack, slot.getIndex() * 20 + 12, durMode.get() == DurMode.Top ? 10 : 0);
            }

            drawStack.pop();
        });
    }

    private void centeredText(HudRenderer renderer, String text, double x, double y, double width, Color color, double scale) {
        double textWidth = renderer.textWidth(text, false, scale);
        double offset = (width - textWidth) / 2;
        renderer.text(text, x + offset, y, color, false, scale);
    }

    public enum DurMode {
        Top, // 3, 10
        Bottom // 0, 17
    }
}
