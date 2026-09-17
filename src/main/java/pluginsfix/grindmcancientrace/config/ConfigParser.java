package pluginsfix.grindmcancientrace.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pluginsfix.grindmcancientrace.domain.PriceRequirement;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.TradeReward;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigParser {

    private ConfigParser() {
    }

    public static PluginConfig parse(FileConfiguration config) {
        double spawnChancePercent = config.getDouble("spawn-chance-percent", 2.0);

        boolean hologramEnabled = config.getBoolean("hologram.enabled", true);
        boolean hologramShadow = config.getBoolean("hologram.shadow", true);
        double hologramHeightOffset = config.getDouble("hologram.height-offset", 2.4);
        double hologramTitleScale = config.getDouble("hologram.title-scale", 1.3);
        boolean hologramLineSpacing = config.getBoolean("hologram.line-spacing", true);

        String guiTitle = config.getString("gui.title", "Ancient Race Trade");
        int guiSize = config.getInt("gui.size", 54);
        String fillerMaterial = config.getString("gui.filler-item.material", "BLACK_STAINED_GLASS_PANE");
        String fillerName = config.getString("gui.filler-item.name", " ");
        List<Integer> tradeSlots = config.getIntegerList("gui.trade-slots");
        if (tradeSlots.isEmpty()) {
            tradeSlots = List.of(10, 12, 14, 16, 28, 30, 32, 34);
        }

        List<String> loreHeader = config.getStringList("gui.lore-format.header");
        List<String> lorePriceSection = config.getStringList("gui.lore-format.price-section");
        List<String> loreStatusSection = config.getStringList("gui.lore-format.status-section");

        String dbFileName = config.getString("database.file-name", "data.db");
        int dbPoolSize = config.getInt("database.maximum-pool-size", 6);
        long dbTimeout = config.getLong("database.connection-timeout-ms", 10000);
        int effectsInterval = config.getInt("effects-check-interval-seconds", 10);

        Map<Profession, ProfessionConfig> professions = new EnumMap<>(Profession.class);
        ConfigurationSection profsSection = config.getConfigurationSection("professions");

        if (profsSection != null) {
            for (Profession profession : Profession.values()) {
                ConfigurationSection pSection = profsSection.getConfigurationSection(profession.key());
                if (pSection == null) continue;

                String displayName = pSection.getString("display-name", profession.name());
                String villagerProf = pSection.getString("villager-profession", profession.vanillaProfession());

                List<TradeReward> rewards = parseRewards(pSection.getConfigurationSection("rewards"));
                List<PriceRequirement> prices = parsePrices(pSection.getConfigurationSection("prices"));

                professions.put(profession, new ProfessionConfig(profession, displayName, villagerProf, rewards, prices));
            }
        }

        return new PluginConfig(
                spawnChancePercent,
                hologramEnabled,
                hologramShadow,
                hologramHeightOffset,
                hologramTitleScale,
                hologramLineSpacing,
                guiTitle,
                guiSize,
                fillerMaterial,
                fillerName,
                tradeSlots,
                loreHeader,
                lorePriceSection,
                loreStatusSection,
                dbFileName,
                dbPoolSize,
                dbTimeout,
                effectsInterval,
                professions
        );
    }

    private static List<TradeReward> parseRewards(ConfigurationSection section) {
        if (section == null) return List.of();
        List<TradeReward> rewards = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null) {
                Map<?, ?> map = section.getMapList(key).isEmpty() ? null : null;
                continue;
            }
            TradeReward reward = parseSingleReward(itemSec);
            if (reward != null) {
                rewards.add(reward);
            }
        }

        List<Map<?, ?>> mapList = section.getParent().getMapList("rewards");
        if (!mapList.isEmpty()) {
            for (Map<?, ?> map : mapList) {
                TradeReward reward = parseRewardFromMap(map);
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }

        return rewards;
    }

    @SuppressWarnings("unchecked")
    private static TradeReward parseRewardFromMap(Map<?, ?> map) {
        String id = String.valueOf(map.get("id"));
        String type = String.valueOf(map.get("type")).toUpperCase();
        String name = map.containsKey("name") ? String.valueOf(map.get("name")) : null;
        List<String> lore = map.containsKey("lore") ? (List<String>) map.get("lore") : List.of();

        return switch (type) {
            case "ITEM" -> {
                String material = String.valueOf(map.get("material"));
                int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
                Map<String, Integer> enchants = new HashMap<>();
                if (map.containsKey("enchants") && map.get("enchants") instanceof Map<?, ?> enchMap) {
                    enchMap.forEach((k, v) -> enchants.put(String.valueOf(k), ((Number) v).intValue()));
                }
                Integer cmd = map.containsKey("custom-model-data") ? ((Number) map.get("custom-model-data")).intValue() : null;
                yield new TradeReward.ItemReward(id, material, amount, name, lore, enchants, cmd);
            }
            case "EFFECT" -> {
                String effectType = String.valueOf(map.get("effect-type"));
                int amplifier = map.containsKey("amplifier") ? ((Number) map.get("amplifier")).intValue() : 0;
                long hours = map.containsKey("duration-hours") ? ((Number) map.get("duration-hours")).longValue() : 24L;
                String material = map.containsKey("material") ? String.valueOf(map.get("material")) : "POTION";
                yield new TradeReward.EffectReward(id, effectType, amplifier, hours, material, name, lore);
            }
            case "MONEY" -> {
                double amount = map.containsKey("amount") ? ((Number) map.get("amount")).doubleValue() : 0.0;
                String material = map.containsKey("material") ? String.valueOf(map.get("material")) : "GOLD_INGOT";
                yield new TradeReward.MoneyReward(id, amount, material, name, lore);
            }
            case "DONATE_POINTS" -> {
                int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 0;
                String material = map.containsKey("material") ? String.valueOf(map.get("material")) : "NETHER_STAR";
                yield new TradeReward.DonatePointsReward(id, amount, material, name, lore);
            }
            case "COMMAND" -> {
                List<String> commands = map.containsKey("commands") ? (List<String>) map.get("commands") : List.of();
                String material = map.containsKey("material") ? String.valueOf(map.get("material")) : "COMMAND_BLOCK";
                yield new TradeReward.CommandReward(id, commands, material, name, lore);
            }
            default -> null;
        };
    }

    private static TradeReward parseSingleReward(ConfigurationSection sec) {
        String id = sec.getString("id", sec.getName());
        String type = sec.getString("type", "ITEM").toUpperCase();
        String name = sec.getString("name");
        List<String> lore = sec.getStringList("lore");

        return switch (type) {
            case "ITEM" -> {
                String material = sec.getString("material", "STONE");
                int amount = sec.getInt("amount", 1);
                Map<String, Integer> enchants = new HashMap<>();
                ConfigurationSection enchSec = sec.getConfigurationSection("enchants");
                if (enchSec != null) {
                    for (String eKey : enchSec.getKeys(false)) {
                        enchants.put(eKey, enchSec.getInt(eKey));
                    }
                }
                Integer cmd = sec.contains("custom-model-data") ? sec.getInt("custom-model-data") : null;
                yield new TradeReward.ItemReward(id, material, amount, name, lore, enchants, cmd);
            }
            case "EFFECT" -> {
                String effectType = sec.getString("effect-type", "SPEED");
                int amplifier = sec.getInt("amplifier", 0);
                long hours = sec.getLong("duration-hours", 24);
                String material = sec.getString("material", "POTION");
                yield new TradeReward.EffectReward(id, effectType, amplifier, hours, material, name, lore);
            }
            case "MONEY" -> {
                double amount = sec.getDouble("amount", 0.0);
                String material = sec.getString("material", "GOLD_INGOT");
                yield new TradeReward.MoneyReward(id, amount, material, name, lore);
            }
            case "DONATE_POINTS" -> {
                int amount = sec.getInt("amount", 0);
                String material = sec.getString("material", "NETHER_STAR");
                yield new TradeReward.DonatePointsReward(id, amount, material, name, lore);
            }
            case "COMMAND" -> {
                List<String> commands = sec.getStringList("commands");
                String material = sec.getString("material", "COMMAND_BLOCK");
                yield new TradeReward.CommandReward(id, commands, material, name, lore);
            }
            default -> null;
        };
    }

    private static List<PriceRequirement> parsePrices(ConfigurationSection section) {
        if (section == null) return List.of();
        List<PriceRequirement> prices = new ArrayList<>();

        List<Map<?, ?>> mapList = section.getParent().getMapList("prices");
        if (!mapList.isEmpty()) {
            for (Map<?, ?> map : mapList) {
                PriceRequirement price = parsePriceFromMap(map);
                if (price != null) {
                    prices.add(price);
                }
            }
        } else {
            for (String key : section.getKeys(false)) {
                ConfigurationSection pSec = section.getConfigurationSection(key);
                if (pSec != null) {
                    PriceRequirement price = parseSinglePrice(pSec);
                    if (price != null) {
                        prices.add(price);
                    }
                }
            }
        }

        return prices;
    }

    private static PriceRequirement parsePriceFromMap(Map<?, ?> map) {
        String id = String.valueOf(map.get("id"));
        String type = String.valueOf(map.get("type")).toUpperCase();
        String description = map.containsKey("description") ? String.valueOf(map.get("description")) : id;

        return switch (type) {
            case "ITEM" -> {
                String material = String.valueOf(map.get("material"));
                int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
                Integer cmd = map.containsKey("custom-model-data") ? ((Number) map.get("custom-model-data")).intValue() : null;
                yield new PriceRequirement.ItemPrice(id, material, amount, cmd, description);
            }
            case "MONEY" -> {
                double amount = map.containsKey("amount") ? ((Number) map.get("amount")).doubleValue() : 0.0;
                yield new PriceRequirement.MoneyPrice(id, amount, description);
            }
            case "DONATE_POINTS" -> {
                int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 0;
                yield new PriceRequirement.DonatePointsPrice(id, amount, description);
            }
            case "TASK" -> {
                String taskType = map.containsKey("task-type") ? String.valueOf(map.get("task-type")) : "KILL_MOB";
                String target = map.containsKey("target") ? String.valueOf(map.get("target")) : "ZOMBIE";
                int count = map.containsKey("required-count") ? ((Number) map.get("required-count")).intValue() : 1;
                yield new PriceRequirement.TaskPrice(id, taskType, target, count, description);
            }
            default -> null;
        };
    }

    private static PriceRequirement parseSinglePrice(ConfigurationSection sec) {
        String id = sec.getString("id", sec.getName());
        String type = sec.getString("type", "ITEM").toUpperCase();
        String description = sec.getString("description", id);

        return switch (type) {
            case "ITEM" -> {
                String material = sec.getString("material", "STONE");
                int amount = sec.getInt("amount", 1);
                Integer cmd = sec.contains("custom-model-data") ? sec.getInt("custom-model-data") : null;
                yield new PriceRequirement.ItemPrice(id, material, amount, cmd, description);
            }
            case "MONEY" -> {
                double amount = sec.getDouble("amount", 0.0);
                yield new PriceRequirement.MoneyPrice(id, amount, description);
            }
            case "DONATE_POINTS" -> {
                int amount = sec.getInt("amount", 0);
                yield new PriceRequirement.DonatePointsPrice(id, amount, description);
            }
            case "TASK" -> {
                String taskType = sec.getString("task-type", "KILL_MOB");
                String target = sec.getString("target", "ZOMBIE");
                int count = sec.getInt("required-count", 1);
                yield new PriceRequirement.TaskPrice(id, taskType, target, count, description);
            }
            default -> null;
        };
    }
}
