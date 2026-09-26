package net.shxp3.crine.ui.client.gui

import net.shxp3.crine.Crine
import net.shxp3.crine.ui.client.altmanager.GuiAltManager
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.MouseUtils.mouseWithinBounds
import net.shxp3.crine.utils.extensions.skin
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.RoundedUtil
import net.shxp3.crine.utils.render.Stencil
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.util.ResourceLocation
import java.util.UUID
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.min

/**
 * Crine main menu — Meridian console style.
 * A single sharp command sheet: wordmark header, hairline-separated
 * command rows, settings/quit footer, session strip below.
 */
class GuiMainMenu : GuiScreen(), GuiYesNoCallback {

    private val rowHover = FloatArray(4)
    private val footHover = FloatArray(2)
    private val rowEnter = FloatArray(6)
    private var enterAnim = 0f
    private var exitAnim = 0f
    private var exitTarget: GuiScreen? = null
    private var openMs = 0L

    // hitboxes
    private var sheetX = 0f
    private var sheetY = 0f
    private var sheetW = 320f
    private var rowY = FloatArray(4)
    private var rowH = 46f
    private var footY = 0f
    private var footH = 28f
    private var footX1 = 0f
    private var footW1 = 0f
    private var footX2 = 0f
    private var footW2 = 0f

    override fun initGui() {
        buttonList.clear()
        enterAnim = 0f
        exitAnim = 0f
        exitTarget = null
        openMs = System.currentTimeMillis()
        for (i in rowEnter.indices) rowEnter[i] = 0f
        for (i in rowHover.indices) rowHover[i] = 0f
        for (i in footHover.indices) footHover[i] = 0f
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)

        ThemedBackground.draw(width, height, mouseX, mouseY)

        enterAnim = ThemedUI.lerp(enterAnim, 1f, 0.10f)
        if (exitTarget != null) exitAnim = ThemedUI.lerp(exitAnim, 1f, 0.16f)
        val easeIn = ThemedUI.easeOutCubic(enterAnim)
        val easeOut = ThemedUI.easeInCubic(exitAnim)

        val elapsed = (System.currentTimeMillis() - openMs) / 1000f
        for (i in rowEnter.indices) {
            rowEnter[i] = ThemedUI.stagger(elapsed, i, staggerSec = 0.06f, durationSec = 0.45f) * (1f - easeOut)
        }
        val globalA = (easeIn * (1f - easeOut)).coerceIn(0f, 1f)

        // ── Console sheet ───────────────────────────────────────────────
        sheetW = min(340f, sw - 24f)
        sheetX = (sw - sheetW) / 2f
        val headH = 58f
        rowH = 46f
        val rowsH = rowH * 4
        footH = 28f
        val sheetH = headH + rowsH + 12f + footH + 12f
        sheetY = ((sh - sheetH - 44f) / 2f).coerceAtLeast(16f) + (1f - rowEnter[0]) * 14f

        if (rowEnter[0] > 0.02f) {
            ThemedUI.drawPanel(sheetX, sheetY, sheetW, sheetH, accent)
        }

        // header: wordmark + version pill
        if (rowEnter[0] > 0.02f) {
            val a = rowEnter[0]
            val brandF = Fonts.SFBold40
            val bx = sheetX + 14f
            val by = sheetY + 12f
            brandF.drawStringWithShadow("CRINE", bx, by,
                Color(240, 242, 248, (255 * a).toInt()).rgb)
            val ver = "v${Crine.CLIENT_VERSION}"
            val pillW = Fonts.SFBold30.getStringWidth(ver) + 18f
            val pillH = Fonts.SFBold30.FONT_HEIGHT + 7f
            ThemedUI.drawPill(sheetX + sheetW - 14f - pillW, by + 1f, pillW, pillH, ver, accent)
            // caption under wordmark
            Fonts.SFBold24.drawString("MINECRAFT 1.8.9", bx, by + brandF.FONT_HEIGHT + 3f,
                Color(120, 126, 142, (200 * a).toInt()).rgb, false)
            // header hairline
            Gui.drawRect((sheetX + 12).toInt(), (sheetY + headH - 8).toInt(),
                (sheetX + sheetW - 12).toInt(), (sheetY + headH - 7).toInt(),
                Color(255, 255, 255, (16 * a).toInt()).rgb)
        }

