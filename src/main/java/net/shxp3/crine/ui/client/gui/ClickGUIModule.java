package net.shxp3.crine.ui.client.gui;

import net.shxp3.crine.event.EventTarget;
import net.shxp3.crine.event.PacketEvent;
import net.shxp3.crine.features.module.Module;
import net.shxp3.crine.features.module.ModuleCategory;
import net.shxp3.crine.features.module.ModuleInfo;
import net.shxp3.crine.features.value.BoolValue;
import net.shxp3.crine.ui.client.gui.clickgui.DropdownGui;
import net.shxp3.crine.ui.client.gui.nova.NovaClickGui;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import org.lwjgl.input.Keyboard;

@ModuleInfo(name = "ClickGUI", category = ModuleCategory.CLIENT, keyBind = Keyboard.KEY_RSHIFT, canEnable = false, array = false)
public class ClickGUIModule extends Module {
    public static final BoolValue fastRenderValue = new BoolValue("FastRender", true);

    @Override
    public void onEnable() {
        mc.displayGuiScreen(NovaClickGui.getInstance());
    }

    @EventTarget(ignoreCondition = true)
    public void onPacket(final PacketEvent event) {
        final Packet packet = event.getPacket();

        if (packet instanceof S2EPacketCloseWindow && (mc.currentScreen instanceof DropdownGui || mc.currentScreen instanceof NovaClickGui)) {
            event.cancelEvent();
        }
    }
}
