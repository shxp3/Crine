package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.min

/**
 * Crine 1.0 background — flat line style.
 * Solid base + dot grid + top hairline + corner ticks + vignette.
 * No aurora blobs, no glow orbs — intentionally different from the old style.
 */
object ThemedBackground {

    private var openMs = System.currentTimeMillis()

    @JvmStatic
    fun draw(width: Int, height: Int) {
        draw(width, height, width / 2, height / 2)
    }

    @JvmStatic
    fun draw(width: Int, height: Int, mouseX: Int, mouseY: Int) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)

        val px = (mouseX / sw.coerceAtLeast(1f) - 0.5f).coerceIn(-0.5f, 0.5f)
        val py = (mouseY / sh.coerceAtLeast(1f) - 0.5f).coerceIn(-0.5f, 0.5f)

        // Base — near-black flat
        Gui.drawRect(0, 0, width, height, Color(6, 8, 12, 255).rgb)

        // Very subtle top lift (flat, no blob)
        RenderUtils.drawGradientSidewaysV(
            0.0, 0.0, sw.toDouble(), (sh * 0.30).toDouble(),
            Color(11, 14, 20, 255).rgb, Color(6, 8, 12, 0).rgb
        )

        // Thin accent hairline at very top (flat, 1px feel -> 2px rect)
        Gui.drawRect(0, 0, width, 2, Color(accent.red, accent.green, accent.blue, 55).rgb)
        // Short centered segment — brighter
        val segW = min(220f, sw * 0.4f)
        val segX0 = ((sw - segW) / 2f).toInt()
        Gui.drawRect(segX0, 0, (segX0 + segW).toInt(), 2,
            Color(accent.red, accent.green, accent.blue, 170).rgb)

        // Dot grid — flat, static, slight parallax
        drawDots(sw, sh, px, py)

        // Faint diagonal lines (flat technical feel)
        drawDiagonals(sw, sh, accent)

        // Corner ticks — crisp L shapes
        drawCornerTicks(sw, sh, accent)

        // Bottom vignette only (flat fade)
        RenderUtils.drawGradientSidewaysV(0.0, 0.0, sw.toDouble(), 60.0,
            Color(0, 0, 0, 110).rgb, Color(0, 0, 0, 0).rgb)
        RenderUtils.drawGradientSidewaysV(0.0, (sh - 130).toDouble(), sw.toDouble(), sh.toDouble(),
            Color(0, 0, 0, 0).rgb, Color(0, 0, 0, 160).rgb)
    }

    private fun drawDots(sw: Float, sh: Float, px: Float, py: Float) {
        val step = 22f
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glPointSize(1.2f)
        GL11.glBegin(GL11.GL_POINTS)
        var y = step / 2f
        while (y < sh) {
            var x = step / 2f
            while (x < sw) {
                val edge = 1f - kotlin.math.abs(x / sw - 0.5f) * 1.2f
                val edgeY = 1f - kotlin.math.abs(y / sh - 0.5f) * 1.2f
                val fade = (edge * edgeY).coerceIn(0.1f, 1f)
                val a = (20 * fade).toFloat() / 255f
                GL11.glColor4f(0.55f, 0.60f, 0.72f, a)
                GL11.glVertex2f(x + px * 5f, y + py * 4f)
                x += step
            }
            y += step
        }
        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    private fun drawDiagonals(sw: Float, sh: Float, accent: Color) {
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(1f)
        GL11.glBegin(GL11.GL_LINES)
        // two faint diagonals in corners
        GL11.glColor4f(accent.red / 255f, accent.green / 255f, accent.blue / 255f, 0.05f)
        val d = 140f
        GL11.glVertex2f(0f, 0f); GL11.glVertex2f(d, d)
        GL11.glVertex2f(sw, sh); GL11.glVertex2f(sw - d, sh - d)
        GL11.glColor4f(1f, 1f, 1f, 0.03f)
        GL11.glVertex2f(0f, sh); GL11.glVertex2f(d, sh - d)
        GL11.glVertex2f(sw, 0f); GL11.glVertex2f(sw - d, d)
        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    private fun drawCornerTicks(sw: Float, sh: Float, accent: Color) {
        val len = 14f
        val th = 1.5f
        val m = 10f
        val c = Color(accent.red, accent.green, accent.blue, 90)
        // TL
        Gui.drawRect(m.toInt(), m.toInt(), (m + len).toInt(), (m + th).toInt(), c.rgb)
        Gui.drawRect(m.toInt(), m.toInt(), (m + th).toInt(), (m + len).toInt(), c.rgb)
        // TR
        Gui.drawRect((sw - m - len).toInt(), m.toInt(), (sw - m).toInt(), (m + th).toInt(), c.rgb)
        Gui.drawRect((sw - m - th).toInt(), m.toInt(), (sw - m).toInt(), (m + len).toInt(), c.rgb)
        // BL
        Gui.drawRect(m.toInt(), (sh - m - th).toInt(), (m + len).toInt(), (sh - m).toInt(), c.rgb)
        Gui.drawRect(m.toInt(), (sh - m - len).toInt(), (m + th).toInt(), (sh - m).toInt(), c.rgb)
        // BR
        Gui.drawRect((sw - m - len).toInt(), (sh - m - th).toInt(), (sw - m).toInt(), (sh - m).toInt(), c.rgb)
        Gui.drawRect((sw - m - th).toInt(), (sh - m - len).toInt(), (sw - m).toInt(), (sh - m).toInt(), c.rgb)
    }
}
