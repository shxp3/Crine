package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element

import net.minecraft.client.gui.GuiTextField

class SearchBox(id: Int, x: Int, y: Int, w: Int, h: Int) : GuiTextField(id, net.minecraft.client.Minecraft.getMinecraft().fontRendererObj, x, y, w, h) {
    init {
        enableBackgroundDrawing = false
    }
}
