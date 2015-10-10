/*
 * BungeeTabListPlus - a BungeeCord plugin to customize the tablist
 *
 * Copyright (C) 2014 - 2015 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.bungeetablistplus.tablisthandler;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.api.bungee.IPlayer;
import codecrafter47.bungeetablistplus.api.bungee.tablist.TabList;
import codecrafter47.bungeetablistplus.player.FakePlayer;
import codecrafter47.bungeetablistplus.skin.PlayerSkin;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.Team;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Florian Stober
 */
public class CustomTabList18 extends net.md_5.bungee.tab.TabList implements PlayerTablistHandler {
    protected TabListHandler tabListHandler;
    boolean isExcluded = false;

    final Collection<String> usernames = new HashSet<>();
    final Map<UUID, String> uuids = new HashMap<>();
    final Set<UUID> requiresUpdate = Sets.newConcurrentHashSet();
    private final Map<String, FakePlayer> bukkitplayers = new ConcurrentHashMap<>();
    private final Multimap<String, String> teamToPlayerMap = MultimapBuilder.hashKeys().arrayListValues().build();
    private final ReentrantLock teamLock = new ReentrantLock();
    private boolean allowTeamPackets = true;

    private final static Pattern PATTERN_VALID_USERNAME = Pattern.compile("(?:\\p{Alnum}|_){1,16}");

