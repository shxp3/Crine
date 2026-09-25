package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.Crine
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.sin

/**
 * Theme picker — glass grid with live gradient previews.
 */
class GuiThemeSelect(private val prev: GuiScreen) : GuiScreen() {

    private var openMs = 0L
    private var scroll = 0f
    private var hoverIndex = -1
    private var leaving = false
    private var leaveStartMs = 0L

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
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val now = System.currentTimeMillis()
        val t = (now - openMs) / 1000f
        val elapsed = (now - openMs) / 1000f
        val leaveT = if (leaving) ((now - leaveStartMs) / 1000f / 0.35f).coerceIn(0f, 1f) else 0f
        val leaveEase = leaveT * leaveT * (3f - 2f * leaveT)
        val inMul = 1f - leaveEase
        val pulse = (sin(t * 1.6) * 0.5 + 0.5).toFloat()

        ThemedBackground.draw(width, height, mouseX, mouseY)

        // ── Header card ─────────────────────────────────────────────────
        val headW = 340f.coerceAtMost(sw - 28f)
        val headH = 66f
        val headX = (sw - headW) / 2f
        val headY = 16f + (1f - ThemedUI.stagger(elapsed, 0, 0f, 0.4f)) * 14f - leaveEase * 18f
        val headA = ThemedUI.stagger(elapsed, 0, 0f, 0.4f) * inMul
        if (headA > 0.02f) {
            ThemedUI.drawCard(headX, headY, headW, headH, accent, 8f)
            val title = "Theme Color"
            Fonts.SFBold50.drawStringWithShadow(title, (sw - Fonts.SFBold50.getStringWidth(title)) / 2f,
                headY + 10f, Color(242, 243, 250, (255 * headA).toInt()).rgb)
            val sub = "Select an accent theme"
            Fonts.SFBold30.drawString(sub, (sw - Fonts.SFBold30.getStringWidth(sub)) / 2f,
                headY + 10f + Fonts.SFBold50.FONT_HEIGHT + 3f,
                Color(170, 175, 192, (180 * headA).toInt()).rgb, false)
        }

        // ── Grid panel ──────────────────────────────────────────────────
        val gridW = cols * cellW + (cols - 1) * gap
        val startX = (sw - gridW) / 2f
        val startY = headY + headH + 12f
        val rows = ceil(themes.size / cols.toFloat()).toInt()
        val viewBottom = sh - 64f
        val panelPad = 12f
        val panelX = startX - panelPad
        val panelW = gridW + panelPad * 2f
        val visibleH = (viewBottom - startY).coerceAtLeast(80f)
        val panelH = (visibleH + panelPad * 2f).coerceAtMost(sh - startY - 56f)
        val panelA = ThemedUI.stagger(elapsed, 1, 0.08f, 0.45f) * inMul
        if (panelA > 0.02f) {
            ThemedUI.drawCard(panelX, startY - panelPad, panelW, panelH, accent, 8f)
        }

