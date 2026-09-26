
package net.shxp3.crine.utils.login

import me.liuli.elixir.account.CrackedAccount
import net.shxp3.crine.Crine
import net.shxp3.crine.event.SessionEvent
import net.shxp3.crine.ui.client.altmanager.GuiAltManager
import net.shxp3.crine.utils.MinecraftInstance
import net.shxp3.crine.utils.misc.RandomUtils
import net.minecraft.util.Session

object LoginUtils : MinecraftInstance() {
    fun loginCracked(username: String) {
        mc.session = CrackedAccount().also { it.name = username }.session.let { Session(it.username, it.uuid, it.token, it.type) }
        Crine.eventManager.callEvent(SessionEvent())
    }

    fun randomCracked() {
        loginCracked(RandomUtils.randomUsername())
    }
}