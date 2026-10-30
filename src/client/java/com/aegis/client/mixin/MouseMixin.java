package com.aegis.client.mixin;

import com.aegis.client.core.ClickTracker;
import net.minecraft.client.Mouse;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onPress", at = @At("HEAD"), require = 0)
    private void aegis$recordClick(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (Minecraft.getInstance().screen == null) {
            ClickTracker.onMousePress(button, action);
        }
    }
}
