import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportService {

    // 1️⃣ Portfolio Summary per Account
    public static void generatePortfolioSummary(ConcurrentHashMap<Long, Accounts> accountsStore, String filePath) {
        Path path = Path.of(filePath);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Portfolio Summary per Account ===\n");

        accountsStore.values().forEach(account -> {
            sb.append("Account: ").append(account.getAccountId())
                    .append(" - ").append(account.getAccountName()).append("\n");
            account.getPositions().forEach((symbol, qty) -> {
                sb.append("  Symbol: ").append(symbol).append(", Quantity: ").append(qty).append("\n");
            });
        });

        try {
            Files.writeString(path, sb.toString());
            System.out.println("Portfolio summary report generated at: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 2️⃣ Top 5 Users by Portfolio Value
    public static void top5UsersByPortfolioValue(ConcurrentHashMap<Long, Accounts> accountsStore, String filePath) {
        Path path = Path.of(filePath);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Top 5 Users by Portfolio Value ===\n");

        List<Map.Entry<Accounts, Double>> sortedAccounts = accountsStore.values().stream()
                .map(account -> {
                    double value = account.getTrades().stream()
                            .collect(Collectors.groupingBy(
                                    Trade::getSymbol,
                                    Collectors.summingDouble(t -> t.getQuantity() * t.getPrice())
                            ))
                            .values().stream().mapToDouble(Double::doubleValue).sum();
                    return Map.entry(account, value);
                })
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList();

        for (Map.Entry<Accounts, Double> entry : sortedAccounts) {
            sb.append("Account: ").append(entry.getKey().getAccountId())
                    .append(", Value: ").append(entry.getValue()).append("\n");
        }

        try {
            Files.writeString(path, sb.toString());
            System.out.println("Top 5 users report generated at: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3️⃣ Total Exposure per Symbol
    public static void totalExposurePerSymbol(ConcurrentHashMap<Long, Accounts> accountsStore, String filePath) {
        Path path = Path.of(filePath);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Total Exposure per Symbol ===\n");

        Map<String, Integer> exposure = accountsStore.values().stream()
                .flatMap(a -> a.getPositions().entrySet().stream())
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Integer::sum
                ));

        exposure.forEach((symbol, qty) -> {
            sb.append("Symbol: ").append(symbol).append(", Total Qty: ").append(qty).append("\n");
        });

        try {
            Files.writeString(path, sb.toString());
            System.out.println("Total exposure report generated at: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 4️⃣ Group Trades by User
    public static void groupTradesByUser(ConcurrentHashMap<Long, Accounts> accountsStore, String filePath) {
        Path path = Path.of(filePath);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Trades Grouped by User ===\n");

        Map<Long, List<Trade>> tradesByUser = accountsStore.values().stream()
                .collect(Collectors.toConcurrentMap(
                        Accounts::getAccountId,
                        Accounts::getTrades
                ));

        tradesByUser.forEach((accountId, trades) -> {
            sb.append("Account: ").append(accountId)
                    .append(", Trades Count: ").append(trades.size()).append("\n");
            for (Trade t : trades) {
                sb.append("  TradeId: ").append(t.tradeId)
                        .append(", Symbol: ").append(t.symbol)
                        .append(", Qty: ").append(t.quantity)
                        .append(", Side: ").append(t.side).append("\n");
            }
        });

        try {
            Files.writeString(path, sb.toString());
            System.out.println("Trades grouped by user report generated at: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper: generate all reports
    public static void generateAllReports(ConcurrentHashMap<Long, Accounts> accountsStore) {
        generatePortfolioSummary(accountsStore, "portfolio_summary.txt");
        top5UsersByPortfolioValue(accountsStore, "top5_users.txt");
        totalExposurePerSymbol(accountsStore, "total_exposure.txt");
        groupTradesByUser(accountsStore, "trades_by_user.txt");
    }
}