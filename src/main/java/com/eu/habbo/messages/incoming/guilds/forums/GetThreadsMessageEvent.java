package com.eu.habbo.messages.incoming.guilds.forums;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.forums.ForumDataMessageComposer;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumThreadsMessageComposer;
import com.eu.habbo.messages.outgoing.handshake.ErrorReportMessageComposer;

public class GetThreadsMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int guildId = packet.readInt();
        int index = packet.readInt();

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);

        if (guild == null) {
            this.client.sendResponse(new ErrorReportMessageComposer(404));
            return;
        }

        this.client.sendResponse(new ForumDataMessageComposer(guild, this.client.getHabbo()));
        this.client.sendResponse(new GuildForumThreadsMessageComposer(guild, this.client.getHabbo(), index));
    }
}
