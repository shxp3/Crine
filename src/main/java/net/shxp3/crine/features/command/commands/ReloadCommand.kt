package net.shxp3.crine.features.command.commands

import net.shxp3.crine.features.command.Command
import net.shxp3.crine.utils.ClientUtils

class ReloadCommand : Command("reload", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        alert("Reloading...")
        ClientUtils.reloadClient()
    }
}
