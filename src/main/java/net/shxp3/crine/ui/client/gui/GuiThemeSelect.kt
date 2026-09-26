package net.shxp3.crine.ui.client.gui

import net.shxp3.crine.Crine
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.MouseUtils.mouseWithinBounds
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.RoundedUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.ceil

/**
 * Theme picker — Meridian console style.
 *
 * A single sharp sheet: split title, uppercase census caption, dense
 * palette grid, hairline footer with Back. No pills, no accent ticks,
 * no glow badges — selection is a baseline underline + LIVE marker.
 */
class GuiThemeSelect(private val prev: GuiScreen) : GuiScreen() {

    private var openMs = 0L
    private var scroll = 0f
    private var hoverIndex = -1
    private var leaving = false
    private var leaveStartMs = 0L
    private var backHover = 0f

    private val themes = ClientTheme.THEME_NAMES
    private val cols = 4
    private val cellW = 118f
    private val cellH = 60f
    private val gap = 10f

    override fun initGui() {
        openMs = System.currentTimeMillis()
        scroll = 0f
        hoverIndex = -1
        leaving = false
        leaveStartMs = 0L
        backHover = 0f
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val now = System.currentTimeMillis()
        val elapsed = (now - openMs) / 1000f
        val leaveT = if (leaving) ((now - leaveStartMs) / 1000f / 0.35f).coerceIn(0f, 1f) else 0f
        val leaveEase = leaveT * leaveT * (3f - 2f * leaveT)
        val inMul = 1f - leaveEase

        ThemedBackground.draw(width, height, mouseX, mouseY)

        // ── Sheet ───────────────────────────────────────────────────────
        val pad = 14f
        val panX = pad
        val panY = pad
        val panW = sw - pad * 2
        val panH = sh - pad * 2
        val innerX = panX + 18f
        val innerW = panW - 36f

        ThemedUI.drawPanel(panX, panY, panW, panH, accent)
        val divY = ThemedUI.drawSplitTitle(innerX, panY + 16f, "Theme", " Color", accent, innerW)

        // census caption
        val capF = Fonts.SFBold24
        val caption = "${themes.size} PALETTES  ·  CLICK TO APPLY"
        capF.drawString(caption, innerX, divY + 8f,
            Color(115, 121, 138, (170 * inMul).toInt()).rgb, false)
        val gridTop = divY + 8f + capF.FONT_HEIGHT + 8f

        // footer geometry (mirrored in mouseClicked)
        val btnH = 22f
        val backW = 120f
        val backX = (sw - backW) / 2f
        val backY = panY + panH - 10f - btnH
        val hintF = Fonts.SFBold24
        val curName = ClientTheme.ClientColorMode.get()
        val hint = "CURRENT: ${curName.uppercase()}"
        val hintY = backY - hintF.FONT_HEIGHT - 8f
        val gridBottom = hintY - 8f

        // ── Grid ────────────────────────────────────────────────────────
        val gridW = cols * cellW + (cols - 1) * gap
        val startX = innerX + (innerW - gridW) / 2f
        val rows = ceil(themes.size / cols.toFloat()).toInt()
        val contentH = rows * (cellH + gap) - gap
        val viewH = (gridBottom - gridTop).coerceAtLeast(60f)
        val maxScroll = (contentH - viewH).coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, maxScroll)

        hoverIndex = -1
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        RenderUtils.prepareScissorBox(panX, gridTop, panX + panW, gridBottom)