        // ── Command rows ────────────────────────────────────────────────
        var by = sheetY + headH
        val labels = arrayOf("Singleplayer", "Multiplayer", "Alt Manager", "Theme")
        val hints = arrayOf("LOCAL WORLDS", "JOIN A SERVER", "ACCOUNTS", "ACCENT PALETTE")
        for (i in labels.indices) {
            rowY[i] = by
            drawCommandRow(sheetX + 12f, by, sheetW - 24f, rowH, labels[i], hints[i],
                mouseX, mouseY, i, accent, rowEnter[i + 1])
            by += rowH
        }

        // ── Footer buttons ──────────────────────────────────────────────
        footY = by + 12f
        val fgap = 8f
        footW1 = (sheetW - 24f - fgap) / 2f
        footW2 = sheetW - 24f - footW1 - fgap
        footX1 = sheetX + 12f
        footX2 = sheetX + 12f + footW1 + fgap
        val footA = rowEnter[5]
        footHover[0] = ThemedUI.drawButton(footX1, footY, footW1, footH, "Settings",
            mouseX, mouseY, footHover[0], accent, appear = footA)
        footHover[1] = ThemedUI.drawButton(footX2, footY, footW2, footH, "Quit",
            mouseX, mouseY, footHover[1], accent, danger = true, appear = footA)

        // ── Session strip + credits ─────────────────────────────────────
        drawSessionStrip(sw, sh, accent, footA, globalA)

