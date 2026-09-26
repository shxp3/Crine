 
package net.shxp3.crine.features.command.commands

import net.shxp3.crine.Crine
import net.shxp3.crine.features.command.Command
import net.shxp3.crine.features.module.ModuleInfo
import net.shxp3.crine.utils.ClientUtils

class HideCommand : Command("hide", emptyArray()) {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when {
                args[1].equals("list", true) -> {
                    alert("§c§lHidden")
                    Crine.moduleManager.modules.filter { !it.array }.forEach {
                        ClientUtils.displayChatMessage("§6> §c${it.name}")
                    }
                    return
                }

                args[1].equals("clear", true) -> {
                    for (module in Crine.moduleManager.modules)
                        module.array = true

                    alert("Cleared hidden modules.")
                    return
                }

                args[1].equals("reset", true) -> {
                    for (module in Crine.moduleManager.modules)
                        module.array = module::class.java.getAnnotation(ModuleInfo::class.java).array

                    alert("Reset hidden modules.")
                    return
                }

                else -> {
                    // Get module by name
                    val module = Crine.moduleManager.getModule(args[1])

                    if (module == null) {
                        alert("Module §a§l${args[1]}§3 not found.")
                        return
                    }

                    // Find key by name and change
                    module.array = !module.array

                    // Response to user
                    alert("Module §a§l${module.name}§3 is now §a§l${if (module.array) "visible" else "invisible"}§3 on the array list.")
                    playEdit()
                    return
                }
            }
        }

        chatSyntax("hide <module/list/clear/reset>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> Crine.moduleManager.modules
                    .map { it.name }
                    .filter { it.startsWith(moduleName, true) }
                    .toList()
            else -> emptyList()
        }
    }
}