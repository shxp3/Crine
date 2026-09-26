package net.shxp3.crine.features.module.modules.client.impl;

import net.shxp3.crine.Crine;
import net.shxp3.crine.features.special.NotificationUtil;
import net.shxp3.crine.features.special.TYPE;
import net.shxp3.crine.ui.client.gui.nova.NovaTheme;
import net.shxp3.crine.ui.font.Fonts;
import net.shxp3.crine.utils.MinecraftInstance;
import net.shxp3.crine.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import java.awt.Color;
import java.util.HashMap;

/**
 * Meridian-style toasts: sharp-edged bottom-right stack, hairline border,
 * square status marker, uppercase micro title, bottom expiry rule.
 * No bloom, no icons.
 */
object Notification : MinecraftInstance() {
    private val fadeStates = HashMap<NotificationUtil, Float>();
    private val offsetXStates = HashMap<NotificationUtil, Float>();
    private val offsetYStates = HashMap<NotificationUtil, Float>();

    private fun statusColor(type: TYPE): Color {
        return when (type) {
            TYPE.SUCCESS -> NovaTheme.OK
            TYPE.ERROR -> NovaTheme.ERR
            TYPE.WARNING -> NovaTheme.WARN
            else -> try {
                NovaTheme.accent()
            } catch (_: Throwable) {
                Color(150, 160, 180)
            }
        }
    }

    fun draw() {
        val width = ScaledResolution(mc).scaledWidth;
        val height = ScaledResolution(mc).scaledHeight;

        if (Crine.notification.list.isNotEmpty()) {
            var accumulatedOffsetY = 0F;

            val iterator = Crine.notification.list.iterator();
            while (iterator.hasNext()) {
                val noti = iterator.next();
                val fadeState = fadeStates.getOrDefault(noti, 0F);
                val offsetX = offsetXStates.getOrDefault(noti, 200F);
                val offsetY = offsetYStates.getOrDefault(noti, 50F);

                val expired = System.currentTimeMillis() > noti.system + noti.timer
                val fadeDelta = (0.0075F * 0.7F * RenderUtils.deltaTime * if (expired) 1F else -1F);
                val newFadeState = (fadeState + fadeDelta).coerceIn(0F, 1F);
                fadeStates[noti] = newFadeState;

                val targetOffsetX = 0F;
                offsetXStates[noti] = offsetX + (targetOffsetX - offsetX) * 0.15F;

                val targetOffsetY = accumulatedOffsetY;
                val newOffsetY = offsetY + (targetOffsetY - offsetY) * 0.15F;
                offsetYStates[noti] = newOffsetY;

                if (newFadeState >= 1F && expired) {
                    iterator.remove();
                    fadeStates.remove(noti);
                    offsetXStates.remove(noti);
                    offsetYStates.remove(noti);
                    continue;
                }

                val alpha = ((1F - newFadeState) * 255).toInt().coerceIn(0, 255)
                val titleF = Fonts.font20SemiBold
                val bodyF = Fonts.font30Bold
                val title = noti.title.uppercase()
                val contentW = bodyF.getStringWidth(noti.content).toFloat()
                val titleW = titleF.getStringWidth(title).toFloat()
                val boxW = maxOf(contentW, titleW) + 30F
                val boxH = 34F
                val slide = offsetXStates.getOrDefault(noti, 0F)
                val x2 = width - 8F + slide
                val x1 = x2 - boxW
                val y2 = height - 6F - newOffsetY
                val y1 = y2 - boxH

                // body
                RenderUtils.drawRect(x1, y1, x2, y2, Color(9, 11, 15, (alpha * 0.92F).toInt()).rgb)
                // hairline border
                val border = Color(255, 255, 255, (alpha * 0.16F).toInt()).rgb
                RenderUtils.drawRect(x1, y1, x2, y1 + 1F, border)
                RenderUtils.drawRect(x1, y2 - 1F, x2, y2, border)
                RenderUtils.drawRect(x1, y1, x1 + 1F, y2, border)
                RenderUtils.drawRect(x2 - 1F, y1, x2, y2, border)

                // square status marker
                val sc = statusColor(noti.type)
                RenderUtils.drawRect(x1 + 8F, y1 + 8F, x1 + 13F, y1 + 13F,
                    Color(sc.red, sc.green, sc.blue, alpha).rgb)

                // title + content
                titleF.drawString(title, x1 + 19F, y1 + 5F,
                    Color(150, 158, 174, alpha).rgb, false)
                bodyF.drawString(noti.content, x1 + 19F, y1 + 5F + titleF.FONT_HEIGHT + 1F,
                    Color(237, 240, 246, alpha).rgb, false)

                // expiry rule
                val total = noti.timer.coerceAtLeast(1).toFloat()
                val remain = (1F - (System.currentTimeMillis() - noti.system) / total).coerceIn(0F, 1F)
                val ruleW = boxW * remain
                if (ruleW > 0.5F) {
                    RenderUtils.drawRect(x1, y2 - 1F, x1 + ruleW, y2,
                        Color(sc.red, sc.green, sc.blue, alpha).rgb)
                }

                accumulatedOffsetY += boxH + 6F;
            }
        }
    }
}
