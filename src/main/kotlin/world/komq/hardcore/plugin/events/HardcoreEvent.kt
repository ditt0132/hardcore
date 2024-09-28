/*
 * Copyright (c) 2023 Komtents Dev Team
 *
 *  Licensed under the General Public License, Version 3.0. (https://opensource.org/licenses/gpl-3.0/)
 */

package world.komq.hardcore.plugin.events

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import io.papermc.paper.advancement.AdvancementDisplay
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import world.komq.hardcore.plugin.HardcorePlugin
import world.komq.hardcore.plugin.objects.HardcoreGameManager.createCorpseNPC
import world.komq.hardcore.plugin.objects.HardcoreGameManager.fakePlayers
import world.komq.hardcore.plugin.objects.HardcoreGameManager.fakeServer
import world.komq.hardcore.plugin.objects.HardcoreGameManager.openInventory
import world.komq.hardcore.plugin.objects.HardcoreGameManager.server
import world.komq.hardcore.plugin.objects.HardcoreGameManager.usableUnbans

/**
 * @author Komtents Dev Team
 */

object HardcoreEvent : Listener {
    @EventHandler
    fun PlayerJoinEvent.onJoin() {
        fakeServer.addPlayer(player)
        joinMessage(null)
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        fakeServer.removePlayer(player)
        quitMessage(null)
    }

    @EventHandler
    fun PlayerAdvancementCriterionGrantEvent.onCriterionGrant() {
        if (advancement.display?.frame() == AdvancementDisplay.Frame.CHALLENGE) {
            HardcorePlugin.instance.logger.info("Usable unbans: " + ++usableUnbans + " ("+(advancement.displayName() as TextComponent).examinableName())
        }
        HardcorePlugin.instance.logger.info("isNull: "+(advancement.display != null))
        HardcorePlugin.instance.logger.info(advancement.display?.frame()?.name)
    }

    @EventHandler
    fun PlayerDeathEvent.onDeath() {
        createCorpseNPC(player, player.location.clone().apply {
            pitch = 0f
            yaw = 0f
            while (block.type.isAir) {
                y -= 0.005
            }
        })

        player.inventory.clear()
        player.banPlayer(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")) + "에 사망했어요!\n" + deathMessage
        )
        deathMessage(null)
    }

    @EventHandler
    fun PlayerUseUnknownEntityEvent.onUseUnknownEntity() {
        fakePlayers.find { it.bukkitEntity.entityId == entityId }?.let {
            openInventory(player, it)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun PlayerToggleSneakEvent.onToggleSneak() {
        if (isSneaking) {
            fakePlayers.find {
                it.location.world == player.location.world &&
                        (it.bukkitEntity.location.distance(player.location) <= 1.5 ||
                                it.bukkitEntity.location.clone().subtract(1.0, 0.0, 0.0)
                                    .distance(player.location) <= 1.5 ||
                                it.bukkitEntity.location.clone().subtract(2.0, 0.0, 0.0)
                                    .distance(player.location) <= 1.5)
            }?.let {
                openInventory(player, it)
            }
        }
    }

    @EventHandler
    fun PlayerCommandPreprocessEvent.onCommandPreProcess() {
        isCancelled = !(message.startsWith("/unbans")
                || message.startsWith("/near")
                || player.isOp)
    }

    @EventHandler
    fun PaperServerListPingEvent.onServerListPing() {
        setHidePlayers(true)
        motd(text("hi, hardcore!", NamedTextColor.RED).decorate(TextDecoration.BOLD))
    }
}
