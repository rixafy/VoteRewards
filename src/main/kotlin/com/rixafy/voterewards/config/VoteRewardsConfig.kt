package com.rixafy.voterewards.config

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import java.security.SecureRandom

class VoteRewardsConfig {
    companion object {
        private fun generateSecureToken(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val random = SecureRandom()
            return (1..26)
                .map { chars[random.nextInt(chars.length)] }
                .joinToString("")
        }

        val CODEC: BuilderCodec<VoteRewardsConfig> = BuilderCodec.builder(
            VoteRewardsConfig::class.java
        ) { VoteRewardsConfig() }
            .append(
                KeyedCodec("Port", Codec.INTEGER),
                { c, v -> c.port = v },
                { c -> c.port }
            ).add()
            .append(
                KeyedCodec("Host", Codec.STRING),
                { c, v -> c.host = v },
                { c -> c.host }
            ).add()
            .append(
                KeyedCodec("Token", Codec.STRING),
                { c, v -> c.token = v },
                { c -> c.token }
            ).add()
            .append(
                KeyedCodec("RewardCommands", Codec.STRING_ARRAY),
                { c, v -> c.rewardCommands = v.toList() },
                { c -> c.rewardCommands.toTypedArray() }
            ).add()
            .append(
                KeyedCodec("BroadcastVotes", Codec.BOOLEAN),
                { c, v -> c.broadcastVotes = v },
                { c -> c.broadcastVotes }
            ).add()
            .append(
                KeyedCodec("BroadcastMessage", Codec.STRING),
                { c, v -> c.broadcastMessage = v },
                { c -> c.broadcastMessage }
            ).add()
            .append(
                KeyedCodec("DebugMode", Codec.BOOLEAN),
                { c, v -> c.debugMode = v },
                { c -> c.debugMode }
            ).add()
            .build()
    }

    private var port: Int = 8192
    private var host: String = "0.0.0.0"
    private var token: String = generateSecureToken()
    private var rewardCommands: List<String> = listOf(
        "give {player} Rock_Gem_Diamond 1",
        "give {player} Rock_Gem_Emerald 2"
    )
    private var broadcastVotes: Boolean = true
    private var broadcastMessage: String = "{player} has voted for the server! Thank you!"
    private var debugMode: Boolean = false

    fun getPort(): Int = port
    fun getHost(): String = host
    fun getToken(): String = token
    fun getRewardCommands(): List<String> = rewardCommands
    fun getBroadcastVotes(): Boolean = broadcastVotes
    fun getBroadcastMessage(): String = broadcastMessage
    fun getDebugMode(): Boolean = debugMode
}
