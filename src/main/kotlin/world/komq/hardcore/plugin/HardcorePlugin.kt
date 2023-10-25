/*
 * Copyright (c) 2023 Komtents Dev Team
 *
 *  Licensed under the General Public License, Version 3.0. (https://opensource.org/licenses/gpl-3.0/)
 */

package world.komq.hardcore.plugin

import cloud.commandframework.execution.CommandExecutionCoordinator
import cloud.commandframework.paper.PaperCommandManager
import org.bukkit.configuration.serialization.ConfigurationSerialization.registerClass
import org.bukkit.plugin.java.JavaPlugin
import world.komq.hardcore.plugin.commands.HardcoreCommand
import world.komq.hardcore.plugin.config.HardcoreCorpseData
import world.komq.hardcore.plugin.objects.HardcoreGameManager.corpses
import world.komq.hardcore.plugin.objects.HardcoreGameManager.fakeServer
import world.komq.hardcore.plugin.objects.HardcoreGameManager.isRunning
import world.komq.hardcore.plugin.objects.HardcoreGameManager.start
import world.komq.hardcore.plugin.objects.HardcoreGameManager.taskId
import world.komq.hardcore.plugin.objects.HardcoreGameManager.usableUnbans
import java.util.function.Function

/**
 * @author Komtents Dev Team
 */

@Suppress("UNCHECKED_CAST")
class HardcorePlugin : JavaPlugin() {

    companion object {
        lateinit var instance: HardcorePlugin
            private set
    }

    override fun onEnable() {
        instance = this

        registerClass(HardcoreCorpseData::class.java)
        corpses.addAll(config.getList("corpses", listOf<HardcoreCorpseData>()) as List<HardcoreCorpseData>)

        if (config.getBoolean("isRunning")) {
            logger.warning("게임이 현재 진행중인 상황입니다. 종료를 원하시는게 아닌 경우 \"/hardcore\" 명령어를 다시 입력하실 필요가 없습니다.")
            start()
        }

        val commandManager = PaperCommandManager(
            this,
            CommandExecutionCoordinator.simpleCoordinator(),
            Function.identity(),
            Function.identity()
        )

        commandManager.command(
            HardcoreCommand.hardcoreCommand(
                commandManager.commandBuilder(
                    "hardcore",
                    { "Hardcore Game command" }
                )
            )
        )

        commandManager.command(
            HardcoreCommand.unbanCommand(
                commandManager.commandBuilder(
                    "unban",
                    { "Custom unban command" }
                )
            )
        )
    }

    override fun onDisable() {
        server.scheduler.cancelTask(taskId)
        server.onlinePlayers.forEach { fakeServer.removePlayer(it) }

        if (isRunning) config.set("isRunning", true) else config.set("isRunning", false)
        config.set("usableUnbans", usableUnbans)
        config.set("corpses", corpses.toList())
        saveConfig()
    }
}