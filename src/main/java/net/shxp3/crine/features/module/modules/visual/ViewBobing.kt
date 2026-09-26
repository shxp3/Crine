package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.MotionEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.FloatValue
import net.shxp3.crine.features.value.ListValue

@ModuleInfo(name = "ViewBobing",  category = ModuleCategory.VISUAL)
class ViewBobing : Module() {
    val miniViewBobing = BoolValue("Mini-View-Bobing", false)
    private val BobChangerValue =
        ListValue("Bob-Changer", arrayOf("Low", "VeryLow", "Meme", "Custom", "Off"), "Low")
    private val CustomYaw =
        FloatValue("Bob-Custom", 0.0F, 0.0F, 10.0F).displayable { BobChangerValue.equals("Custom") }

    @EventTarget
    fun onMotion(event: MotionEvent?) {
        if (mc.thePlayer.onGround){
            when (BobChangerValue.get().lowercase()) {
                "low" -> {
                    mc.thePlayer.cameraYaw = 0.03F
                }

                "verylow" -> {
                    mc.thePlayer.cameraYaw = 0.01F
                }

                "meme" -> {
                    mc.thePlayer.cameraYaw = 10.0F
                }

                "custom" -> {
                    mc.thePlayer.cameraYaw = CustomYaw.get()
                }
            }
        }
    }

    override val tag: String?
        get() = BobChangerValue.get()
}