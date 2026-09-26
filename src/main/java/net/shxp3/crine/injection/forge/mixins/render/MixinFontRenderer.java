 
package net.shxp3.crine.injection.forge.mixins.render;

import net.shxp3.crine.Crine;
import net.shxp3.crine.event.TextEvent;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {
    @ModifyVariable(method ="renderString", at = @At("HEAD"), ordinal = 0)
    private String renderString(String string) {
        if (string == null || Crine.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        Crine.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }
    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), ordinal = 0)
    private String getStringWidth(String string) {
        if (string == null || Crine.eventManager == null)
            return string;

        final TextEvent textEvent = new TextEvent(string);
        Crine.eventManager.callEvent(textEvent);
        return textEvent.getText();
    }
    // the below brreaks if u remove it idk why
     @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
     public void drawString(String p_drawString_1_, float p_drawString_2_, float p_drawString_3_, int p_drawString_4_, boolean p_drawString_5_, CallbackInfoReturnable<Integer> cir) {
     }

     @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
     public void getStringWidth(String p_getStringWidth_1_, CallbackInfoReturnable<Integer> cir) {
     }

}