        if (exitTarget != null && exitAnim > 0.97f) {
            val target = exitTarget
            exitTarget = null
            mc.displayGuiScreen(target)
        }
        GlStateManager.resetColor()
    }

    /**
     * Full-bleed command row: hairline top rule, label left, descriptor
     * right, baseline underline grows on hover. No numbers, no ticks.
     */
    private fun drawCommandRow(x: Float, y: Float, w: Float, h: Float,
                               title: String, hint: String,
                               mouseX: Int, mouseY: Int, idx: Int,
                               accent: Color, appear: Float) {
        if (appear < 0.02f) return
        val dy = y + (1f - ThemedUI.easeOutCubic(appear)) * 8f
        val over = mouseWithinBounds(mouseX, mouseY, x, dy, x + w, dy + h) && exitTarget == null
        rowHover[idx] = ThemedUI.lerp(rowHover[idx], if (over) 1f else 0f, 0.22f)
        val hov = rowHover[idx]

        // top hairline
        Gui.drawRect(x.toInt(), dy.toInt(), (x + w).toInt(), (dy + 1).toInt(),
            Color(255, 255, 255, (14 * appear).toInt()).rgb)
        if (hov > 0.02f) {
            RenderUtils.drawRect(x, dy + 1f, x + w, dy + h,
                Color(255, 255, 255, (hov * 10 * appear).toInt()).rgb)
        }

        val titleF = Fonts.SFBold35
        val hintF = Fonts.SFBold24
        val tx = x + 4f + hov * 3f
        val top = dy + (h - (titleF.FONT_HEIGHT + 2f + hintF.FONT_HEIGHT)) / 2f
        titleF.drawStringWithShadow(title, tx, top,
            ThemedUI.blend(Color(170, 176, 192, (235 * appear).toInt()),
                Color(245, 246, 250, (255 * appear).toInt()), hov).rgb)
        hintF.drawString(hint, tx, top + titleF.FONT_HEIGHT + 2f,
            Color(115, 121, 138, ((150 + hov * 60) * appear).toInt()).rgb, false)

        // descriptor arrow side: hover underline from the right edge
        val uw = (w * 0.35f) * hov
        if (uw > 1f) {
            Gui.drawRect((x + w - uw).toInt(), (dy + h - 3).toInt(), (x + w).toInt(), (dy + h - 2).toInt(),
                Color(accent.red, accent.green, accent.blue, (220 * appear).toInt()).rgb)
        }
        // status square at right, lights up on hover
        val sq = 5f
        val sqx = x + w - 8f - sq
        val sqy = dy + h / 2f - sq / 2f - 6f
        Gui.drawRect(sqx.toInt(), sqy.toInt(), (sqx + sq).toInt(), (sqy + sq).toInt(),
            ThemedUI.blend(Color(70, 77, 92, (200 * appear).toInt()),
                Color(accent.red, accent.green, accent.blue, (255 * appear).toInt()), hov).rgb)
    }

    private fun drawSessionStrip(sw: Float, sh: Float, accent: Color, appear: Float, globalA: Float) {
        if (appear < 0.02f) return
        val name = try { mc.session.username } catch (_: Throwable) { "Player" }
        val prem = try { mc.session.token.length >= 32 } catch (_: Throwable) { false }
        val status = if (prem) "PREMIUM" else "OFFLINE"

        val head = 18f
        val nameW = Fonts.SFBold30.getStringWidth(name).toFloat()
        val totalW = head + 8f + nameW + 10f + Fonts.SFBold24.getStringWidth(status) + 12f
        val sx = (sw - totalW) / 2f
        val stripY = (sheetY + sheetH() + 10f).coerceAtMost(sh - 40f)

        drawSquareHead(sx, stripY, head, accent, appear)
        Fonts.SFBold30.drawStringWithShadow(name, sx + head + 8f, stripY + 1f,
            Color(225, 227, 236, (225 * appear).toInt()).rgb)
        val dotCol = if (prem) ThemedUI.withAlpha(accent, (230 * appear).toInt())
        else Color(200, 90, 95, (230 * appear).toInt())
        val statusX = sx + head + 8f + nameW + 10f
        Gui.drawRect(statusX.toInt(), (stripY + 4).toInt(), (statusX + 4).toInt(), (stripY + 8).toInt(), dotCol.rgb)
        Fonts.SFBold24.drawString(status, statusX + 8f, stripY + 1f,
            Color(140, 145, 162, (200 * appear).toInt()).rgb, false)

        val foot = "CRINE ${Crine.CLIENT_VERSION}  ·  BY ${Crine.CLIENT_CREATOR}"
        Fonts.SFBold24.drawString(foot, (sw - Fonts.SFBold24.getStringWidth(foot)) / 2f, sh - 20f,
            Color(255, 255, 255, (55 * globalA).toInt()).rgb, false)
    }

    private fun sheetH(): Float {
        return 58f + rowH * 4 + 12f + footH + 12f
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mouseX: Int, mouseY: Int, btn: Int) {
        if (btn != 0 || exitTarget != null) return
        val targets = arrayOf<GuiScreen>(
            GuiWorldList(this),
            GuiServerList(this),
            GuiAltManager(this),
            GuiThemeSelect(this)
        )
        for (i in targets.indices) {
            if (mouseWithinBounds(mouseX, mouseY, sheetX + 12f, rowY[i], sheetX + 12f + sheetW - 24f, rowY[i] + rowH)) {
                exitTarget = targets[i]; exitAnim = 0f; return
            }
        }
        if (mouseWithinBounds(mouseX, mouseY, footX1, footY, footX1 + footW1, footY + footH)) {
            exitTarget = GuiOptions(this, mc.gameSettings); exitAnim = 0f; return
        }
        if (mouseWithinBounds(mouseX, mouseY, footX2, footY, footX2 + footW2, footY + footH)) {
            mc.shutdown()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
    }

    override fun doesGuiPauseGame() = false

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Square stencil-clipped head. */
    private fun drawSquareHead(x: Float, y: Float, size: Float, accent: Color, appear: Float) {
        val skin = resolveSessionSkin()
        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x, y, x + size, y + size, 2f)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        GlStateManager.color(1f, 1f, 1f, 1f)
        RenderUtils.drawHead(skin, x.toInt(), y.toInt(), size.toInt(), size.toInt(), Color.WHITE.rgb)
        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()
        // hairline frame
        RenderUtils.drawRectBasedBorder(x, y, x + size, y + size, 1f, Color(255, 255, 255, (60 * appear).toInt()).rgb)
    }

    private fun resolveSessionSkin(): ResourceLocation {
        mc.thePlayer?.skin?.let { return it }
        val profile = mc.session.profile
        val id: UUID? = profile?.id
        if (id != null) {
            mc.netHandler?.getPlayerInfo(id)?.locationSkin?.let { return it }
        }
        val name = mc.session.username
        if (!name.isNullOrEmpty()) {
            return AbstractClientPlayer.getLocationSkin(name)
        }
        return if (id != null) DefaultPlayerSkin.getDefaultSkin(id) else DefaultPlayerSkin.getDefaultSkinLegacy()
    }
}
