package net.shxp3.crine.ui.client.gui.nova

import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.RoundedUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Immediate-mode primitives for the Meridian console.
 * Stateless: every animation value is passed in by the caller.
 */
object NovaRender {

    // ── Surfaces ──────────────────────────────────────────────────────────

    fun sheet(x: Float, y: Float, w: Float, h: Float, alphaMul: Float = 1f) {
        val a = alphaMul.coerceIn(0f, 1f)
        RoundedUtil.drawRound(x, y, w, h, NovaTheme.SHEET_R, withMul(NovaTheme.SHEET, a))
        RoundedUtil.drawRoundOutline(
            x, y, w, h, NovaTheme.SHEET_R, 1f,
            Color(0, 0, 0, 0), withMul(NovaTheme.SHEET_LINE, a)
        )
    }

    fun menu(x: Float, y: Float, w: Float, h: Float, alphaMul: Float = 1f) {
        val a = alphaMul.coerceIn(0f, 1f)
        RoundedUtil.drawRound(x, y, w, h, NovaTheme.INNER_R, withMul(NovaTheme.MENU, a))
        RoundedUtil.drawRoundOutline(
            x, y, w, h, NovaTheme.INNER_R, 1f,
            Color(0, 0, 0, 0), withMul(NovaTheme.LINE_STRONG, a)
        )
    }

    fun well(x: Float, y: Float, w: Float, h: Float, alphaMul: Float = 1f) {
        val a = alphaMul.coerceIn(0f, 1f)
        RoundedUtil.drawRound(x, y, w, h, NovaTheme.BOX_R, withMul(NovaTheme.WELL, a))
        RoundedUtil.drawRoundOutline(
            x, y, w, h, NovaTheme.BOX_R, 1f,
            Color(0, 0, 0, 0), withMul(NovaTheme.LINE, a)
        )
    }

    // ── Hairlines ─────────────────────────────────────────────────────────

    fun hline(x: Float, y: Float, w: Float, c: Color = NovaTheme.LINE) {
        Gui.drawRect(x.toInt(), y.toInt(), (x + w).toInt(), (y + 1).toInt(), c.rgb)
    }

    fun vline(x: Float, y: Float, h: Float, c: Color = NovaTheme.LINE) {
        Gui.drawRect(x.toInt(), y.toInt(), (x + 1).toInt(), (y + h).toInt(), c.rgb)
    }

    fun wash(x: Float, y: Float, w: Float, h: Float, alpha: Int) {
        if (alpha <= 0) return
        RenderUtils.drawRect(x, y, x + w, y + h, Color(255, 255, 255, alpha.coerceIn(0, 255)).rgb)
    }

    private fun withMul(c: Color, mul: Float): Color {
        if (mul >= 1f) return c
        return Color(c.red, c.green, c.blue, (c.alpha * mul).toInt().coerceIn(0, 255))
    }

    // ── Type helpers ──────────────────────────────────────────────────────

    /** Draw [text] trimmed with ellipsis to fit [maxW]. Returns drawn width. */
    fun drawTrimmed(font: GameFontAlias, text: String, x: Float, y: Float, color: Int, maxW: Float): Float {
        val str = trimTo(font, text, maxW)
        font.drawString(str, x, y, color, false)
        return font.getStringWidth(str).toFloat()
    }

    fun trimTo(font: GameFontAlias, text: String, maxW: Float): String {
        if (maxW <= 0f) return ""
        if (font.getStringWidth(text) <= maxW) return text
        var s = text
        while (s.length > 1 && font.getStringWidth("$s…") > maxW) {
            s = s.substring(0, s.length - 1)
        }
        return "$s…"
    }

    // ── Console widgets ───────────────────────────────────────────────────

    /** Section eyebrow: micro label + rule to the right. Returns baseline Y. */
    fun sectionHead(x: Float, y: Float, w: Float, label: String, accent: Color): Float {
        val f = NovaTheme.Type.tiny
        f.drawString(label, x, y, NovaTheme.TEXT_2.rgb, false)
        val lw = f.getStringWidth(label).toFloat()
        hline(x + lw + 6f, y + f.FONT_HEIGHT / 2f, (w - lw - 6f).coerceAtLeast(0f))
        return y
    }

