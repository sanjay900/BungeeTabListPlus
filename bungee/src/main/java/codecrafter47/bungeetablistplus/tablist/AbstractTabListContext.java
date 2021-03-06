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

package codecrafter47.bungeetablistplus.tablist;

import codecrafter47.bungeetablistplus.api.bungee.IPlayer;
import codecrafter47.bungeetablistplus.api.bungee.ServerGroup;
import codecrafter47.bungeetablistplus.api.bungee.tablist.TabListContext;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Optional;

public abstract class AbstractTabListContext implements TabListContext {

    @Override
    public TabListContext setPlayer(IPlayer player) {
        return new DelegatingTabListContext(this) {
            @Override
            public IPlayer getPlayer() {
                return player;
            }

            @Override
            public Optional<ServerInfo> getServer() {
                return player.getServer();
            }

            @Override
            public Optional<ServerGroup> getServerGroup() {
                return player.getServer().map(ServerInfo::getName).map(GenericServerGroup::of);
            }
        };
    }

    @Override
    public TabListContext setOtherCount(int otherCount) {
        return new DelegatingTabListContext(this) {
            @Override
            public int getOtherPlayerCount() {
                return otherCount;
            }
        };
    }

    @Override
    public TabListContext setServerGroup(ServerGroup serverGroup) {
        if (serverGroup.getServerNames().size() == 1) {
            return new DelegatingTabListContext(this) {
                @Override
                public Optional<ServerGroup> getServerGroup() {
                    return Optional.of(serverGroup);
                }

                @Override
                public Optional<ServerInfo> getServer() {
                    return Optional.ofNullable(ProxyServer.getInstance().getServerInfo(serverGroup.getServerNames().iterator().next()));
                }
            };
        } else {
            return new DelegatingTabListContext(this) {
                @Override
                public Optional<ServerGroup> getServerGroup() {
                    return Optional.of(serverGroup);
                }
            };
        }
    }
}
