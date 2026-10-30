package com.aegis.client;

import com.aegis.client.core.AegisConfig;
import com.aegis.client.core.ModuleCategory;
import com.aegis.client.core.ModuleManager;
import com.aegis.client.gui.AegisMenuScreen;
import com.aegis.client.hud.HudRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Main entrypoint for Aegis Client. No automated attacks or aim manipulation are included. */
public final class AegisClient implements ClientModInitializer {
    public static final String MOD_ID = "aegis";
    public static final ModuleManager MODULES = new ModuleManager();

    private static KeyMapping openMenu;
    private static int armorDelay;
    private static int totemDelay;
    private static int surroundDelay;

    private static final int[] ARMOR_MENU_SLOTS = {5, 6, 7, 8};
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Item[][] ARMOR_TIERS = {
            {Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.GOLDEN_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET},
            {Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
            {Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
            {Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.GOLDEN_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}
    };

    @Override
    public void onInitializeClient() {
        registerModules();
        AegisConfig.load(MODULES);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "client")
        );
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aegis.open_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT_SHIFT, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(AegisClient::tick);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "client_hud"), HudRenderer::render
        );
    }

    private static void registerModules() {
        MODULES.register("cps", "CPS Counter", "Live left and right mouse click rate.", ModuleCategory.HUD, true);
        MODULES.register("keystrokes", "Keystrokes", "Live WASD, jump, sneak and sprint display.", ModuleCategory.HUD, true);
        MODULES.register("fps", "FPS Counter", "Current rendered frames per second.", ModuleCategory.PERFORMANCE, true);
        MODULES.register("ping", "Ping Display", "Connection latency reported by the current server.", ModuleCategory.PERFORMANCE, true);
        MODULES.register("coordinates", "Coordinates", "Player XYZ coordinates and facing direction.", ModuleCategory.MOVEMENT, true);
        MODULES.register("armor", "Armor Status", "Equipped armor durability and warning colors.", ModuleCategory.HUD, true);
        MODULES.register("potions", "Potion HUD", "Active potion-effect count and time information.", ModuleCategory.HUD, false);
        MODULES.register("cooldown", "Attack Cooldown", "Vanilla attack-strength recharge indicator.", ModuleCategory.COMBAT_INFO, true);
        MODULES.register("target", "Target HUD", "Information for the entity currently under the crosshair.", ModuleCategory.COMBAT_INFO, true);
        MODULES.register("autototem", "AutoTotem", "Moves a carried totem into the offhand when available.", ModuleCategory.DEFENSE, false);
        MODULES.register("autoarmor", "AutoArmor", "Equips a higher material-tier armor piece from inventory.", ModuleCategory.DEFENSE, false);
        MODULES.register("surround", "Surround", "Best-effort defensive block placement around your feet.", ModuleCategory.DEFENSE, false);
    }

    private static void tick(Minecraft client) {
        while (openMenu.consumeClick()) client.setScreen(new AegisMenuScreen());

        if (armorDelay > 0) armorDelay--;
        if (totemDelay > 0) totemDelay--;
        if (surroundDelay > 0) surroundDelay--;

        var player = client.player;
        if (player == null || client.level == null || client.gameMode == null) return;
        if (client.screen != null || player.isSpectator()) return;

        if (MODULES.enabled("autoarmor") && armorDelay == 0) equipBestArmor(client, player);
        if (MODULES.enabled("autototem") && totemDelay == 0) moveTotemToOffhand(client, player);
        if (MODULES.enabled("surround") && surroundDelay == 0 && player.onGround()) placeSurroundBlock(client, player);
    }

    public static void saveConfig() { AegisConfig.save(MODULES); }

    public static void notify(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.displayClientMessage(Component.literal("§b[Aegis] §f" + message), true);
    }

    private static void equipBestArmor(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        var menu = player.inventoryMenu;
        if (player.containerMenu != menu || !menu.getCarried().isEmpty()) return;
        for (int index = 0; index < EQUIPMENT_SLOTS.length; index++) {
            int bestSlot = -1;
            int bestRank = armorRank(player.getItemBySlot(EQUIPMENT_SLOTS[index]), index);
            for (int slot = 9; slot <= 44; slot++) {
                ItemStack candidate = menu.getSlot(slot).getItem();
                int rank = armorRank(candidate, index);
                if (rank > bestRank && usable(candidate)) { bestRank = rank; bestSlot = slot; }
            }
            if (bestSlot >= 0) {
                clickSlot(client, player, bestSlot);
                clickSlot(client, player, ARMOR_MENU_SLOTS[index]);
                clickSlot(client, player, bestSlot);
                armorDelay = 12;
                return;
            }
        }
    }

    private static int armorRank(ItemStack stack, int armorIndex) {
        if (stack.isEmpty()) return 0;
        Item[] tiers = ARMOR_TIERS[armorIndex];
        for (int i = 0; i < tiers.length; i++) if (stack.is(tiers[i])) return i + 1;
        return 0;
    }

    private static boolean usable(ItemStack stack) {
        return !stack.isEmpty() && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() - 1);
    }

    private static void moveTotemToOffhand(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        var menu = player.inventoryMenu;
        if (player.containerMenu != menu || !menu.getCarried().isEmpty()) return;
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        for (int slot = 9; slot <= 44; slot++) {
            ItemStack candidate = menu.getSlot(slot).getItem();
            if (candidate.is(Items.TOTEM_OF_UNDYING)) {
                clickSlot(client, player, slot);
                clickSlot(client, player, 45);
                clickSlot(client, player, slot);
                totemDelay = 20;
                return;
            }
        }
    }

    private static void clickSlot(Minecraft client, net.minecraft.client.player.LocalPlayer player, int slot) {
        client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, player);
    }

    private static void placeSurroundBlock(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        int hotbarSlot = findBlock(player);
        if (hotbarSlot < 0) return;
        BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        int previous = player.getInventory().selected;
        for (Direction direction : directions) {
            BlockPos target = feet.relative(direction);
            if (!client.level.getBlockState(target).isAir()) continue;
            BlockPos support = target.below();
            var state = client.level.getBlockState(support);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            player.getInventory().selected = hotbarSlot;
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.0, 0.5, 0.0), Direction.UP, support, false);
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            player.getInventory().selected = previous;
            surroundDelay = 4;
            return;
        }
        player.getInventory().selected = previous;
        surroundDelay = 2;
    }

    private static int findBlock(net.minecraft.client.player.LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return slot;
        }
        return -1;
    }
}