        for (i in themes.indices) {
            val c = i % cols
            val r = i / cols
            val cellAppear = ThemedUI.stagger(elapsed, i, 0.035f, 0.35f) * inMul
            if (cellAppear < 0.02f) continue

            val x = startX + c * (cellW + gap)
            val y = gridTop + r * (cellH + gap) - scroll +
                (1f - cellAppear) * 12f + leaveEase * 18f
            if (y + cellH < gridTop - 4f || y > gridBottom + 4f) continue

            val selected = themes[i].equals(curName, ignoreCase = true)
            val over = !leaving && cellAppear > 0.6f &&
                mouseWithinBounds(mouseX, mouseY, x, y, x + cellW, y + cellH)
            if (over) hoverIndex = i

            val preview = ClientTheme.getColorFromName(themes[i], 0, 255, false)
            val preview2 = ClientTheme.getColorFromName(themes[i], 8, 255, false)
            val aMul = cellAppear
            val cr = 3f

            // sharp cell
            val fill = when {
                selected -> Color(20, 24, 32, (238 * aMul).toInt())
                over -> Color(16, 19, 26, (228 * aMul).toInt())
                else -> Color(11, 14, 19, (214 * aMul).toInt())
            }
            RoundedUtil.drawRound(x, y, cellW, cellH, cr, fill)
            RoundedUtil.drawRoundOutline(x, y, cellW, cellH, cr, 1f, Color(0, 0, 0, 0),
                when {
                    selected -> ThemedUI.withAlpha(accent, (200 * aMul).toInt())
                    over -> Color(255, 255, 255, (60 * aMul).toInt())
                    else -> Color(255, 255, 255, (24 * aMul).toInt())
                })

            // two square swatches (flat halves, no rounding)
            val barX = x + 8f
            val barY = y + 8f
            val barW = cellW - 16f
            val barH = 16f
            val half = barW / 2f
            Gui.drawRect(barX.toInt(), barY.toInt(), (barX + half).toInt(), (barY + barH).toInt(),
                Color(preview.red, preview.green, preview.blue, (255 * aMul).toInt()).rgb)
            Gui.drawRect((barX + half).toInt(), barY.toInt(), (barX + barW).toInt(), (barY + barH).toInt(),
                Color(preview2.red, preview2.green, preview2.blue, (255 * aMul).toInt()).rgb)

            // name + LIVE marker
            val nameF = Fonts.SFBold30
            nameF.drawString(themes[i], x + 8f, y + 32f,
                Color(222, 226, 236, (232 * aMul).toInt()).rgb, false)
            if (selected) {
                val live = "LIVE"
                val lf = Fonts.SFBold24
                lf.drawString(live, x + cellW - 8f - lf.getStringWidth(live), y + 33f,
                    ThemedUI.withAlpha(accent, (255 * aMul).toInt()).rgb, false)
            }

            // baseline emphasis
            if (selected || over) {
                val uw = (cellW - 16f) * (if (selected) 1f else 0.45f)
                Gui.drawRect((x + 8).toInt(), (y + cellH - 3).toInt(),
                    (x + 8 + uw).toInt(), (y + cellH - 2).toInt(),
                    ThemedUI.withAlpha(if (selected) accent else preview, ((if (selected) 230 else 150) * aMul).toInt()).rgb)
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        // scrollbar — hairline track, accent thumb
        if (maxScroll > 1f) {
            val trackX = startX + gridW + 7f
            Gui.drawRect(trackX.toInt(), gridTop.toInt(), (trackX + 2).toInt(), gridBottom.toInt(),
                Color(255, 255, 255, 10).rgb)
            val thumbH = (viewH / contentH * viewH).coerceAtLeast(22f)
            val thumbY = gridTop + (scroll / maxScroll) * (viewH - thumbH)
            Gui.drawRect(trackX.toInt(), thumbY.toInt(), (trackX + 2).toInt(), (thumbY + thumbH).toInt(),
                ThemedUI.withAlpha(accent, 150).rgb)
        }

        // current hint
        hintF.drawString(hint, (sw - hintF.getStringWidth(hint)) / 2f, hintY,
            Color(140, 145, 162, (160 * inMul).toInt()).rgb, false)

        // back button
        val backAppear = ThemedUI.stagger(elapsed, 4, 0.08f, 0.4f) * inMul
        backHover = ThemedUI.drawButton(backX, backY + (1f - backAppear) * 10f, backW, btnH, "Back",
            mouseX, mouseY, backHover, accent, appear = backAppear)

        GlStateManager.resetColor()

        if (leaving && leaveT >= 1f) {
            mc.displayGuiScreen(prev)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0 || leaving) return
        val sw = width.toFloat()
        val sh = height.toFloat()
        val elapsed = (System.currentTimeMillis() - openMs) / 1000f

        val pad = 14f
        val panY = pad
        val panH = sh - pad * 2
        val innerW = sw - pad * 2 - 36f
        val divY = panY + 16f + Fonts.font40Bold.FONT_HEIGHT + 8f
        val gridTop = divY + 8f + Fonts.SFBold24.FONT_HEIGHT + 8f

        val btnH = 22f
        val backW = 120f
        val backX = (sw - backW) / 2f
        val backY = panY + panH - 10f - btnH

        val backAppear = ThemedUI.stagger(elapsed, 4, 0.08f, 0.4f)
        if (backAppear > 0.6f && mouseWithinBounds(mouseX, mouseY, backX, backY, backX + backW, backY + btnH)) {
            beginLeave()
            return
        }

        val panX = pad
        val innerX = panX + 18f
        val hintY = backY - Fonts.SFBold24.FONT_HEIGHT - 8f
        val gridBottom = hintY - 8f
        val gridW = cols * cellW + (cols - 1) * gap
        val startX = innerX + (innerW - gridW) / 2f
        val rows = ceil(themes.size / cols.toFloat()).toInt()
        val contentH = rows * (cellH + gap) - gap
        val viewH = (gridBottom - gridTop).coerceAtLeast(60f)
        val maxScroll = (contentH - viewH).coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, maxScroll)

        for (i in themes.indices) {
            val cellAppear = ThemedUI.stagger(elapsed, i, 0.035f, 0.35f)
            if (cellAppear < 0.6f) continue
            val c = i % cols
            val r = i / cols
            val x = startX + c * (cellW + gap)
            val y = gridTop + r * (cellH + gap) - scroll + (1f - cellAppear) * 12f
            if (y + cellH < gridTop - 4f || y > gridBottom + 4f) continue
            if (mouseWithinBounds(mouseX, mouseY, x, y, x + cellW, y + cellH)) {
                ClientTheme.ClientColorMode.set(themes[i])
                try {
                    Crine.fileManager.saveConfig(Crine.fileManager.themeConfig)
                } catch (_: Throwable) {
                }
                return
            }
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        if (leaving) return
        val d = org.lwjgl.input.Mouse.getEventDWheel()
        if (d != 0) {
            scroll -= if (d > 0) 30f else -30f
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (leaving) return
        if (keyCode == Keyboard.KEY_ESCAPE) beginLeave()
    }

    private fun beginLeave() {
        leaving = true
        leaveStartMs = System.currentTimeMillis()
    }

    override fun doesGuiPauseGame() = false
}
