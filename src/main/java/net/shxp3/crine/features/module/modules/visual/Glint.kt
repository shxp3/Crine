 
package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.ColorValue
import net.shxp3.crine.features.value.IntegerValue
import net.shxp3.crine.features.value.ListValue
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.utils.render.ColorUtils
import java.awt.Color

@ModuleInfo(name = "Glint", category = ModuleCategory.VISUAL)
class Glint : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Theme", "AnotherRainbow", "Custom"), "Custom")
    private val colorValue = ColorValue("Color", Color(255,255,255)).displayable { modeValue.equals("Custom") }
    fun getColor(): Color {
        return when (modeValue.get().lowercase()) {
            "theme" -> ClientTheme.getColor(1)
            "anotherrainbow" -> ColorUtils.skyRainbow(10, 0.9F, 1F, 1.0)
            else -> colorValue.get()
        }
    }
}