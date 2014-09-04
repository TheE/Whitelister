/**
 * Copyright (C) 2013 - 2014, Whitelister team and contributors
 *
 * This file is part of Whitelister.
 *
 * Whitelister is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Whitelister is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Whitelister. If not, see <http://www.gnu.org/licenses/>.
 */
package de.minehattan.whitelister.manager;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Provides an abstraction layer to work with the underlying whitelist. A
 * whitelist consists of UUIDs and the last known username associated with this
 * UUID.
 */
public interface WhitelistManager {

    /**
     * Adds the given UUID to the whitelist and associates it with the given
     * name.
     * 
     * @param uniqueId
     *            the UUID
     * @param name
     *            the name
     */
    void add(UUID uniqueId, String name);

    /**
     * Removes the given UUID from the whitelist.
     * 
     * @param uniqueId
     *            the UUID
     */
    void remove(UUID uniqueId);

    /**
     * Updates the name that is associated with the given UUID.
     * 
     * @param uniqueId
     *            the UUID
     * @param name
     *            the name
     */
    void updateName(UUID uniqueId, String name);

    /**
     * Gets the UUID that is associated with the given name or {@code null} if
     * there is none.
     * 
     * @param name
     *            the name
     * @return the corresponding UUID
     */
    @Nullable
    UUID getUniqueID(String name);

    /**
     * Returns whether the whitelist contains the given UUID.
     * 
     * @param uniqueId
     *            the UUID
     * @return true if the UUID is on the whitelist
     */
    boolean contains(UUID uniqueId);

    /**
     * Gets an immutable representation of the UUIDs on the whitelist and the
     * associated names.
     * 
     * @return an immutable representation of the current whitelist
     */
    Map<UUID, String> getWhitelist();

}
