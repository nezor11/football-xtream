package com.footballxtream.player

import com.footballxtream.model.ChannelGroup

/**
 * Hands the channels to play from the channels screen to the player without serializing them through
 * navigation arguments. Holds the ordered playlist + current index so the player can zap to the
 * next/previous channel with the remote.
 */
class PlaybackSession {

    @Volatile
    var channels: List<ChannelGroup> = emptyList()
        private set

    @Volatile
    var index: Int = 0
        private set

    val current: ChannelGroup?
        get() = channels.getOrNull(index)

    fun start(playlist: List<ChannelGroup>, startIndex: Int) {
        channels = playlist
        index = startIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
    }

    fun next(): ChannelGroup? {
        if (channels.isEmpty()) return null
        index = (index + 1) % channels.size
        return current
    }

    fun previous(): ChannelGroup? {
        if (channels.isEmpty()) return null
        index = (index - 1 + channels.size) % channels.size
        return current
    }
}
