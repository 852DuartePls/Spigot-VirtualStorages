package net.duart.virtualstorage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class VirtualBackpack implements Listener {

    private final Plugin plugin;
    private final HashMap<UUID, Integer> currentPageIndexMap = new HashMap<>();
    private final HashMap<UUID, ArrayList<Inventory>> backpacks = new HashMap<>();

    public VirtualBackpack(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openBackpack(Player player) {
        UUID playerId = player.getUniqueId();
        currentPageIndexMap.put(playerId, 0);
        ArrayList<Inventory> pages = getBackpackPages(playerId);
        Inventory currentPage = pages.get(0);
        loadBackpackFromYAML(playerId, pages);
        player.openInventory(currentPage);
    }

    private void loadBackpackFromYAML(UUID playerId, ArrayList<Inventory> pages) {
        File playerFile = new File(plugin.getDataFolder(), playerId.toString() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        for (int i = 0; i < pages.size(); i++) {
            Inventory page = pages.get(i);
            for (int slot = 0; slot < page.getSize(); slot++) {
                if (playerConfig.contains("pages." + i + ".slot" + slot)) {
                    ItemStack item = playerConfig.getItemStack("pages." + i + ".slot" + slot);
                    if (item != null) {
                        page.setItem(slot, item);
                    }
                }
            }
        }
    }

    private ArrayList<Inventory> getBackpackPages(UUID playerId) {
        int maxPages = getMaxPages(playerId);
        ArrayList<Inventory> pages = backpacks.get(playerId);
        if (pages == null) {
            pages = new ArrayList<>();
            for (int i = 0; i < maxPages; i++) {
                Inventory page = Bukkit.createInventory(null, 54, "§9◆ Backpack - Page " + (i + 1) + " of " + maxPages + " ◆");
                pages.add(page);
            }
            addNavigationItems(pages);
            backpacks.put(playerId, pages);
        }
        return pages;
    }

    private int getMaxPages(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            for (int i = 999; i >= 1; i--) {
                if (player.hasPermission("virtualstorages.use." + i)) {
                    return i;
                }
            }
        }
        return 1;
    }

    public void updatePermissions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            ArrayList<Inventory> pages = getBackpackPages(playerId);
            if (!pages.isEmpty()) {
                int oldMaxPages = pages.size();
                pages.clear();
                int maxPages = getMaxPages(playerId);
                for (int i = 0; i < maxPages; i++) {
                    Inventory page = Bukkit.createInventory(null, 54, "§9◆ Backpack - Page " + (i + 1) + " of " + maxPages + " ◆");
                    ArrayList<Inventory> oldPages = backpacks.get(playerId);
                    if (oldPages != null && i < oldPages.size()) {
                        Inventory oldPage = oldPages.get(i);
                        for (int slot = 0; slot < oldPage.getSize(); slot++) {
                            ItemStack item = oldPage.getItem(slot);
                            if (item != null && !isNavigationItem(item)) {
                                page.setItem(slot, item);
                            }
                        }
                    }
                    pages.add(page);
                }
                addNavigationItems(pages);
            }
        }
    }

    private void addNavigationItems(ArrayList<Inventory> pages) {
        int totalPages = pages.size();

        for (int i = 0; i < totalPages; i++) {
            Inventory page = pages.get(i);

            if (i > 0) {
                ItemStack leftArrow = new ItemStack(Material.ARROW);
                ItemMeta leftArrowMeta = leftArrow.getItemMeta();
                assert leftArrowMeta != null;
                leftArrowMeta.setDisplayName("§c<< ᴘʀᴇᴠɪᴏᴜs ᴘᴀɢᴇ");
                leftArrow.setItemMeta(leftArrowMeta);
                page.setItem(45, leftArrow);
            }

            if (i < totalPages - 1) {
                ItemStack rightArrow = new ItemStack(Material.ARROW);
                ItemMeta rightArrowMeta = rightArrow.getItemMeta();
                assert rightArrowMeta != null;
                rightArrowMeta.setDisplayName("§aɴᴇxᴛ ᴘᴀɢᴇ >>");
                rightArrow.setItemMeta(rightArrowMeta);
                page.setItem(53, rightArrow);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        InventoryView inventoryView = event.getView();
        String inventoryTitle = inventoryView.getTitle();

        if (inventoryTitle.contains("Backpack - Page")) {
            ArrayList<Inventory> pages = getBackpackPages(playerId);
            int currentPageIndex = currentPageIndexMap.getOrDefault(playerId, 0);
            Inventory currentPage = pages.get(currentPageIndex);

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(currentPage)) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                    ItemMeta meta = clickedItem.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        String displayName = meta.getDisplayName();
                        if (displayName.equals("§c<< ᴘʀᴇᴠɪᴏᴜs ᴘᴀɢᴇ") || displayName.equals("§aɴᴇxᴛ ᴘᴀɢᴇ >>")) {
                            int slot = event.getSlot();
                            if (slot == 45 || slot == 53) {
                                boolean isLeftClick = event.getClick().isLeftClick();
                                if (isLeftClick) {
                                    event.setCancelled(true);

                                    int direction = slot == 45 ? -1 : 1;
                                    changePage(playerId, direction);

                                    ArrayList<Inventory> updatedPages = getBackpackPages(playerId);
                                    Inventory updatedPage = updatedPages.get(currentPageIndexMap.getOrDefault(playerId, 0));
                                    player.openInventory(updatedPage);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void changePage(UUID playerId, int direction) {
        ArrayList<Inventory> pages = backpacks.get(playerId);
        int currentPageIndex = currentPageIndexMap.getOrDefault(playerId, 0);
        int newPageIndex = currentPageIndex + direction;
        if (newPageIndex >= 0 && newPageIndex < pages.size()) {
            currentPageIndexMap.put(playerId, newPageIndex);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        InventoryView inventoryView = event.getView();
        String inventoryTitle = inventoryView.getTitle();

        if (inventoryTitle.contains("Backpack - Page")) {
            if (inventoryView.getPlayer() instanceof Player) {
                saveBackpackInventory(playerId, getBackpackPages(playerId));
            }
        }
    }

    private void saveBackpackInventory(UUID playerId, ArrayList<Inventory> pages) {
        File playerFile = new File(plugin.getDataFolder(), playerId.toString() + ".yml");
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        for (int i = 0; i < pages.size(); i++) {
            Inventory page = pages.get(i);
            for (int slot = 0; slot < page.getSize(); slot++) {
                ItemStack item = page.getItem(slot);
                if (item != null && isNavigationItem(item)) {
                    playerConfig.set("pages." + i + ".slot" + slot, item);
                } else {
                    playerConfig.set("pages." + i + ".slot" + slot, null);
                }
            }
        }

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            System.err.println("Error al guardar el archivo YAML:");
            e.printStackTrace();
        }
    }

    private boolean isNavigationItem(ItemStack item) {
        if (item.getType() == Material.ARROW) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                return !displayName.equals("§c<< ᴘʀᴇᴠɪᴏᴜs ᴘᴀɢᴇ") && !displayName.equals("§aɴᴇxᴛ ᴘᴀɢᴇ >>");
            }
        }
        return true;
    }

    public void saveAllBackpacks() {
        for (UUID playerId : backpacks.keySet()) {
            ArrayList<Inventory> pages = backpacks.get(playerId);
            saveBackpackInventory(playerId, pages);
        }
    }
}