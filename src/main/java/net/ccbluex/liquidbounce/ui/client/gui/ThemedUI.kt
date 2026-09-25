package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiTextField
import java.awt.Color

/**
 * Crine 1.0 design system — "Flat Line" style.
 * Replaces the old glassmorphism/glow/gradient look:
 * flat fills, 1px line borders, small radius, left accent ticks.
 * Same public API so all menus pick up the new look automatically.
 */
object ThemedUI {

    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun easeOutCubic(x: Float): Float {
        val t = x.coerceIn(0f, 1f); return 1f - (1f - t) * (1f - t) * (1f - t)
    }
    fun easeInCubic(x: Float): Float {
        val t = x.coerceIn(0f, 1f); return t * t * t
    }

    fun blend(a: Color, b: Color, t: Float): Color {
        val i = 1f - t
        return Color(
            (a.red   * i + b.red   * t).toInt().coerceIn(0, 255),
            (a.green * i + b.green * t).toInt().coerceIn(0, 255),
            (a.blue  * i + b.blue  * t).toInt().coerceIn(0, 255),
            (a.alpha * i + b.alpha * t).toInt().coerceIn(0, 255),
        )
    }

    fun withAlpha(c: Color, a: Int): Color =
        Color(c.red, c.green, c.blue, a.coerceIn(0, 255))

    fun accent(): Color = ClientTheme.getColor(0)
    fun accent2(): Color = ClientTheme.getColor(10)

    private fun lineColor(hovered: Boolean, selected: Boolean, accent: Color, baseA: Int = 255): Color {
        return when {
            selected -> withAlpha(accent, (200 * baseA / 255f).toInt())
            hovered -> withAlpha(accent, (140 * baseA / 255f).toInt())
            else -> Color(34, 40, 52, baseA)
        }
    }

    // ── Flat panel ────────────────────────────────────────────────────────

    /** Flat panel: solid fill + 1px line border + small top accent tick. No glow, no gradients. */
    @JvmOverloads
    fun drawPanel(x: Float, y: Float, w: Float, h: Float, accent: Color, stripe: Boolean = true) {
        val r = 8f
        // base
        RoundedUtil.drawRound(x, y, w, h, r, Color(11, 14, 20, 242))
        // border
        RoundedUtil.drawRoundOutline(x, y, w, h, r, 1f, Color(0, 0, 0, 0), Color(32, 38, 50, 255))
        // top accent tick (short flat segment, not full gradient line)
        val tickW = 42f.coerceAtMost(w * 0.3f)
        RoundedUtil.drawRound(x + 12f, y + 5f, tickW, 2f, 1f, withAlpha(accent, 200))
        if (stripe && h > 60f) {
            // thin flat spine — no glow bed
            RoundedUtil.drawRound(x + 7f, y + 16f, 2f, h - 32f, 1f, Color(30, 36, 48, 255))
            RoundedUtil.drawRound(x + 7f, y + 16f, 2f, (h - 32f) * 0.45f, 1f, withAlpha(accent, 220))
        }
    }

    /** Floating card variant — flat, slightly lighter fill. */
    fun drawCard(x: Float, y: Float, w: Float, h: Float, accent: Color, radius: Float = 8f) {
        RoundedUtil.drawRound(x, y, w, h, radius, Color(13, 16, 23, 245))
        RoundedUtil.drawRoundOutline(x, y, w, h, radius, 1f, Color(0, 0, 0, 0), Color(34, 40, 52, 255))
        // header rule
        RoundedUtil.drawRound(x + 12f, y + 6f, 28f, 2f, 1f, withAlpha(accent, 190))
    }

    /**
     * Split accent title. Returns divider Y.
     * Flat: left white, right accent, small square marker.
     */
    fun drawSplitTitle(x: Float, y: Float, leftWord: String, rightWord: String,
                       accent: Color, innerW: Float): Float {
        val font = Fonts.font40Bold
        val tw = font.getStringWidth(leftWord).toFloat()
        font.drawStringWithShadow(leftWord, x, y, Color(238, 239, 246).rgb)
        font.drawStringWithShadow(rightWord, x + tw, y, accent.rgb)
        // small flat square after title (not glow dot)
        val dotX = x + tw + font.getStringWidth(rightWord) + 6f
        RoundedUtil.drawRound(dotX, y + font.FONT_HEIGHT - 5f, 4f, 4f, 1f, withAlpha(accent, 230))
        val divY = y + font.FONT_HEIGHT + 8f
        drawDivider(x, divY, innerW, accent)
        return divY
    }

