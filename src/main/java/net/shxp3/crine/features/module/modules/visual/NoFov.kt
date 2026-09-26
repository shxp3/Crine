package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.FloatValue

@ModuleInfo("NoFOV", ModuleCategory.VISUAL)
object NoFov : Module() {
    val fov = FloatValue("FOV", 1f, 0f,1.5f)
}