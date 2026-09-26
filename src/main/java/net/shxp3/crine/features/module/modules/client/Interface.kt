package net.shxp3.crine.features.module.modules.client

import net.shxp3.crine.Crine
import net.shxp3.crine.event.*
import net.shxp3.crine.features.module.Module
import net.shxp3.crine.features.module.ModuleCategory
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.features.module.modules.client.impl.DynamicIsland
import net.shxp3.crine.features.module.modules.client.impl.HotbarStatBars
import net.shxp3.crine.features.module.modules.client.impl.Notification
import net.shxp3.crine.injection.access.StaticStorage
import net.shxp3.crine.features.value.BoolValue
import net.shxp3.crine.features.value.ListValue
import net.shxp3.crine.features.value.OptionValue
import net.shxp3.crine.utils.EntityUtils
import net.shxp3.crine.utils.SlotUtils
import net.shxp3.crine.utils.animation.Animation
import net.shxp3.crine.utils.animation.Easing
import net.shxp3.crine.utils.timer.TimerMS
import net.minecraft.client.gui.GuiChat
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "Interface", category = ModuleCategory.CLIENT, array = false, defaultOn = true, loadConfig = false)
object Interface : Module() {
    val dynamicIsland = BoolValue("Dynamic-Island", true)
    val targetHudDN = BoolValue("TargetHUD-Dynamic-Island", false)
    val notification = BoolValue("Notification", true)
    val inventoryAnimation = BoolValue("Inventory-Animation", false)
    /**
     * Hotbar rendering style.
     *  - Vanilla : the original 1.8 widget texture (untouched).
     *  - Rounded : single rounded chip behind all 9 slots, animated
     *              selection highlight slides between slots.
     *  - Modern  : 9 separated rounded cards with gaps, selected card
     *              gets ClientTheme accent glow + bottom indicator strip
     *              + slight item scale-up. Minimalist Lunar/Badlion vibe.
     */
    val hotbarStyle = ListValue("Hotbar-Style", arrayOf("Vanilla", "Rounded", "Modern"), "Modern")
    val statBars = BoolValue("Stat-Bars", true)
    val statBarsText = BoolValue("Stat-Bars-Text", true).displayable { statBars.get() }
    val statBarsBloom = BoolValue("Stat-Bars-Bloom", true).displayable { statBars.get() }
    private val buttonOption = OptionValue("Button", false)
    val buttonRounded = BoolValue("Rounded-Button", false).displayable { buttonOption.get() }
    val buttonFont = BoolValue("Font-Button", false).displayable { buttonOption.get() }
    private val shaderValue = OptionValue("Shader", true)
    val bloomValue = BoolValue("Bloom", true).displayable { shaderValue.get() }
    val blurValue = BoolValue("Blur", true).displayable { shaderValue.get() }
    /**
     * Reduces non-essential HUD visual effects (blur/bloom) for higher FPS.
     * Default off — preserves current appearance. Does not affect gameplay modules.
     */
    val performanceMode = BoolValue("Performance-Mode", false)

    @JvmStatic
    fun isBloomActive(): Boolean = bloomValue.get() && !performanceMode.get() && !FPSBoost.isLowEnd()

    @JvmStatic
    fun isBlurActive(): Boolean = blurValue.get() && !performanceMode.get() && !FPSBoost.isLowEnd()

    val animationSlot = Animation(Easing.EASE_OUT_CIRC, 300)
    var attackTarget: EntityLivingBase? = null
    private val targetTimer = TimerMS()
    @EventTarget
    fun onTick(event: TickEvent) {
        mc.guiAchievement.clearAchievements()
        if (Keyboard.isKeyDown(Keyboard.KEY_PERIOD) && mc.currentScreen == null) {
            mc.displayGuiScreen(GuiChat("."))
        }
    }
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (attackTarget != event.targetEntity) {
            if (EntityUtils.isSelected(event.targetEntity, true)) {
                attackTarget = event.targetEntity as EntityLivingBase
            }
        }
        targetTimer.reset()
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        animationSlot.run(SlotUtils.getSlot() * 20.0)
        if (targetTimer.hasTimePassed(500)) {
            attackTarget = null
        }
        if (dynamicIsland.get()) {
            DynamicIsland.draw(event)
        }
        if (notification.get()) {
            Notification.draw()
        }
        if (statBars.get()) {
            HotbarStatBars.draw(StaticStorage.scaledResolution)
        }
    }
}
