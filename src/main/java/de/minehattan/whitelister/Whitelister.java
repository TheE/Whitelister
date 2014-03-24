package de.minehattan.whitelister;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.iterators.PlayerIteratorAction;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

import de.minehattan.whitelister.FlatFileWhitelistManager;
import de.minehattan.whitelister.WhitelistManager;

@ComponentInformation(friendlyName = "Whitelister", desc = "Protects the server with an automatic and flexible whitelist")
public class Whitelister extends BukkitComponent implements Listener {

    private boolean maintenanceMode = false;
    private LocalConfiguration config;
    private WhitelistManager whitelistManager;

    public static class LocalConfiguration extends ConfigurationBase {
        @Setting("messages.notOnWhitelist")
        public String notOnWhitelistMessage = "You are not on the Whitelist.";
        @Setting("messages.maintenanceMode")
        public String maintenanceMessage = "The server is currently in maintenance mode. Please try again in a few minutes.";
        @Setting("messages.maintenanceEnabled")
        public String maintenanceEnabledMessage = "Maintenance-Mode has been enabled - only OPs can join now.";
        @Setting("updateInterval")
        public int updateInterval = 30;
        @Setting("caseSensitiveLookup")
        public boolean caseSensitive = true;
    }

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        registerCommands(TopLevelCommand.class);
        CommandBook.registerEvents(this);

        whitelistManager = new FlatFileWhitelistManager(config.updateInterval, config.caseSensitive);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);

        whitelistManager.loadWhitelist();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLoginEvent(final AsyncPlayerPreLoginEvent event) {
        CommandBook.logger().info(event.getName() + " is trying to join...");

        if (maintenanceMode) {
            Future<Boolean> isOp = CommandBook.server().getScheduler()
                    .callSyncMethod(CommandBook.inst(), new Callable<Boolean>() {

                        @Override
                        public Boolean call() {
                            return CommandBook.server().getOfflinePlayer(event.getName()).isOp();
                        }

                    });

            try {
                if (!isOp.get()) {
                    event.disallow(Result.KICK_OTHER, config.maintenanceMessage);
                    CommandBook.logger().info("Disallow (maintenance mode)");
                }
            } catch (Exception e) {
                CommandBook.logger().warning(
                        "Error whole checking op-status for " + event.getName() + ", " + e);
                event.disallow(Result.KICK_OTHER, config.maintenanceMessage);
                CommandBook.logger().info("Disallow (maintenance mode)");
            }
        } else if (!whitelistManager.isOnWhitelist(event.getName())) {
            event.disallow(Result.KICK_WHITELIST, config.notOnWhitelistMessage);
            CommandBook.logger().info("Disallow (not on whitelist)");
        }
    }

    public class TopLevelCommand {
        @Command(aliases = { "whitelist", "wl" }, desc = "Central command to manage the whitelist")
        @NestedCommand(Commands.class)
        public void whitelistCmd(CommandContext context, CommandSender sender) {
        }
    }

    public class Commands {
        @Command(aliases = { "add" }, usage = "[player]", desc = "Adds the given name to the whitelist", min = 1, max = 1)
        @CommandPermissions({ "whitelister.add" })
        public void add(CommandContext args, CommandSender sender) throws CommandException {
            String name = args.getString(0);
            if (whitelistManager.isOnWhitelist(name)) {
                throw new CommandException("'" + name + "' is already on the whitelist.");
            }

            whitelistManager.addToWhitelist(name);
            sender.sendMessage("'" + name + "' was added to the whitelist.");
        }

        @Command(aliases = { "remove", "rm" }, usage = "[player]", desc = "Removes the given name from the whitelist", min = 1, max = 1)
        @CommandPermissions({ "whitelister.add" })
        public void remove(CommandContext args, CommandSender sender) throws CommandException {
            String name = args.getString(0);
            if (!whitelistManager.isOnWhitelist(name)) {
                throw new CommandException("'" + name + "' is not on the whitelist.");
            }

            whitelistManager.removeFromWhitelist(name);
            sender.sendMessage("'" + name + "' was removed from the whitelist.");
        }

        @Command(aliases = { "reload" }, desc = "Reloads the whitelist", max = 0)
        @CommandPermissions({ "whitelister.check" })
        public void reload(CommandContext args, CommandSender sender) throws CommandException {
            whitelistManager.loadWhitelist();
            sender.sendMessage("Whitelist reloaded.");
        }

        @Command(aliases = { "check" }, usage = "[player]", desc = "Checks if the player of the given name is on the whitelist", min = 1, max = 1)
        @CommandPermissions({ "whitelister.check" })
        public void check(CommandContext args, CommandSender sender) throws CommandException {
            String name = args.getString(0);
            sender.sendMessage(whitelistManager.isOnWhitelist(name) ? ChatColor.GREEN + "'" + name
                    + "' is on the whitelist." : ChatColor.RED + "'" + name + "' is not on the whitelist.");
        }

        @Command(aliases = { "list" }, desc = "Lists all players on the whitelist", max = 0)
        @CommandPermissions({ "whitelister.list" })
        public void list(CommandContext args, CommandSender sender) throws CommandException {
            sender.sendMessage(StringUtils.join(whitelistManager.getImmutableWhitelist(), ", "));
        }

        @Command(aliases = { "maintenance" }, desc = "Sets the server into maintenance mode, alowing only OPs to login", flags = "cn", max = 0)
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

                CommandBook.server().broadcastMessage(
                        ChatColor.RED + config.maintenanceEnabledMessage);

                if (!args.hasFlag('n')) {
                    (new PlayerIteratorAction(sender) {

                        @Override
                        public void onCaller(Player player) {
                        }

                        @Override
                        public void onVictim(CommandSender sender, Player player) {
                        }

                        @Override
                        public void perform(Player player) {
                            if (!player.isOp()) {
                                player.kickPlayer(config.maintenanceMessage);
                            }
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

}
