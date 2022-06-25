package io.github.divios.lib.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.divios.core_lib.database.DataManagerAbstract;
import io.github.divios.core_lib.database.SQLiteConnector;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.scheduler.Schedulers;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.files.Settings;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dTransaction.Transactions;
import io.github.divios.lib.dLib.registry.RecordBookEntry;
import io.github.divios.lib.dLib.shop.ShopAccount;
import io.github.divios.lib.dLib.shop.dShop;
import io.github.divios.lib.dLib.shop.view.ShopView;
import io.github.divios.lib.dLib.shop.view.ShopViewFactory;
import io.github.divios.lib.storage.migrations.initialMigration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class databaseManager extends DataManagerAbstract {

    private static final DailyShop plugin = DailyShop.get();

    protected final ExecutorService asyncPool = Executors.newSingleThreadExecutor();

    public databaseManager() {
        super(new SQLiteConnector(plugin));
        super.databaseConnector.connect(initialMigration::migrate);

        Schedulers.sync().runRepeating(() -> asyncPool.execute(this::dropOldLogEntries),
                15, TimeUnit.SECONDS, 30, TimeUnit.MINUTES);
    }

    private static final JsonParser parser = new JsonParser();

    public Collection<dShop> getShops() {

        Map<String, dShop> shops = new HashMap<>();

        this.databaseConnector.connect(connection -> {
            try (Statement statement = connection.createStatement()) {

                String query = "SELECT shop_id, gui_serial, shop_timer, shop_timestamp, " +
                        "account_serial, item_serial " +
                        "FROM Shops " +
                        "NATURAL JOIN Guis " +
                        "NATURAL JOIN Accounts " +
                        "NATURAL JOIN Items";

                ResultSet rs = statement.executeQuery(query);

                while (rs.next()) {
                    String shop_name = rs.getString(1);

                    if (!shops.containsKey(shop_name)) {
                        JsonElement gui_serial = parser.parse(rs.getString(2));
                        int timer = rs.getInt(3);
                        Timestamp timestamp = rs.getTimestamp(4);
                        JsonElement account = parser.parse(rs.getString(5));

                        dShop newShop = new dShop(shop_name, gui_serial, timestamp, timer);
                        newShop.setAccount(ShopAccount.fromJson(account));

                        shops.put(shop_name, newShop);

                    } else {

                        try {
                            JsonElement json = parser.parse(rs.getString(6));
                            shops.get(shop_name).addItem(dItem.fromJson(json));

                        } catch (IllegalStateException | JsonSyntaxException ignored) {
                        }
                    }

                }

            }
        });

        return shops.values();
    }

    public CompletableFuture<Collection<dShop>> getShopsAsync() {
        return CompletableFuture.supplyAsync(this::getShops);
    }


    public Set<dItem> getShopItems(String shop_name) {
        Set<dItem> items = new LinkedHashSet<>();

        this.databaseConnector.connect(connection -> {
            String selectFarms = "SELECT item_serial " +
                    "FROM Items " +
                    "WHERE shop_name = ?";

            try (PreparedStatement statement = connection.prepareStatement(selectFarms)) {
                statement.setString(1, shop_name);

                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    try {
                        JsonElement json = parser.parse(result.getString(1));
                        items.add(dItem.fromJson(json));
                    } catch (IllegalStateException | JsonSyntaxException ignored) {
                    }
                }
            }
        });

        return items;
    }

    public CompletableFuture<Set<dItem>> getShopItemsAsync(String name) {
        return CompletableFuture.supplyAsync(() -> getShopItems(name));

    }

    public void createShop(dShop shop) {
        this.databaseConnector.connect(connection -> {

            String createShop = "INSERT OR REPLACE INTO Shops (shop_id, shop_timer, shop_timestamp) " +
                    "VALUES (?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(createShop)) {
                statement.setString(1, shop.getName());
                statement.setInt(2, shop.getTimer());
                statement.setTimestamp(3, shop.getTimestamp());

                statement.executeUpdate();
            }

            addItems(shop.getName(), shop.getItems());
            updateGui(shop.getName(), shop.getView());
            if (shop.getAccount() != null) updateAccount(shop.getName(), shop.getAccount());

        });
    }

    public Future<?> createShopAsync(dShop shop) {
        return asyncPool.submit(() -> createShop(shop));
    }

    public void renameShop(String oldName, String newName) {
        this.databaseConnector.connect(connection -> {
            String renameShop = "UPDATE Shops " +
                    "SET name = ? WHERE name = ? collate nocase";
            try (PreparedStatement statement = connection.prepareStatement(renameShop)) {
                statement.setString(1, newName);
                statement.setString(2, oldName);
                statement.executeUpdate();
            }

            String renameTable = "ALTER TABLE " + this.getTablePrefix() + "shop_" + oldName +
                    " RENAME TO " + this.getTablePrefix() + "shop_" + newName;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(renameTable);
            }
        });
    }

    public Future<?> renameShopAsync(String oldName, String newName) {
        return asyncPool.submit(() -> renameShop(oldName, newName));
    }

    public void deleteShop(String name) {
        this.databaseConnector.connect(connection -> {
            String deleteShop = "DELETE FROM Shops WHERE shop_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(deleteShop)) {
                statement.setString(1, name);

                statement.executeUpdate();
            }
        });
    }

    public Future<?> deleteShopAsync(String name) {
        return asyncPool.submit(() -> deleteShop(name));
    }

    public void addItem(String shop_name, dItem item) {
        this.databaseConnector.connect(connection -> {

            String createShop = "INSERT OR REPLACE INTO Items (item_uuid, item_serial, shop_id) " +
                    "VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(createShop)) {

                statement.setString(1, item.getID());
                statement.setString(2, item.toJson().toString());
                statement.setString(3, shop_name);

                statement.executeUpdate();
            }
        });
    }

    public void addItems(String shop_name, Collection<dItem> items) {
        this.databaseConnector.connect(connection -> {

            String addItem = "REPLACE INTO Items (item_uuid, item_serial, shop_id) " +
                    "VALUES (?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(addItem)) {
                for (dItem item : items) {
                    statement.clearParameters();

                    statement.setString(1, item.getID());
                    statement.setString(2, item.toJson().toString());
                    statement.setString(3, shop_name);

                    statement.addBatch();
                }

                statement.executeBatch();
            }

        });
    }

    public Future<?> addItemAsync(String shop_name, dItem item) {
        return asyncPool.submit(() -> addItem(shop_name, item));
    }

    public Future<?> addItemsAsync(String shop_name, Collection<dItem> items) {
        return asyncPool.submit(() -> addItems(shop_name, items));
    }

    public void deleteItem(String shop_name, UUID uid) {
        this.databaseConnector.connect(connection -> {
            String deleteItem = "DELETE FROM Items WHERE item_uuid = ?" +
                    " AND shop_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(deleteItem)) {
                statement.setString(1, uid.toString());
                statement.setString(2, shop_name);

                statement.executeUpdate();
            }
        });
    }

    public Future<?> deleteItemAsync(String shop_name, UUID uid) {
        return asyncPool.submit(() -> deleteItem(shop_name, uid));
    }

    public void deleteAllItems(String shop_name) {
        this.databaseConnector.connect(connection -> {
            String query = "DELETE FROM Items WHERE shop_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, shop_name);
                statement.executeUpdate();
            }
        });
    }

    public Future<?> deleteAllItemsAsync(String shop_name) {
        return asyncPool.submit(() -> deleteAllItems(shop_name));
    }

    public void updateItem(String shop_name, dItem item) {
        this.databaseConnector.connect(connection -> {
            String updateItem = "REPLACE INTO Items (item_uuid, item_serial) " +
                    "VALUES(?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(updateItem)) {
                statement.setString(1, item.toJson().toString());
                statement.setString(2, item.getUUID().toString());

                statement.executeUpdate();
            }
        });
    }

    public Future<?> updateItemAsync(String shop_name, dItem item) {
        return asyncPool.submit(() -> updateItem(shop_name, item));
    }

    public void updateGui(String shop_name, ShopView gui) {
        this.databaseConnector.connect(connection -> {
            String updateGui = "REPLACE INTO Guis (gui_serial, shop_id)" +
                    "VALUES(?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(updateGui)) {
                statement.setString(1, ShopViewFactory.toJson(gui).toString());
                statement.setString(2, shop_name);

                statement.executeUpdate();
            }
        });
    }

    public Future<?> updateGuiAsync(String shop_name, ShopView gui) {
        return asyncPool.submit(() -> updateGui(shop_name, gui));
    }

    public void updateAccount(String shop_name, ShopAccount account) {
        if (account == null) return;

        this.databaseConnector.connect(connection -> {
            String updateGui = "REPLACE INTO Accounts (account_serial, shop_id) " +
                    "VALUES(?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(updateGui)) {
                statement.setString(1, account.toJson().toString());
                statement.setString(2, shop_name);
                statement.executeUpdate();
            }
        });
    }

    public Future<?> updateAccountAsync(String shop_name, ShopAccount account) {
        return asyncPool.submit(() -> updateAccount(shop_name, account));
    }

    public void removeAccount(String shop_name) {
        this.databaseConnector.connect(connection -> {
            String updateGui = "DELETE FROM Accounts WHERE shop_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(updateGui)) {
                statement.setString(1, shop_name);
                statement.executeUpdate();
            }

        });
    }

    public Future<?> removeAccountAsync(String shop_name) {
        return asyncPool.submit(() -> removeAccount(shop_name));
    }

    public void updateTimeStamp(String shop_name, Timestamp timestamp) {
        this.databaseConnector.connect(connection -> {
            String updateTimeStamp = "UPDATE Shops " +
                    "SET shop_timestamp = ? WHERE shop_id = ? collate nocase";

            try (PreparedStatement statement = connection.prepareStatement(updateTimeStamp)) {
                statement.setDate(1, new java.sql.Date(timestamp.getTime()));
                statement.setString(2, shop_name);

                statement.executeUpdate();
            }
        });
    }

    public Future<?> updateTimeStampAsync(String shop_name, Timestamp timestamp) {
        return asyncPool.submit(() -> updateTimeStamp(shop_name, timestamp));
    }

    public void updateTimer(String shop_name, int timer) {
        this.databaseConnector.connect(connection -> {
            String updateTimeStamp = "UPDATE Shops " +
                    "SET shop_timer = ? WHERE shop_id = ? collate nocase";

            try (PreparedStatement statement = connection.prepareStatement(updateTimeStamp)) {
                statement.setInt(1, timer);
                statement.setString(2, shop_name);

                statement.executeUpdate();
            }
        });
    }

    public Future<?> updateTimerAsync(String shop_name, int timer) {
        return asyncPool.submit(() -> updateTimer(shop_name, timer));
    }

    public void addLogEntry(RecordBookEntry entry) {
        this.databaseConnector.connect(connection -> {

            String createShop = "INSERT INTO " + this.getTablePrefix() +
                    "log" + " (player, shopID, itemUUID, rawItem, type, price, quantity, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(createShop)) {

                statement.setString(1, entry.getPlayer());
                statement.setString(2, entry.getShopID());
                statement.setString(3, entry.getItemID());
                statement.setString(4, ItemUtils.serialize(entry.getRawItem()));
                statement.setString(5, entry.getType().name());
                statement.setDouble(6, entry.getPrice());
                statement.setInt(7, entry.getQuantity());
                statement.setString(8, new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(entry.getTimestamp()));
                statement.executeUpdate();
            }
        });
    }

    public Future<?> addLogEntryAsync(RecordBookEntry entry) {
        return asyncPool.submit(() -> addLogEntry(entry));
    }

    public Collection<RecordBookEntry> getLogEntries(int limit) {

        Deque<RecordBookEntry> entries = new ArrayDeque<>();
        this.databaseConnector.connect(connection -> {

            try (Statement statement = connection.createStatement()) {
                String getLogs = "SELECT * FROM " + this.getTablePrefix() + "log" + " LIMIT " + limit;
                ResultSet result = statement.executeQuery(getLogs);

                while (result.next()) {

                    Date timestamp = null;

                    try {
                        timestamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
                                .parse(result.getString("timestamp"));

                        RecordBookEntry entry = RecordBookEntry.createEntry()
                                .withPlayer(result.getString("player"))
                                .withShopID(result.getString("shopID"))
                                .withItemID(result.getString("itemUUID"))
                                .withRawItem(ItemUtils.deserialize(result.getString("rawItem")))
                                .withType(Transactions.Type.valueOf(result.getString("type").toUpperCase()))
                                .withPrice(result.getDouble("price"))
                                .withQuantity(result.getInt("quantity"))
                                .withTimestamp(timestamp == null ? new Timestamp(System.currentTimeMillis()) : timestamp)
                                .create();

                        entries.push(entry);

                    } catch (Exception ignored) {
                    }
                }
            }
        });
        return entries;
    }

    public CompletableFuture<Collection<RecordBookEntry>> getLogEntriesAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> getLogEntries(limit));
    }

    public void finishAsyncQueries() {
        asyncPool.shutdown();
        try {
            asyncPool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public void dropOldLogEntries() {
        int days = Settings.LOGS_REMOVED.getValue().getAsIntOrDefault(20);
        long time = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        Date date = new Date(time);

        getLogEntries(Integer.MAX_VALUE).forEach(entry -> {
            if (entry.getTimestamp().before(date))
                deleteEntry(entry);
        });
    }

    private void deleteEntry(RecordBookEntry entry) {
        this.databaseConnector.connect(connection -> {
            String updateTimeStamp = "DELETE FROM " + this.getTablePrefix() + "log" +
                    " WHERE timestamp = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateTimeStamp)) {
                statement.setString(1, FORMAT.format(entry.getTimestamp()));

                statement.executeUpdate();
            }
        });
    }

}
