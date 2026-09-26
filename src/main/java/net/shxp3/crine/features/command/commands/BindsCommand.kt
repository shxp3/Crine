
package net.shxp3.crine.features.command.commands

import net.shxp3.crine.Crine
import net.shxp3.crine.features.command.Command
import net.shxp3.crine.utils.ClientUtils
import org.lwjgl.input.Keyboard

class BindsCommand : Command("binds", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            if (args[1].equals("clear", true)) {
                for (module in Crine.moduleManager.modules)
                    module.keyBind = Keyboard.KEY_NONE

                alert("Removed all binds.")
                return
            }
        }

        alert("§c§lBinds")
        Crine.moduleManager.modules.filter { it.keyBind != Keyboard.KEY_NONE }.forEach {
            ClientUtils.displayChatMessage("§6> §c${it.name}: §a§l${net.shxp3.crine.utils.KeybindHelper.getDisplayName(it.keyBind)}")
        }
        chatSyntax("binds clear")
    }
}
