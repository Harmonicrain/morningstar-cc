package com.eu.habbo.messages.outgoing.guilds.forums;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.forums.ForumThread;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.messages.outgoing.handshake.ErrorReportMessageComposer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class GuildForumThreadsMessageComposer extends MessageComposer {
    public final Guild guild;
    public final Habbo habbo;
    public final int index;

    public GuildForumThreadsMessageComposer(Guild guild, Habbo habbo, int index) {
        this.guild = guild;
        this.habbo = habbo;
        this.index = index;
    }

    @Override
    protected ServerMessage composeInternal() {
        ArrayList<ForumThread> threads;

        try {
            threads = new ArrayList<>(ForumThread.getByGuildId(guild.getId()));
        } catch (Exception e) {
            return new ErrorReportMessageComposer(500).compose();
        }

        threads.sort(Comparator.comparingInt(o -> o.isPinned() ? Integer.MAX_VALUE : o.getUpdatedAt()));
        Collections.reverse(threads);

        Iterator<ForumThread> it = threads.iterator();
        int count = Math.min(Math.max(threads.size() - this.index, 0), 20);

        this.response.init(Outgoing.GuildForumThreadsMessageComposer);
        this.response.appendInt(this.guild.getId());
        this.response.appendInt(this.index);
        this.response.appendInt(count);
        int lastSeenAt = ForumDataMessageComposer.getForumLastSeenAt(this.habbo, this.guild.getId());

        for (int i = 0; i < index; i++) {
            if (!it.hasNext())
                break;

            it.next();
        }

        for (int i = 0; i < count; i++) {
            if (!it.hasNext())
                break;

            it.next().serialize(this.response, lastSeenAt);
        }

        return this.response;
    }

    public Guild getGuild() {
        return guild;
    }

    public int getIndex() {
        return index;
    }
}
