package kassuk.addon.blackout.hud;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author KassuK
 * @author OLEPOSSU
 */

public class TargetHud extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode")
        .description("What mode to use for the TargetHud.")
        .defaultValue(Mode.Blackout)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("Scale")
        .description("Scale to render at")
        .defaultValue(2)
        .range(0, 5)
        .sliderRange(0, 5)
        .build()
    );
    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Background Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(0, 0, 0, 200))
        .build()
    );
    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Text Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<SettingColor> healthColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Health Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> absorptionColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Absorption Color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .build()
    );
    private final Setting<Double> damageTilt = sgGeneral.add(new DoubleSetting.Builder()
        .name("Damage Tilt")
        .description("How many degrees should the box be rotated when enemy takes damage.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 45)
        .build()
    );

    public static final HudElementInfo<TargetHud> INFO = new HudElementInfo<>(BlackOut.HUD_BLACKOUT, "TargetHud", "A target hud the fuck you thinkin bruv.", TargetHud::new);

    public TargetHud() {
        super(INFO);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private AbstractClientPlayerEntity target;
    private String renderName = null;
    private SkinTextures renderSkinTextures = null;
    private float renderHealth;
    private float renderPing;

    private double scaleProgress = 0;
    private long damageTime;

    private UUID lastTarget = null;
    private float lastHp = 0;
    private boolean popped = false;

    private final Map<AbstractClientPlayerEntity, Integer> tog = new HashMap<>();

    @EventHandler(priority = 10000)
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        List<AbstractClientPlayerEntity> toRemove = new ArrayList<>();

        for (Map.Entry<AbstractClientPlayerEntity, Integer> entry : tog.entrySet()) {
            if (mc.world.getPlayers().contains(entry.getKey()) && !entry.getKey().isSpectator() && entry.getKey().getHealth() > 0) {
                continue;
            }

            toRemove.add(entry.getKey());
        }

        toRemove.forEach(tog::remove);

        mc.world.getPlayers().forEach(player -> {
            if (player.isOnGround()) {
                if (tog.containsKey(player)) {
                    tog.replace(player, tog.get(player) + 1);
                } else {
                    tog.put(player, 1);
                }
            }
        });

        if (target != null) {
            if (target.getUuid().equals(lastTarget)) {
                float diff = Math.max(lastHp - target.getHealth() - target.getAbsorptionAmount(), 0);

                if (diff > 1) {
                    damageTime = System.currentTimeMillis();
                }
            }
            lastTarget = target.getUuid();
            lastHp = popped ? 0 : target.getHealth() + target.getAbsorptionAmount();
            popped = false;
        } else {
            lastTarget = null;
            lastHp = 0;
            damageTime = 0;
        }
    }

    @EventHandler(priority = 10000)
    private void onReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) {return;}

        if (packet.getStatus() != 35) {return;}

        Entity entity = packet.getEntity(mc.world);

        if (entity instanceof PlayerEntity player && player == target) {
            popped = true;
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mode.get() == Mode.Blackout) {
            int height = 100;
            int width = 200;

            setSize(width * scale.get(), height * scale.get());

            updateTarget();
            if (renderName == null) {
                return;
            }

            scaleProgress = MathHelper.clamp(scaleProgress + (target == null ? -renderer.delta : renderer.delta), 0, 1);

            double scaleAnimation = scaleProgress * scaleProgress * scaleProgress;
            if (scaleAnimation < 0.01) {
                return;
            }

            double translateX = x + (1 - scaleAnimation) * getWidth() / 2f;
            double translateY = y + (1 - scaleAnimation) * getHeight() / 2f;
            double scale = scaleAnimation * this.scale.get();

            // Damage tilt
            float tilt = (float) (Math.max(0, 500 - System.currentTimeMillis() + damageTime) / 500f * damageTilt.get());
            // stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((tilt)));

            // Background
            renderer.quad(
                translateX,
                translateY,
                width * scale,
                height * scale,
                bgColor.get()
            );
            /*
            RoundedQuadRenderer.render(
                renderer,
                translateX,
                translateY,
                width * scale,
                height * scale,
                15 * scale,
                bgColor.get()
            );
             */

            // Face
            if (renderSkinTextures != null) {
                drawFace(renderer, (float) (scaleAnimation * this.scale.get().floatValue()), x + (1 - scaleAnimation) * getWidth() / 2f, y + (1 - scaleAnimation) * getHeight() / 2f, tilt);
            }

            // Name
            renderer.text(renderName, translateX + 60 * scale, translateY + 20 * scale, textColor.get(), false, scale / 2);

            // Ping
            String ping = Math.round(renderPing) + "ms";
            renderer.text(ping, translateX + 60 * scale, translateY + 30 * scale, textColor.get(), false, scale / 2);

            // Health
            String health = String.format("%.1f", renderHealth);
            renderer.text(health, translateX + 20 * scale, translateY + 81 * scale - renderer.textHeight(false, scale / 2) / 2, textColor.get(), false, scale / 2);

            float barAnimation = MathHelper.lerp(mc.getRenderTickCounter().getTickProgress(true) / 10, lastHp, renderHealth);

            float barStart = Math.max(mc.textRenderer.getWidth(String.valueOf(Math.round((renderHealth) * 10) / 10f)),
                mc.textRenderer.getWidth("36.0")) + 25;

            // Health Bar
            if (barAnimation > 0) {
                renderer.quad(
                    translateX + barStart * scale,
                    translateY + 77 * scale,
                    MathHelper.clamp(barAnimation / 20, 0, 1) * (width - 24 - barStart) * scale,
                    8 * scale,
                    healthColor.get()
                );
                /*
                RoundedQuadRenderer.render(
                    renderer,
                    translateX + barStart * scale,
                    translateY + 77 * scale,
                    MathHelper.clamp(barAnimation / 20, 0, 1) * (width - 24 - barStart) * scale,
                    8 * scale,
                    3 * scale,
                    healthColor.get()
                );
                 */
            }
            if (barAnimation > 20) {
                renderer.quad(
                    translateX + barStart * scale,
                    translateY + 77 * scale,
                    MathHelper.clamp((barAnimation - 20) / 16, 0, 1) * (width - 24 - barStart) * scale,
                    8 * scale,
                    absorptionColor.get()
                );
                /*
                RoundedQuadRenderer.render(
                    renderer,
                    translateX + barStart * scale,
                    translateY + 77 * scale,
                    MathHelper.clamp((barAnimation - 20) / 16, 0, 1) * (width - 24 - barStart) * scale,
                    8 * scale,
                    3 * scale,
                    absorptionColor.get()
                );
                 */
            }
        }
        if (mode.get() == Mode.ExhibitionOld) {
            int height = 60;
            int width = 240;
            setSize(width * scale.get(), height * scale.get());

            updateTarget();

            if (target == null || renderName == null) {
                return;
            }

            // Background
            renderer.quad(x, y, width * scale.get(), height * scale.get(), bgColor.get());

            // Face
            renderer.quad(x + 1 * scale.get(), y + 1 * scale.get(), 58 * scale.get(), 58 * scale.get(), new Color(102, 102, 102, 255));

            if (renderSkinTextures != null) {
                drawFace(renderer, scale.get().floatValue(), x, y, 0);
            }

            // Name
            renderer.text(renderName, x + 66 * scale.get(), y + 4 * scale.get(), textColor.get(), false, scale.get());

            // Health
            String healthText = Math.round((renderHealth) * 10) / 10f + " Dist: " + Math.round(mc.player.distanceTo(target) * 10) / 10f;
            renderer.text(healthText, x + 66 * scale.get(), y + (35 * scale.get() - renderer.textHeight(false, scale.get() / 2) / 2), textColor.get(), false, scale.get() / 2);

            // Bar

            int progress = MathHelper.ceil(MathHelper.clamp(renderHealth, 0, 20));

            for (int i = 0; i < 10; i++) {
                renderer.quad(
                    x + (66 + i * 16) * scale.get(),
                    y + 22 * scale.get(),
                    6 * Math.min(progress, 2) * scale.get(),
                    6 * scale.get(),
                    new Color(204, 204, 0, 255)
                );
                progress -= 2;

                if (progress <= 0) {
                    break;
                }
            }

            // Misc info
            String miscTextLine1 = "Yaw: " + Math.round((target.getYaw()) * 10) / 10f + " Pitch: " + Math.round(target.getPitch() * 10) / 10f + " BodyYaw: " + Math.round((target.getBodyYaw()) * 10) / 10f;
            renderer.text(miscTextLine1, x + 66 * scale.get(), y + (45 * scale.get() - renderer.textHeight(false, scale.get() / 2) / 2), textColor.get(), false, scale.get() / 2);
            String miscTextLine2 = "TOG: " + (tog.getOrDefault(target, 0)) + " HURT: " + ((target.hurtTime) * 10) / 10f + " TE: " + target.age;
            renderer.text(miscTextLine2, x + 66 * scale.get(), y + (55 * scale.get() - renderer.textHeight(false, scale.get() / 2) / 2), textColor.get(), false, scale.get() / 2);
        }
        if (mode.get() == Mode.Exhibition) {
            double height = 60 * scale.get();
            double width = 190 * scale.get();
            setSize(width, height);

            updateTarget();

            if (target == null || renderName == null) {
                return;
            }

            // Background
            renderer.quad(
                x + -2 * scale.get(),
                y + -2 * scale.get(),
                width + 4 * scale.get(),
                height + 4 * scale.get(),
                new Color(52, 52, 52, 255)
            );
            renderer.quad(
                x + -1 * scale.get(),
                y + -1 * scale.get(),
                width + 2 * scale.get(),
                height + 2 * scale.get(),
                new Color(32, 32, 32, 255)
            );
            renderer.quad(
                x, y,
                width, height,
                new Color(52, 52, 52, 255)
            );

            //PlayerModel

            // Name
            renderer.text(renderName, x + 41 * 1.5 * scale.get(), y + 2 * 1.5 * scale.get(), textColor.get(), false, 1.5 * scale.get() * 0.5);

            // Health and Distance
            String healthText = Math.round((renderHealth) * 10) / 10f + " Dist: " + Math.round(mc.player.distanceTo(target) * 10) / 10f;
            renderer.text(healthText, x + 83 * 0.75 * scale.get(), y + (40 * 0.75 * scale.get()) - renderer.textHeight(false, 0.75 * scale.get() * 0.5) / 2, textColor.get(), false, 0.75 * scale.get() * 0.5);

            // Bar

            int progress = (int) (Math.ceil(MathHelper.clamp(renderHealth, 0, 20)));

            for (int i = 0; i < 10; i++) {
                renderer.quad(
                    x + (41 + i * 8) * 1.5 * scale.get(),
                    y + 12 * 1.5 * scale.get(),
                    3 * Math.min(progress, 2) * 1.5 * scale.get(),
                    3 * 1.5 * scale.get(),
                    new Color(204, 204, 0, 255)
                );

                progress -= 2;

                if (progress <= 0) {
                    break;
                }
            }

            renderer.post(() -> {
                //Armor
                Matrix3x2fStack drawStack = renderer.drawContext.getMatrices();
                drawStack.pushMatrix();

                drawStack.translate(x, y, 0);
                drawStack.scale(scale.get().floatValue() * 1.35f, scale.get().floatValue() * 1.35f, 1);

                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack itemStack = mc.player.getEquippedStack(slot);

                    renderer.drawContext.drawItem(itemStack, (3 - slot.getIndex()) * 20 + 42, 25);
                }

                //Item
                ItemStack itemStack = target.getMainHandStack();

                renderer.drawContext.drawItem(itemStack, 122, 25);

                drawStack.popMatrix();
            });
        }
    }

    private void drawFace(HudRenderer renderer, float scale, double x, double y, float tilt) {
        renderer.post(() -> {
            Matrix3x2fStack drawStack = renderer.drawContext.getMatrices();

            drawStack.pushMatrix();

            drawStack.translate((float) x, (float) y, 0);
            drawStack.scale(scale, scale, 1);
            drawStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));

            PlayerSkinDrawer.draw(renderer.drawContext, renderSkinTextures, 20, 18, 32, -1);

            drawStack.popMatrix();
        });
    }

    private void updateTarget() {
        target = null;
        if (mc.world == null) {return;}

        AbstractClientPlayerEntity closest = null;
        double distance = Double.MAX_VALUE;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {continue;}
            if (Friends.get().isFriend(player)) {continue;}

            double d = mc.player.distanceTo(player);

            if (d < distance) {
                closest = player;
                distance = d;
            }
        }

        target = closest;
        if (target == null && isInEditor()) {
            target = mc.player;
        }

        if (target != null) {
            renderName = target.getName().getString();
            renderHealth = target.getHealth() + target.getAbsorptionAmount();
            renderSkinTextures = target.getSkinTextures();

            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(target.getUuid());
            renderPing = playerListEntry == null ? -1 : playerListEntry.getLatency();
        }
    }
    public enum Mode {
        Blackout,
        ExhibitionOld,
        Exhibition
    }
}
