package net.shxp3.crine.discordrpc

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.IPCListener
import com.jagrosh.discordipc.entities.RichPresence
import com.jagrosh.discordipc.entities.User
import com.jagrosh.discordipc.entities.pipe.PipeStatus
import net.shxp3.crine.Crine
import net.shxp3.crine.features.module.modules.client.DiscordRPC
import net.shxp3.crine.ui.client.gui.GuiMainMenu
import net.shxp3.crine.utils.ClientUtils
import net.shxp3.crine.utils.MinecraftInstance
import net.shxp3.crine.utils.ServerUtils
import net.minecraft.client.gui.GuiMultiplayer
import org.json.JSONObject
import java.time.OffsetDateTime
import kotlin.concurrent.thread

object CrineRPC : MinecraftInstance(){

    private val ipcClient = IPCClient(1501705033086009515)
    private val timestamp = OffsetDateTime.now()
    private var running = false


    fun run() {
        ipcClient.setListener(object : IPCListener {
            override fun onReady(client: IPCClient?) {
                running = true
                thread {
                    while (running) {
                        update()
                        try {
                            Thread.sleep(1000L)
                        } catch (ignored: InterruptedException) {
                        }
                    }
                }
            }

            override fun onClose(client: IPCClient?, json: JSONObject?) {
                running = false
            }
        })
        try {
            ipcClient.connect()
        } catch (e: Exception) {
            ClientUtils.logError("DiscordRPC failed to start")
        } catch (e: RuntimeException) {
            ClientUtils.logError("DiscordRPC failed to start")
        }
    }

    private fun update() {
        val builder = RichPresence.Builder()
        // Set playing client time
        builder.setStartTimestamp(timestamp)

        builder.setLargeImage("https://crine.github.io/file/C.png", "Crine Client")

        if (mc.theWorld != null) {
            if (mc.theWorld.isRemote) {
                builder.setDetails("Playing : ${if (DiscordRPC.serverValue.get()) ServerUtils.getRemoteIp() else "Multiplayer"}")
            } else if (mc.isSingleplayer) {
                builder.setDetails("Playing : Singleplayer")
            }
        } else {
            builder.setDetails("Hack Client 1.8.9")
        }

        builder.setState("Version : Build ${Crine.CLIENT_VERSION}")
        if (ipcClient.status == PipeStatus.CONNECTED) ipcClient.sendRichPresence(builder.build())
    }

    fun stop() {
        if (ipcClient.status == PipeStatus.CONNECTED) ipcClient.close()
    }
}