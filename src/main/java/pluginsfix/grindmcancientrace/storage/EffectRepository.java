package pluginsfix.grindmcancientrace.storage;

import pluginsfix.grindmcancientrace.domain.ActiveEffect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class EffectRepository {
    private final Database database;
    private final Logger logger;

    public EffectRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public void saveEffect(ActiveEffect effect) {
        String sql = """
                INSERT OR REPLACE INTO player_active_effects (player_uuid, effect_type, amplifier, expires_at)
                VALUES (?, ?, ?, ?);
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effect.playerUuid().toString());
            ps.setString(2, effect.effectType());
            ps.setInt(3, effect.amplifier());
            ps.setLong(4, effect.expiresAtMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to save active effect for player " + effect.playerUuid() + ": " + e.getMessage());
        }
    }

    public Optional<ActiveEffect> getEffect(UUID playerUuid, String effectType) {
        String sql = """
                SELECT amplifier, expires_at
                FROM player_active_effects
                WHERE player_uuid = ? AND effect_type = ?;
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, effectType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int amplifier = rs.getInt("amplifier");
                    long expiresAt = rs.getLong("expires_at");
                    return Optional.of(new ActiveEffect(playerUuid, effectType, amplifier, expiresAt));
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to get active effect for player " + playerUuid + ": " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<ActiveEffect> getAllActiveEffects(UUID playerUuid) {
        String sql = """
                SELECT effect_type, amplifier, expires_at
                FROM player_active_effects
                WHERE player_uuid = ?;
                """;

        List<ActiveEffect> effects = new ArrayList<>();
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String effectType = rs.getString("effect_type");
                    int amplifier = rs.getInt("amplifier");
                    long expiresAt = rs.getLong("expires_at");
                    effects.add(new ActiveEffect(playerUuid, effectType, amplifier, expiresAt));
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to get all active effects for player " + playerUuid + ": " + e.getMessage());
        }

        return effects;
    }

    public void removeExpiredEffects(long currentTimeMillis) {
        String sql = "DELETE FROM player_active_effects WHERE expires_at <= ?;";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentTimeMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to cleanup expired effects: " + e.getMessage());
        }
    }

    public void removeEffect(UUID playerUuid, String effectType) {
        String sql = "DELETE FROM player_active_effects WHERE player_uuid = ? AND effect_type = ?;";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, effectType);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to remove active effect for player " + playerUuid + ": " + e.getMessage());
        }
    }
}
