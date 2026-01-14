package com.rixafy.voterewards.reward

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.universe.Universe
import com.rixafy.voterewards.VoteRewardsPlugin
import com.rixafy.voterewards.model.Vote
import java.util.logging.Level

class RewardManager(private val plugin: VoteRewardsPlugin) {

    fun giveRewards(vote: Vote) {
        val config = plugin.config.get()
        val rewardCommands = config.getRewardCommands()

        if (rewardCommands.isEmpty()) {
            plugin.logger.at(Level.WARNING).log("No reward commands configured!")
            return
        }

        for (command in rewardCommands) {
            val processedCommand = command.replace("{player}", vote.username)
                .replace("{service}", vote.serviceName)
                .replace("{address}", vote.address)
                .replace("{timestamp}", vote.timestamp)

            try {
                executeCommand(processedCommand)

                if (config.getDebugMode()) {
                    plugin.logger.at(Level.INFO).log("Executed reward command: $processedCommand")
                }
            } catch (e: Exception) {
                plugin.logger.at(Level.SEVERE).log("Failed to execute reward command: $processedCommand", e)
            }
        }
    }

    fun broadcastVote(vote: Vote) {
        val config = plugin.config.get()

        if (!config.getBroadcastVotes()) {
            return
        }

        val messageText = config.getBroadcastMessage()
            .replace("{player}", vote.username)
            .replace("{service}", vote.serviceName)
            .replace("&", "§")

        try {
            broadcastMessage(messageText)
        } catch (e: Exception) {
            plugin.logger.at(Level.SEVERE).log("Failed to broadcast vote message", e)
        }
    }

    private fun executeCommand(command: String) {
        val commandManager = HytaleServer.get().commandManager
        val consoleSender = object : CommandSender {
            override fun sendMessage(message: Message) {
                // Console sender - ignore message output
            }

            override fun getDisplayName(): String = "VoteRewards"

            override fun getUuid(): java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

            override fun hasPermission(permission: String): Boolean = true

            override fun hasPermission(permission: String, defaultValue: Boolean): Boolean = true
        }
        commandManager.handleCommand(consoleSender, command)
    }

    private fun broadcastMessage(messageText: String) {
        val universe = Universe.get()
        val message = Message.raw(messageText)

        for (player in universe.players) {
            player.sendMessage(message)
        }
    }
}
