package pluginsfix.grindmcancientrace.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public final class TaskProgressRepository {
    private final Database database;
    private final Logger logger;

    public TaskProgressRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public int getProgress(UUID playerUuid, String taskKey) {
        String sql = """
                SELECT progress_count
                FROM player_task_progress
                WHERE player_uuid = ? AND task_key = ?;
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, taskKey.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("progress_count");
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to get task progress for player " + playerUuid + ": " + e.getMessage());
        }

        return 0;
    }

    public void incrementProgress(UUID playerUuid, String taskKey, int amount) {
        String sql = """
                INSERT INTO player_task_progress (player_uuid, task_key, progress_count)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, task_key) DO UPDATE SET progress_count = progress_count + ?;
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, taskKey.toUpperCase());
            ps.setInt(3, amount);
            ps.setInt(4, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to increment task progress for player " + playerUuid + ": " + e.getMessage());
        }
    }

    public void decrementProgress(UUID playerUuid, String taskKey, int amount) {
        String sql = """
                UPDATE player_task_progress
                SET progress_count = MAX(0, progress_count - ?)
                WHERE player_uuid = ? AND task_key = ?;
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, playerUuid.toString());
            ps.setString(3, taskKey.toUpperCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to decrement task progress for player " + playerUuid + ": " + e.getMessage());
        }
    }
}
