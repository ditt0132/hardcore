/*
 * Copyright (c) 2023 Komtents Dev Team
 *
 *  Licensed under the General Public License, Version 3.0. (https://opensource.org/licenses/gpl-3.0/)
 */

package world.komq.hardcore.plugin.events

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
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

        server.onlinePlayers.filter { player.uniqueId != it.uniqueId }.forEach { otherP ->
            server.advancementIterator().forEach { advancement ->
                if (!advancement.key().value().contains("recipes")) {
                    otherP.getAdvancementProgress(advancement).awardedCriteria.forEach { criterion ->
                        player.getAdvancementProgress(advancement).awardCriteria(criterion)
                    }
                }
            }
        }
    }

    @EventHandler
    fun PlayerQuitEvent.onQuit() {
        fakeServer.removePlayer(player)
        quitMessage(null)
    }

    @EventHandler
    fun PlayerAdvancementCriterionGrantEvent.onCriterionGrant() {
        var isRootAdvancementDone = false

        server.onlinePlayers.forEach {
            if (!advancement.key().value().contains("recipes")) {
                it.getAdvancementProgress(advancement).awardCriteria(criterion)

                if (server.advancementIterator().asSequence()
                        .filter { advc -> !advc.key.value().contains("recipes") && !advc.key.value().endsWith("root") && advc.root == advancement.root }
                        .all {  advc -> it.getAdvancementProgress(advc).isDone }) {

                    isRootAdvancementDone = true
                }
            }
        }

        if (isRootAdvancementDone) {
            ++usableUnbans
        }
    }

    @EventHandler
    fun AsyncChatEvent.onChat() {
        if (!player.isOp) isCancelled = true
    }

    @EventHandler
    fun PlayerDeathEvent.onDeath() {
        createCorpseNPC(player, player.location.clone().apply {
            pitch = 0f
            yaw = 0f
            while (block.type.isAir) { y -= 0.005 }
        })

        player.inventory.clear()
        player.banPlayer(" ")
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
                it.bukkitEntity.location.distance(player.location) <= 1.5 ||
                        it.bukkitEntity.location.clone().subtract(1.0, 0.0, 0.0).distance(player.location) <= 1.5 ||
                        it.bukkitEntity.location.clone().subtract(2.0, 0.0, 0.0).distance(player.location) <= 1.5
            }?.let {
                openInventory(player, it)
            }
        }
    }

    @EventHandler
    fun PlayerCommandPreprocessEvent.onCommandPreProcess() {
        if (!player.isOp) isCancelled = true
    }

    @EventHandler
    fun PaperServerListPingEvent.onServerListPing() {
        setHidePlayers(true)
        motd(text("HARDCORE", NamedTextColor.RED).decorate(TextDecoration.BOLD))
    }
}