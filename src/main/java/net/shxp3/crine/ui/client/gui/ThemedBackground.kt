package net.shxp3.crine.ui.client.gui

import net.shxp3.crine.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Meridian menu backdrop.
 * Flat near-black base + faint static hairline grid + bottom shade.
 * Same API as before so all menus inherit the new look untouched.
 */
object ThemedBackground {

    @JvmStatic
    fun draw(width: Int, height: Int) {
        draw(width, height, width / 2, height / 2)
    }

    @JvmStatic
    fun draw(width: Int, height: Int, mouseX: Int, mouseY: Int) {
        val sw = width.toFloat()
        val sh = height.toFloat()

        // Base — flat, slightly blue-black
        Gui.drawRect(0, 0, width, height, Color(5, 7, 11, 255).rgb)

        // Static hairline grid (one batched pass, no parallax, no dots)
        drawGrid(sw, sh)

        // Bottom shade keeps menu text legible over world blur-through
        RenderUtils.drawGradientSidewaysV(0.0, (sh - 150).toDouble(), sw.toDouble(), sh.toDouble(),
            Color(0, 0, 0, 0).rgb, Color(0, 0, 0, 150).rgb)
        RenderUtils.drawGradientSidewaysV(0.0, 0.0, sw.toDouble(), 70.0,
            Color(0, 0, 0, 90).rgb, Color(0, 0, 0, 0).rgb)
    }

    private fun drawGrid(sw: Float, sh: Float) {
        val step = 44f
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(1f)
        GL11.glBegin(GL11.GL_LINES)
        GL11.glColor4f(1f, 1f, 1f, 0.028f)
        var x = 0.5f
        while (x < sw) {
            GL11.glVertex2f(x, 0f)
            GL11.glVertex2f(x, sh)
            x += step
        }
        var y = 0.5f
        while (y < sh) {
            GL11.glVertex2f(0f, y)
            GL11.glVertex2f(sw, y)
            y += step
        }
        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }
}
