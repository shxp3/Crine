package net.shxp3.crine.file

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.shxp3.crine.Crine
import net.shxp3.crine.features.macro.Macro
import net.shxp3.crine.features.module.EnumAutoDisableType
import net.shxp3.crine.file.configs.*
import net.shxp3.crine.utils.ClientUtils
import net.shxp3.crine.utils.MinecraftInstance
import java.io.*

class FileManager : MinecraftInstance() {
    val dir = File(mc.mcDataDir, "Crine")
    val fontsDir = File(dir, "fonts")
    val configsDir = File(dir, "configs")
    val legacySettingsDir = File(dir, "legacy-settings.json")
    val accountsConfig = AccountsConfig(File(dir, "accounts.json"))
    var friendsConfig = FriendsConfig(File(dir, "friends.json"))
    val subscriptsConfig = ScriptConfig(File(dir, "subscripts.json"))
    val specialConfig = SpecialConfig(File(dir, "special.json"))
    val themeConfig = ThemeConfig(File(dir, "themeColor.json"))
    val clientSettingsConfig = ClientSettingsConfig(File(dir, "clientsettings.json"))
    val clienthudConfig = ClientHUDConfig(File(dir, "clienthud.json"))
    /**
     * Setup everything important
     */
    init {
        setupFolder()
    }

    /**
     * Setup folder
     */
    fun setupFolder() {
        if (!dir.exists()) {
            dir.mkdir()
        }

        if (!fontsDir.exists()) {
            fontsDir.mkdir()
        }

        if (!configsDir.exists()) {
            configsDir.mkdir()
        }

    }

    /**
     * Load a list of configs
     *
     * @param configs list
     */
    fun loadConfigs(vararg configs: FileConfig) {
        if (Crine.destruced) return
        for (fileConfig in configs)
            loadConfig(fileConfig)
    }

    /**
     * Load one config
     *
     * @param config to load
     */
    fun loadConfig(config: FileConfig) {
        if (Crine.destruced) return
        if (!config.hasConfig()) {
            ClientUtils.logInfo("[FileManager] Skipped loading config: " + config.file.name + ".")
            saveConfig(config, true)
            return
        }
        try {
            config.loadConfig(config.loadConfigFile())
            ClientUtils.logInfo("[FileManager] Loaded config: " + config.file.name + ".")
        } catch (t: Throwable) {
            ClientUtils.logError("[FileManager] Failed to load config file: " + config.file.name + ".", t)
        }
    }

    /**
     * Save all configs in file manager
     */
    fun saveAllConfigs() {
        if (Crine.destruced) return
        for (field in javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val obj = field[this]
                if (obj is FileConfig) {
                    saveConfig(obj)
                }
            } catch (e: IllegalAccessException) {
                ClientUtils.logError("[FileManager] Failed to save config file of field " + field.name + ".", e)
            }
        }
    }

    /**
     * Save a list of configs
     *
     * @param configs list
     */
    fun saveConfigs(vararg configs: FileConfig) {
        if (Crine.destruced) return
        for (fileConfig in configs) saveConfig(fileConfig)
    }

    /**
     * Save one config
     *
     * @param config to save
     */
    fun saveConfig(config: FileConfig) {
        if (Crine.destruced) return
        saveConfig(config, true)
    }

    /**
     * Save one config
     *
     * @param config         to save
     * @param ignoreStarting check starting
     */
    private fun saveConfig(config: FileConfig, ignoreStarting: Boolean) {
        if (Crine.destruced) return
        if (!ignoreStarting && Crine.isStarting) return
        try {
            if (!config.hasConfig()) config.createConfig()
            config.saveConfigFile(config.saveConfig())
            ClientUtils.logInfo("[FileManager] Saved config: " + config.file.name + ".")
        } catch (t: Throwable) {
            ClientUtils.logError("[FileManager] Failed to save config file: " + config.file.name + ".", t)
        }
    }

    /**
     * Load background for background
     */

    @Throws(IOException::class)
    fun loadLegacy(): Boolean {
        if (Crine.destruced) return false
        var modified = false
        val modulesFile = File(dir, "modules.json")
        if (modulesFile.exists()) {
            modified = true
            val fr = FileReader(modulesFile)
            try {
                val jsonElement = JsonParser().parse(BufferedReader(fr))
                for ((key, value) in jsonElement.asJsonObject.entrySet()) {
                    val module = Crine.moduleManager.getModule(key)
                    if (module != null) {
                        val jsonModule = value as JsonObject
                        module.state = jsonModule["State"].asBoolean
                        module.keyBind = jsonModule["KeyBind"].asInt
                        if (jsonModule.has("Array")) module.array = jsonModule["Array"].asBoolean
                        if (jsonModule.has("AutoDisable")) module.autoDisable =
                            EnumAutoDisableType.valueOf(jsonModule["AutoDisable"].asString)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            try {
                fr.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            ClientUtils.logInfo("Deleted Legacy config " + modulesFile.name + " " + modulesFile.delete())
        }

        val valuesFile = File(dir, "values.json")
        if (valuesFile.exists()) {
            modified = true
            val fr = FileReader(valuesFile)
            try {
                val jsonObject = JsonParser().parse(BufferedReader(fr)).asJsonObject
                for ((key, value) in jsonObject.entrySet()) {
                    val module = Crine.moduleManager.getModule(key)
                    if (module != null) {
                        val jsonModule = value as JsonObject
                        for (moduleValue in module.values) {
                            val element = jsonModule[moduleValue.name]
                            if (element != null) moduleValue.fromJson(element)
                        }
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            try {
                fr.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            ClientUtils.logInfo("Deleted Legacy config " + valuesFile.name + " " + valuesFile.delete())
        }

        val macrosFile = File(dir, "macros.json")
        if (macrosFile.exists()) {
            modified = true
            val fr = FileReader(macrosFile)
            try {
                val jsonArray = JsonParser().parse(BufferedReader(fr)).asJsonArray
                for (jsonElement in jsonArray) {
                    val macroJson = jsonElement.asJsonObject
                    Crine.macroManager.macros
                        .add(Macro(macroJson["key"].asInt, macroJson["command"].asString))
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            try {
                fr.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            ClientUtils.logInfo("Deleted Legacy config " + macrosFile.name + " " + macrosFile.delete())
        }

        val shortcutsFile = File(dir, "shortcuts.json")
        if (shortcutsFile.exists()) shortcutsFile.delete()

        return modified
    }

    companion object {
        val PRETTY_GSON = GsonBuilder().setPrettyPrinting().create()
    }
}
