package com.rixafy.voterewards.model

data class Vote(
    val serviceName: String,
    val username: String,
    val address: String,
    val timestamp: String
) {
    override fun toString(): String {
        return "Vote(service=$serviceName, username=$username, address=$address, timestamp=$timestamp)"
    }
}
