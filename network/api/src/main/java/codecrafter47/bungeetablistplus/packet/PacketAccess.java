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

package codecrafter47.bungeetablistplus.packet;

import net.md_5.bungee.api.connection.Connection;

import java.util.UUID;

public interface PacketAccess {
    boolean isTabHeaderFooterSupported();

    /**
     * Sets the player list header and footer for the given connection
     * @param connection the connection
     * @param header the header as json text
     * @param footer the footer as json text
     */
    void setTabHeaderAndFooter(Connection.Unsafe connection, String header, String footer);

    Batch createBatch();

    interface Batch {

        void createTeam(String name, String player);

        void addPlayerToTeam(String team, String player);

        void removePlayerFromTeam(String team, String player);

        void removeTeam(String name);

        void createOrUpdatePlayer(UUID player, String username, int gamemode, int ping, String[][] properties);

        void updateDisplayName(UUID player, String displayName);

        void updatePing(UUID player, int ping);

        void removePlayer(UUID player);

        void spawnPlayer(int entityId, UUID player);

        void send(Connection.Unsafe connection);
    }

    interface TabHeaderPacketAccess {

        /**
         * Sets the player list header and footer for the given connection
         * @param connection the connection
         * @param header the header as json text
         * @param footer the footer as json text
         */
        void setTabHeaderFooter(Connection.Unsafe connection, String header, String footer);
    }
}
