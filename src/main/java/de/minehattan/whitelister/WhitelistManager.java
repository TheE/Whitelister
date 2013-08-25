package de.minehattan.whitelister;

import java.util.Set;

public interface WhitelistManager {
    
    public void addToWhitelist(String name);
    
    public Set<String> getImmutableWhitelist();
    
    public boolean isOnWhitelist(String name);
    
    public void loadWhitelist();

    public void removeFromWhitelist(String name);

}
