package pluginsfix.grindmcancientrace.config;

import pluginsfix.grindmcancientrace.domain.Profession;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PluginConfig(
        double spawnChancePercent,
        boolean hologramEnabled,
        boolean hologramShadow,
        double hologramHeightOffset,
        double hologramTitleScale,
        boolean hologramLineSpacing,
        String guiTitle,
        int guiSize,
        String fillerMaterial,
        String fillerName,
        List<Integer> tradeSlots,
        List<String> loreHeader,
        List<String> lorePriceSection,
        List<String> loreStatusSection,
        String databaseFileName,
        int databasePoolSize,
        long databaseTimeoutMs,
        int effectsCheckIntervalSeconds,
        Map<Profession, ProfessionConfig> professions
) {
    public Optional<ProfessionConfig> getProfessionConfig(Profession profession) {
        return Optional.ofNullable(professions.get(profession));
    }
}
