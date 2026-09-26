package net.shxp3.crine.features.module.modules.client.impl

import net.shxp3.crine.event.*
import net.shxp3.crine.features.module.modules.client.Interface
import net.shxp3.crine.features.module.modules.client.TargetHUD
import net.shxp3.crine.ui.client.gui.colortheme.ClientTheme
import net.shxp3.crine.ui.font.Fonts.*
import net.shxp3.crine.utils.*
import net.shxp3.crine.utils.animation.Animation
import net.shxp3.crine.utils.animation.Easing
import net.shxp3.crine.utils.extensions.skin
import net.shxp3.crine.utils.render.BlurUtils
import net.shxp3.crine.utils.render.RenderUtils
import net.shxp3.crine.utils.render.Stencil
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import java.awt.Color

object DynamicIsland : MinecraftInstance() {

    var disabler: Float? = null
    var mainTarget: EntityLivingBase? = null

    private const val WHITE = -1
    private val BAR_BG     = Color(50, 50, 50, 255).rgb
    private val TH_BAR_BG  = Color(0, 0, 0, 200).rgb
    private val TH_BORDER  = Color.WHITE.rgb

    private var posX  = 0F
    private var posY  = 0F
    private var posX2 = 0F
    private var posY2 = 0F
    private var alpha = 0

    private val animPosX  = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animPosY  = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animPosX2 = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animPosY2 = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animAlpha = Animation(Easing.EASE_IN_OUT_BACK, 500)

    fun draw(event: Render2DEvent) {
        animPosX.run(posX.toDouble())
        animPosY.run(posY.toDouble())
        animPosX2.run(posX2.toDouble())
        animPosY2.run(posY2.toDouble())
        animAlpha.run(alpha.toDouble())

        val x1 = animPosX.value.toFloat()
        val y1 = animPosY.value.toFloat()
        val x2 = animPosX2.value.toFloat()
        val y2 = animPosY2.value.toFloat()
        val aInt = animAlpha.value.toInt()
        GL11.glPushMatrix()
        if (Interface.isBlurActive()) {
            BlurUtils.blurAreaRounded(x1 + 1F, y1 + 1F, x2 - 1F, y2 - 1F, 8F, 10F)
        }
        if (Interface.isBloomActive()) RenderUtils.drawBloomRoundedRect(
            x1, y1, x2, y2, 6F, 1F,
            Color(10, 10, 10, aInt),
            RenderUtils.ShaderBloom.BLOOMONLY
        )
        else RenderUtils.drawRoundedRect(x1, y1, x2, y2, 6F, Color(10, 10, 10, aInt).rgb)

        GlStateManager.resetColor()
        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x1, y1, x2, y2, 0F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)

        val player    = mc.thePlayer
        val sr        = event.scaledResolution
        val halfWidth = sr.scaledWidth / 2F

        mainTarget = when {
            mc.currentScreen is GuiChat -> player
            Interface.attackTarget != null -> Interface.attackTarget
            else -> null
        }
        val target = mainTarget

        when {
            !TargetHUD.state && target != null && Interface.targetHudDN.get() -> {
                val name = target.name
                drawTH(0F, SFBold35.getStringWidth(name).toFloat(), target, name, sr)
            }
            else -> {
                val text  = "Crine §f| ${player.name} | ${ServerUtils.getRemoteIp()} | ${Minecraft.getDebugFPS()}fps"
                val textW = SFBold35.getStringWidth(text)
                set(halfWidth - textW / 2F - 9F, 11F,
                    halfWidth + textW / 2F + 9F,
                    14F + SFBold35.height + 12F)
                SFBold35.drawStringWithShadow(text, x1 + 10F, y1 + 9F, ClientTheme.getColorWithAlpha(0, 255).rgb)
            }
        }

        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()
        GL11.glPopMatrix()
    }

    private fun set(startX: Float, startY: Float, endX: Float, endY: Float, alpha: Int = 180) {
        posX  = startX
        posY  = startY
        posX2 = endX
        posY2 = endY
        this.alpha = alpha
    }

    private fun drawTH(posY: Float, stringWidth: Float, target: EntityLivingBase, string: String, sr: ScaledResolution) {
        val halfWidth  = sr.scaledWidth  / 2F
        val halfHeight = sr.scaledHeight / 2F
        set(halfWidth + 60F,
            halfHeight - 18F - (posY / 2),
            halfWidth + 60F + 60F + stringWidth + 5F,
            halfHeight + 18F + (posY / 2),
            100)

        val x1 = animPosX.value.toFloat()
        val y1 = animPosY.value.toFloat()
        val x2 = animPosX2.value.toFloat()

        SFBold35.drawCenteredString(
            string,
            x1 + ((x2 - x1) / 2F) + 15F,
            y1 + 7F + posY,
            WHITE
        )

        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x1 + 6F, y1 + 5F + posY, x1 + 32F, y1 + 31F + posY, 6F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        RenderUtils.drawHead(target.skin, x1.toInt() + 6, y1.toInt() + 5 + posY.toInt(), 26, 26, Color.WHITE.rgb)
        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()

        RenderUtils.drawRoundedRect(
            x1 + 40F, y1 + 24.5F + posY,
            ((x2 - 5F) - (x1 + 40F)) - 4F, 4.5F,
            2F, TH_BAR_BG, 1F, TH_BORDER
        )
        RenderUtils.drawGradientRoundedRect(
            x1 + 40F, y1 + 24.5F + posY,
            x1 + 30F + ((x2 - 5F) - (x1 + 30F)) * (target.health / target.maxHealth) - 4F,
            y1 + 29F + posY,
            2, ClientTheme.getColor().rgb, ClientTheme.getColor(90).rgb
        )
    }
}
