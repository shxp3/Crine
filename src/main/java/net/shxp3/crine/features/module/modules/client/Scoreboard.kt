package net.shxp3.crine.features.module.modules.client

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.Render2DEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.module.modules.client.hud.HUDModule
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.FontValue
import net.shxp3.crine.features.value.IntegerValue
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.MouseUtils
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.GuiChat
import org.lwjgl.input.Mouse
import java.awt.Color

@ModuleInfo("Scoreboard", ModuleCategory.CLIENT, defaultOn = true, loadConfig = false)
object Scoreboard : HUDModule() {
    private val alphaValue = IntegerValue("Alpha", 150, 0, 255)
    val textShadow = BoolValue("Text-Shadow", false)
    val showNumber = BoolValue("Show-Number", true)
    val fontValue = FontValue("Font:", Fonts.minecraftFont)
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        draw()
        drag(true)
    }
    fun getAlpha() : Int {
        return alphaValue.get()
    }
}