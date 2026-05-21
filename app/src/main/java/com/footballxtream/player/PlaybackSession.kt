package com.footballxtream.player

import com.footballxtream.model.ChannelGroup

/**
 * Hands the channel to play from the channels screen to the player without serializing it through
 * navigation arguments. The player owns quality switching, so it needs the full variant list.
 */
class PlaybackSession {
    @Volatile
    var current: ChannelGroup? = null
}
