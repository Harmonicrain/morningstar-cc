package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;
import java.util.List;

/**
 * Wired 2.0 runtime movement packet (local outgoing 7115; Habbo header 641, May parser §_-52s§).
 * <p>
 * Carries a flat list of typed movement records so the client can interpolate the slide over
 * {@code animationTime} instead of snapping (legacy ObjectOnRoller). Replaces FloorItemOnRoller for
 * wired-driven furni movement (e.g. wf_chase), giving Habbo-parity smooth motion at fast trigger rates.
 * </p>
 * <p>Record types: 0=user move, 1=furni move, 2=wall item move, 3=user direction update.
 * Only furni moves are emitted today; the format leaves room for the rest.</p>
 */
public class WiredMovementsMessageComposer extends MessageComposer {
    public static final int DEFAULT_ANIMATION_TIME = 500;

    private static final int RECORD_FURNI_MOVE = 1;

    private final List<FurniMove> furniMoves;

    public WiredMovementsMessageComposer(FurniMove move) {
        this.furniMoves = new ArrayList<>(1);
        this.furniMoves.add(move);
    }

    public WiredMovementsMessageComposer(List<FurniMove> moves) {
        this.furniMoves = moves;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredMovementsMessageComposer);
        this.response.appendInt(this.furniMoves.size()); // total record count

        for (FurniMove m : this.furniMoves) {
            this.response.appendInt(RECORD_FURNI_MOVE);
            this.response.appendInt(m.fromX);
            this.response.appendInt(m.fromY);
            this.response.appendInt(m.toX);
            this.response.appendInt(m.toY);
            this.response.appendString(Double.toString(m.fromZ));
            this.response.appendString(Double.toString(m.toZ));
            this.response.appendInt(m.itemId);
            this.response.appendInt(m.animationTime);
            this.response.appendInt(m.rotation);

            if (m.overshootAnimationTime != null) {
                this.response.appendBoolean(true);
                this.response.appendInt(m.overshootAnimationTime);
            } else {
                this.response.appendBoolean(false);
            }

            if (m.curveStrength != null) {
                this.response.appendBoolean(true);
                this.response.appendInt(m.curveStrength);
            } else {
                this.response.appendBoolean(false);
            }
        }

        return this.response;
    }

    /**
     * A single furni-move record. itemId uses the room-visible id (matches FloorItemOnRoller).
     * {@code rotation} is the furni's facing (0-7); the client applies it as {@code (rotation % 8) * 45}
     * degrees, so a move-and-rotate effect (e.g. Change Furni Direction) animates the turn too. It is
     * read from the item <em>after</em> {@code moveFurniTo}, so it carries the post-move rotation.
     */
    public static class FurniMove {
        public final int itemId;
        public final int fromX, fromY, toX, toY;
        public final double fromZ, toZ;
        public final int animationTime;
        public final int rotation;
        public Integer overshootAnimationTime;
        public Integer curveStrength;

        public FurniMove(HabboItem item, RoomTile from, double fromZ, RoomTile to, double toZ, int animationTime) {
            this.itemId = item.getRoomVisibleId();
            this.fromX = from.x;
            this.fromY = from.y;
            this.fromZ = fromZ;
            this.toX = to.x;
            this.toY = to.y;
            this.toZ = toZ;
            this.animationTime = animationTime;
            this.rotation = item.getRotation();
        }
    }
}
