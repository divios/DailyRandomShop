package io.github.divios.lib.dLib;


import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import io.github.divios.core_lib.cache.Lazy;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.Pair;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.economies.economy;
import io.github.divios.dailyShop.economies.vault;
import io.github.divios.dailyShop.files.Settings;
import io.github.divios.dailyShop.lorestategy.loreStrategy;
import io.github.divios.dailyShop.utils.MMOUtils;
import io.github.divios.dailyShop.utils.Utils;
import io.github.divios.lib.dLib.stock.dStock;
import io.github.divios.lib.dLib.stock.factory.dStockFactory;
import io.github.divios.lib.serialize.adapters.dItemAdapter;
import io.github.divios.lib.serialize.jsonSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "deprecation", "unused"})
public class dItem implements Serializable, Cloneable {

    private static final long serialVersionUID = 6529685098267757690L;  // Avoid problems with serialization
    private static final DailyShop plugin = DailyShop.get();

    private HashMap<String, LazyWrapper<?>> cache = new HashMap<>();

    private NBTItem item;
    private dStock stock = null;
    private Lazy<ItemStack> rawItem;

    public static dItem of(ItemStack item) {
        return new dItem(item);
    }

    public static dItem of(ItemStack item, String id) {
        return new dItem(item, id);
    }

    public static dItem of(ItemStack item, String id, int slot) {
        return new dItem(item, id, slot);
    }

    public dItem(@NotNull ItemStack item) {
        this(item, -1);
    }

    public dItem(@NotNull ItemStack item, int slot) {
        this(item, UUID.randomUUID().toString(), slot);
    }

    public dItem(ItemStack item, String id) {
        this(item, id, -1);
    }

    public dItem(ItemStack item, String id, int slot) {
        setItem(item, id, slot);
        initializeCache();
    }

    private dItem() {
    }

    private void initializeCache() {
        if (cache == null) cache = new HashMap<>();    //   ReadObject java bullshit
        cache.put("slot", LazyWrapper.suppliedBy(() -> item.getInteger("dailySlots")));
        cache.put("lore", LazyWrapper.suppliedBy(() -> ItemUtils.getLore(getDailyItem())));
        cache.put("material", LazyWrapper.suppliedBy(() -> getDailyItem().getType()));
        cache.put("durability", LazyWrapper.suppliedBy(() -> getDailyItem().getDurability()));
        cache.put("enchantments", LazyWrapper.suppliedBy(() -> getDailyItem().getEnchantments()));
        cache.put("sellPrice", LazyWrapper.suppliedBy(() -> item.getObject("rds_sellPrice", dPrice.class)));
        cache.put("buyPrice", LazyWrapper.suppliedBy(() -> item.getObject("rds_buyPrice", dPrice.class)));
        cache.put("id", LazyWrapper.suppliedBy(() -> item.getString("rds_UUID")));
        cache.put("rarity", LazyWrapper.suppliedBy(() -> item.getObject("rds_rarity", dRarity.class)));
        cache.put("economy", LazyWrapper.suppliedBy(() -> {
            economy[] econ = {new vault()};
            if (item.hasKey("rds_econ")) {
                econ[0] = economy.deserialize(item.getString("rds_econ"));
                Utils.tryCatchAbstraction(() -> econ[0].test(), e -> econ[0] = new vault());
                try {
                    econ[0].test();
                } catch (NoClassDefFoundError e) {
                    econ[0] = new vault();
                }
            }
            return econ[0];
        }));
        cache.put("commands", LazyWrapper.suppliedBy(() -> item.getObject("rds_cmds", List.class)));
        cache.put("buyPerms", LazyWrapper.suppliedBy(() -> item.getObject("rds_perms_buy", List.class)));
        cache.put("sellPerms", LazyWrapper.suppliedBy(() -> item.getObject("rds_perms_sell", List.class)));
        cache.put("action", LazyWrapper.suppliedBy(() -> item.hasKey("rds_action") ?
                Pair.deserialize(item.getString("rds_action"), dAction.class, String.class) :
                Pair.of(dAction.EMPTY, "")));
        cache.put("set", LazyWrapper.suppliedBy(() -> item.hasKey("rds_setItems") ? item.getInteger("rds_setItems") : null));
        cache.put("confirmGui", LazyWrapper.suppliedBy(() -> item.getBoolean("rds_confirm_gui")));
        cache.put("bundle", LazyWrapper.suppliedBy(() -> item.getObject("rds_bundle", List.class)));
    }

