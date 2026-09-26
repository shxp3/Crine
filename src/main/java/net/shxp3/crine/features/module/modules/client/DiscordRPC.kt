package net.shxp3.crine.features.module.modules.client

import net.shxp3.crine.Crine.clientRPC
import net.shxp3.crine.discordrpc.CrineRPC
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.utils.ClientUtils
import kotlin.concurrent.thread

@ModuleInfo("DiscordRichPresence", ModuleCategory.CLIENT, defaultOn = true)
object DiscordRPC : Module() {
    val serverValue = BoolValue("Show-Server", true)
    override fun onEnable() {
        thread {
            try {
                clientRPC.run()
            } catch (throwable: Throwable) {
                ClientUtils.logError("Failed to run DiscordRPC", throwable)
            }
        }
    }

    override fun onDisable() {
        clientRPC.stop()
    }
}