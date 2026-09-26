package net.shxp3.crine.features.module.modules.other;

import net.shxp3.crine.Crine
import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.TextEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.TextValue
import net.shxp3.crine.utils.misc.RandomUtils
import net.shxp3.crine.utils.misc.StringUtils
import net.shxp3.crine.utils.render.ColorUtils

@ModuleInfo(name = "StreamerMode", category = ModuleCategory.OTHER)
object StreamerMode : Module() {
    private val nameDisplay = TextValue("Name", "CrineUser")
    private val allPlayersValue = BoolValue("Sensor-Player", false)

    @EventTarget
    fun onText(event: TextEvent) {
        if (mc.thePlayer == null || event.text!!.startsWith("/") || event.text!!.startsWith(Crine.commandManager.prefix + ""))
            return;
        event.text = StringUtils.replace(
            event.text,
            mc.thePlayer.name,
            ColorUtils.translateAlternateColorCodes(nameDisplay.get()) + "§r"
        )
        if (allPlayersValue.get()) {
            for (playerInfo in mc.netHandler.playerInfoMap) {
                event.text = StringUtils.replace(
                    event.text,
                    playerInfo.gameProfile.name,
                    RandomUtils.randomString(playerInfo.gameProfile.name.length) + "§f"
                )
            }
        }
    }
}