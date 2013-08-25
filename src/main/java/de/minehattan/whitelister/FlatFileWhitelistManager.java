package de.minehattan.whitelister;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.commandbook.CommandBook;

public class FlatFileWhitelistManager implements WhitelistManager {

    private final Set<String> whitelist;
    private final File whitelistFile;

    private volatile boolean needsReload;

    private static final String filename = "white-list.txt";

    public FlatFileWhitelistManager(int frequency, boolean caseSensitive) {
        // setup the whitelist-file
        whitelistFile = new File(filename);
        if (!whitelistFile.exists()) {
            try {
                whitelistFile.createNewFile();
            } catch (IOException e) {
                CommandBook.logger().severe("Failed to create default whitelist-file, " + e);
            }
        }
        
        //setup the whitelist
        whitelist = Collections.newSetFromMap(caseSensitive ? new ConcurrentHashMap<String, Boolean>() : new ConcurrentSkipListMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER));
        loadWhitelist();

        WhitelistWatcher whitelistWatcher = new WhitelistWatcher();
        whitelistWatcher.runTaskTimerAsynchronously(CommandBook.inst(), frequency * 20, frequency * 20);
    }

    @Override
    public synchronized void addToWhitelist(String name) {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(whitelistFile, true),
                    "utf-8"));
            output.newLine();
            output.write(name);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
            }
        }

        whitelist.add(name);
    }

    @Override
    public Set<String> getImmutableWhitelist() {
        return Collections.unmodifiableSet(whitelist);
    }

    @Override
    public boolean isOnWhitelist(String player) {
        if (needsReload) {
            CommandBook.logger().info("Executing scheduled whitelist reload.");
            loadWhitelist();
            needsReload = false;
        }

        return whitelist.contains(player);
    }

    @Override
    public synchronized void loadWhitelist() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(whitelistFile), "utf-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                whitelist.add(line);
            }
        } catch (FileNotFoundException ignore) {
        } catch (IOException e) {
            CommandBook.logger().warning("Failed to (re)load whitelist" + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }

    }

    @Override
    public synchronized void removeFromWhitelist(String name) {
        File tempFile = new File(filename + ".tmp");

        BufferedReader reader = null;
        BufferedWriter output = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(whitelistFile), "utf-8"));
            output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile, true), "utf-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(name)) {
                    continue;
                }
                output.write(line);
                output.newLine();
            }
        } catch (FileNotFoundException ignore) {
        } catch (IOException e) {
            CommandBook.logger().warning("Failed to (re)load whitelist" + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

        if (tempFile.renameTo(whitelistFile)) {
            whitelist.remove(name);
        }
    }

    public class WhitelistWatcher extends BukkitRunnable {

        private long lastModified;

        public WhitelistWatcher() {
            lastModified = whitelistFile.lastModified();
        }

        @Override
        public void run() {
            if (whitelistFile.lastModified() != lastModified) {
                lastModified = whitelistFile.lastModified();
                needsReload = true;
            }
        }
    }
}
