 
package net.shxp3.crine.features.module.modules.visual

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.Render3DEvent
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.ColorValue
import net.shxp3.crine.features.value.FloatValue
import net.shxp3.crine.features.value.IntegerValue
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.utils.animation.Animation
import net.shxp3.crine.utils.animation.Easing
import net.shxp3.crine.utils.block.BlockUtils.canBeClicked
import net.shxp3.crine.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "BlockOverlay", category = ModuleCategory.VISUAL)
class BlockOverlay : Module() {
    private val clientTheme = BoolValue("Client-Theme", false)
    private val outlineValue = BoolValue("Outline", false)
    private val widthValue = FloatValue("Line-Width", 2.0F, 0.0F, 10.0F).displayable { outlineValue.get() }
    private val colorBlockValue = ColorValue("Color", Color(255,255,255), false)
    private val colorBlockAlphaValue = IntegerValue("Block-Alpha", 255, 0, 255)
    private var animation: Animation? = null
    private val currentBlock: BlockPos?
        get() {
            val blockPos = mc.objectMouseOver?.blockPos ?: return null
            if (canBeClicked(blockPos) && mc.theWorld.worldBorder.contains(blockPos)) {
                return blockPos
            }

            return null
        }
    private val currentDamage: Double
        get() {
            if (mc.playerController.curBlockDamageMP == 0F) return 1.0
            return mc.playerController.curBlockDamageMP.toDouble()
        }
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val blockPos = currentBlock ?: return
        val color = if (clientTheme.get()) ClientTheme.getColorWithAlpha(1, colorBlockAlphaValue.get()) else Color(
            colorBlockValue.get().red,
            colorBlockValue.get().green,
            colorBlockValue.get().blue,
            colorBlockAlphaValue.get()
        )
        if (animation == null) {
            animation = Animation(Easing.LINEAR, 40)
            animation!!.value = currentDamage
        }
        animation!!.run(currentDamage)
        GlStateManager.pushMatrix()
        RenderUtils.drawBlockBox(blockPos, color, outlineValue.get(), !outlineValue.get(), widthValue.get(), animation!!.value.toFloat())
        GlStateManager.popMatrix()
        GlStateManager.resetColor()
    }
}
