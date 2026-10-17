package com.papergame.mixin;

import com.papergame.PaperGameClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replaces the rendered stack for number-labelled Paper Game items without changing inventory data. */
@Mixin(DrawContext.class)
public abstract class DrawContextItemMixin {
    @ModifyVariable(method = "drawItem(Lnet/minecraft/item/ItemStack;II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private ItemStack papergame$disguiseNumberItem(ItemStack stack) {
        return PaperGameClient.disguiseForRendering(stack);
    }
}