    fun drawDivider(x: Float, y: Float, w: Float, accent: Color) {
        // flat 1px base line + short accent segment
        RoundedUtil.drawRound(x, y, w, 1f, 0.5f, Color(30, 36, 48, 255))
        RoundedUtil.drawRound(x, y, w.coerceAtMost(56f), 1f, 0.5f, withAlpha(accent, 230))
    }

    /**
     * Flat line button. Returns updated hover anim.
     * @param appear 0..1 entrance progress
     */
    @JvmOverloads
    fun drawButton(
        x: Float, y: Float, w: Float, h: Float, label: String,
        mouseX: Int, mouseY: Int, hovAnim: Float, accent: Color,
        danger: Boolean = false, primary: Boolean = false, enabled: Boolean = true,
        appear: Float = 1f
    ): Float {
        val a = appear.coerceIn(0f, 1f)
        if (a < 0.02f) return hovAnim
        val drawY = y + (1f - easeOutCubic(a)) * 10f
        val hov = enabled && a > 0.6f && mouseWithinBounds(mouseX, mouseY, x, drawY, x + w, drawY + h)
        val newAnim = lerp(hovAnim, if (hov) 1f else 0f, 0.25f)
        val r = 6f
        val mul = (if (enabled) 1f else 0.45f) * a

        // fill — flat, no gradients
        val fill = when {
            !enabled -> Color(12, 14, 19, (200 * mul).toInt())
            primary -> {
                // primary = solid accent, dark text
                Color(accent.red, accent.green, accent.blue, (235 * mul).toInt())
            }
            danger -> Color(22, 14, 16, (225 * mul).toInt())
            else -> {
                val lift = (newAnim * 10).toInt()
                Color(15 + lift, 18 + lift, 26 + lift, (230 * mul).toInt())
            }
        }
        RoundedUtil.drawRound(x, drawY, w, h, r, fill)

        // border — flat 1px
        val border = when {
            !enabled -> Color(28, 32, 42, (200 * mul).toInt())
            primary -> withAlpha(accent, (255 * mul).toInt())
            danger -> blend(Color(70, 36, 40, (255 * mul).toInt()),
                Color(200, 80, 90, (255 * mul).toInt()), newAnim)
            else -> blend(Color(38, 44, 58, (255 * mul).toInt()),
                withAlpha(accent, (230 * mul).toInt()), newAnim * 0.85f)
        }
        RoundedUtil.drawRoundOutline(x, drawY, w, h, r, 1f, Color(0, 0, 0, 0), border)

        // left tick for hovered / primary (flat marker, replaces glow)
        if ((newAnim > 0.05f && !primary) || (primary && enabled)) {
            val tickH = h - 12f
            val tickA = if (primary) 255 else (newAnim * 230).toInt()
            val tickCol = if (danger) Color(205, 85, 95, (tickA * mul).toInt())
            else if (primary) Color(10, 12, 16, (200 * mul).toInt())
            else withAlpha(accent, (tickA * mul).toInt())
            RoundedUtil.drawRound(x + 5f, drawY + 6f, 2f, tickH.coerceAtLeast(4f), 1f, tickCol)
        }

        val font = Fonts.SFBold35
        val tx = x + w / 2f - font.getStringWidth(label) / 2f
        val ty = drawY + h / 2f - font.FONT_HEIGHT / 2f
        val tCol = when {
            !enabled -> Color(110, 115, 130, (140 * a).toInt())
            primary -> Color(10, 12, 16, (255 * a).toInt())
            danger -> blend(Color(190, 160, 165, (255 * a).toInt()),
                Color(255, 140, 145, (255 * a).toInt()), newAnim)
            else -> blend(Color(175, 180, 195, (255 * a).toInt()),
                Color(245, 246, 250, (255 * a).toInt()), newAnim)
        }
        // flat text — no shadow for primary (dark on accent), shadow otherwise
        if (primary && enabled) font.drawString(label, tx, ty, tCol.rgb, false)
        else font.drawStringWithShadow(label, tx, ty, tCol.rgb)
        return newAnim
    }

