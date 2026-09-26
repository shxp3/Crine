package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.PacketEvent
import net.shxp3.crine.event.Render2DEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.ColorValue
import net.shxp3.crine.features.value.IntegerValue
import net.shxp3.crine.features.value.ListValue
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.utils.extensions.setAlpha
import net.shxp3.crine.utils.render.RenderUtils
import net.minecraft.network.play.server.S19PacketEntityStatus
import java.awt.Color

@ModuleInfo(name = "HurtCam", category = ModuleCategory.VISUAL, canEnable = false)
class HurtCam : Module() {
    val modeValue = ListValue("Mode", arrayOf("Vanilla", "Cancel", "FPS"), "Vanilla")
    private val colorValue = ColorValue("Color", Color(255,255,255)).displayable { modeValue.equals("FPS") }
    private val colorThmeme = BoolValue("ColorTheme", false).displayable { modeValue.equals("FPS") }
    private val timeValue = IntegerValue("FPSTime", 1000, 0, 1500).displayable { modeValue.equals("FPS") }
    private val fpsHeightValue = IntegerValue("FPSHeight", 25, 10, 50).displayable { modeValue.equals("FPS") }

    private var hurt = 0L

    @EventTarget
    fun onRender2d(event: Render2DEvent) {
        if (hurt == 0L) return

        val passedTime = System.currentTimeMillis() - hurt
        if (passedTime > timeValue.get()) {
            hurt = 0L
            return
        }

        val color = getColor((((timeValue.get() - passedTime) / timeValue.get().toFloat()) * 255).toInt())
        val color1 = getColor(0)
        val width = event.scaledResolution.scaledWidth_double
        val height = event.scaledResolution.scaledHeight_double

        RenderUtils.drawGradientSidewaysV(0.0, 0.0, width, fpsHeightValue.get().toDouble(), color.rgb, color1.rgb)
        RenderUtils.drawGradientSidewaysV(0.0, height - fpsHeightValue.get(), width, height, color1.rgb, color.rgb)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        when (modeValue.get().lowercase()) {
            "fps" -> {
                if (packet is S19PacketEntityStatus) {
                    if (packet.opCode.toInt() == 2 && mc.thePlayer.equals(packet.getEntity(mc.theWorld))) {
                        hurt = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    private fun getColor(alpha: Int): Color {
        return if (colorThmeme.get()) ClientTheme.getColorWithAlpha(1, alpha) else colorValue.get().setAlpha(alpha)
    }

    // always handle event
    override fun handleEvents() = true
}
