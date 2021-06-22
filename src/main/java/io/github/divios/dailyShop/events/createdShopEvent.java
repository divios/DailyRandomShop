package io.github.divios.dailyShop.events;

import io.github.divios.lib.itemHolder.dShop;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

public class createdShopEvent extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCanceled = false;

    private final dShop deletedShop;
    private final Timestamp timestamp;

    public createdShopEvent(dShop deletedShop) {
        this.deletedShop = deletedShop;
        this.timestamp = new Timestamp(System.currentTimeMillis());

    }

    public dShop getShop() { return deletedShop; }

    public Timestamp getTimestamp() { return timestamp; }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