        hoverIndex = -1
        // clip grid to panel
        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val sf = sr.scaleFactor
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)
        org.lwjgl.opengl.GL11.glScissor(
            (panelX * sf).toInt(),
            ((sr.scaledHeight - (startY - panelPad + panelH)) * sf).toInt(),
            (panelW * sf).toInt(), (panelH * sf).toInt()
        )
        for (i in themes.indices) {
            val c = i % cols
            val r = i / cols
            val cellAppear = ThemedUI.stagger(elapsed, i, 0.035f, 0.35f) * inMul
            if (cellAppear < 0.02f) continue

            val x = startX + c * (cellW + gap)
            val y = startY + r * (cellH + gap) - scroll + (1f - cellAppear) * 16f + leaveEase * 22f
            if (y + cellH < startY - 8f || y > startY - panelPad + panelH) continue

            val selected = themes[i].equals(ClientTheme.ClientColorMode.get(), ignoreCase = true)
            val over = !leaving && cellAppear > 0.6f &&
                mouseWithinBounds(mouseX, mouseY, x, y, x + cellW, y + cellH)
            if (over) hoverIndex = i

            val preview = ClientTheme.getColorFromName(themes[i], 0, 255, false)
            val preview2 = ClientTheme.getColorFromName(themes[i], 8, 255, false)
            val aMul = cellAppear

            // flat cell — no glow halo
            RoundedUtil.drawRound(x, y, cellW, cellH, 6f, Color(13, 16, 23, (235 * aMul).toInt()))
            // flat preview bar (two solid halves, no gradient)
            val barX = x + 8f
            val barY = y + 8f
            val barW = cellW - 16f
            val barH = 18f
            RoundedUtil.drawRound(barX, barY, barW / 2f, barH, 4f,
                Color(preview.red, preview.green, preview.blue, (255 * aMul).toInt()))
            RoundedUtil.drawRound(barX + barW / 2f - 4f, barY, barW / 2f + 4f, barH, 4f,
                Color(preview2.red, preview2.green, preview2.blue, (255 * aMul).toInt()))
            // flat left tick when selected / hovered
            if (selected || over) {
                RoundedUtil.drawRound(x + 4f, y + 8f, 2f, cellH - 16f, 1f,
                    Color(preview.red, preview.green, preview.blue, (230 * aMul).toInt()))
            }
            Fonts.SFBold30.drawString(themes[i], x + 12f, y + 34f,
                Color(222, 226, 236, (232 * aMul).toInt()).rgb, false)

            // selected flat badge
            if (selected) {
                val check = "ON"
                val cw = Fonts.SFBold24.getStringWidth(check) + 12f
                val chh = Fonts.SFBold24.FONT_HEIGHT + 6f
                val checkX = x + cellW - cw - 7f
                val checkY = y + 32f
                RoundedUtil.drawRound(checkX, checkY, cw, chh, 3f,
                    Color(preview.red, preview.green, preview.blue, (230 * aMul).toInt()))
                Fonts.SFBold24.drawString(check, checkX + cw / 2f - Fonts.SFBold24.getStringWidth(check) / 2f,
                    checkY + chh / 2f - Fonts.SFBold24.FONT_HEIGHT / 2f,
                    Color(10, 10, 14, (255 * aMul).toInt()).rgb, false)
            } else if (over) {
                RoundedUtil.drawRound(x + cellW - 9f, y + 34f, 5f, 5f, 1f,
                    Color(preview.red, preview.green, preview.blue, (220 * aMul).toInt()))
            }

            val outline = when {
                selected -> Color(preview.red, preview.green, preview.blue, (235 * aMul).toInt())
                over -> Color(60, 68, 86, (255 * aMul).toInt())
                else -> Color(30, 35, 47, (255 * aMul).toInt())
            }
            RoundedUtil.drawRoundOutline(x, y, cellW, cellH, 6f, if (selected) 1.5f else 1f,
                Color(0, 0, 0, 0), outline)
            // inner hairline
            RoundedUtil.drawRoundOutline(x + 0.6f, y + 0.6f, cellW - 1.2f, cellH - 1.2f, 9.4f, 0.7f,
                Color(0, 0, 0, 0), Color(255, 255, 255, (14 * aMul).toInt()))
        }
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST)

        // scrollbar
        val contentH = rows * (cellH + gap)
        val maxScroll = (contentH - visibleH).coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, maxScroll)
        if (maxScroll > 1f && panelA > 0.05f) {
            val trackX = panelX + panelW - 5f
            val trackY = startY
            val trackH = visibleH
            RoundedUtil.drawRound(trackX, trackY, 2f, trackH, 1f, Color(28, 32, 44, 255))
            val thumbH = (visibleH / contentH * trackH).coerceAtLeast(24f)
            val thumbY = trackY + (scroll / maxScroll) * (trackH - thumbH)
            RoundedUtil.drawRound(trackX, thumbY, 2f, thumbH, 1f,
                Color(accent.red, accent.green, accent.blue, 200))
        }

        // back pill
        val backAppear = ThemedUI.stagger(elapsed, 4, 0.08f, 0.4f) * inMul
        val backW = 120f
        val backH = 24f
        val backX = (sw - backW) / 2f
        val backY = sh - 40f + (1f - backAppear) * 12f + leaveEase * 22f
        val backOver = !leaving && backAppear > 0.6f &&
            mouseWithinBounds(mouseX, mouseY, backX, backY, backX + backW, backY + backH)
        if (backAppear > 0.02f) {
            RoundedUtil.drawRound(backX, backY, backW, backH, 6f, Color(13, 16, 23, (235 * backAppear).toInt()))
            RoundedUtil.drawRoundOutline(backX, backY, backW, backH, 6f, 1f, Color(0, 0, 0, 0),
                if (backOver) Color(accent.red, accent.green, accent.blue, (200 * backAppear).toInt())
                else Color(32, 38, 50, (255 * backAppear).toInt()))
            if (backOver) {
                RoundedUtil.drawRound(backX + 5f, backY + 6f, 2f, backH - 12f, 1f,
                    Color(accent.red, accent.green, accent.blue, (230 * backAppear).toInt()))
            }
            Fonts.SFBold30.drawString("<  Back",
                backX + (backW - Fonts.SFBold30.getStringWidth("<  Back")) / 2f,
                backY + (backH - Fonts.SFBold30.FONT_HEIGHT) / 2f,
                Color(242, 243, 250, (240 * backAppear).toInt()).rgb, false)
        }

        // current theme hint
        val cur = "Current: ${ClientTheme.ClientColorMode.get()}"
        Fonts.SFBold24.drawString(cur, (sw - Fonts.SFBold24.getStringWidth(cur)) / 2f,
            headY + headH + panelH + 16f,
            Color(160, 165, 182, (150 * panelA + pulse * 20 * panelA).toInt()).rgb, false)

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
        val backAppear = ThemedUI.stagger(elapsed, 4, 0.08f, 0.4f)
        val backW = 120f
        val backH = 24f
        val backX = (sw - backW) / 2f
        val backY = sh - 40f + (1f - backAppear) * 12f
        if (backAppear > 0.6f && mouseWithinBounds(mouseX, mouseY, backX, backY, backX + backW, backY + backH)) {
            beginLeave()
            return
        }

        val headH = 66f
        val startY = 16f + headH + 12f
        val gridW = cols * cellW + (cols - 1) * gap
        val startX = (sw - gridW) / 2f
        for (i in themes.indices) {
            val cellAppear = ThemedUI.stagger(elapsed, i, 0.035f, 0.35f)
            if (cellAppear < 0.6f) continue
            val c = i % cols
            val r = i / cols
            val x = startX + c * (cellW + gap)
            val y = startY + r * (cellH + gap) - scroll + (1f - cellAppear) * 16f
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
