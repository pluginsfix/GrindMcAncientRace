package pluginsfix.grindmcancientrace.domain;

import java.util.Arrays;
import java.util.Optional;

public enum Profession {
    BLACKSMITH("blacksmith", "ARMORER"),
    FARMER("farmer", "FARMER"),
    WARRIOR("warrior", "WEAPONSMITH"),
    MINER("miner", "TOOLSMITH"),
    BANNER_BEARER("banner_bearer", "FLETCHER"),
    ALCHEMIST("alchemist", "CLERIC"),
    CHEF("chef", "BUTCHER"),
    WEAPONSMITH("weaponsmith", "WEAPONSMITH");

    private final String key;
    private final String vanillaProfession;

    Profession(String key, String vanillaProfession) {
        this.key = key;
        this.vanillaProfession = vanillaProfession;
    }

    public String key() {
        return key;
    }

    public String vanillaProfession() {
        return vanillaProfession;
    }

    public static Optional<Profession> fromKey(String key) {
        if (key == null) return Optional.empty();
        String normalized = key.toLowerCase().replace("-", "_").trim();
        return Arrays.stream(values())
                .filter(p -> p.key.equalsIgnoreCase(normalized) || p.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
