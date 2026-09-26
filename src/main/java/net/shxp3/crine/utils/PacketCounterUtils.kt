package net.shxp3.crine.utils

import net.shxp3.crine.Crine
import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.Listenable
import net.shxp3.crine.event.PacketEvent
import net.shxp3.crine.event.TickEvent
import net.shxp3.crine.utils.timer.MSTimer

object PacketCounterUtils : Listenable {

    init {
        Crine.eventManager.registerListener(this)
    }

    private var inBound = 0
    private var outBound = 0
    var avgInBound = 0
    var avgOutBound = 0
    private val packetTimer = MSTimer()

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.isServerSide()) {
            inBound++
        } else {
            outBound++
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (packetTimer.hasTimePassed(1000L)) {
            avgInBound = inBound
            avgOutBound = outBound
            outBound = 0
            inBound = 0
            packetTimer.reset()
        }
    }

    override fun handleEvents() = true
}