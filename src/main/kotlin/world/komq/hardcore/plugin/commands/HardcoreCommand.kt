/*
 * Copyright (c) 2023 Komtents Dev Team
 *
 *  Licensed under the General Public License, Version 3.0. (https://opensource.org/licenses/gpl-3.0/)
 */

// SPDX-License-identifier: GPL-3.0-only

package world.komq.hardcore.plugin.commands

import cloud.commandframework.Command
import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.context.CommandContext
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.BanList
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.ban.ProfileBanList
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import world.komq.hardcore.plugin.objects.HardcoreGameManager.isRunning
import world.komq.hardcore.plugin.objects.HardcoreGameManager.plugin
import world.komq.hardcore.plugin.objects.HardcoreGameManager.server
import world.komq.hardcore.plugin.objects.HardcoreGameManager.start
import world.komq.hardcore.plugin.objects.HardcoreGameManager.stop
import world.komq.hardcore.plugin.objects.HardcoreGameManager.usableUnbans
import java.util.*

/**
 * @author Komtents Dev Team
 */

object HardcoreCommand {
    fun hardcoreCommand(builder: Command.Builder<CommandSender>): Command.Builder<CommandSender> {
        return builder.apply { rootBuilder ->
            rootBuilder.permission { sender -> sender.isOp }
        }.handler {
            if (!isRunning) {
                isRunning = true
                plugin.logger.info("게임 시작")
                start()
            } else {
                isRunning = false
                plugin.logger.info("게임 종료")
                stop()
            }
        }
    }

    fun nearCommand(builder: Command.Builder<CommandSender>): Command.Builder<CommandSender> {

        return builder.handler { ctx ->
            if (ctx.sender !is Player) return@handler;
            Bukkit.getOnlinePlayers()
                .filter { it != ctx.sender }
                .filter { it.location.distance((ctx.sender as Player).location) <= 50 }
                .map { it.name }
                .takeIf { it.isNotEmpty() }
                ?.also {
                    ctx.sender.sendMessage(text("근처에 있는 플레이어: (50m)"))
                    ctx.sender.sendMessage(text(it.joinToString(", ")))
                }
                ?: ctx.sender.sendMessage("근처에 아무도 없어요... (50m)")
        }
    }

    fun unbanCommand(builder: Command.Builder<CommandSender>): Command.Builder<CommandSender> {
        return builder.apply { rootBuilder ->
            rootBuilder.permission { sender -> sender.isOp }
        }.argument(CommandArgument(true, "target", BannedPlayerArgumentParser, OfflinePlayer::class.java))
            .handler { ctx ->
                if (usableUnbans > 0) {
                    val target = ctx.get<OfflinePlayer>("target")

                    (server.getBanList(BanList.Type.PROFILE) as ProfileBanList).pardon(target.playerProfile)
                    --usableUnbans
                    ctx.sender.sendMessage(text("${target.name}님의 차단을 해제하였습니다."))
                    ctx.sender.sendMessage(text("남아 있는 차단 해제 횟수: $usableUnbans"))
                } else ctx.sender.sendMessage(text("사용 가능한 차단 해제 횟수가 없습니다.", NamedTextColor.RED))
            }
    }

    fun unbansCommand(builder: Command.Builder<CommandSender>): Command.Builder<CommandSender> {
        return builder.handler { ctx ->
            ctx.sender.sendMessage("남은 언밴 수: $usableUnbans")
        }
    }

    private object BannedPlayerArgumentParser : ArgumentParser<CommandSender, OfflinePlayer> {
        override fun parse(
            commandContext: CommandContext<CommandSender>,
            inputQueue: Queue<String>
        ): ArgumentParseResult<OfflinePlayer> {
            val input = inputQueue.peek()
            val player = server.getOfflinePlayerIfCached(input)

            return if (player != null) {
                inputQueue.remove()
                ArgumentParseResult.success(player)
            } else {
                ArgumentParseResult.failure(IllegalArgumentException("플레이어를 찾을 수 없습니다."))
            }
        }

        override fun suggestions(commandContext: CommandContext<CommandSender>, input: String): MutableList<String> {
            return server.offlinePlayers.filter { it.isBanned }.mapNotNull { it.name }.toMutableList()
        }
    }
}