    /**
     * @return the ItemStack that this instance holds
     */
    public ItemStack getDailyItem() {
        saveStock();
        return item.getItem();
    }

    private void setRawItem(@NotNull ItemStack rawItem) {
        item.setString("rds_rawItem", ItemUtils.serialize(rawItem));
        this.rawItem = Lazy.suppliedBy(() -> ItemUtils.deserialize(item.getString("rds_rawItem")));
    }

    public ItemStack getRealItem() {
        return getRealItem(false);
    }

    /**
     * Gets the raw item, this is, the item's held
     * by this instance without all the daily metadata
     *
     * @return s
     */
    public ItemStack getRealItem(boolean getAsNewItem) {

        if (getAsNewItem && MMOUtils.isMMOItemsOn() && MMOUtils.isMMOItem(rawItem.get().clone())) {
            try {
                return MMOUtils.createNewMMOItemFromExisting(rawItem.get().clone());
            } catch (Exception e) {
                return rawItem.get();
            }
        }

        return rawItem.get();
    }

    /**
     * @param item the new item to be held by this instance
     */
    public void setItem(@NotNull ItemStack item, String id, int slot) {
        this.item = new NBTItem(item);
        initializeCache();
        if (getID() == null || getID().isEmpty()) {
            setRawItem(item);
            setID(id);
            setSlot(slot);
            setRarity(new dRarity());       //Defaults to Common
            setConfirm_gui(true);           // Defaults true
            setEconomy(new vault());        // Default Vault
            setBuyPrice(Settings.DEFAULT_BUY.getValue().getAsDouble()); // Default buy price
            setSellPrice(Settings.DEFAULT_SELL.getValue().getAsDouble()); // Default sell price
            if (getQuantity() > 1) setSetItems(getQuantity());   // Initialize quantity
        }

        rawItem = Lazy.suppliedBy(() -> ItemUtils.deserialize(this.item.getString("rds_rawItem")));
        migratePerms();
        stock = retrieveStock();
    }

    public void setItem(@NotNull ItemStack item) {
        setItem(item, null, -1);
    }

    /**
     * Private method to transfer all daily item meta
     *
     * @param item s
     */
    private ItemStack copyAllMetadata(@NotNull ItemStack item) {
        dItem transfer = dItem.of(item);
        transfer.setID(getID());
        transfer.setDisplayName(getDisplayName());
        transfer.setLore(getLore());
        getAction().stream(transfer::setAction);
        transfer.setStock(getStock());
        transfer.setSetItems(getSetItems().get());
        transfer.setQuantity(getQuantity());
        transfer.setBundle(getBundle().get());
        transfer.setBuyPrice(getBuyPrice().get());
        transfer.setSellPrice(getSellPrice().get());
        transfer.setEconomy(getEconomy());
        transfer.setDurability(getDurability(), false);
        transfer.setRarity(getRarity());
        transfer.setPermsBuy(getPermsBuy().get());
        transfer.setPermsSell(getPermsSell().get());
        transfer.setConfirm_gui(isConfirmGuiEnabled());

        return transfer.getDailyItem();
    }

    /**
     * Sets the slot of this item
     *
     * @param slot
     * @return
     */
    public dItem setSlot(int slot) {
        item.setInteger("dailySlots", slot);
        cache.get("slot").reset();
        return this;
    }

    /**
     * Gets the slot of this item
     *
     * @return
     */
    public int getSlot() {
        return (int) cache.get("slot").get();
    }

    public dItem setMultipleSlots(List<Integer> slots) {
        item.setObject("slots", slots);
        return this;
    }

    public List<Integer> getMultipleSlots() {
        if (!isMultipleSlots()) return null;
        return item.getObject("slots", List.class);
    }

    public boolean isMultipleSlots() {
        return item.hasKey("slots");
    }

    /**
     * Sets the meta of the item
     *
     * @param meta
     * @return
     */
    public dItem setMeta(ItemMeta meta) {
        ItemStack itemA = getDailyItem();
        itemA.setItemMeta(meta);
        setItem(itemA);

        ItemStack itemB = getRealItem();
        itemA.setItemMeta(meta);
        setRawItem(itemB);

        return this;
    }

