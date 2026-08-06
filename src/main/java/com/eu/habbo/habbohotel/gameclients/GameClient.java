package com.eu.habbo.habbohotel.gameclients;

import com.eu.habbo.Emulator;
import com.eu.habbo.crypto.HabboEncryption;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.items.chests.ChestTradeSession;
import com.eu.habbo.habbohotel.wired.core.WiredCapabilityState;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.plugin.events.emulator.OutgoingPacketEvent;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class GameClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);

    private final Channel channel;
    private final HabboEncryption encryption;

    private Habbo habbo;
    private boolean handshakeFinished;
    private String machineId = "";
    private volatile WiredCapabilityState wiredCapabilityState = WiredCapabilityState.unsupported();
    private volatile int activeChestId;
    private volatile int activeChestRoomId;
    private volatile ChestTradeSession chestTradeSession;

    public final ConcurrentHashMap<Integer, Integer> incomingPacketCounter = new ConcurrentHashMap<>(25);
    public final ConcurrentHashMap<Object, Long> messageTimestamps = new ConcurrentHashMap<>();
    public long lastPacketCounterCleared = Emulator.getIntUnixTimestamp();

    public GameClient(Channel channel) {
        this.channel = channel;
        this.encryption = Emulator.getCrypto().isEnabled()
                ? new HabboEncryption(
                    Emulator.getCrypto().getExponent(),
                    Emulator.getCrypto().getModulus(),
                    Emulator.getCrypto().getPrivateExponent())
                : null;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public HabboEncryption getEncryption() {
        return encryption;
    }

    public Habbo getHabbo() {
        return this.habbo;
    }

    public void setHabbo(Habbo habbo) {
        this.habbo = habbo;
    }

    public boolean isHandshakeFinished() {
        return handshakeFinished;
    }

    public void setHandshakeFinished(boolean handshakeFinished) {
        this.handshakeFinished = handshakeFinished;
    }

    public String getMachineId() {
        return this.machineId;
    }

    public void setMachineId(String machineId) {
        if (machineId == null) {
            throw new RuntimeException("Cannot set machineID to NULL");
        }

        this.machineId = machineId;
    }

    public WiredCapabilityState getWiredCapabilityState() {
        return this.wiredCapabilityState;
    }

    public void setWiredCapabilityState(WiredCapabilityState wiredCapabilityState) {
        WiredCapabilityState next = wiredCapabilityState == null
                ? WiredCapabilityState.unsupported()
                : wiredCapabilityState;
        if (this.wiredCapabilityState.roomId() != next.roomId()) {
            if (this.chestTradeSession != null && this.habbo != null) {
                Emulator.getGameEnvironment().getChestManager()
                        .abortTrade(this, false, 3);
            }
            this.clearActiveChest();
        }
        this.wiredCapabilityState = next;
    }

    public void setActiveChest(int roomId, int chestId) {
        this.activeChestRoomId = Math.max(0, roomId);
        this.activeChestId = Math.max(0, chestId);
    }

    public boolean isActiveChest(int roomId, int chestId) {
        return roomId > 0 && chestId > 0
                && this.activeChestRoomId == roomId
                && this.activeChestId == chestId;
    }

    public void clearActiveChest() {
        this.activeChestRoomId = 0;
        this.activeChestId = 0;
    }

    public ChestTradeSession getChestTradeSession() {
        return this.chestTradeSession;
    }

    public synchronized boolean beginChestTradeSession(ChestTradeSession session) {
        if (session == null || this.chestTradeSession != null) {
            return false;
        }
        this.chestTradeSession = session;
        return true;
    }

    public synchronized ChestTradeSession clearChestTradeSession() {
        ChestTradeSession previous = this.chestTradeSession;
        this.chestTradeSession = null;
        return previous;
    }

    public void sendResponse(MessageComposer composer) {
        this.sendResponse(composer.compose());
    }

    public void sendResponse(ServerMessage response) {
        if (this.channel.isOpen()) {
            if (response == null || response.getHeader() <= 0) {
                return;
            }

            OutgoingPacketEvent event = new OutgoingPacketEvent(this.habbo, response.getComposer(), response);
            Emulator.getPluginManager().fireEvent(event);

            if (event.isCancelled()) {
                return;
            }

            if (event.hasCustomMessage()) {
                response = event.getCustomMessage();
            }

            this.channel.write(response, this.channel.voidPromise());
            this.channel.flush();
        }
    }

    public void sendResponses(ArrayList<ServerMessage> responses) {
        if (this.channel.isOpen()) {
            for (ServerMessage response : responses) {
                if (response == null || response.getHeader() <= 0) {
                    return;
                }

                OutgoingPacketEvent event = new OutgoingPacketEvent(this.habbo, response.getComposer(), response);
                Emulator.getPluginManager().fireEvent(event);

                if (event.isCancelled()) {
                    continue;
                }

                if (event.hasCustomMessage()) {
                    response = event.getCustomMessage();
                }

                this.channel.write(response);
            }

            this.channel.flush();
        }
    }

    public void dispose() {
        try {
            if (this.chestTradeSession != null && this.habbo != null) {
                Emulator.getGameEnvironment().getChestManager()
                        .abortTrade(this, false, 3);
            }
            this.wiredCapabilityState = WiredCapabilityState.unsupported();
            this.clearActiveChest();
            this.channel.close();

            if (this.habbo != null) {
                if (this.habbo.getHabboInfo() != null) {
                    com.eu.habbo.habbohotel.wired.menu.WiredMenuPreferences.evict(
                            this.habbo.getHabboInfo().getId());
                }
                if (this.habbo.isOnline()) {
                    this.habbo.getHabboInfo().setOnline(false);
                    this.habbo.disconnect();
                }

                this.habbo = null;
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }
}
