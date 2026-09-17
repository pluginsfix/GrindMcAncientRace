package pluginsfix.grindmcancientrace.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pluginsfix.grindmcancientrace.config.PluginConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;

    public Database(File dataFolder, PluginConfig config) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, config.databaseFileName());
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(config.databasePoolSize());
        hikariConfig.setConnectionTimeout(config.databaseTimeoutMs());
        hikariConfig.setPoolName("AncientRacePool");

        this.dataSource = new HikariDataSource(hikariConfig);
        initTables();
    }

    private void initTables() {
        String createTradesTable = """
                CREATE TABLE IF NOT EXISTS ancient_villager_trades (
                    villager_uuid TEXT NOT NULL,
                    profession TEXT NOT NULL,
                    slot_index INTEGER NOT NULL,
                    reward_id TEXT NOT NULL,
                    price_id TEXT NOT NULL,
                    PRIMARY KEY (villager_uuid, slot_index)
                );
                """;

        String createEffectsTable = """
                CREATE TABLE IF NOT EXISTS player_active_effects (
                    player_uuid TEXT NOT NULL,
                    effect_type TEXT NOT NULL,
                    amplifier INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, effect_type)
                );
                """;

        String createTaskProgressTable = """
                CREATE TABLE IF NOT EXISTS player_task_progress (
                    player_uuid TEXT NOT NULL,
                    task_key TEXT NOT NULL,
                    progress_count INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, task_key)
                );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTradesTable);
            stmt.execute(createEffectsTable);
            stmt.execute(createTaskProgressTable);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
