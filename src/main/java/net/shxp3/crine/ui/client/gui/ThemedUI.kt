package net.shxp3.crine.ui.client.gui

import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.MouseUtils.mouseWithinBounds
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.RoundedUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiTextField
import java.awt.Color

/**
 * Meridian design primitives for all menus.
 *
 * Sharp-edged console language: 3px corners, hairline borders, flat fills,
 * underline emphasis. Same public API as the previous system so every
 * existing menu (server list, world list, theme select, alt manager)
 * picks up the new look without edits.
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

    // ── Sharp panel ───────────────────────────────────────────────────────

    /** Console sheet: flat fill + hairline border + bottom weight rule. */
    @JvmOverloads
    fun drawPanel(x: Float, y: Float, w: Float, h: Float, accent: Color, stripe: Boolean = true) {
        val r = 3f
        RoundedUtil.drawRound(x, y, w, h, r, Color(9, 11, 15, 244))
        RoundedUtil.drawRoundOutline(x, y, w, h, r, 1f, Color(0, 0, 0, 0), Color(255, 255, 255, 30))
        // baseline weight — a single darker rule along the bottom edge
        Gui.drawRect(x.toInt() + 8, (y + h - 3).toInt(), (x + w).toInt() - 8, (y + h - 2).toInt(),
            Color(255, 255, 255, 10).rgb)
        if (stripe) {
            Gui.drawRect(x.toInt() + 12, (y + h - 3).toInt(), (x + 12 + 34).toInt(), (y + h - 2).toInt(),
                withAlpha(accent, 200).rgb)
        }
    }

    /** Floating card variant — flat, slightly lighter fill. */
    fun drawCard(x: Float, y: Float, w: Float, h: Float, accent: Color, radius: Float = 3f) {
        RoundedUtil.drawRound(x, y, w, h, radius, Color(12, 15, 20, 246))
        RoundedUtil.drawRoundOutline(x, y, w, h, radius, 1f, Color(0, 0, 0, 0), Color(255, 255, 255, 28))
        // footer rule with short accent segment
        Gui.drawRect(x.toInt() + 12, (y + h - 7).toInt(), (x + w).toInt() - 12, (y + h - 6).toInt(),
            Color(255, 255, 255, 14).rgb)
        Gui.drawRect(x.toInt() + 12, (y + h - 7).toInt(), (x + 12 + 30).toInt(), (y + h - 6).toInt(),
            withAlpha(accent, 190).rgb)
    }

    /**
     * Split title. Returns divider Y.
     * Left word white, right word accent, full-width hairline below.
     */
    fun drawSplitTitle(x: Float, y: Float, leftWord: String, rightWord: String,
                       accent: Color, innerW: Float): Float {
        val font = Fonts.font40Bold
        val tw = font.getStringWidth(leftWord).toFloat()
        font.drawStringWithShadow(leftWord, x, y, Color(238, 239, 246).rgb)
        font.drawStringWithShadow(rightWord, x + tw, y, accent.rgb)
        val divY = y + font.FONT_HEIGHT + 8f
        drawDivider(x, divY, innerW, accent)
        return divY
    }

    fun drawDivider(x: Float, y: Float, w: Float, accent: Color) {
        Gui.drawRect(x.toInt(), y.toInt(), (x + w).toInt(), (y + 1).toInt(), Color(255, 255, 255, 15).rgb)
    }

    /**
     * Sharp console button. Returns updated hover anim.
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
        val r = 3f
        val mul = (if (enabled) 1f else 0.45f) * a

        val fill = when {
            !enabled -> Color(12, 14, 19, (200 * mul).toInt())
            primary -> Color(accent.red, accent.green, accent.blue, (235 * mul).toInt())
            danger -> Color(24, 13, 15, (225 * mul).toInt())
            else -> {
                val lift = (newAnim * 8).toInt()
                Color(14 + lift, 17 + lift, 24 + lift, (230 * mul).toInt())
            }
        }
        RoundedUtil.drawRound(x, drawY, w, h, r, fill)

        val border = when {
            !enabled -> Color(255, 255, 255, (24 * mul).toInt())
            primary -> withAlpha(accent, (255 * mul).toInt())
            danger -> blend(Color(120, 50, 55, (255 * mul).toInt()),
                Color(220, 95, 100, (255 * mul).toInt()), newAnim)
            else -> blend(Color(255, 255, 255, (26 * mul).toInt()),
                withAlpha(accent, (230 * mul).toInt()), newAnim * 0.85f)
        }
        RoundedUtil.drawRoundOutline(x, drawY, w, h, r, 1f, Color(0, 0, 0, 0), border)

        // hovered underline — emphasis lives at the baseline, not the edge
        if (newAnim > 0.05f && enabled && !primary) {
            val uw = (w - 16f) * newAnim
            Gui.drawRect((x + 8).toInt(), (drawY + h - 3).toInt(), (x + 8 + uw).toInt(), (drawY + h - 2).toInt(),
                withAlpha(if (danger) Color(220, 95, 100) else accent, (230 * mul).toInt()).rgb)
        }

        val font = Fonts.SFBold35
        val tx = x + w / 2f - font.getStringWidth(label) / 2f
        val ty = drawY + h / 2f - font.FONT_HEIGHT / 2f
        val tCol = when {
            !enabled -> Color(110, 115, 130, (140 * a).toInt())
            primary -> Color(8, 10, 14, (255 * a).toInt())
            danger -> blend(Color(190, 160, 165, (255 * a).toInt()),
                Color(255, 140, 145, (255 * a).toInt()), newAnim)
            else -> blend(Color(175, 180, 195, (255 * a).toInt()),
                Color(245, 246, 250, (255 * a).toInt()), newAnim)
        }
        if (primary && enabled) font.drawString(label, tx, ty, tCol.rgb, false)
        else font.drawStringWithShadow(label, tx, ty, tCol.rgb)
        return newAnim
    }

    /** Small sharp badge (version, status). */
    fun drawPill(x: Float, y: Float, w: Float, h: Float, label: String, accent: Color, hot: Boolean = false, hov: Float = 0f) {
        val r = 3f
        RoundedUtil.drawRound(x, y, w, h, r, Color(10, 13, 18, 232))
        val borderBase = if (hot) withAlpha(accent, 200) else Color(255, 255, 255, 30)
        RoundedUtil.drawRoundOutline(x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            blend(borderBase, withAlpha(accent, 220), (hov * 0.7f).coerceIn(0f, 1f)))
        val f = Fonts.SFBold30
        f.drawString(label, x + w / 2f - f.getStringWidth(label) / 2f, y + h / 2f - f.FONT_HEIGHT / 2f,
            Color(200, 205, 218, 235).rgb, false)
    }

    /** Cascade progress. */
    fun stagger(elapsedSec: Float, index: Int, staggerSec: Float = 0.3f, durationSec: Float = 0.65f): Float {
        val t = ((elapsedSec - index * staggerSec) / durationSec).coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }

    /** Sharp list / row cell with baseline emphasis. */
    fun drawRow(x: Float, y: Float, w: Float, h: Float, accent: Color,
                hovered: Boolean, selected: Boolean) {
        val r = 3f
        val fill = when {
            selected -> Color(20, 24, 32, 238)
            hovered -> Color(16, 19, 26, 228)
            else -> Color(11, 14, 19, 214)
        }
        RoundedUtil.drawRound(x, y, w, h, r, fill)
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            when {
                selected -> withAlpha(accent, 200)
                hovered -> Color(255, 255, 255, 60)
                else -> Color(255, 255, 255, 24)
            }
        )
        if (selected || hovered) {
            val uw = (w - 16f) * (if (selected) 1f else 0.45f)
            Gui.drawRect((x + 8).toInt(), (y + h - 3).toInt(), (x + 8 + uw).toInt(), (y + h - 2).toInt(),
                withAlpha(accent, if (selected) 230 else 140).rgb)
        }
    }

    /** Sharp text-field: dark fill + hairline border, full-border focus. */
    fun drawTextField(field: GuiTextField, x: Float, y: Float, w: Float, h: Float,
                      placeholder: String, accent: Color, focusAnim: Float): Float {
        resizeField(field, (w - 12f).toInt().coerceAtLeast(10), 12)
        val focused = field.isFocused
        val newAnim = lerp(focusAnim, if (focused) 1f else 0f, 0.25f)
        val r = 3f
        RoundedUtil.drawRound(x, y, w, h, r, Color(6, 8, 11, 232))
        RoundedUtil.drawRoundOutline(
            x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            blend(Color(255, 255, 255, 26), withAlpha(accent, 220), newAnim)
        )
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
