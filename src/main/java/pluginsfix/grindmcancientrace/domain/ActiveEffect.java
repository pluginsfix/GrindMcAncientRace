package pluginsfix.grindmcancientrace.domain;

import java.util.UUID;

public record ActiveEffect(
        UUID playerUuid,
        String effectType,
        int amplifier,
        long expiresAtMillis
) {
    public boolean isExpired(long currentTimeMillis) {
        return currentTimeMillis >= expiresAtMillis;
    }

    public long remainingSeconds(long currentTimeMillis) {
        long remaining = (expiresAtMillis - currentTimeMillis) / 1000L;
        return Math.max(0L, remaining);
    }

    public String formatRemainingTime(long currentTimeMillis) {
        long totalSeconds = remainingSeconds(currentTimeMillis);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return String.format("%02d ч. %02d мин.", hours, minutes);
        }
        return String.format("%02d мин. %02d сек.", minutes, seconds);
    }
}
