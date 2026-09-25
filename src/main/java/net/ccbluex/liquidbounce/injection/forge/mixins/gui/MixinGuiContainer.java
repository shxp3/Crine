package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.Crine;
import net.ccbluex.liquidbounce.features.module.modules.client.Interface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.lwjgl.opengl.GL11;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends MixinGuiScreen {

    @Shadow
    protected int xSize;
    @Shadow
    protected int ySize;
    @Shadow
    protected int guiLeft;
    @Shadow
    public Container inventorySlots;
    @Shadow
    protected int guiTop;

    private long guiOpenTime = -1;

    private boolean translated = false;

    @Shadow
    protected abstract boolean checkHotbarKeys(int keyCode);

    @Shadow private int dragSplittingButton;
    @Shadow private int dragSplittingRemnant;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGuiReturn(CallbackInfo callbackInfo) {
        guiOpenTime = System.currentTimeMillis();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }
    @Overwrite
    public void onGuiClosed() {
        if (this.mc.thePlayer != null) {
            this.inventorySlots.onContainerClosed(this.mc.thePlayer);
        }

    }
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void drawScreenHead(CallbackInfo callbackInfo) {
        if (!Crine.INSTANCE.getDestruced()) {
            Minecraft mc = Minecraft.getMinecraft();
            GuiScreen guiScreen = mc.currentScreen;
            mc.currentScreen.drawWorldBackground(0);
            if (Interface.INSTANCE.getInventoryAnimation().get() && Interface.INSTANCE.getState() && guiScreen instanceof GuiInventory) {
                double pct = Math.max(300 - (System.currentTimeMillis() - guiOpenTime), 0) / ((double) 300);
                if (pct != 0) {
                    GL11.glPushMatrix();
                    pct = EaseUtils.INSTANCE.easeInCirc(pct);

                    double scale = 1 - pct;
                    GL11.glScaled(scale, scale, scale);
                    GL11.glTranslated(((guiLeft + (xSize * 0.5 * pct)) / scale) - guiLeft,
                            ((guiTop + (ySize * 0.5d * pct)) / scale) - guiTop,
                            0);
                    translated = true;
                }
            }
        }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void checkCloseClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!Crine.INSTANCE.getDestruced()) {
            if (mouseButton - 100 == mc.gameSettings.keyBindInventory.getKeyCode()) {
                mc.thePlayer.closeScreen();
                ci.cancel();
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void checkHotbarClicks(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        checkHotbarKeys(mouseButton - 100);
    }

    @Inject(method = "updateDragSplitting", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void fixRemnants(CallbackInfo ci) {
        if (this.dragSplittingButton == 2) {
            this.dragSplittingRemnant = mc.thePlayer.inventory.getItemStack().getMaxStackSize();
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawScreenReturn(CallbackInfo callbackInfo) {
        if (translated) {
            GL11.glPopMatrix();
            translated = false;
        }
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawSlot(Slot slot, CallbackInfo ci) {
    }
}
