import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class Accounts {

    private long accountId;
    private String accountName;

    // symbol → quantity
    private ConcurrentHashMap<String, Integer> positions = new ConcurrentHashMap<>();

    // list of trades
    private List<Trade> trades = Collections.synchronizedList(new ArrayList<>());

    public Accounts(long accountId, String accountName) {
        this.accountId = accountId;
        this.accountName = accountName;
    }

    public long getAccountId() { return accountId; }
    public String getAccountName() { return accountName; }

    public ConcurrentHashMap<String, Integer> getPositions() {
        return positions;
    }

    public List<Trade> getTrades() {
        return trades;
    }
}
