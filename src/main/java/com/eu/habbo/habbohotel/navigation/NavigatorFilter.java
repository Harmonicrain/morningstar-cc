package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.guilds.Guild;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class NavigatorFilter {
    public final String viewName;

    public NavigatorFilter(String viewName) {
        this.viewName = viewName;
    }

    public void filter(Method method, Object value, List<SearchResultList> collection) {
        if (method == null) {
            return;
        }

        if (value instanceof String) {
            if (((String) value).isEmpty()) {
                return;
            }
        }

        for (SearchResultList result : collection) {
            if (!result.filter) {
                continue;
            }

            this.filterRooms(method, value, result.rooms);
        }
    }

    public void filterRooms(Method method, Object value, List<Room> result) {
        if (method == null) {
            return;
        }

        if (value instanceof String) {
            if (((String) value).isEmpty()) {
                return;
            }
        }

        List<Room> toRemove = new ArrayList<>();
        try {
            method.setAccessible(true);

            for (Room room : result) {
                Object o = method.invoke(room);
                if (o.getClass() == value.getClass()) {
                    if (o instanceof String) {
                        NavigatorFilterComparator comparator = Emulator.getGameEnvironment().getNavigatorManager().comperatorForField(method);

                        if (comparator != null) {
                            if (method.getName().equals("getTags")) {
                                boolean matched = false;

                                for (String tag : ((String) o).split(";")) {
                                    if (this.applies(comparator, tag, (String) value)) {
                                        matched = true;
                                        break;
                                    }
                                }

                                if (!matched) {
                                    toRemove.add(room);
                                }
                            } else if (method.getName().equals("getGuildName")) {
                                String guildSearchText = (String) o;
                                Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(room.getGuildId());

                                if (guild != null) {
                                    guildSearchText = guildSearchText + " " + guild.getDescription();
                                }

                                if (!this.applies(comparator, guildSearchText, (String) value)) {
                                    toRemove.add(room);
                                }
                            } else if (!this.applies(comparator, (String) o, (String) value)) {
                                toRemove.add(room);
                            }
                        } else {
                            toRemove.add(room);
                        }
                    } else if (o instanceof String[]) {
                        for (String s : (String[]) o) {
                            NavigatorFilterComparator comparator = Emulator.getGameEnvironment().getNavigatorManager().comperatorForField(method);

                            if (comparator != null) {
                                if (!this.applies(comparator, s, (String) value)) {
                                    toRemove.add(room);
                                }
                            }
                        }
                    } else {
                        if (o != value) {
                            toRemove.add(room);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        result.removeAll(toRemove);
        toRemove.clear();
    }

    public abstract List<SearchResultList> getResult(Habbo habbo);

    public List<SearchResultList> getResult(Habbo habbo, NavigatorFilterField filterField, String value, int roomCategory) {
        return this.getResult(habbo);
    }

    private boolean applies(NavigatorFilterComparator comparator, String o, String value) {
        switch (comparator) {
            case CONTAINS:
                if (StringUtils.containsIgnoreCase(o,
                        value)) {
                    return true;
                }
                break;

            case EQUALS:
                if (o.equals(value)) {
                    return true;
                }
                break;

            case EQUALS_IGNORE_CASE:
                if (o.equalsIgnoreCase(value)) {
                    return true;
                }
        }

        return false;
    }
}