    /**
     * Small flat badge (version, status).
     */
    fun drawPill(x: Float, y: Float, w: Float, h: Float, label: String, accent: Color, hot: Boolean = false, hov: Float = 0f) {
        val r = 4f
        RoundedUtil.drawRound(x, y, w, h, r, Color(13, 16, 22, 230))
        val borderBase = if (hot) withAlpha(accent, 200) else Color(36, 42, 56, 255)
        RoundedUtil.drawRoundOutline(x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            blend(borderBase, withAlpha(accent, 220), (hov * 0.7f).coerceIn(0f, 1f)))
        // tiny left dot
        RoundedUtil.drawRound(x + 6f, y + h / 2f - 2f, 4f, 4f, 1f, withAlpha(accent, 220))
        val f = Fonts.SFBold30
        f.drawString(label, x + 14f, y + h / 2f - f.FONT_HEIGHT / 2f,
            Color(200, 205, 218, 235).rgb, false)
    }

    /**
     * Cascade progress.
     */
    fun stagger(elapsedSec: Float, index: Int, staggerSec: Float = 0.3f, durationSec: Float = 0.65f): Float {
        val t = ((elapsedSec - index * staggerSec) / durationSec).coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }

    /** Flat list / row cell with left selection tick. No gradients, no halo. */
    fun drawRow(x: Float, y: Float, w: Float, h: Float, accent: Color,
                hovered: Boolean, selected: Boolean) {
        val r = 6f
        val fill = when {
            selected -> Color(18, 22, 30, 235)
            hovered -> Color(15, 18, 25, 225)
            else -> Color(12, 15, 21, 210)
        }
        RoundedUtil.drawRound(x, y, w, h, r, fill)
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            when {
                selected -> withAlpha(accent, 200)
                hovered -> Color(52, 60, 76, 255)
                else -> Color(30, 35, 47, 255)
            }
        )
        if (selected || hovered) {
            val tickA = if (selected) 230 else 150
            RoundedUtil.drawRound(x + 4f, y + 6f, 2f, h - 12f, 1f, withAlpha(accent, tickA))
        }
    }

    /** Flat text-field: dark fill + 1px border + bottom accent line on focus. */
    fun drawTextField(field: GuiTextField, x: Float, y: Float, w: Float, h: Float,
                      placeholder: String, accent: Color, focusAnim: Float): Float {
        resizeField(field, (w - 12f).toInt().coerceAtLeast(10), 12)
        val focused = field.isFocused
        val newAnim = lerp(focusAnim, if (focused) 1f else 0f, 0.25f)
        val r = 6f
        RoundedUtil.drawRound(x, y, w, h, r, Color(10, 13, 19, 230))
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            blend(Color(32, 38, 50, 255), withAlpha(accent, 220), newAnim)
        )
        if (newAnim > 0.02f) {
            RoundedUtil.drawRound(x + 8f, y + h - 2f, w - 16f, 1.5f, 0.75f,
                withAlpha(accent, (220 * newAnim).toInt()))
        }
        field.drawTextBox()
        if (field.text.isEmpty() && !focused) {
            Fonts.SFBold35.drawString(placeholder,
                x + 8f, y + h / 2f - Fonts.SFBold35.FONT_HEIGHT / 2f,
                Color(110, 115, 132, 170).rgb, false)
        }
        return newAnim
    }

    // ── Reflective resize for GuiTextField ────────────────────────────────────
    private val fieldWidthRef  by lazy { findField("width",  "field_146218_h") }
    private val fieldHeightRef by lazy { findField("height", "field_146219_i") }

    private fun findField(vararg names: String): java.lang.reflect.Field? {
        for (n in names) try {
            val f = GuiTextField::class.java.getDeclaredField(n); f.isAccessible = true; return f
        } catch (_: NoSuchFieldException) {}
        return null
    }

    fun resizeField(field: GuiTextField, w: Int, h: Int) {
        try { fieldWidthRef?.setInt(field, w) } catch (_: Throwable) {}
        try { fieldHeightRef?.setInt(field, h) } catch (_: Throwable) {}
    }
}
