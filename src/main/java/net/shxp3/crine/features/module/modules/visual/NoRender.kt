package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.PacketEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.minecraft.network.play.server.S45PacketTitle

@ModuleInfo(name = "NoRender",  category = ModuleCategory.VISUAL, array = false)
object NoRender : Module() {
    val fireEffect = BoolValue("Fire", true)
    val bossHealth = BoolValue("Boss-Health", false)
    private val titleValue = BoolValue("Title", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S45PacketTitle && titleValue.get())
            event.cancelEvent()
    }
}