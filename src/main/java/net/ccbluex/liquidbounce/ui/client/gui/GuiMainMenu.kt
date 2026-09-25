package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.Crine
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.extensions.skin
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.ccbluex.liquidbounce.utils.render.Stencil
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
 * Crine 1.0 main menu — "Flat Line" style.
 * Single centered column, numbered rows, minimal footer, no top-right pills,
 * no updater, no changelog. Hero reads "Crine".
 */
class GuiMainMenu : GuiScreen(), GuiYesNoCallback {

    private val navHover = FloatArray(4)
    private val footHover = FloatArray(2)
    private val rowEnter = FloatArray(6)
    private var enterAnim = 0f
    private var exitAnim = 0f
    private var exitTarget: GuiScreen? = null
    private var openMs = 0L

    // hitboxes
    private var colX = 0f
    private var colW = 300f
    private var navY = FloatArray(4)
    private var navH = 44f
    private var footY = 0f
    private var footH = 28f
    private var footX1 = 0f
    private var footW1 = 0f
    private var footX2 = 0f
    private var footW2 = 0f
    private var sessY = 0f
    private var sessH = 40f

    override fun initGui() {
        buttonList.clear()
        enterAnim = 0f
        exitAnim = 0f
        exitTarget = null
        openMs = System.currentTimeMillis()
        for (i in rowEnter.indices) rowEnter[i] = 0f
        for (i in navHover.indices) navHover[i] = 0f
        for (i in footHover.indices) footHover[i] = 0f
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)

        ThemedBackground.draw(width, height, mouseX, mouseY)

        enterAnim = lerp(enterAnim, 1f, 0.10f)
        if (exitTarget != null) exitAnim = lerp(exitAnim, 1f, 0.14f)
        val easeIn = easeOutCubic(enterAnim)
        val easeOut = easeInCubic(exitAnim)

        val elapsed = (System.currentTimeMillis() - openMs) / 1000f
        for (i in rowEnter.indices) {
            rowEnter[i] = ThemedUI.stagger(elapsed, i, staggerSec = 0.06f, durationSec = 0.45f) * (1f - easeOut)
        }
        val globalA = (easeIn * (1f - easeOut)).coerceIn(0f, 1f)

        // ── Top bar: brand left only, version inline. No pills. ──────────────
        drawTopBar(sw, easeIn, globalA)

        // ── Center column ───────────────────────────────────────────────────
        colW = min(300f, sw - 28f)
        colX = (sw - colW) / 2f

        val heroH = 92f
        val navGap = 6f
        navH = 44f
        val navBlock = navH * 4 + navGap * 3
        footH = 28f
        sessH = 40f
        val gapHeroNav = 14f
        val gapNavFoot = 8f
        val gapFootSess = 8f
        val totalH = heroH + gapHeroNav + navBlock + gapNavFoot + footH + gapFootSess + sessH
        var cy = (sh - totalH) / 2f + 8f
        cy = cy.coerceAtLeast(30f)

        // ── Hero card: flat, "Crine" + 1.0 badge ────────────────────────────
        val heroA = rowEnter[0]
        if (heroA > 0.02f) {
            val hy = cy + (1f - heroA) * 12f
            drawHero(colX, hy, colW, heroH, accent, heroA)
        }

        // ── Nav rows: numbered flat lines ───────────────────────────────────
        var by = cy + heroH + gapHeroNav
        val labels = arrayOf("Singleplayer", "Multiplayer", "Alt Manager", "Theme Color")
        val hints = arrayOf("Local worlds", "Join a server", "Accounts", "Accent colors")
        val nums = arrayOf("01", "02", "03", "04")
        for (i in labels.indices) {
            navY[i] = by
            drawNavRow(colX, by, colW, navH, nums[i], labels[i], hints[i],
                mouseX, mouseY, i, accent, rowEnter[i + 1])
            by += navH + navGap
        }

        // ── Footer: two flat buttons ────────────────────────────────────────
        footY = by + gapNavFoot - navGap
        val fgap = 8f
        footW1 = (colW - fgap) / 2f
        footW2 = colW - footW1 - fgap
        footX1 = colX
        footX2 = colX + footW1 + fgap
        val footA = rowEnter[5]
        footHover[0] = drawFlatButton(footX1, footY, footW1, footH, "Settings", mouseX, mouseY,
            footHover[0], footA, accent, ButtonKind.NORMAL)
        footHover[1] = drawFlatButton(footX2, footY, footW2, footH, "Quit", mouseX, mouseY,
            footHover[1], footA, accent, ButtonKind.QUIT)

