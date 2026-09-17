package pluginsfix.grindmcancientrace.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

public final class TradeCooldownRepository {
    private final Database database;
    private final Logger logger;

    public TradeCooldownRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public boolean isTradeOnCooldown(UUID playerUuid, UUID villagerUuid, int slotIndex, long now) {
        return getCooldownRemaining(playerUuid, villagerUuid, slotIndex, now).isPresent();
    }

    public OptionalLong getCooldownRemaining(UUID playerUuid, UUID villagerUuid, int slotIndex, long now) {
        String sql = """
                SELECT available_at
                FROM player_trade_cooldowns
                WHERE player_uuid = ? AND villager_uuid = ? AND slot_index = ?;
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, villagerUuid.toString());
            ps.setInt(3, slotIndex);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long availableAt = rs.getLong("available_at");
                    if (availableAt > now) {
                        return OptionalLong.of(availableAt - now);
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to check trade cooldown: " + e.getMessage());
        }

        return OptionalLong.empty();
    }

    public void setTradeCooldown(UUID playerUuid, UUID villagerUuid, int slotIndex, long availableAtMillis) {
        String sql = """
                INSERT OR REPLACE INTO player_trade_cooldowns (player_uuid, villager_uuid, slot_index, available_at)
                VALUES (?, ?, ?, ?);
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, villagerUuid.toString());
            ps.setInt(3, slotIndex);
            ps.setLong(4, availableAtMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to set trade cooldown: " + e.getMessage());
        }
    }

    public String formatCooldown(long remainingMillis) {
        long totalSeconds = Math.max(0, remainingMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return String.format("%02d ч. %02d мин.", hours, minutes);
        }
        return String.format("%02d мин. %02d сек.", minutes, seconds);
    }
}
