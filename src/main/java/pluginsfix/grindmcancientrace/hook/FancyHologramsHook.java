package pluginsfix.grindmcancientrace.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.text.Messages;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class FancyHologramsHook {
    private final Plugin plugin;
    private final PluginConfig config;
    private final Messages messages;
    private final Logger logger;
    private boolean available;

    public FancyHologramsHook(Plugin plugin, PluginConfig config, Messages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.logger = plugin.getLogger();
        checkAvailability();
    }

    public void checkAvailability() {
        Plugin fancyPlugin = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        this.available = fancyPlugin != null && fancyPlugin.isEnabled();
    }

    public boolean isAvailable() {
        return available;
    }

    public void createOrUpdateHologram(Entity entity, Profession profession) {
        if (!available || !config.hologramEnabled() || entity == null || !entity.isValid()) {
            return;
        }

        String hologramName = "ancient_race_" + entity.getUniqueId().toString().substring(0, 8);
        Location loc = entity.getLocation().clone().add(0, config.hologramHeightOffset(), 0);

        List<String> lines = new ArrayList<>();
        lines.add(messages.getRaw("hologram.line1"));
        if (config.hologramLineSpacing()) {
            lines.add("");
        }
        lines.add(messages.getRaw("hologram.line2"));

        String profName = messages.getRaw("professions." + profession.key());
        String line3 = messages.getRaw("hologram.line3").replace("<profession>", profName);
        lines.add(line3);

        try {
            Class<?> pluginClass = Class.forName("de.oliver.fancyholograms.FancyHologramsPlugin");
            Method getMethod = pluginClass.getMethod("get");
            Object pluginInstance = getMethod.invoke(null);

            Method getManagerMethod = pluginClass.getMethod("getHologramManager");
            Object manager = getManagerMethod.invoke(pluginInstance);

            Method getHologramMethod = manager.getClass().getMethod("getHologram", String.class);
            Object existingHologram = getHologramMethod.invoke(manager, hologramName);

            if (existingHologram != null) {
                Method getDataMethod = existingHologram.getClass().getMethod("getData");
                Object data = getDataMethod.invoke(existingHologram);

                Method setTextMethod = data.getClass().getMethod("setText", List.class);
                setTextMethod.invoke(data, lines);

                try {
                    Method setShadowMethod = data.getClass().getMethod("setTextHasShadow", boolean.class);
                    setShadowMethod.invoke(data, config.hologramShadow());
                } catch (NoSuchMethodException ignored) {
                }

                try {
                    Method setLocationMethod = data.getClass().getMethod("setLocation", Location.class);
                    setLocationMethod.invoke(data, loc);
                } catch (NoSuchMethodException ignored) {
                }

                Method refreshMethod = existingHologram.getClass().getMethod("refreshHologram");
                refreshMethod.invoke(existingHologram);
            } else {
                Class<?> dataClass = Class.forName("de.oliver.fancyholograms.api.data.TextHologramData");
                Object data = dataClass.getConstructor(String.class, Location.class).newInstance(hologramName, loc);

                Method setTextMethod = dataClass.getMethod("setText", List.class);
                setTextMethod.invoke(data, lines);

                try {
                    Method setShadowMethod = dataClass.getMethod("setTextHasShadow", boolean.class);
                    setShadowMethod.invoke(data, config.hologramShadow());
                } catch (NoSuchMethodException ignored) {
                }

                Method createMethod = manager.getClass().getMethod("create", Class.forName("de.oliver.fancyholograms.api.data.HologramData"));
                Object hologram = createMethod.invoke(manager, data);

                Method addHologramMethod = manager.getClass().getMethod("addHologram", Class.forName("de.oliver.fancyholograms.api.hologram.Hologram"));
                addHologramMethod.invoke(manager, hologram);
            }
        } catch (Throwable t) {
            logger.fine("FancyHolograms reflection invocation: " + t.getMessage());
        }
    }

    public void removeHologram(UUID entityUuid) {
        if (!available || entityUuid == null) return;
        String hologramName = "ancient_race_" + entityUuid.toString().substring(0, 8);

        try {
            Class<?> pluginClass = Class.forName("de.oliver.fancyholograms.FancyHologramsPlugin");
            Method getMethod = pluginClass.getMethod("get");
            Object pluginInstance = getMethod.invoke(null);

            Method getManagerMethod = pluginClass.getMethod("getHologramManager");
            Object manager = getManagerMethod.invoke(pluginInstance);

            Method getHologramMethod = manager.getClass().getMethod("getHologram", String.class);
            Object existingHologram = getHologramMethod.invoke(manager, hologramName);

            if (existingHologram != null) {
                Method removeMethod = manager.getClass().getMethod("removeHologram", Class.forName("de.oliver.fancyholograms.api.hologram.Hologram"));
                removeMethod.invoke(manager, existingHologram);
            }
        } catch (Throwable t) {
            logger.fine("FancyHolograms removal invocation: " + t.getMessage());
        }
    }
}
