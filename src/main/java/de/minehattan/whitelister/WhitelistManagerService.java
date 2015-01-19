/*
 * Copyright (C) 2013 - 2015, Whitelister team and contributors
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
package de.minehattan.whitelister;

import java.io.IOException;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.sk89q.squirrelid.Profile;
import com.sk89q.squirrelid.resolver.ProfileService;

import de.minehattan.whitelister.manager.WhitelistManager;

/**
 * Checks a WhitelistManager to resolve UUIDs.
 */
public class WhitelistManagerService implements ProfileService {

    private final WhitelistManager manager;

    /**
     * Initialzes this WhitelistManagerService.
     * 
     * @param manager
     *            the WhitelistManager
     */
    public WhitelistManagerService(WhitelistManager manager) {
        this.manager = manager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sk89q.squirrelid.resolver.ProfileService#getIdealRequestLimit()
     */
    @Override
    public int getIdealRequestLimit() {
        return Integer.MAX_VALUE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sk89q.squirrelid.resolver.ProfileService#findByName(java.lang.String)
     */
    @Nullable
    @Override
    public Profile findByName(String name) throws IOException, InterruptedException {
        Profile ret = null;
        UUID uniqueId = manager.getUniqueID(name);
        if (uniqueId != null) {
            ret = new Profile(uniqueId, name);
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sk89q.squirrelid.resolver.ProfileService#findAllByName(java.lang.
     * Iterable)
     */
    @Override
    public ImmutableList<Profile> findAllByName(Iterable<String> names) throws IOException,
            InterruptedException {
        Builder<Profile> builder = ImmutableList.builder();
        for (String name : names) {
            Profile profile = findByName(name);
            if (profile != null) {
                builder.add(profile);
            }
        }
        return builder.build();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sk89q.squirrelid.resolver.ProfileService#findAllByName(java.lang.
     * Iterable, com.google.common.base.Predicate)
     */
    @Override
    public void findAllByName(Iterable<String> names, Predicate<Profile> consumer) throws IOException,
            InterruptedException {
        for (String name : names) {
            Profile profile = findByName(name);
            if (profile != null) {
                consumer.apply(profile);
            }
        }
    }

}