        // ── Session line: flat status row ───────────────────────────────────
        sessY = footY + footH + gapFootSess
        drawSessionLine(colX, sessY, colW, sessH, accent, footA)

        // ── Bottom corners ──────────────────────────────────────────────────
        Fonts.SFBold30.drawString("Crine 1.0", 14f,
            sh - 14f - Fonts.SFBold30.FONT_HEIGHT,
            Color(255, 255, 255, (50 * globalA).toInt()).rgb, false)
        val creator = "by ${Crine.CLIENT_CREATOR}"
        Fonts.SFBold30.drawString(creator, sw - 14f - Fonts.SFBold30.getStringWidth(creator),
            sh - 14f - Fonts.SFBold30.FONT_HEIGHT,
            Color(255, 255, 255, (50 * globalA).toInt()).rgb, false)

        if (exitTarget != null && exitAnim > 0.97f) {
            val target = exitTarget
            exitTarget = null
            mc.displayGuiScreen(target)
        }
    }

    // ── Top bar — flat, no pills ────────────────────────────────────────────

    private fun drawTopBar(sw: Float, easeIn: Float, globalA: Float) {
        val slide = (1f - easeIn) * -18f
        val brandFont = Fonts.Nunito40
        val bx = 14f + slide
        val by = 12f
        brandFont.drawStringWithShadow("Crine", bx, by, Color(242, 243, 250, (255 * globalA).toInt()).rgb)
        val ver = "v${Crine.CLIENT_VERSION}"
        Fonts.SFBold30.drawString(ver, bx + brandFont.getStringWidth("Crine") + 6f,
            by + brandFont.FONT_HEIGHT - Fonts.SFBold30.FONT_HEIGHT - 1f,
            Color(150, 155, 172, (200 * globalA).toInt()).rgb, false)
        // right side: flat text only
        val right = "MC 1.8.9"
        Fonts.SFBold30.drawString(right, sw - 14f - Fonts.SFBold30.getStringWidth(right) + (1f - easeIn) * 18f,
            by + 3f, Color(120, 125, 140, (160 * globalA).toInt()).rgb, false)
        // thin baseline under top bar
        RoundedUtil.drawRound(14f, by + brandFont.FONT_HEIGHT + 6f, 34f, 2f, 1f,
            Color(ClientTheme.getColor(0).red, ClientTheme.getColor(0).green,
                ClientTheme.getColor(0).blue, (180 * globalA).toInt()))
    }

    // ── Hero ────────────────────────────────────────────────────────────────

    private fun drawHero(x: Float, y: Float, w: Float, h: Float, accent: Color, a: Float) {
        val r = 8f
        RoundedUtil.drawRound(x, y, w, h, r, Color(12, 15, 21, (242 * a).toInt()))
        RoundedUtil.drawRoundOutline(x, y, w, h, r, 1f, Color(0, 0, 0, 0),
            Color(32, 38, 50, (255 * a).toInt()))
        // small accent tick top
        RoundedUtil.drawRound(x + 12f, y + 8f, 30f, 2f, 1f, withAlpha(accent, (210 * a).toInt()))

        val heroFont = Fonts.Nunito60
        val name = "Crine"
        val nameW = heroFont.getStringWidth(name)
        val hx = x + (w - nameW) / 2f
        val hyText = y + 14f
        heroFont.drawStringWithShadow(name, hx, hyText, Color(245, 246, 252, (255 * a).toInt()).rgb)
        // accent dot after name
        RoundedUtil.drawRound(hx + nameW + 5f, hyText + heroFont.FONT_HEIGHT - 7f, 5f, 5f, 1f,
            withAlpha(accent, (235 * a).toInt()))

        // version badge — flat outline box
        val ver = "v${Crine.CLIENT_VERSION}"
        val pillW = Fonts.SFBold30.getStringWidth(ver) + 18f
        val pillH = Fonts.SFBold30.FONT_HEIGHT + 7f
        val pillX = x + (w - pillW) / 2f
        val pillY = hyText + heroFont.FONT_HEIGHT + 6f
        RoundedUtil.drawRound(pillX, pillY, pillW, pillH, 4f, Color(10, 13, 19, (230 * a).toInt()))
        RoundedUtil.drawRoundOutline(pillX, pillY, pillW, pillH, 4f, 1f, Color(0, 0, 0, 0),
            Color(44, 50, 66, (255 * a).toInt()))
        Fonts.SFBold30.drawString(ver, pillX + pillW / 2f - Fonts.SFBold30.getStringWidth(ver) / 2f,
            pillY + pillH / 2f - Fonts.SFBold30.FONT_HEIGHT / 2f,
            Color(200, 205, 220, (235 * a).toInt()).rgb, false)

        val sub = "precision  ·  control  ·  flow"
        val subW = Fonts.SFBold30.getStringWidth(sub)
        Fonts.SFBold30.drawString(sub, x + (w - subW) / 2f, pillY + pillH + 7f,
            Color(130, 135, 152, (190 * a).toInt()).rgb, false)
    }

    // ── Nav row — flat numbered line ────────────────────────────────────────

    private fun drawNavRow(x: Float, y: Float, w: Float, h: Float,
                           num: String, title: String, hint: String,
                           mouseX: Int, mouseY: Int, idx: Int,
                           accent: Color, appear: Float) {
        if (appear < 0.02f) return
        val dy = y + (1f - easeOutCubic(appear)) * 10f
        val over = mouseWithinBounds(mouseX, mouseY, x, dy, x + w, dy + h) && exitTarget == null
        navHover[idx] = lerp(navHover[idx], if (over) 1f else 0f, 0.25f)
        val hov = navHover[idx]
        val r = 6f

        val baseFill = Color(13, 16, 23, (235 * appear).toInt())
        val hoverFill = Color(17, 21, 30, (235 * appear).toInt())
        RoundedUtil.drawRound(x, dy, w, h, r,
            blend(baseFill, hoverFill, hov))
        RoundedUtil.drawRoundOutline(x, dy, w, h, r, 1f, Color(0, 0, 0, 0),
            blend(Color(30, 35, 47, (255 * appear).toInt()),
                withAlpha(accent, (220 * appear).toInt()), hov * 0.9f))

        // left tick
        if (hov > 0.04f) {
            RoundedUtil.drawRound(x + 5f, dy + 8f, 2f, h - 16f, 1f,
                withAlpha(accent, (230 * hov * appear).toInt()))
        } else {
            RoundedUtil.drawRound(x + 5f, dy + 8f, 2f, h - 16f, 1f,
                Color(30, 35, 47, (255 * appear).toInt()))
        }

        // index number
        val numF = Fonts.SFBold30
        numF.drawString(num, x + 14f, dy + h / 2f - numF.FONT_HEIGHT / 2f,
            Color(110, 115, 132, ((150 + hov * 60) * appear).toInt()).rgb, false)

        // text
        val tx = x + 44f + hov * 2f
        val titleF = Fonts.SFBold35
        val hintF = Fonts.SFBold24
        val blockH = titleF.FONT_HEIGHT + 2f + hintF.FONT_HEIGHT
        val top = dy + (h - blockH) / 2f
        titleF.drawStringWithShadow(title, tx, top,
            blend(Color(175, 180, 196, (235 * appear).toInt()),
                Color(245, 246, 250, (255 * appear).toInt()), hov).rgb)
        hintF.drawString(hint, tx, top + titleF.FONT_HEIGHT + 2f,
            Color(120, 125, 142, ((150 + hov * 50) * appear).toInt()).rgb, false)

        // arrow — flat chevron
        val arrow = ">"
        val aw = Fonts.SFBold35.getStringWidth(arrow)
        Fonts.SFBold35.drawString(arrow, x + w - 14f - aw + hov * 2f,
            dy + h / 2f - Fonts.SFBold35.FONT_HEIGHT / 2f,
            Color(accent.red, accent.green, accent.blue, ((110 + hov * 130) * appear).toInt()).rgb, false)
    }

    private enum class ButtonKind { NORMAL, QUIT }

    private fun drawFlatButton(x: Float, y: Float, w: Float, h: Float, label: String,
                               mouseX: Int, mouseY: Int, hovIn: Float, appear: Float,
                               accent: Color, kind: ButtonKind): Float {
        if (appear < 0.02f) return hovIn
        val dy = y + (1f - easeOutCubic(appear)) * 8f
        val over = mouseWithinBounds(mouseX, mouseY, x, dy, x + w, dy + h) && exitTarget == null
        val hov = lerp(hovIn, if (over) 1f else 0f, 0.25f)
        val r = 6f
        val fill = blend(Color(13, 16, 23, (230 * appear).toInt()),
            Color(19, 23, 32, (230 * appear).toInt()), hov)
        RoundedUtil.drawRound(x, dy, w, h, r, fill)
        val border = if (kind == ButtonKind.QUIT)
            blend(Color(30, 35, 47, (255 * appear).toInt()),
                Color(200, 85, 95, (230 * appear).toInt()), hov)
        else
            blend(Color(30, 35, 47, (255 * appear).toInt()),
                withAlpha(accent, (220 * appear).toInt()), hov)
        RoundedUtil.drawRoundOutline(x, dy, w, h, r, 1f, Color(0, 0, 0, 0), border)
        val f = Fonts.SFBold30
        val tCol = if (kind == ButtonKind.QUIT)
            blend(Color(165, 170, 185, (230 * appear).toInt()),
                Color(255, 140, 145, (255 * appear).toInt()), hov)
        else
            blend(Color(170, 175, 192, (230 * appear).toInt()),
                Color(245, 246, 250, (255 * appear).toInt()), hov)
        f.drawStringWithShadow(label, x + w / 2f - f.getStringWidth(label) / 2f,
            dy + h / 2f - f.FONT_HEIGHT / 2f, tCol.rgb)
        return hov
    }

    private fun drawSessionLine(x: Float, y: Float, w: Float, h: Float,
                                accent: Color, appear: Float) {
        if (appear < 0.02f) return
        val dy = y + (1f - easeOutCubic(appear)) * 8f
        val r = 6f
        RoundedUtil.drawRound(x, dy, w, h, r, Color(11, 14, 20, (230 * appear).toInt()))
        RoundedUtil.drawRoundOutline(x, dy, w, h, r, 1f, Color(0, 0, 0, 0),
            Color(30, 35, 47, (255 * appear).toInt()))

        val head = 22f
        val hx = x + 9f
        val hy = dy + (h - head) / 2f
        // flat ring around head — no glow
        RoundedUtil.drawRoundOutline(hx - 1f, hy - 1f, head + 2f, head + 2f, 6f, 1f,
            Color(0, 0, 0, 0), withAlpha(accent, (160 * appear).toInt()))
        drawRoundHead(hx, hy, head, 5f)

        val name = try { mc.session.username } catch (_: Throwable) { "Player" }
        Fonts.SFBold30.drawStringWithShadow(name, hx + head + 9f, dy + 7f,
            Color(228, 229, 238, (225 * appear).toInt()).rgb)
        val prem = try { mc.session.token.length >= 32 } catch (_: Throwable) { false }
        val status = if (prem) "Premium" else "Offline"
        val dotCol = if (prem) withAlpha(accent, (230 * appear).toInt())
        else Color(200, 90, 95, (230 * appear).toInt())
        // status dot + text
        val statusY = dy + 7f + Fonts.SFBold30.FONT_HEIGHT + 2f
        RoundedUtil.drawRound(hx + head + 9f, statusY + 1f, 4f, 4f, 1f, dotCol)
        Fonts.SFBold24.drawString(status, hx + head + 17f, statusY,
            Color(140, 145, 162, (210 * appear).toInt()).rgb, false)
        val mcVer = "MC 1.8.9"
        Fonts.SFBold24.drawString(mcVer, x + w - 9f - Fonts.SFBold24.getStringWidth(mcVer),
            dy + h / 2f - Fonts.SFBold24.FONT_HEIGHT / 2f,
            Color(110, 115, 132, (170 * appear).toInt()).rgb, false)
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
            if (mouseWithinBounds(mouseX, mouseY, colX, navY[i], colX + colW, navY[i] + navH)) {
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

    /** Stencil-clipped round head. */
    private fun drawRoundHead(x: Float, y: Float, size: Float, radius: Float) {
        val skin = resolveSessionSkin()
        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x, y, x + size, y + size, radius)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        GlStateManager.color(1f, 1f, 1f, 1f)
        RenderUtils.drawHead(skin, x.toInt(), y.toInt(), size.toInt(), size.toInt(), Color.WHITE.rgb)
        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()
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

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOutCubic(x: Float): Float { val t = x.coerceIn(0f, 1f); return 1f - (1f - t) * (1f - t) * (1f - t) }
    private fun easeInCubic(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * t }

    private fun blend(a: Color, b: Color, t: Float) = ThemedUI.blend(a, b, t)
    private fun withAlpha(c: Color, a: Int) = Color(c.red, c.green, c.blue, a.coerceIn(0, 255))
}
