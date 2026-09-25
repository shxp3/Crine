package net.ccbluex.liquidbounce.features.special;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public class ClientSpoof extends MinecraftInstance implements Listenable {

    public static final boolean enabled = false;

    @EventTarget
    public void onPacket(PacketEvent event) {
    }

    @Override
    public boolean handleEvents() {
        return true;
    }
}
