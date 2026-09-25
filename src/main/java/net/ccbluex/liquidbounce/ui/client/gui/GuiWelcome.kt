package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.Crine
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import java.awt.Color

/**
 * Crine 1.0 startup splash — flat line style.
 * Centered flat card, version badge, minimal progress line.
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

        val leaveT = if (leaving) ((now - leaveStartMs) / 1000f / 0.4f).coerceIn(0f, 1f) else 0f
        val leaveEase = leaveT * leaveT * (3f - 2f * leaveT)

        ThemedBackground.draw(width, height, mouseX, mouseY)

        val nameA = ThemedUI.stagger(t, 0, 0.05f, 0.5f) * (1f - leaveEase)
        val cardA = ThemedUI.stagger(t, 1, 0.12f, 0.5f) * (1f - leaveEase)
        val promptA = ThemedUI.stagger(t, 2, 0.14f, 0.45f) * (1f - leaveEase)
        val footA = ThemedUI.stagger(t, 3, 0.14f, 0.45f) * (1f - leaveEase)

        val cx = sw / 2f
        val cardW = 360f.coerceAtMost(sw - 32f)
        val cardH = 168f
        val cardX = cx - cardW / 2f
        var cardY = sh * 0.5f - cardH / 2f - 24f + (1f - cardA) * 18f + leaveEase * -24f

        // flat card
        if (cardA > 0.02f) {
            RoundedUtil.drawRound(cardX, cardY, cardW, cardH, 8f, Color(12, 15, 21, (242 * cardA).toInt()))
            RoundedUtil.drawRoundOutline(cardX, cardY, cardW, cardH, 8f, 1f,
                Color(0, 0, 0, 0), Color(32, 38, 50, (255 * cardA).toInt()))
            // top tick + baseline
            RoundedUtil.drawRound(cardX + 14f, cardY + 9f, 30f, 2f, 1f,
                Color(accent.red, accent.green, accent.blue, (210 * cardA).toInt()))
            RoundedUtil.drawRound(cardX + 14f, cardY + cardH - 28f, cardW - 28f, 1f, 0.5f,
                Color(30, 36, 48, (255 * cardA).toInt()))
        }

        val titleFont = Fonts.Nunito60
        val name = "Crine"
        val nameW = titleFont.getStringWidth(name)
        val nameX = cx - nameW / 2f
        val nameY = cardY + 20f + (1f - nameA) * 12f
        titleFont.drawStringWithShadow(name, nameX, nameY,
            Color(245, 246, 252, (255 * nameA).toInt()).rgb)
        RoundedUtil.drawRound(nameX + nameW + 5f, nameY + titleFont.FONT_HEIGHT - 7f, 5f, 5f, 1f,
            Color(accent.red, accent.green, accent.blue, (235 * nameA).toInt()))

        // version badge — flat outline
        val ver = "v${Crine.CLIENT_VERSION}"
        val pillW = Fonts.SFBold30.getStringWidth(ver) + 20f
        val pillH = Fonts.SFBold30.FONT_HEIGHT + 7f
        val pillX = cx - pillW / 2f
        val pillY = nameY + titleFont.FONT_HEIGHT + 8f + (1f - cardA) * 8f
        if (cardA > 0.02f) {
            RoundedUtil.drawRound(pillX, pillY, pillW, pillH, 4f, Color(10, 13, 19, (230 * cardA).toInt()))
            RoundedUtil.drawRoundOutline(pillX, pillY, pillW, pillH, 4f, 1f,
                Color(0, 0, 0, 0), Color(44, 50, 66, (255 * cardA).toInt()))
            Fonts.SFBold30.drawString(ver, pillX + pillW / 2f - Fonts.SFBold30.getStringWidth(ver) / 2f,
                pillY + pillH / 2f - Fonts.SFBold30.FONT_HEIGHT / 2f,
                Color(200, 205, 220, (235 * cardA).toInt()).rgb, false)
        }

        val tag = "precision  ·  control  ·  flow"
        Fonts.SFBold30.drawString(tag, cx - Fonts.SFBold30.getStringWidth(tag) / 2f,
            pillY + pillH + 9f + (1f - cardA) * 6f,
            Color(130, 135, 152, (185 * cardA).toInt()).rgb, false)

        // flat progress line inside card bottom
        if (cardA > 0.05f) {
            val barW = cardW - 40f
            val barX = cx - barW / 2f
            val barY = cardY + cardH - 16f
            RoundedUtil.drawRound(barX, barY, barW, 2f, 1f, Color(30, 36, 48, (255 * cardA).toInt()))
            val prog = ((t * 0.55f) % 1.4f).coerceAtMost(1f)
            val fillW = barW * prog.coerceIn(0f, 1f)
            if (fillW > 2f) {
                RoundedUtil.drawRound(barX, barY, fillW, 2f, 1f,
                    Color(accent.red, accent.green, accent.blue, (220 * cardA).toInt()))
            }
        }

        // prompt — flat outline button look
        val prompt = "Press SPACE to continue"
        val pW = Fonts.SFBold40.getStringWidth(prompt) + 28f
        val pH = Fonts.SFBold40.FONT_HEIGHT + 12f
        val pX = cx - pW / 2f
        val pY = cardY + cardH + 20f + (1f - promptA) * 12f + leaveEase * 20f
        if (promptA > 0.02f) {
            RoundedUtil.drawRound(pX, pY, pW, pH, 6f, Color(12, 15, 21, (235 * promptA).toInt()))
            RoundedUtil.drawRoundOutline(pX, pY, pW, pH, 6f, 1f, Color(0, 0, 0, 0),
                Color(accent.red, accent.green, accent.blue, (170 * promptA).toInt()))
            // left tick
            RoundedUtil.drawRound(pX + 6f, pY + 6f, 2f, pH - 12f, 1f,
                Color(accent.red, accent.green, accent.blue, (220 * promptA).toInt()))
            Fonts.SFBold40.drawString(prompt, pX + pW / 2f - Fonts.SFBold40.getStringWidth(prompt) / 2f,
                pY + pH / 2f - Fonts.SFBold40.FONT_HEIGHT / 2f,
                Color(235, 236, 245, (240 * promptA).toInt()).rgb, false)
        }

        val foot = "Crine 1.0  ·  by ${Crine.CLIENT_CREATOR}"
        Fonts.SFBold24.drawString(foot, cx - Fonts.SFBold24.getStringWidth(foot) / 2f,
            sh - 26f + (1f - footA) * 10f + leaveEase * 16f,
            Color(110, 115, 132, (150 * footA).toInt()).rgb, false)

        GlStateManager.resetColor()

        if (leaving && leaveT >= 1f) {
            val menu = GuiMainMenu()
            Crine.mainMenu = menu
            mc.displayGuiScreen(menu)
        }
    }

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
