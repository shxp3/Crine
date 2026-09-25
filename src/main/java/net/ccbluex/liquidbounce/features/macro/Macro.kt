package net.ccbluex.liquidbounce.features.macro

import net.ccbluex.liquidbounce.Crine

class Macro(val key: Int, val command: String) {
    fun exec() {
        Crine.commandManager.executeCommands(command)
    }
}