    public CustomTabList18(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public ProxiedPlayer getPlayer() {
        return player;
    }

    @Override
    public void onServerChange() {
        try {
            // remove all those names from the clients tab, he's on another server now
            synchronized (bukkitplayers) {
                bukkitplayers.clear();
            }
            synchronized (usernames) {
                PlayerListItem packet = new PlayerListItem();
                packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
                PlayerListItem.Item[] items = new PlayerListItem.Item[uuids.size() + usernames.
                        size()];
                int i = 0;
                for (Entry<UUID, String> entry : uuids.entrySet()) {
                    PlayerListItem.Item item = new PlayerListItem.Item();
                    item.setUuid(entry.getKey());
                    item.setUsername(entry.getValue());
                    items[i++] = item;
                }
                for (String username : usernames) {
                    PlayerListItem.Item item = items[i++] = new PlayerListItem.Item();
                    item.setUsername(username);
                    item.setDisplayName(username);
                }
                packet.setItems(items);
                if (BungeeTabListPlus.getInstance().getProtocolVersionProvider().getProtocolVersion(player) >= 47) {
                    player.unsafe().sendPacket(packet);
                } else {
                    // Split up the packet
                    for (PlayerListItem.Item item : packet.getItems()) {
                        PlayerListItem p2 = new PlayerListItem();
                        p2.setAction(packet.getAction());
                        p2.setItems(new PlayerListItem.Item[]{
                                item
                        });
                        player.unsafe().sendPacket(p2);
                    }
                }
                uuids.clear();
                usernames.clear();
            }
            isExcluded = false;
            teamLock.lock();
            try {
                teamToPlayerMap.clear();
            } finally {
                teamLock.unlock();
            }
        } catch (Throwable th) {
            BungeeTabListPlus.getInstance().reportError(th);
        }
        this.tabListHandler.switchServer();
    }

    @Override
    public void exclude() {
        isExcluded = true;
        // only 1.7 clients
        if (BungeeTabListPlus.getInstance().getProtocolVersionProvider().getProtocolVersion(player) < 47) {
            synchronized (bukkitplayers) {
                synchronized (usernames) {
                    for (String s : bukkitplayers.keySet()) {
                        if (!usernames.contains(s)) {
                            PlayerListItem pli = new PlayerListItem();
                            pli.setAction(PlayerListItem.Action.ADD_PLAYER);
                            PlayerListItem.Item item = new PlayerListItem.Item();
                            item.setPing(0);
                            item.setUsername(s);
                            item.setProperties(new String[0][0]);
                            pli.setItems(new PlayerListItem.Item[]{item});
                            getPlayer().unsafe().sendPacket(pli);
                            usernames.add(s);
                        }
                    }
                }
            }
        }
        setAllowTeamPackets(true);
    }

    @Override
    public void onUpdate(PlayerListItem pli) {
        try {
            pli = CustomTabList18.rewrite(pli);

            for (PlayerListItem.Item i : pli.getItems()) {
                synchronized (bukkitplayers) {
                    String name = i.getUsername();
                    if (name == null && i.getUuid() != null) {
                        name = uuids.get(i.getUuid());
                    }
                    if (name != null) {
                        switch (pli.getAction()) {
                            case ADD_PLAYER:
                                FakePlayer player = new FakePlayer(name, getPlayer().getServer() != null ? getPlayer().getServer().getInfo() : null);
                                player.setPing(i.getPing());
                                player.setGamemode(i.getGamemode());
                                if (i.getUuid() != null && i.getProperties() != null) {
                                    for (String[] strings : i.getProperties()) {
                                        if (strings.length == 3 && strings[0].equals("textures")) {
                                            player.setSkin(new PlayerSkin(i.getUuid(), strings));
                                        }
                                    }
                                }
                                bukkitplayers.put(name, player);
                                break;
                            case UPDATE_GAMEMODE:
                                bukkitplayers.computeIfPresent(name, (s, fakePlayer) -> {
                                    fakePlayer.setGamemode(i.getGamemode());
                                    return fakePlayer;
                                });
                                break;
                            case UPDATE_LATENCY:
                                bukkitplayers.computeIfPresent(name, (s, fakePlayer) -> {
                                    fakePlayer.setPing(i.getPing());
                                    return fakePlayer;
                                });
                                break;
                            case UPDATE_DISPLAY_NAME:
                                break;
                            case REMOVE_PLAYER:
                                bukkitplayers.remove(name);
                                break;
                        }
                    }
                }
            }

            if (BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().excludeServers.
                    contains(getPlayer().getServer().getInfo().getName()) || isExcluded || BungeeTabListPlus.getInstance().getProtocolVersionProvider().getProtocolVersion(getPlayer()) >= 47) {
                if ((pli.getAction() == PlayerListItem.Action.ADD_PLAYER) || (pli.getAction() == PlayerListItem.Action.REMOVE_PLAYER) || pli.getItems()[0].getUuid().equals(getPlayer().getUniqueId())) {
                    // Pass the Packet to the client
                    player.unsafe().sendPacket(pli);
                    for (PlayerListItem.Item item : pli.getItems()) {
                        if (item.getUuid() != null) {
                            requiresUpdate.add(item.getUuid());
                        }
                    }

                    // update list on the client
                    BungeeTabListPlus.getInstance().updateTabListForPlayer(player);
                }
                // save which packets are send to the client
                synchronized (usernames) {
                    for (PlayerListItem.Item item : pli.getItems()) {
                        if (pli.getAction() == PlayerListItem.Action.ADD_PLAYER) {
                            if (item.getUuid() != null) {
                                uuids.put(item.getUuid(), item.getUsername());
                            } else {
                                usernames.add(item.getUsername());
                            }
                        } else if (pli.getAction() == PlayerListItem.Action.REMOVE_PLAYER) {
                            if (item.getUuid() != null) {
                                uuids.remove(item.getUuid());
                            } else {
                                usernames.remove(item.getUsername());
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            BungeeTabListPlus.getInstance().reportError(th);
        }
    }

    @Override
    public void onPingChange(int i) {
        // nothing to do
    }

    @Override
    public void onConnect() {
        // nothing to do
    }

    @Override
    public void onDisconnect() {
        // nothing to do
    }

    public int size() {
        return uuids.size();
    }

    @Override
    public boolean isExcluded() {
        return isExcluded;
    }

    @Override
    public List<IPlayer> getPlayers() {
        return Lists.newArrayList(bukkitplayers.values());
    }

    /**
     * Fixes some things.
     */
    public static PlayerListItem rewrite(PlayerListItem playerListItem) {
        for (PlayerListItem.Item item : playerListItem.getItems()) {
            if (item.getUuid() == null) // Old style ping
            {
                continue;
            }
            UserConnection player = BungeeCord.getInstance().getPlayerByOfflineUUID(item.getUuid());
            if (player == null) {
                player = (UserConnection) BungeeCord.getInstance().getPlayer(item.getUuid());
            }
            if (player != null) {
                item.setUuid(player.getUniqueId());
                LoginResult loginResult = player.getPendingConnection().getLoginProfile();
                if (loginResult != null) {
                    String[][] props = new String[loginResult.getProperties().length][];
                    for (int i = 0; i < props.length; i++) {
                        props[i] = new String[]
                                {
                                        loginResult.getProperties()[i].getName(),
                                        loginResult.getProperties()[i].getValue(),
                                        loginResult.getProperties()[i].getSignature()
                                };
                    }
                    item.setProperties(props);
                } else {
                    item.setProperties(new String[0][0]);
                }
                if (playerListItem.getAction() == PlayerListItem.Action.ADD_PLAYER || playerListItem.getAction() == PlayerListItem.Action.UPDATE_GAMEMODE) {
                    player.setGamemode(item.getGamemode());
                }
                player.setPing(player.getPing());
            }
        }
        return playerListItem;
    }

    @Override
    public void sendTablist(TabList tabList) {
        if (tabListHandler instanceof TabList18) {
            if (tabList.getSize() < 80 || tabList.shouldShrink()) {
                setTabListHandler(new TabList18v3(this));
            }
        } else if (tabListHandler instanceof TabList18v3) {
            if (tabList.getSize() >= 80 && !tabList.shouldShrink()) {
                setTabListHandler(new TabList18(this));
            }
        } else if (tabListHandler instanceof ScoreboardTabList) {
            if (!BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().useScoreboardToBypass16CharLimit) {
                setTabListHandler(new MyTabList(this));
            }
        } else if (tabListHandler instanceof MyTabList) {
            if (BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().useScoreboardToBypass16CharLimit) {
                setTabListHandler(new ScoreboardTabList(this));
            }
        }
        tabListHandler.sendTabList(tabList);
        requiresUpdate.clear();
    }

    @Override
    public void unload() {
        tabListHandler.unload();
    }

    @Override
    public void setTabListHandler(TabListHandler tabListHandler) {
        if (this.tabListHandler != null) {
            this.tabListHandler.unload();
        }
        this.tabListHandler = tabListHandler;
        if (tabListHandler instanceof TabList18v3) {
            setAllowTeamPackets(false);
        } else {
            setAllowTeamPackets(true);
        }
    }

    /**
     * listener method to team packets sent to the client
     *
     * @param packet the packet
     * @return whether the packet has been modified
     */
    public boolean onTeamPacket(Team packet) {
        boolean modified = false;
        teamLock.lock();
        try {
            if (packet.getMode() == 0 || packet.getMode() == 3) {
                // add players
                Set<String> humanPlayers = Arrays.stream(packet.getPlayers()).filter(player -> PATTERN_VALID_USERNAME.matcher(player).matches()).collect(Collectors.toSet());
                if (!humanPlayers.isEmpty()) {
                    teamToPlayerMap.putAll(packet.getName(), humanPlayers);
                    if (!allowTeamPackets) {
                        BungeeTabListPlus.getInstance().getLogger().warning("Scoreboard teams don't work with tab_size < 80.\nTransforming packet " + packet);
                        packet.setPlayers(Arrays.stream(packet.getPlayers()).filter(player -> !humanPlayers.contains(player)).toArray(String[]::new));
                        BungeeTabListPlus.getInstance().getLogger().warning("Scoreboard teams don't work with tab_size < 80.\nTransformed packet " + packet);
                        modified = true;
                    }
                }
            } else if (packet.getMode() == 4) {
                // remove players
                Set<String> humanPlayers = Arrays.stream(packet.getPlayers()).filter(player -> PATTERN_VALID_USERNAME.matcher(player).matches()).collect(Collectors.toSet());
                if (!humanPlayers.isEmpty()) {
                    for (String humanPlayer : humanPlayers) {
                        teamToPlayerMap.remove(packet.getName(), humanPlayer);
                    }
                    if (!allowTeamPackets) {
                        BungeeTabListPlus.getInstance().getLogger().warning("Scoreboard teams don't work with tab_size < 80.\nTransforming packet " + packet);
                        packet.setPlayers(Arrays.stream(packet.getPlayers()).filter(player -> !humanPlayers.contains(player)).toArray(String[]::new));
                        BungeeTabListPlus.getInstance().getLogger().warning("Scoreboard teams don't work with tab_size < 80.\nTransformed packet " + packet);
                        modified = true;
                    }
                }
            } else if (packet.getMode() == 1) {
                teamToPlayerMap.removeAll(packet.getName());
            }
        } finally {
            teamLock.unlock();
        }
        return modified;
    }

    public void setAllowTeamPackets(boolean allowTeamPackets) {
        teamLock.lock();
        try {
            if (allowTeamPackets != this.allowTeamPackets) {
                if (allowTeamPackets) {
                    // send all missing players
                    for (Entry<String, Collection<String>> entry : teamToPlayerMap.asMap().entrySet()) {
                        Team team = new Team(entry.getKey());
                        team.setMode((byte) 3);
                        team.setPlayers(entry.getValue().toArray(new String[entry.getValue().size()]));
                    }
                } else {
                    // remove players
                    for (Entry<String, Collection<String>> entry : teamToPlayerMap.asMap().entrySet()) {
                        Team team = new Team(entry.getKey());
                        team.setMode((byte) 4);
                        team.setPlayers(entry.getValue().toArray(new String[entry.getValue().size()]));
                    }
                }
            }
            this.allowTeamPackets = allowTeamPackets;
        } finally {
            teamLock.unlock();
        }
    }
}
