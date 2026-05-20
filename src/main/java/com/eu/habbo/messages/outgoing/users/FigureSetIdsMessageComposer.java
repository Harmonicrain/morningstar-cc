package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.ClothItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.procedure.TIntProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class FigureSetIdsMessageComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(FigureSetIdsMessageComposer.class);
    private final ArrayList<Integer> idList = new ArrayList<>();
    private final ArrayList<String> nameList = new ArrayList<>();

    public FigureSetIdsMessageComposer(Habbo habbo) {
        LOGGER.debug("[FigureSetIds] Building for user {} (id={}), clothing count={}",
            habbo.getHabboInfo().getUsername(), habbo.getHabboInfo().getId(),
            habbo.getInventory().getWardrobeComponent().getClothing().size());

        habbo.getInventory().getWardrobeComponent().getClothing().forEach(new TIntProcedure() {
            @Override
            public boolean execute(int value) {
                ClothItem item = Emulator.getGameEnvironment().getCatalogManager().clothing.get(value);

                if (item != null) {
                    for (Integer j : item.setId) {
                        FigureSetIdsMessageComposer.this.idList.add(j);
                    }

                    FigureSetIdsMessageComposer.this.nameList.add(item.name);
                } else {
                    LOGGER.warn("[FigureSetIds] clothing_id={} NOT FOUND in CatalogManager.clothing!", value);
                }

                return true;
            }
        });

        LOGGER.debug("[FigureSetIds] Sending {} setIds={}, names={}", this.idList.size(), this.idList, this.nameList);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FigureSetIdsMessageComposer);
        this.response.appendInt(this.idList.size());
        this.idList.forEach(this.response::appendInt);
        this.response.appendInt(this.nameList.size());
        this.nameList.forEach(this.response::appendString);
        return this.response;
    }

    public ArrayList<Integer> getIdList() {
        return idList;
    }

    public ArrayList<String> getNameList() {
        return nameList;
    }
}
