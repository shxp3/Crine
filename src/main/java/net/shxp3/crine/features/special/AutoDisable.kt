package net.shxp3.crine.features.special

import net.shxp3.crine.Crine
import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.Listenable
import net.shxp3.crine.event.PacketEvent
import net.shxp3.crine.event.WorldEvent
import net.shxp3.crine.features.module.EnumAutoDisableType
import net.shxp3.crine.features.module.EnumTriggerType
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object AutoDisable : Listenable {

    @EventTarget
    fun onWorld(event: WorldEvent) {
        Crine.moduleManager.modules
            .filter { it.state && it.autoDisable == EnumAutoDisableType.RESPAWN && it.triggerType == EnumTriggerType.TOGGLE }
            .forEach { module ->
                module.state = false
            }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            Crine.moduleManager.modules
                .filter { it.state && it.autoDisable == EnumAutoDisableType.FLAG && it.triggerType == EnumTriggerType.TOGGLE }
                .forEach { module ->
                    module.state = false
                }
        }
    }

    fun handleGameEnd() {
        Crine.moduleManager.modules
            .filter { it.state && it.autoDisable == EnumAutoDisableType.GAME_END }
            .forEach { module ->
                module.state = false
            }
    }

    override fun handleEvents() = true
}
