package net.shxp3.crine.features.module.modules.client

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.TickEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.IntegerValue

@ModuleInfo(name = "FPSBoost", category = ModuleCategory.CLIENT, defaultOn = true, array = false)
object FPSBoost : Module() {
    val lowEndMode = BoolValue("LowEnd-Mode", true)
    val fastGraphics = BoolValue("Fast-Graphics", true).displayable { lowEndMode.get() }
    val lowRenderDistance = BoolValue("Low-Render-Distance", true).displayable { lowEndMode.get() }
    val renderDistance = IntegerValue("Render-Distance", 4, 2, 8).displayable { lowEndMode.get() && lowRenderDistance.get() }
    val minimalParticles = BoolValue("Minimal-Particles", true).displayable { lowEndMode.get() }
    val disableClouds = BoolValue("No-Clouds", true).displayable { lowEndMode.get() }
    val noVsync = BoolValue("No-VSync", true).displayable { lowEndMode.get() }
    val noEntityShadow = BoolValue("No-Entity-Shadow", true).displayable { lowEndMode.get() }
    val lowSmoothLighting = BoolValue("Low-Smooth-Lighting", true).displayable { lowEndMode.get() }

    @JvmStatic
    fun isLowEnd(): Boolean = state && lowEndMode.get()

    @EventTarget
    fun onTick(event: TickEvent) {
        if (!isLowEnd()) return
        if (mc.thePlayer == null || mc.gameSettings == null) return
        // Apply once per second to avoid fighting the user every tick.
        if (mc.thePlayer.ticksExisted % 20 != 0) return

        val gs = mc.gameSettings
        if (fastGraphics.get()) {
            gs.fancyGraphics = false
        }
        if (lowRenderDistance.get()) {
            if (gs.renderDistanceChunks != renderDistance.get()) {
                gs.renderDistanceChunks = renderDistance.get()
            }
        }
        if (minimalParticles.get() && gs.particleSetting != 2) {
            gs.particleSetting = 2
        }
        if (disableClouds.get() && gs.clouds != 0) {
            gs.clouds = 0
        }
        if (noVsync.get() && gs.enableVsync) {
            gs.enableVsync = false
        }
        if (noEntityShadow.get() && gs.entityShadows) {
            gs.entityShadows = false
        }
        if (lowSmoothLighting.get() && gs.ambientOcclusion != 1) {
            gs.ambientOcclusion = 1
        }
    }
}
