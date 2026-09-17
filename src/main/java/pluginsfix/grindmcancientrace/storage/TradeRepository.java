package pluginsfix.grindmcancientrace.storage;

import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.config.ProfessionConfig;
import pluginsfix.grindmcancientrace.domain.PriceRequirement;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.TradeReward;
import pluginsfix.grindmcancientrace.domain.VillagerTrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class TradeRepository {
    private final Database database;
    private final PluginConfig config;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, List<VillagerTrade>> cache;

    public TradeRepository(Database database, PluginConfig config, Logger logger) {
        this.database = database;
        this.config = config;
        this.logger = logger;
        this.cache = new ConcurrentHashMap<>();
    }

    public Optional<List<VillagerTrade>> getTrades(UUID villagerUuid, Profession profession) {
        List<VillagerTrade> cached = cache.get(villagerUuid);
        if (cached != null) {
            return Optional.of(cached);
        }

        List<VillagerTrade> loaded = loadFromDatabase(villagerUuid, profession);
        if (!loaded.isEmpty()) {
            cache.put(villagerUuid, loaded);
            return Optional.of(loaded);
        }

        return Optional.empty();
    }

    public void saveTrades(UUID villagerUuid, Profession profession, List<VillagerTrade> trades) {
        cache.put(villagerUuid, trades);

        String sql = """
                INSERT OR REPLACE INTO ancient_villager_trades (villager_uuid, profession, slot_index, reward_id, price_id)
                VALUES (?, ?, ?, ?, ?);
                """;

        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (VillagerTrade trade : trades) {
                ps.setString(1, villagerUuid.toString());
                ps.setString(2, profession.key());
                ps.setInt(3, trade.slotIndex());
                ps.setString(4, trade.reward().id());
                ps.setString(5, trade.price().id());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            logger.warning("Failed to save trades for villager " + villagerUuid + ": " + e.getMessage());
        }
    }

    private List<VillagerTrade> loadFromDatabase(UUID villagerUuid, Profession profession) {
        Optional<ProfessionConfig> profConfigOpt = config.getProfessionConfig(profession);
        if (profConfigOpt.isEmpty()) {
            return List.of();
        }

        ProfessionConfig profConfig = profConfigOpt.get();
        String sql = """
                SELECT slot_index, reward_id, price_id
                FROM ancient_villager_trades
                WHERE villager_uuid = ?
                ORDER BY slot_index ASC;
                """;

        List<VillagerTrade> trades = new ArrayList<>();
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, villagerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int slotIndex = rs.getInt("slot_index");
                    String rewardId = rs.getString("reward_id");
                    String priceId = rs.getString("price_id");

                    Optional<TradeReward> rewardOpt = profConfig.rewardsPool().stream()
                            .filter(r -> r.id().equalsIgnoreCase(rewardId))
                            .findFirst();

                    Optional<PriceRequirement> priceOpt = profConfig.pricesPool().stream()
                            .filter(p -> p.id().equalsIgnoreCase(priceId))
                            .findFirst();

                    if (rewardOpt.isPresent() && priceOpt.isPresent()) {
                        trades.add(new VillagerTrade(slotIndex, rewardOpt.get(), priceOpt.get()));
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to load trades for villager " + villagerUuid + ": " + e.getMessage());
        }

        return Collections.unmodifiableList(trades);
    }

    public void removeVillager(UUID villagerUuid) {
        cache.remove(villagerUuid);
        String sql = "DELETE FROM ancient_villager_trades WHERE villager_uuid = ?;";
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, villagerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to delete trades for villager " + villagerUuid + ": " + e.getMessage());
        }
    }
}
