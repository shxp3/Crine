 
package net.shxp3.crine.features.command.commands

import net.shxp3.crine.Crine
import net.shxp3.crine.features.command.Command

class FakeNameCommand : Command("SetFakeName", emptyArray()){
    override fun execute(args: Array<String>) {
        if(args.size > 2) {
            val module = Crine.moduleManager.getModule(args[1]) ?: return
            module.name = args[2]
        } else
            chatSyntax("SetFakeName <Module> <Name>")
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