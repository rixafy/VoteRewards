package com.rixafy.voterewards.protocol

import com.rixafy.voterewards.model.Vote
import org.json.JSONObject

class VoteParser {

    fun parseVoteV2(jsonData: String): Vote? {
        return try {
            val json = JSONObject(jsonData)

            val serviceName = json.optString("serviceName", "Unknown")
            val username = json.optString("username", "")
            val address = json.optString("address", "0.0.0.0")
            val timestamp = json.optString("timestamp", System.currentTimeMillis().toString())

            Vote(serviceName, username, address, timestamp)
        } catch (e: Exception) {
            null
        }
    }

    fun validateVote(vote: Vote): Boolean {
        if (vote.serviceName.isBlank()) return false
        if (vote.username.isBlank()) return false
        if (vote.address.isBlank()) return false
        if (vote.timestamp.isBlank()) return false
        return true
    }
}
