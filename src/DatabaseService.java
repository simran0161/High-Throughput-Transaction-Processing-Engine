import java.sql.*;
import java.util.*;

public class DatabaseService {

    // Insert account if it does not exist
    public static void saveAccount(Accounts account) {
        String sql = "INSERT INTO accounts(account_id, account_name) VALUES (?, ?) ON CONFLICT (account_id) DO NOTHING";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, account.getAccountId());
            ps.setString(2, account.getAccountName());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Insert trades
    public static void saveTrades(List<Trade> trades) {
        String sql = "INSERT INTO trades(trade_id, account_id, symbol, quantity, price, side, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (trade_id) DO NOTHING";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Trade t : trades) {
                ps.setInt(1, t.tradeId);
                ps.setLong(2, t.accountId);
                ps.setString(3, t.symbol);
                ps.setInt(4, t.quantity);
                ps.setDouble(5, t.price);
                ps.setString(6, t.side);
                ps.setTimestamp(7, Timestamp.valueOf(t.timestamp));
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Insert positions
    public static void savePositions(Map<String, Integer> positions, long accountId) {
        String sql = "INSERT INTO positions(account_id, symbol, quantity) VALUES (?, ?, ?) ON CONFLICT (account_id, symbol) DO UPDATE SET quantity = EXCLUDED.quantity";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Integer> entry : positions.entrySet()) {
                ps.setLong(1, accountId);
                ps.setString(2, entry.getKey());
                ps.setInt(3, entry.getValue());
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}