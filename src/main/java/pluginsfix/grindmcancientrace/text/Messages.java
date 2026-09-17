package pluginsfix.grindmcancientrace.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class Messages {
    private final Plugin plugin;
    private final Logger logger;
    private final MiniMessage miniMessage;
    private final Map<String, String> stringCache;
    private final Map<String, List<String>> listCache;
    private String prefix;

    public Messages(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.miniMessage = MiniMessage.miniMessage();
        this.stringCache = new HashMap<>();
        this.listCache = new HashMap<>();
        reload();
    }

    public void reload() {
        stringCache.clear();
        listCache.clear();

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        this.prefix = yaml.getString("prefix", "");

        for (String key : yaml.getKeys(true)) {
            if (yaml.isString(key)) {
                stringCache.put(key, yaml.getString(key));
            } else if (yaml.isList(key)) {
                listCache.put(key, yaml.getStringList(key));
            }
        }
    }

    public String getRaw(String key) {
        String value = stringCache.get(key);
        if (value == null) {
            logger.warning("Missing message key in messages.yml: " + key);
            return "<red>Missing key: " + key + "</red>";
        }
        return value;
    }

    public List<String> getRawList(String key) {
        List<String> list = listCache.get(key);
        if (list == null) {
            logger.warning("Missing message list key in messages.yml: " + key);
            return Collections.emptyList();
        }
        return list;
    }

    public Component getComponent(String key, TagResolver... resolvers) {
        String raw = getRaw(key);
        return miniMessage.deserialize(raw, resolvers);
    }

    public List<Component> getComponentList(String key, TagResolver... resolvers) {
        List<String> rawList = getRawList(key);
        return rawList.stream()
                .map(line -> miniMessage.deserialize(line, resolvers))
                .toList();
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        if (sender == null) return;
        String raw = getRaw(key);
        Component component = miniMessage.deserialize(prefix + raw, resolvers);
        sender.sendMessage(component);
    }

    public void sendPlain(CommandSender sender, String key, TagResolver... resolvers) {
        if (sender == null) return;
        String raw = getRaw(key);
        Component component = miniMessage.deserialize(raw, resolvers);
        sender.sendMessage(component);
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }
}
