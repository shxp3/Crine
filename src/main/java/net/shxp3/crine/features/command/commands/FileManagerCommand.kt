package net.shxp3.crine.features.command.commands

import net.shxp3.crine.Crine
import net.shxp3.crine.features.command.Command
import net.shxp3.crine.utils.ClientUtils

class FileManagerCommand : Command("FileManager", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.isNotEmpty()) {
            when (args[1].lowercase()) {
                "save" -> {
                    Crine.fileManager.saveAllConfigs()
                    alert("Successfully saved config")
                }
                "load" -> {
                    Crine.fileManager.loadConfig(Crine.fileManager.accountsConfig)
                    Crine.fileManager.loadConfig(Crine.fileManager.friendsConfig)
                    Crine.fileManager.loadConfig(Crine.fileManager.themeConfig)
                    Crine.fileManager.loadConfig(Crine.fileManager.clienthudConfig)
                    alert("Successfully loaded config")
                }
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        return listOf("save",
            "load").filter { it.startsWith(args[0], true) }
    }
}