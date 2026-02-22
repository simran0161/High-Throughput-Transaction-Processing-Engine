import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;

class Trade {
    int tradeId;
    long accountId;
    String symbol;
    int quantity;
    double price;
    String side;
    LocalDateTime timestamp;

    public Trade(int tradeId, long accountId, String symbol, int quantity, double price, String side, LocalDateTime timestamp) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Only positive quantity is allowed");
        }
        if (!side.equalsIgnoreCase("BUY") && !side.equalsIgnoreCase("SELL")) {
            throw new IllegalArgumentException("Invalid trade side");
        }
        this.tradeId = tradeId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side.toUpperCase();
        this.timestamp = timestamp;
    }

    public double getPrice() { return price; }
    public long getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public String getSide() { return side; }
}

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Path path = Path.of("trades.csv");
        List<String> content = Files.readAllLines(path);

        // Optional: print all lines
//        for (String line : content) {
//            System.out.println(line);
//        }

        List<Trade> trades = new ArrayList<>();

        // Parse trades from CSV (skip header)
        for (int i = 1; i < content.size(); i++) {
            String[] line = content.get(i).split(",");
            Trade newTrade = new Trade(
                    Integer.parseInt(line[0]),
                    Long.parseLong(line[1]),
                    line[2],
                    Integer.parseInt(line[3]),
                    Double.parseDouble(line[4]),
                    line[5],
                    LocalDateTime.parse(line[6]) // ISO format parser handles 'T'
            );
            trades.add(newTrade);
        }

        // Concurrent portfolio
        ConcurrentHashMap<Long, Accounts> accountsStore = new ConcurrentHashMap<>();
        // Thread pool
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        for (Trade trade : trades) {
            executor.submit(() -> {
                try {

                    Accounts account = accountsStore.computeIfAbsent(
                            trade.getAccountId(),
                            id -> new Accounts(id, "User-" + id)
                    );

                    // Add trade to account
                    int signedQty = trade.getSide().equals("BUY")
                            ? trade.getQuantity()
                            : -trade.getQuantity();

                    account.getPositions()
                            .compute(trade.getSymbol(), (sym, oldQty) -> {

                                int current = (oldQty == null) ? 0 : oldQty;
                                int newQty = current + signedQty;

                                if (newQty < 0) {
                                    throw new IllegalStateException(
                                            "Insufficient position to sell " + sym +
                                                    " for account " + account.getAccountId());
                                }

                                return newQty;
                            });

                    // Only add trade if position update successful
                    account.getTrades().add(trade);

                } catch (Exception e) {
                    System.err.println("Trade processing failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Persist accounts, positions, trades
        for (Accounts account : accountsStore.values()) {
            DatabaseService.saveAccount(account);
            DatabaseService.savePositions(account.getPositions(), account.getAccountId());
            DatabaseService.saveTrades(account.getTrades());
        }

        ReportService.generateAllReports(accountsStore);
    }
}