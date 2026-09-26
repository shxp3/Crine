package net.shxp3.crine.features.macro

import net.shxp3.crine.event.EventTarget
import net.shxp3.crine.event.KeyEvent
import net.shxp3.crine.event.Listenable
import net.shxp3.crine.utils.MinecraftInstance

class MacroManager : Listenable, MinecraftInstance() {
    val macros = ArrayList<Macro>()

    @EventTarget
    fun onKey(event: KeyEvent) {
        macros.filter { it.key == event.key }.forEach { it.exec() }
    }

    override fun handleEvents() = true
}