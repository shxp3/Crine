package net.shxp3.crine.injection.forge.mixins.gui;

import net.shxp3.crine.Crine;
import net.shxp3.crine.features.module.modules.client.Interface;
import net.minecraft.client.gui.achievement.GuiAchievement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiAchievement.class)
public class MixinGuiAchievement {
    @Inject(method = "updateAchievementWindow", at = @At("HEAD"), cancellable = true)
    private void injectAchievements(CallbackInfo ci) {
        if (Crine.moduleManager != null
                && Crine.moduleManager.getModule(Interface.class) != null
                && Crine.moduleManager.getModule(Interface.class).getState())
            ci.cancel();
    }
}
