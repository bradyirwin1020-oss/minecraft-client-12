package com.aegis.client.hud;

import com.aegis.client.AegisClient;
import com.aegis.client.core.AegisClientSettings;
import com.aegis.client.core.ClickTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;

        int scale = Math.max(70, Math.min(150, AegisClientSettings.hudScale));
        int x = Math.max(0, AegisClientSettings.hudX);
        int y = Math.max(0, AegisClientSettings.hudY);
        int w = 214;
        int line = 12;
        int lines = 1;
        var modules = AegisClient.MODULES;
        if (modules.enabled("cps")) lines++;
        if (modules.enabled("keystrokes")) lines++;
        if (modules.enabled("fps")) lines++;
        if (modules.enabled("ping")) lines++;
        if (modules.enabled("coordinates")) lines++;
        if (modules.enabled("armor")) lines++;
        if (modules.enabled("potions")) lines++;
        if (modules.enabled("cooldown")) lines++;
        if (modules.enabled("target") && client.crosshairPickEntity instanceof LivingEntity) lines += 2;
        int h = 12 + lines * line + 7;

        float factor = scale / 100.0f;
        int scaledW = Math.round(w * factor);
        int scaledH = Math.round(h * factor);
        int alpha = Math.max(35, Math.min(100, AegisClientSettings.hudOpacity)) * 255 / 100;
        graphics.fill(x, y, x + scaledW, y + scaledH, (alpha << 24) | 0x00111D2B);
        graphics.fill(x, y, x + 2, y + scaledH, AegisClientSettings.accentColor());

        int textX = x + Math.round(9 * factor);
        int textY = y + Math.round(6 * factor);
        int step = Math.max(9, Math.round(line * factor));
        graphics.text(client.font, "AEGIS CLIENT", textX, textY, AegisClientSettings.accentColor(), true);
        textY += step;

        if (modules.enabled("cps")) {
            graphics.text(client.font, "CPS  L " + ClickTracker.leftCps() + "   R " + ClickTracker.rightCps(), textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("keystrokes")) {
            String keys = (client.options.keyUp.isDown() ? "W" : "w") + "  "
                    + (client.options.keyLeft.isDown() ? "A" : "a") + "  "
                    + (client.options.keyDown.isDown() ? "S" : "s") + "  "
                    + (client.options.keyRight.isDown() ? "D" : "d") + "   "
                    + (client.options.keyJump.isDown() ? "JUMP" : "jump");
            graphics.text(client.font, "KEYS  " + keys, textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("fps")) {
            graphics.text(client.font, "FPS  " + client.getFps(), textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("ping")) {
            int ping = -1;
            var connection = client.getConnection();
            if (connection != null) {
                var info = connection.getPlayerInfo(player.getUUID());
                if (info != null) ping = info.getLatency();
            }
            graphics.text(client.font, "PING  " + (ping < 0 ? "--" : ping + " ms"), textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("coordinates")) {
            String coords = String.format(Locale.ROOT, "XYZ  %.0f  %.0f  %.0f  %s", player.getX(), player.getY(), player.getZ(), player.getDirection().getName().toUpperCase(Locale.ROOT));
            graphics.text(client.font, coords, textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("armor")) {
            String armor = "ARMOR  " + durability(player.getItemBySlot(EquipmentSlot.HEAD)) + "  "
                    + durability(player.getItemBySlot(EquipmentSlot.CHEST)) + "  "
                    + durability(player.getItemBySlot(EquipmentSlot.LEGS)) + "  "
                    + durability(player.getItemBySlot(EquipmentSlot.FEET));
            graphics.text(client.font, armor, textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("potions")) {
            graphics.text(client.font, "EFFECTS  " + player.getActiveEffects().size() + " active", textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("cooldown")) {
            int percent = Math.round(player.getAttackStrengthScale(0.0F) * 100.0F);
            graphics.text(client.font, "ATTACK  " + percent + "%", textX, textY, 0xFFF2F5FA, true);
            textY += step;
        }
        if (modules.enabled("target") && client.crosshairPickEntity instanceof LivingEntity target) {
            String name = target.getName().getString();
            if (name.length() > 20) name = name.substring(0, 20);
            graphics.text(client.font, "TARGET  " + name, textX, textY, AegisClientSettings.accentColor(), true);
            textY += step;
            graphics.text(client.font, String.format(Locale.ROOT, "HEALTH  %.1f / %.1f   DIST  %.1fm", target.getHealth(), target.getMaxHealth(), player.distanceTo(target)), textX, textY, 0xFFF2F5FA, true);
        }
    }

    private static String durability(ItemStack stack) {
        if (stack.isEmpty()) return "--";
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return "∞";
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return Math.max(0, Math.min(100, Math.round(100.0f * remaining / stack.getMaxDamage()))) + "%";
    }
}
