package pluginsfix.grindmcancientrace.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pluginsfix.grindmcancientrace.domain.ActiveEffect;
import pluginsfix.grindmcancientrace.storage.EffectRepository;
import pluginsfix.grindmcancientrace.text.Messages;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceholderApiHook extends PlaceholderExpansion {
    private final Plugin plugin;
    private final EffectRepository effectRepository;
    private final Messages messages;
    private final Map<UUID, String> playerLastProfession;

    public PlaceholderApiHook(Plugin plugin, EffectRepository effectRepository, Messages messages) {
        this.plugin = plugin;
        this.effectRepository = effectRepository;
        this.messages = messages;
        this.playerLastProfession = new ConcurrentHashMap<>();
    }

    public void setPlayerProfession(UUID playerUuid, String professionName) {
        if (playerUuid != null && professionName != null) {
            playerLastProfession.put(playerUuid, professionName);
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ancientrace";
    }

    @Override
    public @NotNull String getAuthor() {
        return "pluginsfix";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("profession")) {
            return playerLastProfession.getOrDefault(player.getUniqueId(), "");
        }

        if (params.startsWith("effect_remaining_")) {
            String effectType = params.substring("effect_remaining_".length()).toUpperCase();
            Optional<ActiveEffect> effect = effectRepository.getEffect(player.getUniqueId(), effectType);
            if (effect.isPresent() && !effect.get().isExpired(System.currentTimeMillis())) {
                return effect.get().formatRemainingTime(System.currentTimeMillis());
            }
            return "";
        }

        if (params.startsWith("has_effect_")) {
            String effectType = params.substring("has_effect_".length()).toUpperCase();
            Optional<ActiveEffect> effect = effectRepository.getEffect(player.getUniqueId(), effectType);
            return (effect.isPresent() && !effect.get().isExpired(System.currentTimeMillis())) ? "true" : "false";
        }

        return null;
    }
}
