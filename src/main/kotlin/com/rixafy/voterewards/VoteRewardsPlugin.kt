package com.rixafy.voterewards

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.util.Config
import com.rixafy.voterewards.config.VoteRewardsConfig
import com.rixafy.voterewards.event.VoteReceivedEvent
import com.rixafy.voterewards.model.Vote
import com.rixafy.voterewards.reward.RewardManager
import com.rixafy.voterewards.server.VotifierServer
import java.util.logging.Level
import javax.annotation.Nonnull

class VoteRewardsPlugin(@Nonnull init: JavaPluginInit) : JavaPlugin(init) {

    companion object {
        private lateinit var instance: VoteRewardsPlugin

        fun get(): VoteRewardsPlugin = instance
    }

    val config: Config<VoteRewardsConfig> = this.withConfig(VoteRewardsConfig.CODEC)

    private lateinit var votifierServer: VotifierServer
    private lateinit var rewardManager: RewardManager

    override fun setup() {
        instance = this

        logger.at(Level.INFO).log("Setting up VoteRewards plugin...")

        rewardManager = RewardManager(this)

        logger.at(Level.INFO).log("VoteRewards setup complete!")
    }

    override fun start() {
        logger.at(Level.INFO).log("Starting VoteRewards plugin...")

        val cfg = config.get()

        // Save default config if it doesn't exist
        val configFile = dataDirectory.resolve("config.json").toFile()
        val isNewConfig = !configFile.exists()
        if (isNewConfig) {
            config.save().join()
            logger.at(Level.INFO).log("======================================")
            logger.at(Level.INFO).log("Created new config with random token")
            logger.at(Level.INFO).log("Config location: ${configFile.absolutePath}")
            logger.at(Level.INFO).log("======================================")
        }

        votifierServer = VotifierServer(
            this,
            cfg.getHost(),
            cfg.getPort(),
            cfg.getToken()
        )

        try {
            votifierServer.start()
            logger.at(Level.INFO).log("VoteRewards started successfully!")
        } catch (e: Exception) {
            logger.at(Level.SEVERE).log("Failed to start Votifier server", e)
        }
    }

    override fun shutdown() {
        logger.at(Level.INFO).log("Shutting down VoteRewards plugin...")

        if (::votifierServer.isInitialized && votifierServer.isRunning()) {
            votifierServer.stop()
        }

        logger.at(Level.INFO).log("VoteRewards shutdown complete!")
    }

    fun handleVote(vote: Vote) {
        val event = VoteReceivedEvent(vote)

        val dispatcher = HytaleServer.get().eventBus.dispatchFor(VoteReceivedEvent::class.java, null as Void?)

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(event)
        }

        if (event.isCancelled()) {
            if (config.get().getDebugMode()) {
                logger.at(Level.INFO).log("Vote cancelled by event listener: $vote")
            }
            return
        }

        rewardManager.giveRewards(vote)
        rewardManager.broadcastVote(vote)

        logger.at(Level.INFO).log("Processed vote from ${vote.username} via ${vote.serviceName}")
    }
}