    public dItem setSpawnerType(EntityType type) {
        if (!isSpawner()) return this;

        item.setString("ms_mob", type.getName());  // MineableSpawner

        NBTCompound compound = item.getOrCreateCompound("SilkSpawners");  // SilkSpawner
        compound.setString("entity", type.getName());

        compound = item.getOrCreateCompound("BlockEntityTag");  // Vanilla
        compound.setString("EntityId", type.getName());

        NBTCompound subCompound = compound.getOrCreateCompound("EntityTag");
        subCompound.setString("id", "minecraft:" + type.getName());

        subCompound = compound.getOrCreateCompound("SpawnData");
        subCompound.setString("id", "minecraft:" + type.getName());

        compound.getOrCreateCompound("SpawnPotentials");

        compound.setString("id", "mob_spawner");

        // RawItem
        NBTItem rawItem = new NBTItem(getRealItem());

        compound = rawItem.getOrCreateCompound("SilkSpawners");  // SilkSpawner
        compound.setString("entity", type.getName());

        compound = rawItem.getOrCreateCompound("BlockEntityTag");  // Vanilla
        compound.setString("EntityId", type.getName());

        subCompound = compound.getOrCreateCompound("EntityTag");
        subCompound.setString("id", "minecraft:" + type.getName());

        subCompound = compound.getOrCreateCompound("SpawnData");
        subCompound.setString("id", "minecraft:" + type.getName());

        compound.getOrCreateCompound("SpawnPotentials");

        compound.setString("id", "mob_spawner");

        setRawItem(rawItem.getItem());

        return this;
    }

    public EntityType getSpawnerType() {
        if (!isSpawner()) return null;

        return EntityType.fromName(item.getCompound("BlockEntityTag").getString("EntityId"));
    }

    public boolean isSpawner() {
        return getMaterial() == Material.SPAWNER;
    }

    /**
     * Sets the display name of the item
     *
     * @param name
     * @return
     */
    public dItem setDisplayName(@NotNull String name) {
        setItem(ItemUtils.setName(getDailyItem(), name));
        setRawItem(ItemUtils.setName(getRealItem(), name));
        return this;
    }

    /**
     * Gets the displayName of the item
     *
     * @return
     */
    public String getDisplayName() {
        return Utils.isEmpty(ItemUtils.getName(getDailyItem())) ?
                getDailyItem().getType().name() :
                ItemUtils.getName(getDailyItem());
    }

    /**
     * Sets the lore of the item. Supports Color Codes
     *
     * @param lore
     * @return
     */
    public dItem setLore(@NotNull List<String> lore) {
        setItem(ItemUtils.setLore(getDailyItem(), lore));
        setRawItem(ItemUtils.setLore(getRealItem(), lore));
        cache.get("lore").reset();
        return this;
    }

    public dItem applyLore(loreStrategy strategy, Object... data) {
        setItem(strategy.applyLore(item.getItem(), data));
        //setRawItem(strategy.applyLore(getRawItem(), data));
        cache.get("lore").reset();
        return this;
    }

    /**
     * Gets the lore of the item
     *
     * @return
     */
    public @NotNull
    List<String> getLore() {
        return (List<String>) cache.get("lore").get();
    }

    /**
     * Sets the material of the item
     *
     * @param m
     * @return
     */
    public dItem setMaterial(@NotNull XMaterial m) {
        setItem(ItemUtils.setMaterial(getDailyItem(), m));
        setRawItem(ItemUtils.setMaterial(getRealItem(), m));
        if (m.name().contains("GLASS"))
            setDurability(m.parseItem().getDurability(), true);
        cache.get("material").reset();
        return this;

    }

    /**
     * Gets the material of the item
     *
     * @return
     */
    public @NotNull
    Material getMaterial() {
        return (Material) cache.get("material").get();
    }

    /**
     * Set this item as a custom head with the given texture as base64
     *
     * @param s Base64 texture
     * @return the new item with the texture applied
     */

    public dItem setCustomPlayerHead(String s) {
        setMaterial(XMaterial.PLAYER_HEAD);
        setItem(ItemUtils.applyTexture(getDailyItem(), s));
        setRawItem(ItemUtils.applyTexture(getRealItem(), s));
        item.setString("rds_headUrl", s);
        return this;
    }

