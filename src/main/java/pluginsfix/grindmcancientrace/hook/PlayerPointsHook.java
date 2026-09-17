package pluginsfix.grindmcancientrace.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PlayerPointsHook {
    private Object apiInstance;
    private Method lookMethod;
    private Method takeMethod;
    private Method giveMethod;

    public PlayerPointsHook() {
        tryHook();
    }

    private void tryHook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin == null || !plugin.isEnabled()) {
            this.apiInstance = null;
            return;
        }

        try {
            Method getApi = plugin.getClass().getMethod("getAPI");
            this.apiInstance = getApi.invoke(plugin);
            if (this.apiInstance != null) {
                Class<?> apiClass = this.apiInstance.getClass();
                this.lookMethod = apiClass.getMethod("look", UUID.class);
                this.takeMethod = apiClass.getMethod("take", UUID.class, int.class);
                this.giveMethod = apiClass.getMethod("give", UUID.class, int.class);
            }
        } catch (Throwable ignored) {
            this.apiInstance = null;
        }
    }

    public boolean isAvailable() {
        if (this.apiInstance == null) {
            tryHook();
        }
        return this.apiInstance != null;
    }

    public int getPoints(UUID playerUuid) {
        if (!isAvailable()) return 0;
        try {
            Object res = lookMethod.invoke(apiInstance, playerUuid);
            return res instanceof Number num ? num.intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public boolean hasPoints(UUID playerUuid, int amount) {
        return getPoints(playerUuid) >= amount;
    }

    public boolean takePoints(UUID playerUuid, int amount) {
        if (!isAvailable()) return false;
        try {
            Object res = takeMethod.invoke(apiInstance, playerUuid, amount);
            return Boolean.TRUE.equals(res);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean givePoints(UUID playerUuid, int amount) {
        if (!isAvailable()) return false;
        try {
            Object res = giveMethod.invoke(apiInstance, playerUuid, amount);
            return Boolean.TRUE.equals(res);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
