package net.shxp3.crine.utils

import net.shxp3.crine.Crine
import net.shxp3.crine.utils.render.ColorUtils.stripColor
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.scoreboard.ScorePlayerTeam

object EntityUtils : MinecraftInstance() {
    fun isSelected(entity: Entity, canAttackCheck: Boolean): Boolean {
        if (entity is EntityLivingBase && entity.isEntityAlive && entity !== mc.thePlayer) {
            if (!entity.isInvisible()) {
                if (entity is EntityPlayer) {
                    if (canAttackCheck) {
                        if (entity.isSpectator) return false
                        if (entity.isPlayerSleeping) return false
                        if (isFriend(entity)) return false
                    }
                    return true
                }
                return false
            }
        }
        return false
    }

    fun canRayCast(entity: Entity): Boolean {
        return entity is EntityPlayer && !isFriend(entity)
    }

    fun isFriend(entity: Entity): Boolean {
        return entity is EntityPlayer && entity.getName() != null && Crine.fileManager.friendsConfig.isFriend(stripColor(entity.getName()))
    }

    fun getName(networkPlayerInfoIn: NetworkPlayerInfo): String {
        return if (networkPlayerInfoIn.displayName != null) networkPlayerInfoIn.displayName.formattedText else ScorePlayerTeam.formatPlayerName(
            networkPlayerInfoIn.playerTeam,
            networkPlayerInfoIn.gameProfile.name
        )
    }
}
