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
package de.minehattan.whitelister;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.entity.player.UUIDUtil;
import com.sk89q.commandbook.util.entity.player.iterators.PlayerIteratorAction;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.squirrelid.Profile;
import com.sk89q.squirrelid.resolver.CombinedProfileService;
import com.sk89q.squirrelid.resolver.HttpRepositoryService;
import com.sk89q.squirrelid.resolver.ProfileService;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

import de.minehattan.whitelister.manager.MySQLWhitelistManager;
import de.minehattan.whitelister.manager.WhitelistManager;

/**
 * The central entry-point of Whitelister.
 */
@ComponentInformation(friendlyName = "Whitelister", desc = "Protects the server with an automatic and flexible whitelist")
public class Whitelister extends BukkitComponent implements Listener {

    private volatile boolean maintenanceMode;
    private LocalConfiguration config;
    private WhitelistManager whitelistManager;
    private ProfileService resolver;

    /**
     * The configuration.
     */
    public static class LocalConfiguration extends ConfigurationBase {
        @Setting("messages.notOnWhitelist")
        private String notOnWhitelistMessage = "You are not on the Whitelist.";
        @Setting("messages.maintenanceMode")
        private String maintenanceMessage = "The server is currently in maintenance mode. Please try again in a few minutes.";
        @Setting("messages.maintenanceEnabled")
        private String maintenanceEnabledMessage = "Maintenance-Mode has been enabled - only OPs can join now.";
        @Setting("mysql.dsn")
        private String mysqlDsn = "jdbc:mysql://localhost/minecraft";
        @Setting("mysql.tableName")
        private String mysqlTableName = "whitelist";
        @Setting("mysql.user")
        private String mysqlUser = "minecraft";
        @Setting("mysql.password")
        private String mysqlPassword = "password";
    }

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        registerCommands(TopLevelCommand.class);
        CommandBook.registerEvents(this);

        whitelistManager = setupWhitelistManager();