    /**
     * Get if the item is a player head with a custom texture applied to it.
     *
     * @return
     */
    public boolean isCustomHead() {
        return getMaterial() == Material.PLAYER_HEAD && item.hasKey("rds_headUrl");
    }

    /**
     * Get the custom texture of the item
     *
     * @return null if it is not a customHead or the url
     */
    public String getCustomHeadUrl() {
        if (!isCustomHead()) return null;
        return item.getString("rds_headUrl");
    }

    /**
     * Sets the durability of the item
     *
     * @param durability
     * @return
     */
    public dItem setDurability(short durability, boolean glass) {
        if (!glass) {
            setItem(ItemUtils.setDurability(getDailyItem(), (short) (getDailyItem().getType().getMaxDurability() - durability)));
            setRawItem(ItemUtils.setDurability(getRealItem(), (short) (getRealItem().getType().getMaxDurability() - durability)));
        } else {
            setItem(ItemUtils.setDurability(getDailyItem(), durability));
            setRawItem(ItemUtils.setDurability(getRealItem(), durability));
        }
        cache.get("durability").reset();
        return this;
    }

    /**
     * Gets the durability of the item
     *
     * @return
     */
    public short getDurability() {
        return (short) cache.get("durability").get();
    }

    /**
     * Adds enchantment to item
     *
     * @param ench
     * @return
     */
    public dItem addEnchantments(@NotNull Enchantment ench, int lvl) {
        setItem(ItemUtils.addEnchant(getDailyItem(), ench, lvl));
        setRawItem(ItemUtils.addEnchant(getRealItem(), ench, lvl));
        cache.get("enchantments").reset();
        return this;
    }

    /**
     * Removes enchantment from item
     *
     * @param ench
     * @return
     */
    public dItem removeEnchantments(@NotNull Enchantment ench) {
        setItem(ItemUtils.removeEnchant(getDailyItem(), ench));
        setRawItem(ItemUtils.removeEnchant(getRealItem(), ench));
        cache.get("enchantments").reset();
        return this;
    }

    /**
     * gets a map containing all the enchants of this item
     *
     * @return
     */
    public @NotNull
    Map<Enchantment, Integer> getEnchantments() {
        return (Map<Enchantment, Integer>) cache.get("enchantments").get();
    }

    /**
     * Sets the amount of the item
     *
     * @param amount
     * @return
     */
    public dItem setQuantity(int amount) {
        ItemStack auxI = getDailyItem();
        auxI.setAmount(amount);
        ItemStack auxE = getRealItem();
        auxE.setAmount(amount);
        setItem(auxI);
        setRawItem(auxE);
        return this;
    }

    /**
     * Gets the amount of the item
     *
     * @return
     */
    public int getQuantity() {
        return item.getItem().getAmount();
    }

    /**
     * Returns the max stack size of this item
     *
     * @return
     */
    public int getMaxStackSize() {
        return item.getItem().getMaxStackSize();
    }

    /**
     * Return if the item has a flag
     *
     * @param flag
     * @return
     */
    public boolean hasFlag(ItemFlag flag) {
        return ItemUtils.hasItemFlags(getDailyItem(), flag);
    }

    /**
     * Set the flag of an item.
     *
     * @param flag
     * @return
     */
    public dItem setFlag(ItemFlag flag) {
        setItem(ItemUtils.addItemFlags(getDailyItem(), flag));
        setRawItem(ItemUtils.addItemFlags(getRealItem(), flag));
        return this;
    }

    /**
     * Toggles a flag from the item
     *
     * @param flag
     * @return
     */
    public dItem toggleFlag(ItemFlag flag) {

        if (ItemUtils.hasItemFlags(getDailyItem(), flag)) {
            setItem(ItemUtils.removeItemFlags(getDailyItem(), flag));
            setRawItem(ItemUtils.removeItemFlags(getRealItem(), flag));
        } else {
            setItem(ItemUtils.addItemFlags(getDailyItem(), flag));
            setRawItem(ItemUtils.addItemFlags(getRealItem(), flag));
        }
        return this;
    }

