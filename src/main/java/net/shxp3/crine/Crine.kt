package net.shxp3.crine

import net.shxp3.crine.discordrpc.CrineRPC
import net.shxp3.crine.utils.MouseKeybindHandler
import net.shxp3.crine.web.LocalWebServer
import net.minecraftforge.common.MinecraftForge
import net.shxp3.crine.event.ClientShutdownEvent
import net.shxp3.crine.event.EventManager
import net.shxp3.crine.features.command.CommandManager
import net.shxp3.crine.features.macro.MacroManager
import net.shxp3.crine.features.module.ModuleManager
import net.shxp3.crine.features.module.modules.client.Interface
import net.shxp3.crine.features.special.ClientSpoof
import net.shxp3.crine.features.special.CombatManager
import net.shxp3.crine.features.special.NotificationManager
import net.shxp3.crine.file.FileManager
import net.shxp3.crine.file.config.ConfigManager
import net.shxp3.crine.script.ScriptManager
import net.shxp3.crine.ui.client.gui.ClickGUIModule
import net.shxp3.crine.ui.client.gui.GuiWelcome
import net.shxp3.crine.ui.client.keybind.KeyBindManager
import net.shxp3.crine.ui.font.Fonts
import net.shxp3.crine.utils.*
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.client.ClientCommandHandler
import org.lwjgl.opengl.Display
import kotlin.concurrent.thread

object Crine {
    // Client information
    const val CLIENT_NAME = "Crine"
    const val CLIENT_CLOUD = "https://crine.github.io/cloud"
    private var CLIENT_STATUS = false
    const val COLORED_NAME = "§CC§Frine"
    const val CLIENT_CREATOR = "Shape"
    /** Display + update-check version — keep in sync with crine.github.io/version.json */
    const val CLIENT_VERSION = "1.0"
    var destruced = false

    const val CLIENT_LOADING = "Initialzing Minecraft"

    @JvmField
    val CLIENT_TITLE = "$CLIENT_NAME $CLIENT_VERSION" + if (CLIENT_STATUS) " (Beta)" else ""

    var isStarting = true
    var isLoadingConfig = true


    // Managers
    lateinit var moduleManager: ModuleManager

    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    lateinit var fileManager: FileManager
    lateinit var scriptManager: ScriptManager
    lateinit var combatManager: CombatManager
    lateinit var macroManager: MacroManager
    lateinit var configManager: ConfigManager
    lateinit var notification: NotificationManager

    // Some UI things
    lateinit var mainMenu: GuiScreen
    lateinit var keyBindManager: KeyBindManager
    lateinit var clientRPC: CrineRPC

    /**
     * Execute if client will be started
     */
    fun initClient() {
        ClientUtils.logInfo("Loading $CLIENT_NAME $CLIENT_VERSION, by $CLIENT_CREATOR")
        ClientUtils.logInfo("Initialzing...")
        Display.setTitle("Initialzing Crine...")
        val startTime = System.currentTimeMillis()
        // Create file manager
        fileManager = FileManager()
        configManager = ConfigManager()
        // Create event manager
        eventManager = EventManager()
        // Create event manager
        Display.setTitle("Loading event")
        eventManager = EventManager()
        MinecraftForge.EVENT_BUS.register(MouseKeybindHandler)
        // Forge-side handler that hides vanilla hearts/armor/food when
        // Interface.Stat-Bars is on (see HotbarStatBarsHandler kdoc — Mixin
        // inject didn't reliably suppress them in this build).
        MinecraftForge.EVENT_BUS.register(net.shxp3.crine.features.module.modules.client.impl.HotbarStatBarsHandler)
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(ClientSpoof())
        eventManager.registerListener(InventoryUtils)
        eventManager.registerListener(SessionUtils())
        eventManager.registerListener(StatisticsUtils())
        eventManager.registerListener(SlotUtils)
        eventManager.registerListener(PacketUtils)
        // Create command manager
        commandManager = CommandManager()
        // Create Notification Manager
        notification = NotificationManager()
        clientRPC = CrineRPC
        // Load client fonts
        Display.setTitle("Load Fonts")
        Fonts.loadFonts()
        macroManager = MacroManager()
        eventManager.registerListener(macroManager)
        // Setup module manager and register modules
        Display.setTitle("Load Module")
        moduleManager = ModuleManager()
        moduleManager.registerModules()

        try {
            // ScriptManager, Remapper will be lazy loaded when scripts are enabled
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.logError("Failed to load scripts.", throwable)
        }

        Display.setTitle("Load config")
        fileManager.loadConfigs(
            fileManager.accountsConfig,
            fileManager.friendsConfig,
            fileManager.specialConfig,
            fileManager.themeConfig,
            fileManager.clientSettingsConfig,
            fileManager.clienthudConfig,
            fileManager.subscriptsConfig
        )
        // Register commands
        commandManager.registerCommands()

        // KeyBindManager
        keyBindManager = KeyBindManager()

        combatManager = CombatManager()
        eventManager.registerListener(combatManager)

        mainMenu = GuiWelcome()
        moduleManager.registerModule(ClickGUIModule())
        // Load configs
        configManager.loadLegacySupport()
        configManager.loadConfigSet()
        // Set is starting status
        isStarting = false
        isLoadingConfig = false
        Interface.state = true


        fileManager.loadConfigs()


        ClientUtils.logInfo("Loading Script Subscripts...")
        Display.setTitle("Loading Script")
        try {

            // ScriptManager
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.logError("Failed to load scripts.", throwable)
        }
        Display.setTitle(CLIENT_TITLE)
        LocalWebServer.start()
        ClientUpdater.checkAsync()
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION started!")
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION loaded in ${(System.currentTimeMillis() - startTime)}ms!")
    }


    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        if (!isStarting && !isLoadingConfig) {
            ClientUtils.logInfo("Shutting down $CLIENT_NAME $CLIENT_VERSION!")

            // Call client shutdown
            eventManager.callEvent(ClientShutdownEvent())

            // Save all available configs
            LocalWebServer.stop()
            configManager.save(true, true)
            fileManager.saveAllConfigs()
        }
    }
}
