package net.shxp3.crine.features.special;

import net.shxp3.crine.event.EventTarget;
import net.shxp3.crine.event.Listenable;
import net.shxp3.crine.event.PacketEvent;
import net.shxp3.crine.utils.MinecraftInstance;

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
