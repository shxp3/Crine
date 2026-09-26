package net.shxp3.crine.features.module.modules.client

import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.FontValue
import net.shxp3.crine.ui.font.Fonts

@ModuleInfo(name = "ChatManager", category = ModuleCategory.CLIENT, array = false, defaultOn = true)
class GuiChatModule : Module() {
    val chatRectValue = BoolValue("Chat-Rect", false)
    val chatLimitValue = BoolValue("No-Chat-Limit", true)
    val chatCombine = BoolValue("Chat-Combine", true)
    val chatAnimValue = BoolValue("Chat-Animation", true)
}