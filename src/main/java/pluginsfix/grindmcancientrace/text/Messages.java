package pluginsfix.grindmcancientrace.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
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
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            yaml.setDefaults(defaultYaml);
            yaml.options().copyDefaults(true);
            try {
                yaml.save(file);
            } catch (IOException ignored) {
            }
        }

        this.prefix = yaml.getString("prefix", "");

        for (String key : yaml.getKeys(true)) {
            if (yaml.isString(key)) {
                stringCache.put(key, yaml.getString(key));
            } else if (yaml.isList(key)) {
                listCache.put(key, yaml.getStringList(key));
            }
        }
    }

    public static String toMiniMessage(String input) {
        if (input == null) return "";

        String normalized = input.replace('§', '&');
        Matcher matcher = HEX_PATTERN.matcher(normalized);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1).toLowerCase() + ">");
        }
        matcher.appendTail(sb);
        String text = sb.toString();

        text = text.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>").replace("&A", "<green>")
                .replace("&b", "<aqua>").replace("&B", "<aqua>")
                .replace("&c", "<red>").replace("&C", "<red>")
                .replace("&d", "<light_purple>").replace("&D", "<light_purple>")
                .replace("&e", "<yellow>").replace("&E", "<yellow>")
                .replace("&f", "<white>").replace("&F", "<white>")
                .replace("&l", "<bold>").replace("&L", "<bold>")
                .replace("&m", "<strikethrough>").replace("&M", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&N", "<underlined>")
                .replace("&o", "<italic>").replace("&O", "<italic>")
                .replace("&k", "<obfuscated>").replace("&K", "<obfuscated>")
                .replace("&r", "<reset>").replace("&R", "<reset>");

        return text;
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

    public Component parse(String rawText, TagResolver... resolvers) {
        String formatted = toMiniMessage(rawText);
        return miniMessage.deserialize(formatted, resolvers);
    }

    public Component getComponent(String key, TagResolver... resolvers) {
        return parse(getRaw(key), resolvers);
    }

    public List<Component> getComponentList(String key, TagResolver... resolvers) {
        List<String> rawList = getRawList(key);
        return rawList.stream()
                .map(line -> parse(line, resolvers))
                .toList();
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        if (sender == null) return;
        String raw = getRaw(key);
        String combined = (prefix.isEmpty() ? "" : prefix + " ") + raw;
        Component component = parse(combined, resolvers);
        sender.sendMessage(component);
    }

    public void sendPlain(CommandSender sender, String key, TagResolver... resolvers) {
        if (sender == null) return;
        String raw = getRaw(key);
        Component component = parse(raw, resolvers);
        sender.sendMessage(component);
    }

    public TagResolver tag(String key, String value) {
        if (value == null) return Placeholder.component(key, Component.empty());
        return Placeholder.component(key, parse(value));
    }

    public TagResolver tag(String key, Component value) {
        if (value == null) return Placeholder.component(key, Component.empty());
        return Placeholder.component(key, value);
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }
}
