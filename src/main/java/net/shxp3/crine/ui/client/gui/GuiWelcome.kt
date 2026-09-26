package net.shxp3.crine.ui.client.gui

import net.shxp3.crine.Crine
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import java.awt.Color

/**
 * Crine startup splash — Meridian style.
 * Bare stage: oversized wordmark, single progress rule, quiet prompt.
 * No cards, no badges, no panels.
 */
class GuiWelcome : GuiScreen() {

    private var openMs = 0L
    private var leaving = false
    private var leaveStartMs = 0L

    override fun initGui() {
        openMs = System.currentTimeMillis()
        leaving = false
        leaveStartMs = 0L
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val now = System.currentTimeMillis()
        val t = (now - openMs) / 1000f

        val leaveT = if (leaving) ((now - leaveStartMs) / 1000f / 0.35f).coerceIn(0f, 1f) else 0f
        val leaveEase = leaveT * leaveT * (3f - 2f * leaveT)

        ThemedBackground.draw(width, height, mouseX, mouseY)

        val nameA = ThemedUI.stagger(t, 0, 0.05f, 0.5f) * (1f - leaveEase)
        val ruleA = ThemedUI.stagger(t, 1, 0.12f, 0.5f) * (1f - leaveEase)
        val promptA = ThemedUI.stagger(t, 2, 0.14f, 0.45f) * (1f - leaveEase)
        val footA = ThemedUI.stagger(t, 3, 0.14f, 0.45f) * (1f - leaveEase)

        val cx = sw / 2f
        val cy = sh * 0.5f - 30f

        // wordmark
        val titleFont = Fonts.SFBold50
        val name = "CRINE"
        val nameW = titleFont.getStringWidth(name).toFloat()
        val rise = (1f - nameA) * 16f + leaveEase * -20f
        if (nameA > 0.02f) {
            titleFont.drawString(name, cx - nameW / 2f, cy + rise,
                Color(242, 244, 250, (255 * nameA).toInt()).rgb, false)
            // version set small, right of wordmark baseline
            val ver = "v${Crine.CLIENT_VERSION}"
            Fonts.SFBold30.drawString(ver, cx + nameW / 2f + 8f,
                cy + rise + titleFont.FONT_HEIGHT - Fonts.SFBold30.FONT_HEIGHT - 2f,
                Color(130, 136, 152, (220 * nameA).toInt()).rgb, false)
        }

        // single progress rule
        if (ruleA > 0.02f) {
            val barW = 210f.coerceAtMost(sw - 60f)
            val barX = cx - barW / 2f
            val barY = cy + titleFont.FONT_HEIGHT + 16f
            Gui.drawRect(barX.toInt(), barY.toInt(), (barX + barW).toInt(), (barY + 1).toInt(),
                Color(255, 255, 255, (22 * ruleA).toInt()).rgb)
            val prog = ((t * 0.55f) % 1.4f).coerceAtMost(1f)
            val fillW = barW * prog.coerceIn(0f, 1f)
            if (fillW > 1f) {
                Gui.drawRect(barX.toInt(), barY.toInt(), (barX + fillW).toInt(), (barY + 1).toInt(),
                    Color(accent.red, accent.green, accent.blue, (230 * ruleA).toInt()).rgb)
            }
            val tag = "PRECISION · CONTROL · FLOW"
            val tagW = Fonts.SFBold24.getStringWidth(tag).toFloat()
            Fonts.SFBold24.drawString(tag, cx - tagW / 2f, barY + 9f,
                Color(115, 121, 138, (170 * ruleA).toInt()).rgb, false)
        }

        // prompt
        if (promptA > 0.02f) {
            val prompt = "PRESS SPACE"
            val pw = Fonts.SFBold30.getStringWidth(prompt).toFloat()
            Fonts.SFBold30.drawString(prompt, cx - pw / 2f,
                cy + titleFont.FONT_HEIGHT + 52f + (1f - promptA) * 10f + leaveEase * 16f,
                Color(200, 205, 220, (200 * promptA * (0.65f + 0.35f * blink())).toInt()).rgb, false)
        }

        val foot = "CRINE ${Crine.CLIENT_VERSION}  ·  BY ${Crine.CLIENT_CREATOR}"
        Fonts.SFBold24.drawString(foot, cx - Fonts.SFBold24.getStringWidth(foot) / 2f, sh - 24f,
            Color(255, 255, 255, (60 * footA).toInt()).rgb, false)

        GlStateManager.resetColor()

        if (leaving && leaveT >= 1f) {
            val menu = GuiMainMenu()
            Crine.mainMenu = menu
            mc.displayGuiScreen(menu)
        }
    }

    private fun blink(): Float = if ((System.currentTimeMillis() / 600L) % 2L == 0L) 1f else 0.25f

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (leaving) return
        if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RETURN) {
            beginLeave()
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (!leaving && mouseButton == 0) beginLeave()
    }

    private fun beginLeave() {
        leaving = true
        leaveStartMs = System.currentTimeMillis()
    }

    override fun doesGuiPauseGame() = false
}