    /**
     * Square checkbox. Checked state grows an inner accent block by [fill].
     */
    fun checkbox(x: Float, y: Float, size: Float, fill: Float, accent: Color, hovered: Boolean, alphaMul: Float = 1f) {
        val a = alphaMul.coerceIn(0f, 1f)
        val border = if (hovered || fill > 0.02f) NovaTheme.withAlpha(accent, (200 * a).toInt())
        else NovaTheme.withAlpha(NovaTheme.LINE_STRONG, (255 * a).toInt())
        // box
        Gui.drawRect(x.toInt(), y.toInt(), (x + size).toInt(), (y + 1).toInt(), border.rgb)
        Gui.drawRect(x.toInt(), (y + size - 1).toInt(), (x + size).toInt(), (y + size).toInt(), border.rgb)
        Gui.drawRect(x.toInt(), y.toInt(), (x + 1).toInt(), (y + size).toInt(), border.rgb)
        Gui.drawRect((x + size - 1).toInt(), y.toInt(), (x + size).toInt(), (y + size).toInt(), border.rgb)
        // inner block
        val f = fill.coerceIn(0f, 1f)
        if (f > 0.02f) {
            val pad = 2f + (1f - f) * (size / 2f - 2f)
            val c = NovaTheme.withAlpha(accent, (235 * a).toInt())
            Gui.drawRect(
                (x + pad).toInt(), (y + pad).toInt(),
                (x + size - pad).toInt(), (y + size - pad).toInt(), c.rgb
            )
        }
    }

    /**
     * Mixer-style fader: 2px track + vertical bar thumb at [frac].
     * Returns thumb bounds as FloatArray(x0, x1) for hit-testing.
     */
    fun fader(x: Float, yCenter: Float, w: Float, frac: Float, accent: Color, hovered: Boolean, active: Boolean): FloatArray {
        val f = frac.coerceIn(0f, 1f)
        hline(x, yCenter, w, NovaTheme.withAlpha(NovaTheme.LINE_STRONG, 200))
        // filled portion
        val fillW = w * f
        if (fillW > 0.5f) {
            val c = if (active || hovered) accent else NovaTheme.withAlpha(accent, 150)
            Gui.drawRect(x.toInt(), yCenter.toInt(), (x + fillW).toInt(), (yCenter + 1).toInt(), c.rgb)
        }
        val tx = x + w * f
        val tw = 5f
        val th = 11f
        val thumb = if (hovered || active)
            Color(240, 242, 247, 255) else Color(170, 176, 190, 255)
        Gui.drawRect(
            (tx - tw / 2f).toInt(), (yCenter - th / 2f + 0.5f).toInt(),
            (tx + tw / 2f).toInt(), (yCenter + th / 2f + 0.5f).toInt(), thumb.rgb
        )
        return floatArrayOf(tx - tw / 2f - 2f, tx + tw / 2f + 2f)
    }

    /**
     * Keybind pill: dark well + micro text. [listening] shows accent border + ellipsis.
     */
    fun keyPill(x: Float, y: Float, w: Float, h: Float, text: String, hovered: Boolean, listening: Boolean, accent: Color): Float {
        well(x, y, w, h)
        if (listening || hovered) {
            RoundedUtil.drawRoundOutline(
                x, y, w, h, NovaTheme.BOX_R, 1f, Color(0, 0, 0, 0),
                NovaTheme.withAlpha(accent, if (listening) 255 else 170)
            )
        }
        val f = NovaTheme.Type.micro
        val label = if (listening) "···" else text
        val tw = f.getStringWidth(label).toFloat()
        f.drawString(
            label, x + w / 2f - tw / 2f, y + h / 2f - f.FONT_HEIGHT / 2f + 0.5f,
            (if (listening) accent else NovaTheme.TEXT_1).rgb, false
        )
        return tw
    }

    /** Color swatch with checkerboard behind translucent colors. */
    fun swatch(x: Float, y: Float, s: Float, color: Color, hovered: Boolean) {
        if (color.alpha < 255) {
            RenderUtils.drawCheckerboard(x, y, s, s, 3)
        }
        RenderUtils.drawRect(x, y, x + s, y + s, color.rgb)
        val border = if (hovered) Color(255, 255, 255, 120) else NovaTheme.LINE_STRONG
        RenderUtils.drawRectBasedBorder(x, y, x + s, y + s, 1f, border.rgb)
    }

    /** Thin scrollbar thumb for a scroll region. */
    fun scrollbar(x: Float, y: Float, h: Float, viewH: Float, contentH: Float, scroll: Float, accent: Color) {
        if (contentH <= viewH + 0.5f) return
        val trackH = h
        val thumbH = (trackH * (viewH / contentH)).coerceAtLeast(14f)
        val maxS = (contentH - viewH).coerceAtLeast(1f)
        val thumbY = y + (trackH - thumbH) * (scroll / maxS).coerceIn(0f, 1f)
        RenderUtils.drawRect(x, thumbY, x + 2f, thumbY + thumbH, NovaTheme.withAlpha(accent, 150).rgb)
    }

    // ── Scissor ───────────────────────────────────────────────────────────

    fun scissorOn(x: Float, y: Float, x2: Float, y2: Float) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        RenderUtils.prepareScissorBox(x, y, x2, y2)
    }

    fun scissorOff() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    fun resetColor() {
        GlStateManager.resetColor()
    }
}

/** Alias so NovaRender signatures stay short. */
typealias GameFontAlias = net.shxp3.crine.ui.font.GameFontRenderer
