package net.shxp3.crine.features.module.modules.client

import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.ColorValue
import net.shxp3.crine.features.value.IntegerValue
import java.awt.Color

@ModuleInfo(name = "CustomClientColor", category = ModuleCategory.CLIENT)
object CustomClientColor : Module() {
    private val colorValue = ColorValue("Color",Color(255,255,255))
    fun getColor() : Color {
        return colorValue.get()
    }
    fun getColor(alpha : Int) : Color {
        return Color(colorValue.get().red, colorValue.get().green, colorValue.get().blue, alpha)
    }
}