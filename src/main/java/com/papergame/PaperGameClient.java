package com.papergame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone Paper Game reconstruction entrypoint. */
public final class PaperGameClient implements ClientModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("papergame.json");
    private static final String[] DEFAULT_ITEMS = {
        "minecraft:paper", "minecraft:diamond", "minecraft:gold_ingot",
        "minecraft:emerald", "minecraft:apple", "minecraft:iron_ingot",
        "minecraft:coal", "minecraft:book", "minecraft:amethyst_shard"
    };
    private static final String[] ITEMS = new String[9];
    private static boolean overlayVisible = true;
    private static boolean hostWins = true;
    private static boolean lastLeverPowered = false;
    private static int selectedNumber = 1;
    private static KeyBinding toggleOverlayKey;
    private static KeyBinding openSettingsKey;
    private static KeyBinding toggleWinnerKey;

    @Override public void onInitializeClient() {
        loadConfig();
        toggleOverlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.papergame.toggle_overlay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.papergame"));
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.papergame.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.papergame"));
        toggleWinnerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.papergame.toggle_winner", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.papergame"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOverlayKey.wasPressed()) overlayVisible = !overlayVisible;
            while (openSettingsKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new SettingsScreen());
            }
            while (toggleWinnerKey.wasPressed()) hostWins = !hostWins;
            // Detect nearby powered levers and update the winner only when the lever state changes.
            if (client.player != null && client.world != null) {
                boolean powered = findNearbyPoweredLever(client.player.getBlockPos());
                if (powered != lastLeverPowered) {
                    lastLeverPowered = powered;
                    hostWins = powered;
                    // The original chooses a result based on the winning side; keep a deterministic
                    // configurable mapping until the exact original selection rule can be confirmed.
                    selectedNumber = hostWins ? 9 : 1;
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!overlayVisible) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.textRenderer == null) return;
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal("Paper Game"), 8, 8, 0xFFFFFF);
            drawContext.drawTextWithShadow(client.textRenderer,
                Text.literal("Winner side: " + (hostWins ? "Host" : "Player") + " (K to switch)"), 8, 21, 0xFFFF55);
            drawContext.drawTextWithShadow(client.textRenderer,
                Text.literal("Result: " + selectedNumber), 8, 34, 0x55FF55);
            for (int i = 0; i < 9; i++) {
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal((i + 1) + " = " + ITEMS[i]), 8, 49 + i * 12, 0xDDEEFF);
            }
        });
    }

    private static boolean findNearbyPoweredLever(BlockPos center) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        for (BlockPos pos : BlockPos.iterateOutwards(center, 6, 3, 6)) {
            var state = client.world.getBlockState(pos);
            if ((state.isOf(Blocks.LEVER)) && state.contains(Properties.POWERED) && state.get(Properties.POWERED)) return true;
        }
        return false;
    }

    /** Used by the rendering mixin. Custom-named stacks "1" through "9" render as configured items. */
    public static ItemStack disguiseForRendering(ItemStack original) {
        if (original == null || original.isEmpty() || !original.hasCustomName()) return original;
        String name = original.getName().getString().trim();
        if (name.length() != 1 || name.charAt(0) < '1' || name.charAt(0) > '9') return original;
        Identifier id = Identifier.tryParse(ITEMS[name.charAt(0) - '1']);
        if (id == null) return original;
        Item item = Registries.ITEM.get(id);
        if (item == null || item == net.minecraft.item.Items.AIR) return original;
        ItemStack disguised = new ItemStack(item, original.getCount());
        return disguised;
    }

    private static void loadConfig() {
        JsonObject root = new JsonObject();
        try { if (Files.exists(CONFIG)) root = GSON.fromJson(Files.readString(CONFIG), JsonObject.class); }
        catch (Exception ignored) { root = new JsonObject(); }
        if (root == null) root = new JsonObject();
        JsonObject numbers = root.has("numbers") && root.get("numbers").isJsonObject() ? root.getAsJsonObject("numbers") : new JsonObject();
        for (int i = 0; i < 9; i++) {
            String key = Integer.toString(i + 1);
            String id = numbers.has(key) && numbers.get(key).isJsonPrimitive() ? numbers.get(key).getAsString() : DEFAULT_ITEMS[i];
            if (!validItem(id)) id = DEFAULT_ITEMS[i];
            ITEMS[i] = id;
            numbers.addProperty(key, id);
        }
        root.add("numbers", numbers);
        saveConfig(root);
    }

    private static boolean validItem(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return false;
        Item item = Registries.ITEM.get(identifier);
        return item != null && Registries.ITEM.getId(item).equals(identifier);
    }

    private static void saveConfig() {
        JsonObject root = new JsonObject();
        JsonObject numbers = new JsonObject();
        for (int i = 0; i < ITEMS.length; i++) numbers.addProperty(Integer.toString(i + 1), ITEMS[i]);
        root.add("numbers", numbers);
        saveConfig(root);
    }

    private static void saveConfig(JsonObject root) {
        try { Files.createDirectories(CONFIG.getParent()); Files.writeString(CONFIG, GSON.toJson(root)); }
        catch (IOException e) { System.err.println("[Paper Game] Could not save config: " + e.getMessage()); }
    }

    private static final class SettingsScreen extends Screen {
        private final String[] edits = new String[9];
        private SettingsScreen() { super(Text.literal("Paper Game Settings")); System.arraycopy(ITEMS, 0, edits, 0, 9); }
        @Override protected void init() {
            int left = width / 2 - 155;
            int top = 38;
            for (int i = 0; i < 9; i++) {
                final int index = i;
                int y = top + i * 22;
                addDrawableChild(ButtonWidget.builder(Text.literal((i + 1) + ": " + edits[i]), b -> {
                    // Cycle through safe example items; exact item IDs can also be edited in config/papergame.json.
                    String[] choices = {"minecraft:paper", "minecraft:diamond", "minecraft:gold_ingot", "minecraft:emerald", "minecraft:apple", "minecraft:iron_ingot", "minecraft:coal", "minecraft:book", "minecraft:amethyst_shard", "minecraft:stick", "minecraft:copper_ingot"};
                    int at = 0;
                    for (int j = 0; j < choices.length; j++) if (choices[j].equals(edits[index])) at = j;
                    edits[index] = choices[(at + 1) % choices.length];
                    b.setMessage(Text.literal((index + 1) + ": " + edits[index]));
                }).dimensions(left, y, 310, 20).build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> {
                for (int i = 0; i < 9; i++) if (validItem(edits[i])) ITEMS[i] = edits[i];
                saveConfig(); close();
            }).dimensions(width / 2 - 155, top + 9 * 22 + 4, 150, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close()).dimensions(width / 2 + 5, top + 9 * 22 + 4, 150, 20).build());
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Click a row to cycle items; exact IDs are in config/papergame.json"), width / 2, 26, 0xAAAAAA);
            super.render(context, mouseX, mouseY, delta);
        }
        @Override public boolean shouldPause() { return false; }
    }
}
