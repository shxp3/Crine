package net.shxp3.crine.utils

import com.google.gson.JsonObject
import net.shxp3.crine.Crine
import net.shxp3.crine.features.command.CommandManager
import net.shxp3.crine.ui.font.Fonts
import net.minecraft.client.Minecraft
import net.minecraft.util.IChatComponent
import org.apache.logging.log4j.LogManager

object
ClientUtils : MinecraftInstance() {
    @JvmStatic
    val logger = LogManager.getLogger("Crine")


    fun logInfo(msg: String) {
        logger.info(msg)
    }

    fun logWarn(msg: String) {
        logger.warn(msg)
    }

    fun logError(msg: String) {
        logger.error(msg)
    }

    fun logError(msg: String, t: Throwable) {
        logger.error(msg, t)
    }

    fun logDebug(msg: String) {
        logger.debug(msg)
    }

    fun displayAlert(message: String) {
        displayChatMessage("[" + Crine.COLORED_NAME + "] " + message)
    }

    fun displayChatMessage(message: String) {
        if (mc.thePlayer == null) {
            logger.info("(MCChat) $message")
            return
        }
        val jsonObject = JsonObject()
        jsonObject.addProperty("text", message)
        mc.thePlayer.addChatMessage(IChatComponent.Serializer.jsonToComponent(jsonObject.toString()))
    }

    fun reloadClient() {
        Crine.commandManager = CommandManager()
        Crine.commandManager.registerCommands()
        Crine.isStarting = true
        Crine.isLoadingConfig = true
        Crine.scriptManager.disableScripts()
        Crine.scriptManager.unloadScripts()
        for (module in Crine.moduleManager.modules)
            Crine.moduleManager.generateCommand(module)
        Crine.scriptManager.loadScripts()
        Crine.scriptManager.enableScripts()
        Fonts.loadFonts()
        Crine.configManager.load(Crine.configManager.nowConfig, false)
        Crine.fileManager.loadConfig(Crine.fileManager.accountsConfig)
        Crine.fileManager.loadConfig(Crine.fileManager.friendsConfig)
        Crine.fileManager.loadConfig(Crine.fileManager.themeConfig)
        Crine.fileManager.loadConfig(Crine.fileManager.clienthudConfig)
        Crine.isStarting = false
        Crine.isLoadingConfig = false
        System.gc()
    }

    /**
     * Minecraft instance
     */
    val mc = Minecraft.getMinecraft()!!

    enum class EnumOSType(val friendlyName: String) {
        WINDOWS("win"), LINUX("linux"), MACOS("mac"), UNKNOWN("unk");
    }
}
