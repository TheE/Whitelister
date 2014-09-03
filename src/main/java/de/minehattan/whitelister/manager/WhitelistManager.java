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

public interface WhitelistManager {

    public void addToWhitelist(UUID id, String name);

    public void updateName(UUID id, String name);

    public Map<UUID, String> getImmutableWhitelist();

    public boolean isOnWhitelist(UUID id);

    public void removeFromWhitelist(UUID id);

}
