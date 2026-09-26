package net.shxp3.crine.features.macro

import net.shxp3.crine.Crine

class Macro(val key: Int, val command: String) {
    fun exec() {
        Crine.commandManager.executeCommands(command)
    }
}