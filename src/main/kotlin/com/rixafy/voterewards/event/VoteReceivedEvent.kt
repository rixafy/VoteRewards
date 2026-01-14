package com.rixafy.voterewards.event

import com.hypixel.hytale.event.ICancellable
import com.hypixel.hytale.event.IEvent
import com.rixafy.voterewards.model.Vote

class VoteReceivedEvent(
    private val vote: Vote
) : IEvent<Void>, ICancellable {

    private var cancelled: Boolean = false

    fun getVote(): Vote = vote

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}
