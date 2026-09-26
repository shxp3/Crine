package net.shxp3.crine.features.module.modules.visual;

import net.shxp3.crine.Crine;
import net.shxp3.crine.event.ClientShutdownEvent;
import net.shxp3.crine.event.EventTarget;
import net.shxp3.crine.event.UpdateEvent;
import net.shxp3.crine.features.module.Module;
import net.shxp3.crine.features.module.ModuleCategory;
import net.shxp3.crine.features.module.ModuleInfo;
import net.shxp3.crine.features.value.ListValue;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

@ModuleInfo(name = "FullBright",category = ModuleCategory.VISUAL)
public class FullBright extends Module {
    private final ListValue modeValue = new ListValue("Mode", new String[] {"Gamma", "NightVision"}, "Gamma");

    private float prevGamma = -1;

    @Override
    public void onEnable() {
        prevGamma = mc.gameSettings.gammaSetting;
    }

    @Override
    public void onDisable() {
        if(prevGamma == -1)
            return;

        mc.gameSettings.gammaSetting = prevGamma;
        prevGamma = -1;
        if(mc.thePlayer != null) mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
    }

    @EventTarget(ignoreCondition = true)
    public void onUpdate(final UpdateEvent event) {
        if (getState()) {
            switch(modeValue.get().toLowerCase()) {
                case "gamma":
                    if(mc.gameSettings.gammaSetting <= 100F)
                        mc.gameSettings.gammaSetting++;
                    break;
                case "nightvision":
                    mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 1337, 1));
                    break;
            }
        }else if(prevGamma != -1F) {
            mc.gameSettings.gammaSetting = prevGamma;
            prevGamma = -1F;
        }
    }

    @EventTarget(ignoreCondition = true)
    public void onShutdown(final ClientShutdownEvent event) {
        onDisable();
    }
}
