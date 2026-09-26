package net.shxp3.crine.utils;

import net.shxp3.crine.event.EntityKilledEvent;
import net.shxp3.crine.event.EventTarget;
import net.shxp3.crine.event.Listenable;
import net.shxp3.crine.event.PacketEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S45PacketTitle;

public class StatisticsUtils implements Listenable {
    private static int kills;
    private static int deaths;

    private static int win;
    private static int totalPlayed;


    @EventTarget
    public void onTargetKilled(EntityKilledEvent e) {
        if (!(e.getTargetEntity() instanceof EntityPlayer)) {
            return;
        }

        kills++;
    }
    @EventTarget(ignoreCondition = true)
    private void onPacket(PacketEvent e) {
        final Packet packet = e.getPacket();
        if (packet instanceof S45PacketTitle) {
            final String title = ((S45PacketTitle) packet).getMessage().getFormattedText();
            if (title.contains("Winner")) {
                win++;
            }
            if(title.contains("BedWar")){
                totalPlayed++;
            }
            if(title.contains("SkyWar")){
                totalPlayed++;
            }
        }
    }

    public static void addDeaths() {
        deaths++;
    }

    public static int getDeaths() {
        return deaths;
    }

    public static int getKills() {
        return kills;
    }

    @Override
    public boolean handleEvents() { return true; }
}
