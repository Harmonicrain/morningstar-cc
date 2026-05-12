package com.eu.habbo.messages.outgoing.rooms.items.jukebox;

import com.eu.habbo.habbohotel.items.SoundTrack;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class PlayListMessageComposer extends MessageComposer {
    private final List<SoundTrack> tracks;
    private final int synchronizationCountMs;

    public PlayListMessageComposer(List<SoundTrack> tracks) {
        this(tracks, 0);
    }

    public PlayListMessageComposer(List<SoundTrack> tracks, int synchronizationCountMs) {
        this.tracks = tracks;
        this.synchronizationCountMs = Math.max(0, synchronizationCountMs);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PlayListMessageComposer);

        this.response.appendInt(this.synchronizationCountMs);
        this.response.appendInt(this.tracks.size());

        for (SoundTrack track : this.tracks) {
            this.response.appendInt(track.getId());
            this.response.appendInt(track.getLength() * 1000);
            this.response.appendString(track.getCode());
            this.response.appendString(track.getAuthor());
        }

        return this.response;
    }

    public List<SoundTrack> getTracks() {
        return tracks;
    }

    public int getSynchronizationCountMs() {
        return synchronizationCountMs;
    }
}