    /**
     * Gets a list of all the flags this item has.
     *
     * @return
     */
    public List<ItemFlag> getAllFlags() {
        return Arrays.stream(ItemFlag.values())
                .filter(this::hasFlag)
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the item is unbreakable
     *
     * @return
     */
    public boolean isUnbreakble() {
        return getDailyItem().getItemMeta().isUnbreakable();
    }

    /**
     * Sets the item as unbreakable
     *
     * @return
     */
    public dItem setUnbreakable() {
        setItem(ItemUtils.setUnbreakable(getDailyItem()));
        setRawItem(ItemUtils.setUnbreakable(getRealItem()));
        return this;
    }

    public boolean isPotion() {
        return XPotion.canHaveEffects(getMaterial());
    }

    /**
     * Gets if the item has the desired potionEffect
     *
     * @param effect the potion effect to check
     * @return true if it has, or false if not or the item is not a potion
     */
    public boolean hasPotionEffect(PotionEffect effect) {
        return ItemUtils.hasPotionEffect(getDailyItem(), effect);
    }

    public List<PotionEffect> getAllPotionEffects() {
        return ItemUtils.getAllPotionEffects(getDailyItem());
    }

    public dItem addPotionEffect(PotionEffect... effect) {
        setItem(ItemUtils.addPotionEffects(getDailyItem(), effect));
        setRawItem(ItemUtils.addPotionEffects(getDailyItem(), effect));
        return this;
    }

    public dItem addPotionEffect(List<PotionEffect> effect) {
        setItem(ItemUtils.addPotionEffects(getDailyItem(), effect));
        setRawItem(ItemUtils.addPotionEffects(getDailyItem(), effect));
        return this;
    }

    public dItem removePotionEffect(List<PotionEffect> effect) {
        setItem(ItemUtils.removePotionEffects(getDailyItem(), effect));
        setRawItem(ItemUtils.removePotionEffects(getDailyItem(), effect));
        return this;
    }

    /**
     * @return the price of the item. Can be random price between the values asigned
     */
    public Optional<dPrice> getBuyPrice() {
        Object o;
        return Optional.ofNullable((o = cache.get("buyPrice").get()) == null ? null : (dPrice) o);
    }

    /**
     * Set the price of the item as a fixed value
     *
     * @param price Fixed price for the item
     * @return
     */
    public dItem setBuyPrice(double price) {
        item.setObject("rds_buyPrice", new dPrice(price));
        cache.get("buyPrice").reset();
        return this;
    }

    /**
     * Set the price of the item as a random value between minPrice and maxPrice
     *
     * @param minPrice lower limit price
     * @param maxPrice upper limit price
     * @return
     */
    public dItem setBuyPrice(double minPrice, double maxPrice) {
        item.setObject("rds_buyPrice", new dPrice(minPrice, maxPrice));
        cache.get("buyPrice").reset();
        return this;
    }

    /**
     * Sets the buy price with a dPrice object
     *
     * @param price
     * @return
     */
    public dItem setBuyPrice(dPrice price) {
        item.setObject("rds_buyPrice", price);
        cache.get("buyPrice").reset();
        return this;
    }

    /**
     * Generates a new price
     *
     * @return
     */
    public dItem generateNewBuyPrice() {
        getBuyPrice().ifPresent(dPrice -> {
            dPrice.generateNewPrice();
            setBuyPrice(dPrice);
        });
        cache.get("buyPrice").reset();
        return this;
    }

    /**
     * @return the price of the item. Can be random price between the values asigned
     */
    public Optional<dPrice> getSellPrice() {
        Object o;
        return Optional.ofNullable((o = cache.get("sellPrice").get()) == null ? null : (dPrice) o);
    }

    /**
     * Set the price of the item as a fixed value
     *
     * @param price Fixed price for the item
     * @return
     */
    public dItem setSellPrice(double price) {
        item.setObject("rds_sellPrice", new dPrice(price));
        cache.get("sellPrice").reset();
        return this;
    }

    /**
     * Set the price of the item as a random value between minPrice and maxPrice
     *
     * @param minPrice lower limit price
     * @param maxPrice upper limit price
     * @return
     */
    public dItem setSellPrice(double minPrice, double maxPrice) {
        cache.get("sellPrice").reset();
        item.setObject("rds_sellPrice", new dPrice(minPrice, maxPrice));
        return this;
    }

    public dItem setSellPrice(dPrice price) {
        item.setObject("rds_sellPrice", price);
        return this;
    }

    public dItem generateNewSellPrice() {
        getSellPrice().ifPresent(dPrice -> {
            dPrice.generateNewPrice();
            setSellPrice(dPrice);
        });
        cache.get("sellPrice").reset();
        return this;
    }

    public String getID() {
        return (String) cache.get("id").get();
    }

    public dItem setID(String id) {
        item.setString("rds_UUID", id);
        cache.get("id").reset();
        return this;
    }

    /**
     * Gets the uuid of the item
     *
     * @return the uuid of this item
     */
    public UUID getUid() {
        return UUID.nameUUIDFromBytes(getID().getBytes());
    }


    public static String getId(ItemStack item) {
        return new NBTItem(item).getString("rds_UUID");
    }

    @NotNull
    public static UUID getUid(ItemStack item) {
        return UUID.nameUUIDFromBytes(getId(item).getBytes());
    }

    /**
     * Set the stock of the item
     *
     * @param stock the stock to set
     * @return
     */
    public dItem setStock(@Nullable dStock stock) {
        this.stock = stock;
        saveStock();
        return this;
    }


    public boolean hasStock() {
        return stock != null;
    }

    /**
     * Returns the stock of the item
     *
     * @return returns the stock of the item. Can be null and means that
     * the feature is disabled
     */
    public dStock getStock() {
        return stock;
    }

    /**
     * Gets the deserialized stock from the item nbt
     */
    private dStock retrieveStock() {

        if (!item.hasKey("rds_stock")) return null;

        try {                                               // Convert legacy Stock
            item.getString("rds_stock");
        } catch (ClassCastException e) {
            int legacyStock = item.getInteger("rds_stock");
            item.setString("rds_stock", dStockFactory.GLOBAL(legacyStock).toBase64());
        }

        String base64 = item.getString("rds_stock");
        if (base64 == null || base64.isEmpty()) {               // Legacy stock
            return dStockFactory.GLOBAL(item.getInteger("rds_stock"));
        }

        return dStock.fromBase64(base64);
    }

    /**
     * Saves the stock as base64
     */
    private void saveStock() {
        item.setString("rds_stock", stock == null ? null : stock.toBase64());    // Check null to reset Stock
    }

    /**
     * Sets the rarity of the item
     *
     * @param rarity rarity to set, can be null
     * @return
     */
    public dItem setRarity(@NotNull dRarity rarity) {
        item.setObject("rds_rarity", rarity);
        cache.get("rarity").reset();
        return this;
    }

    /**
     * Gets the rarity of the item
     *
     * @return an integer symbolizing a rarity. Use utils to format to itemStack or String
     */
    public @NotNull
    dRarity getRarity() {
        return (dRarity) cache.get("rarity").get();
    }

    /**
     * Set the next Rarity
     *
     * @return
     */
    public dItem nextRarity() {
        setRarity(getRarity().next());
        cache.get("rarity").reset();
        return this;
    }

    /**
     * Gets the economy of this item
     *
     * @return
     */
    public @NotNull
    economy getEconomy() {
        return (economy) cache.get("economy").get();
    }

    /**
     * Set an economy for this item
     *
     * @param econ
     * @return
     */
    public dItem setEconomy(@NotNull economy econ) {
        item.setString("rds_econ", econ.serialize());
        cache.get("economy").reset();
        return this;
    }

    /**
     * Gets commands to run when this item is bought
     *
     * @return list of Strings representing commands
     */
    public Optional<List<String>> getCommands() {
        Object o;
        return Optional.ofNullable((o = cache.get("commands").get()) == null ? null : (List<String>) o);
    }

    /**
     * Sets the commands to run when this item is bought
     *
     * @param commands a list of Strings representing commands
     * @return
     */
    public dItem setCommands(@Nullable List<String> commands) {
        item.setObject("rds_cmds", commands);
        cache.get("commands").reset();
        return this;
    }


    private void migratePerms() {
        if (item.hasKey("rds_perms")) {
            setPermsBuy(item.getObject("rds_perms", List.class));
            item.removeKey("rds_perms");
        }
    }

    /**
     * Gets permissions that a player needs to buy this item
     *
     * @return list of Strings representing permissions
     */
    public Optional<List<String>> getPermsBuy() {
        Object o;
        return Optional.ofNullable((o = cache.get("buyPerms").get()) == null ? null : (List<String>) o);
    }

    /**
     * Sets the permission that a player needs to buy this item
     *
     * @param perms a list of Strings representing permissions
     * @return
     */
    public dItem setPermsBuy(@Nullable List<String> perms) {
        item.setObject("rds_perms_buy", perms);
        cache.get("buyPerms").reset();
        return this;
    }

    /**
     * Gets permissions that a player needs to buy this item
     *
     * @return list of Strings representing permissions
     */
    public Optional<List<String>> getPermsSell() {
        Object o;
        return Optional.ofNullable((o = cache.get("sellPerms").get()) == null ? null : (List<String>) o);
    }

    /**
     * Sets the permission that a player needs to buy this item
     *
     * @param perms a list of Strings representing permissions
     * @return
     */
    public dItem setPermsSell(@Nullable List<String> perms) {
        item.setObject("rds_perms_sell", perms);
        cache.get("sellPerms").reset();
        return this;
    }

    /**
     * Gets if confirm_Gui is enable for this item
     *
     * @return true if enabled; false is disabled
     */
    public boolean isConfirmGuiEnabled() {
        return (boolean) cache.get("confirmGui").get();
    }

    /**
     * Enable/disable confirm_Gui for this item
     *
     * @param b true to enable; false to disable
     * @return
     */
    public dItem setConfirm_gui(boolean b) {
        item.setBoolean("rds_confirm_gui", b);
        cache.get("confirmGui").reset();
        return this;
    }

    /**
     * Toggles the value of Confirm_GUI
     *
     * @return
     */
    public dItem toggleConfirm_gui() {
        setConfirm_gui(!isConfirmGuiEnabled());
        return this;
    }

    /**
     * Gets the amount for the set of items
     *
     * @return
     */
    public Optional<Integer> getSetItems() {
        Object o;
        return Optional.ofNullable((o = cache.get("set").get()) == null ? null : (int) o);
    }

    /**
     * Set amount for the set of items
     *
     * @param setItems
     * @return
     */
    public dItem setSetItems(@Nullable Integer setItems) {
        item.setInteger("rds_setItems", setItems);
        setQuantity(setItems == null ? 1 : setItems);
        cache.get("set").reset();
        return this;
    }

    /**
     * Gets a list of uuid, which represents the uuids of items in this bundle
     *
     * @return null if disabled.
     */
    public Optional<List<String>> getBundle() {
        return Optional.ofNullable((List<String>) cache.get("bundle").get());
    }

    /**
     * Sets uuid of the items that this bundle has
     *
     * @param bundle null if want to disabled it
     * @return
     */
    public dItem setBundle(@Nullable List<String> bundle) {
        if (bundle == null) item.removeKey("rds_bundle");
        else item.setObject("rds_bundle", new ArrayList<>(bundle));
        cache.get("bundle").reset();
        return this;
    }

    /**
     * Returns the action of the dItem
     *
     * @return Optional.ofNullable(dAction)
     */
    public Pair<dAction, String> getAction() {
        return (Pair<dAction, String>) cache.get("action").get();

    }

    /**
     * Sets the action of this item
     *
     * @return
     */
    public dItem setAction(@Nullable dAction action, String s) {
        item.setString("rds_action", Pair.of(action, s).serialize());
        cache.get("action").reset();
        return this;
    }

    /**
     * Private method to set Item as AIR
     *
     * @return
     */
    public dItem setAIR() {
        item.setBoolean("rds_AIR", true);
        return this;
    }

    public JsonObject getNBT() {
        return new Gson().fromJson(item.toString(), JsonObject.class);
    }

    public dItem setNBT(JsonObject nbt) {
        item.mergeCompound(new NBTContainer(nbt.toString()));
        return this;
    }

    /**
     * Check if an dItem is masked as AIR
     *
     * @return
     */
    public boolean isAIR() {
        return item.hasKey("rds_AIR");
    }

    public boolean isSIGN() {
        return item.hasKey("rds_SIGN");
    }

    /**
     * Returns a copy of this dItem but different UUID (generated randomly)
     *
     * @return
     */
    public dItem copy() {
        dItem cloned = new dItem(getDailyItem());
        cloned.setID(UUID.randomUUID().toString());
        cloned.setStock(getStock());
        return cloned;
    }

    /**
     * Returns a deep copy of the object, same UUID
     *
     * @return
     */
    @Override
    public dItem clone() {
        return new dItem(getDailyItem());
    }

    public static dItem AIR() {
        return new dItem(XMaterial.GRAY_STAINED_GLASS_PANE.parseItem())
                .setDisplayName("&c")
                .setFlag(ItemFlag.HIDE_ENCHANTS)
                .addEnchantments(Enchantment.DAMAGE_ALL, 1)
                .setAIR();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof dItem))
            return false;
        return this.getUid().equals(((dItem) o).getUid());
    }

    @Override
    public int hashCode() {
        return getUid().hashCode();
    }


    //>>>>>> Serialize stuff <<<<<<//
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeObject(encodeOptions.REFLECTION.serialize(this));
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        NBTCompound itemData = new NBTContainer(new String(Base64.getDecoder()
                .decode((String) in.readObject())));
        ItemStack item = NBTItem.convertNBTtoItem(itemData);
        setItem(item);
    }

    private void readObjectNoData()
            throws ObjectStreamException {

    }

    public boolean isSimilar(@NotNull dItem o) {
        dItem firstItem = this.clone().setStock(null);
        dItem secondItem = o.clone().setStock(null);

        boolean similarStock = compareStocks(o.getStock(), this.getStock());
        boolean similarBuyPrice = o.getBuyPrice().orElse(dPrice.EMPTY()).equals(this.getBuyPrice().orElse(dPrice.EMPTY()));
        boolean similarSellPrice = o.getSellPrice().orElse(dPrice.EMPTY()).equals(this.getSellPrice().orElse(dPrice.EMPTY()));

        return removePrices(firstItem).getDailyItem().isSimilar(removePrices(this).getDailyItem())
                && similarBuyPrice
                && similarSellPrice
                && similarStock;
    }

    private dItem removePrices(dItem item) {
        return item.clone().setSellPrice(null).setBuyPrice(null);
    }

    private boolean compareStocks(dStock o1, dStock o2) {
        if (o1 == null && o2 == null)
            return true;

        else if (o1 == null || o2 == null)
            return false;

        else
            return o1.toString().equals(o2.toString());
    }

    public static final class encodeOptions {

        public transient static final jsonSerialization JSON = new jsonSerialization();
        public transient static final bukkitSerialization BUKKIT = new bukkitSerialization();
        public transient static final reflectionSerialization REFLECTION = new reflectionSerialization();

        private encodeOptions() {
        }

    }

    public static final class jsonSerialization implements jsonSerializer<dItem> {

        private transient static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(dItem.class, new dItemAdapter())
                .create();

        @Override
        public JsonElement toJson(dItem item) {
            return gson.toJsonTree(item);
        }

        @Override
        public dItem fromJson(JsonElement element) {
            return gson.fromJson(element, dItem.class);
        }

        private jsonSerialization() {
        }

    }

    public static final class bukkitSerialization {

        public String serialize(dItem item) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                    dataOutput.writeObject(item.getDailyItem());
                    return Base64.getEncoder().encodeToString(outputStream.toByteArray());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public dItem deserialize(String s) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(s))) {
                try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                    return dItem.of((ItemStack) dataInput.readObject());
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private bukkitSerialization() {
        }

    }

    public static final class reflectionSerialization {

        public String serialize(dItem item) {
            return Base64Coder.encodeString(NBTItem.convertItemtoNBT(item.getDailyItem()).toString());
        }

        public dItem deserialize(String s) {
            NBTCompound itemData = new NBTContainer(Base64Coder.decodeString(s));
            ItemStack item = NBTItem.convertNBTtoItem(itemData);

            return new dItem(item);
        }

    }

    public static class LazyWrapper<T> {

        private final Supplier<T> supplier;
        private Lazy<T> lazy;

        public static <T> LazyWrapper<T> suppliedBy(Supplier<T> supplier) {
            return new LazyWrapper<>(supplier);
        }

        private LazyWrapper(Supplier<T> supplier) {
            this.supplier = supplier;
            lazy = Lazy.suppliedBy(supplier);
        }

        public T get() {
            return lazy.get();
        }

        public void reset() {
            lazy = Lazy.suppliedBy(supplier);
        }

    }


}
