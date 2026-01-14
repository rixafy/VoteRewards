package com.rixafy.voterewards.server

import com.rixafy.voterewards.VoteRewardsPlugin
import com.rixafy.voterewards.protocol.VoteParser
import org.json.JSONObject
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class VotifierServer(
    private val plugin: VoteRewardsPlugin,
    private val host: String,
    private val port: Int,
    private val token: String
) {

    private var serverSocket: ServerSocket? = null
    private var running: Boolean = false
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val voteParser = VoteParser()

    fun start() {
        if (running) {
            plugin.logger.at(java.util.logging.Level.WARNING).log("Votifier server is already running!")
            return
        }

        try {
            serverSocket = ServerSocket(port, 50)
            running = true

            plugin.logger.at(java.util.logging.Level.INFO).log("Votifier V2 server started on $host:$port")
            plugin.logger.at(java.util.logging.Level.INFO).log("Token is stored in config.json - copy it from there for vote sites")

            executor.execute {
                while (running) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            executor.execute { handleConnection(clientSocket) }
                        }
                    } catch (e: SocketException) {
                        if (running) {
                            plugin.logger.at(java.util.logging.Level.SEVERE).log("Socket error in Votifier server", e)
                        }
                    } catch (e: IOException) {
                        if (running) {
                            plugin.logger.at(java.util.logging.Level.SEVERE).log("IO error in Votifier server", e)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            plugin.logger.at(java.util.logging.Level.SEVERE).log("Failed to start Votifier server on port $port", e)
            running = false
        }
    }

    private fun handleConnection(socket: Socket) {
        try {
            socket.use { client ->
                val input = client.getInputStream()
                val output = client.getOutputStream()

                val challenge = generateChallenge()

                val header = "VOTIFIER 2.0 $challenge\n"
                output.write(header.toByteArray())
                output.flush()

                if (plugin.config.get().getDebugMode()) {
                    plugin.logger.at(java.util.logging.Level.INFO).log("Sent challenge: $challenge")
                }

                // Read magic bytes (0x733a) and length
                val magicAndLength = ByteArray(4)
                val bytesRead = input.read(magicAndLength)
                if (bytesRead != 4) {
                    if (plugin.config.get().getDebugMode()) {
                        plugin.logger.at(java.util.logging.Level.WARNING).log("Invalid packet header")
                    }
                    return
                }

                val buffer = ByteBuffer.wrap(magicAndLength)
                val magic = buffer.short.toInt() and 0xFFFF
                val length = buffer.short.toInt() and 0xFFFF

                if (magic != 0x733a) {
                    if (plugin.config.get().getDebugMode()) {
                        plugin.logger.at(java.util.logging.Level.WARNING).log("Invalid magic bytes: ${magic.toString(16)}")
                    }
                    return
                }

                // Read message
                val messageBytes = ByteArray(length)
                var totalRead = 0
                while (totalRead < length) {
                    val read = input.read(messageBytes, totalRead, length - totalRead)
                    if (read == -1) break
                    totalRead += read
                }

                val messageJson = String(messageBytes)
                if (plugin.config.get().getDebugMode()) {
                    plugin.logger.at(java.util.logging.Level.INFO).log("Received message: $messageJson")
                }

                val message = JSONObject(messageJson)
                val signature = message.optString("signature", "")
                val payloadStr = message.optString("payload", "")

                // Verify signature
                if (!verifySignature(payloadStr, signature)) {
                    if (plugin.config.get().getDebugMode()) {
                        plugin.logger.at(java.util.logging.Level.WARNING).log("Invalid signature")
                    }
                    output.write("{\"status\":\"error\"}".toByteArray())
                    return
                }

                // Parse payload
                val payload = JSONObject(payloadStr)
                val receivedChallenge = payload.optString("challenge", "")

                if (receivedChallenge != challenge) {
                    if (plugin.config.get().getDebugMode()) {
                        plugin.logger.at(java.util.logging.Level.WARNING).log("Challenge mismatch")
                    }
                    output.write("{\"status\":\"error\"}".toByteArray())
                    return
                }

                val vote = voteParser.parseVoteV2(payloadStr)

                if (vote == null || !voteParser.validateVote(vote)) {
                    if (plugin.config.get().getDebugMode()) {
                        plugin.logger.at(java.util.logging.Level.WARNING).log("Invalid vote data")
                    }
                    output.write("{\"status\":\"error\"}".toByteArray())
                    return
                }

                plugin.handleVote(vote)
                output.write("{\"status\":\"ok\"}".toByteArray())

                if (plugin.config.get().getDebugMode()) {
                    plugin.logger.at(java.util.logging.Level.INFO).log("Received vote: $vote")
                }
            }
        } catch (e: Exception) {
            if (plugin.config.get().getDebugMode()) {
                plugin.logger.at(java.util.logging.Level.SEVERE).log("Error handling vote connection", e)
            }
        }
    }

    private fun generateChallenge(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..16)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun verifySignature(payload: String, signature: String): Boolean {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(token.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            val calculatedSignature = java.util.Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray()))
            calculatedSignature == signature
        } catch (e: Exception) {
            if (plugin.config.get().getDebugMode()) {
                plugin.logger.at(java.util.logging.Level.SEVERE).log("Error verifying signature", e)
            }
            false
        }
    }

    fun stop() {
        if (!running) {
            return
        }

        running = false

        try {
            serverSocket?.close()
            executor.shutdown()
            plugin.logger.at(java.util.logging.Level.INFO).log("Votifier server stopped")
        } catch (e: IOException) {
            plugin.logger.at(java.util.logging.Level.SEVERE).log("Error stopping Votifier server", e)
        }
    }

    fun isRunning(): Boolean = running
}