        resolver = new CombinedProfileService(new WhitelistManagerService(whitelistManager),
                HttpRepositoryService.forMinecraft());
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);

        whitelistManager = setupWhitelistManager();
    }

    /**
     * Setups the WhitelistManager by initializing the appreciable one.
     * 
     * @return the appreciable WhitelistManager
     */
    private WhitelistManager setupWhitelistManager() {
        // TODO support for json whitelist?
        return new MySQLWhitelistManager(config.mysqlDsn, config.mysqlTableName, config.mysqlUser,
                config.mysqlPassword);
    }

    /**
     * Called asynchronous when a player tries to join the server.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLoginEvent(final AsyncPlayerPreLoginEvent event) {
        CommandBook.logger().info(event.getName() + " is trying to join...");

        if (maintenanceMode) {
            Future<Boolean> future = CommandBook.server().getScheduler()
                    .callSyncMethod(CommandBook.inst(), new Callable<Boolean>() {

                        @Override
                        public Boolean call() {
                            return CommandBook.server().getOfflinePlayer(event.getUniqueId()).isOp();
                        }

                    });

            boolean isOp = false;
            try {
                isOp = future.get();
            } catch (Exception e) {
                CommandBook.logger().log(Level.WARNING,
                        "Error while checking op-status for " + event.getName() + ". ", e);
            }
            if (!isOp) {
                event.disallow(Result.KICK_OTHER, config.maintenanceMessage);
                CommandBook.logger().info("Disallow (maintenance mode)");
            }
        } else if (!whitelistManager.contains(event.getUniqueId())) {
            event.disallow(Result.KICK_WHITELIST, config.notOnWhitelistMessage);
            CommandBook.logger().info("Disallow (not on whitelist)");
        } else {
            // Only update the name for players who are on the Whitelist.
            whitelistManager.updateName(event.getUniqueId(), event.getName());
        }
    }

    /**
     * The top-level commands.
     */
    public class TopLevelCommand {

        /**
         * The {@code whitelist} command.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         */
        @Command(aliases = { "whitelist", "wl" }, desc = "Central command to manage the whitelist")
        @NestedCommand(WhitelistCommands.class)
        public void whitelistCmd(CommandContext args, CommandSender sender) {
        }
    }

    /**
     * All subcommands of the {@code whitelist} command.
     */
    public class WhitelistCommands {
        /**
         * Adds a player to the whitelist.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "add" }, usage = "[name]", desc = "Adds the player of the given name to the whitelist", min = 1, max = 1)
        @CommandPermissions({ "whitelister.add" })
        public void add(CommandContext args, CommandSender sender) throws CommandException {
            String name = args.getString(0);
            UUID id = getUUID(name);

            if (whitelistManager.contains(id)) {
                throw new CommandException("'" + name + "' is already on the whitelist.");
            }

            whitelistManager.add(id, name);
            sender.sendMessage("'" + name + "' was added to the whitelist.");
        }

        /**
         * Removes a player from the whitelist.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "remove", "rm" }, usage = "[name]", desc = "Removes the player of the given name from the whitelist", min = 1, max = 1)
        @CommandPermissions({ "whitelister.remove" })
        public void remove(CommandContext args, CommandSender sender) throws CommandException {
            String name = args.getString(0);
            UUID id = getUUID(name);

            if (!whitelistManager.contains(id)) {
                throw new CommandException("'" + name + "' is not on the whitelist.");
            }

            whitelistManager.remove(id);
            sender.sendMessage("'" + name + "' was removed from the whitelist.");
        }

        /**
         * Checks if a player is whitelisted.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "check" }, usage = "[name]", desc = "Checks if the player of the given name is on the whitelist", min = 1, max = 1)
        @CommandPermissions({ "whitelister.check" })
        public void check(CommandContext args, CommandSender sender) throws CommandException {
            String name = args.getString(0);
            UUID id = getUUID(name);
            sender.sendMessage(whitelistManager.contains(id) ? ChatColor.GREEN + "'" + name
                    + "' is on the whitelist." : ChatColor.RED + "'" + name + "' is not on the whitelist.");
        }

        /**
         * Lists the entries of the whitelist.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "list" }, usage = "[#]", desc = "Lists all players on the whitelist", max = 1)
        @CommandPermissions({ "whitelister.list" })
        public void list(CommandContext args, CommandSender sender) throws CommandException {
            new PaginatedResult<Entry<UUID, String>>("Whitelist (Name - UUID)") {
                @Override
                public String format(Entry<UUID, String> entry) {
                    return ChatColor.GRAY + entry.getValue() + ChatColor.WHITE + " - " + ChatColor.GRAY
                            + entry.getKey();
                }
            } .display(sender, whitelistManager.getWhitelist().entrySet(), args.getInteger(0, 1));
        }

        /**
         * Enables or disables the maintenance mode.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "maintenance" }, desc = "Sets the server into maintenance mode, allowing only OPs to login", flags = "cn", max = 0)
        @CommandPermissions({ "whitelister.maintenance" })
        public void maintenance(CommandContext args, CommandSender sender) throws CommandException {
            if (args.hasFlag('c')) {
                if (!maintenanceMode) {
                    throw new CommandException("Server is not on maintenance mode!");
                }
                maintenanceMode = false;
                sender.sendMessage(ChatColor.GREEN + "Disabled maintenance-mode, everyone can join.");
            } else {
                if (maintenanceMode) {
                    throw new CommandException(
                            "Server is already in maintenance mode - use '/whitelist maintenance -c' to disable it.");
                }
                maintenanceMode = true;

                CommandBook.server().broadcastMessage(ChatColor.RED + config.maintenanceEnabledMessage);

                if (!args.hasFlag('n')) {
                    (new PlayerIteratorAction(sender) {

                        @Override
                        public void onCaller(Player player) {
                        }

                        @Override
                        public void onVictim(CommandSender sender, Player player) {
                        }

                        @Override
                        public boolean perform(Player player) {
                            if (!player.isOp()) {
                                player.kickPlayer(config.maintenanceMessage);
                            }
                            return true;
                        }

                        @Override
                        public void onInform(CommandSender sender, int affected) {
                            sender.sendMessage(ChatColor.RED + "One player has been kicked!");
                        }

                        @Override
                        public void onInformMany(CommandSender sender, int affected) {
                            sender.sendMessage(ChatColor.RED + Integer.toString(affected)
                                    + " players have been kicked!");
                        }

                    }).iterate(Arrays.asList(CommandBook.server().getOnlinePlayers()));
                }
            }
        }
    }

    /**
     * Attempts to get the UUID that identifies the player with the given name.
     * 
     * @param name
     *            the name
     * @return the corresponding UUID
     * @throws CommandException
     *             if the lookup fails or no UUID could be found
     */
    private UUID getUUID(String name) throws CommandException {
        // checks Bukkit Offline players first!
        UUID id = UUIDUtil.convert(name);
        // REVIEW is this logic needed?
        if (id == null) {
            Profile profile = null;
            try {
                profile = resolver.findByName(name);
            } catch (IOException e) {
                throw new CommandException("Failed lookup UUID due to an I/O error.");
            } catch (InterruptedException e) {
                throw new CommandException("UUID lookup was interrupted.");
            }
            if (profile != null) {
                id = profile.getUniqueId();
            }
        }
        if (id == null) {
            throw new CommandException("'" + name
                    + "' could not be assosiated with an UUID - is a valid Minecraft account?");
        }
        return id;
    }

}
