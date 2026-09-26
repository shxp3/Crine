package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.ColorValue
import net.shxp3.crine.features.value.IntegerValue
import java.awt.Color

@ModuleInfo(name = "HitColor",category = ModuleCategory.VISUAL)
class HitColor : Module() {
    val hitColorTheme = BoolValue("ColorTheme", false)
    val hitColorValue = ColorValue("Hit-Color", Color(255,255,255))
    val hitColorAlphaValue = IntegerValue("Hit-Alpha", 255, 0, 255)
}
