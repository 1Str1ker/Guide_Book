package me.white_black.guidebook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GuideBookPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private String guiTitle;
    private String iconSelectorPrefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        updateCaches();
        
        this.getCommand("gb").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("Плагін Guide_book v3.4 (Кнопки-команди та 54 іконки) успішно запущено!");
    }

    private void updateCaches() {
        guiTitle = colorize(getConfig().getString("gui.title", "&8Довідник Сервера"));
        iconSelectorPrefix = getMsg("icon_selector");
    }

    public String colorize(String text) {
        if (text == null) return "";
        Matcher matcher = Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
        while (matcher.find()) {
            String color = matcher.group(1);
            text = text.replace("&#" + color, ChatColor.of("#" + color) + "");
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getMsg(String key) {
        boolean en = getConfig().getString("settings.language", "uk").equalsIgnoreCase("en");
        String msg = "";
        switch (key) {
            case "reloaded": msg = en ? "&aConfiguration reloaded!" : "&aКонфігурацію плагіна успішно оновлено!"; break;
            case "size_changed": msg = en ? "&aGUI size changed!" : "&aРозмір меню змінено!"; break;
            case "invalid_size": msg = en ? "&cSize must be a multiple of 9 (9-54)!" : "&cРозмір має бути кратним 9 (від 9 до 54)!"; break;
            case "deleted": msg = en ? "&aGuide deleted!" : "&aГайд видалено!"; break;
            case "not_found": msg = en ? "&cGuide not found." : "&cГайд не знайдено."; break;
            case "give_book": msg = en ? "&aBlank book given." : "&aЗаготовку видано."; break;
            case "hold_book": msg = en ? "&cHold a Written or Writable book in your hand!" : "&cВізьміть книгу в руку!"; break;
            case "edit_success": msg = en ? "&aGuide updated successfully!" : "&aТекст гайду успішно оновлено!"; break;
            case "already_exists": msg = en ? "&cGuide ID already exists! Use /gb edit" : "&cГайд з таким ID вже існує! Використайте /gb edit"; break;
            case "icon_set": msg = en ? "&aIcon successfully set!" : "&aІконку встановлено!"; break;
            case "admin_help_header": msg = en ? "&6--- &eGuideBook Admin &6---" : "&6--- &eДовідник Сервера (Адмін) &6---"; break;
            case "hover_link": msg = en ? "&7Click to open:\n&f" : "&7Натисніть, щоб відкрити:\n&f"; break;
            case "hover_cmd": msg = en ? "&7Click to execute:\n&f" : "&7Натисніть, щоб виконати:\n&f"; break;
            case "click_read": msg = en ? "&7Click to read" : "&7Натисніть, щоб прочитати"; break;
            case "icon_selector": msg = en ? "&8Select Icon: " : "&8Вибір іконки: "; break;
            case "select": msg = en ? "&aSelect " : "&aОбрати "; break;
            case "usage_edit": msg = en ? "&cUsage: /gb edit <id>" : "&cВикористання: /gb edit <id>"; break;
            case "usage_add": msg = en ? "&cUsage: /gb add <id> [slot]" : "&cВикористання: /gb add <id> [слот]"; break;
            case "default_name": msg = en ? "New Guide" : "Новий Гайд"; break;
            default: msg = key;
        }
        return colorize(getConfig().getString("messages." + key, msg));
    }

    private BaseComponent[] parsePage(String text) {
        text = colorize(text);
        TextComponent pageRoot = new TextComponent();
        Pattern pattern = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)"); 
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String before = text.substring(lastEnd, matcher.start());
                for (BaseComponent c : TextComponent.fromLegacyText(before)) pageRoot.addExtra(c);
            }

            String linkText = matcher.group(1);
            String url = matcher.group(2);

            TextComponent linkComp = new TextComponent();
            for (BaseComponent c : TextComponent.fromLegacyText(linkText)) linkComp.addExtra(c);
            
            // НОВА ЛОГІКА: Якщо починається з "/", то це команда!
            if (url.startsWith("/")) {
                linkComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, url));
                linkComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(getMsg("hover_cmd") + url))));
            } else {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                linkComp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                linkComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(getMsg("hover_link") + url))));
            }
            
            pageRoot.addExtra(linkComp);
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String after = text.substring(lastEnd);
            for (BaseComponent c : TextComponent.fromLegacyText(after)) pageRoot.addExtra(c);
        }

        return new BaseComponent[]{pageRoot};
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) openGui((Player) sender);
            return true;
        }

        boolean en = getConfig().getString("settings.language", "uk").equalsIgnoreCase("en");

        if (args[0].equalsIgnoreCase("adminhelp")) {
            if (sender.isOp() || sender.hasPermission("guidebook.admin")) {
                sender.sendMessage(getMsg("admin_help_header"));
                if (en) {
                    sender.sendMessage("§a/gb give <name> §7- Get a blank book");
                    sender.sendMessage("§a/gb add <id> [slot] §7- Save a new guide to menu");
                    sender.sendMessage("§a/gb edit <id> §7- Update existing guide text");
                    sender.sendMessage("§a/gb size <9-54> §7- Change GUI size");
                    sender.sendMessage("§a/gb drop <id> [player] §7- Drop guide as item");
                    sender.sendMessage("§a/gb delete <id> §7- Delete guide from menu");
                    sender.sendMessage("§a/gb reload §7- Reload config");
                } else {
                    sender.sendMessage("§a/gb give <назва> §7- Отримати книгу-заготовку");
                    sender.sendMessage("§a/gb add <id> [слот] §7- Зберегти новий гайд у меню");
                    sender.sendMessage("§a/gb edit <id> §7- Оновити текст існуючого гайду");
                    sender.sendMessage("§a/gb size <9-54> §7- Змінити розмір головного меню");
                    sender.sendMessage("§a/gb drop <id> [гравець] §7- Видати готовий гайд як предмет");
                    sender.sendMessage("§a/gb delete <id> §7- Видалити гайд із меню");
                    sender.sendMessage("§a/gb reload §7- Оновити конфігурацію");
                }
                sender.sendMessage("§6-------------------------");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            reloadConfig();
            updateCaches();
            sender.sendMessage(getMsg("reloaded"));
            return true;
        }

        if (args[0].equalsIgnoreCase("size") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            if (args.length < 2) return true;
            try {
                int newSize = Integer.parseInt(args[1]);
                if (newSize % 9 != 0 || newSize < 9 || newSize > 54) {
                    sender.sendMessage(getMsg("invalid_size"));
                    return true;
                }
                getConfig().set("gui.size", newSize);
                saveConfig();
                updateCaches();
                sender.sendMessage(getMsg("size_changed"));
            } catch (NumberFormatException ignored) {}
            return true;
        }

        if (args[0].equalsIgnoreCase("delete") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            if (args.length < 2) return true;
            String bookId = args[1].toLowerCase();
            if (getConfig().contains("books." + bookId)) {
                getConfig().set("books." + bookId, null);
                saveConfig();
                sender.sendMessage(getMsg("deleted"));
            } else {
                sender.sendMessage(getMsg("not_found"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack writableBook = new ItemStack(Material.WRITABLE_BOOK);
                ItemMeta meta = writableBook.getItemMeta();
                
                String bookName = getMsg("default_name");
                if (args.length >= 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    bookName = sb.toString().trim();
                }
                
                meta.setDisplayName("§e" + bookName);
                writableBook.setItemMeta(meta);
                player.getInventory().addItem(writableBook);
                player.sendMessage(getMsg("give_book"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("drop") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            if (args.length < 2) return true;
            String bookId = args[1].toLowerCase();
            if (!getConfig().contains("books." + bookId)) {
                sender.sendMessage(getMsg("not_found"));
                return true;
            }
            Player target = args.length == 3 ? Bukkit.getPlayer(args[2]) : (Player) sender;
            if (target != null) {
                target.getInventory().addItem(getBookItemFromConfig(bookId));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("edit") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            if (args.length < 2) {
                sender.sendMessage(getMsg("usage_edit"));
                return true;
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String bookId = args[1].toLowerCase();
                
                if (!getConfig().contains("books." + bookId)) {
                    player.sendMessage(getMsg("not_found"));
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() != Material.WRITTEN_BOOK && item.getType() != Material.WRITABLE_BOOK) {
                    player.sendMessage(getMsg("hold_book"));
                    return true;
                }

                BookMeta meta = (BookMeta) item.getItemMeta();
                String title = bookId;
                if (meta.hasTitle()) {
                    title = meta.getTitle();
                } else if (meta.hasDisplayName()) {
                    title = ChatColor.stripColor(meta.getDisplayName());
                }
                
                getConfig().set("books." + bookId + ".title", title);
                if (meta.hasAuthor()) getConfig().set("books." + bookId + ".author", meta.getAuthor());
                
                List<String> pages = new ArrayList<>();
                if (meta.getPages() != null) {
                    for (String page : meta.getPages()) pages.add(page.replace("§", "&"));
                }
                getConfig().set("books." + bookId + ".pages", pages);
                saveConfig();
                player.sendMessage(getMsg("edit_success"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("add") && (sender.isOp() || sender.hasPermission("guidebook.admin"))) {
            if (args.length < 2) {
                sender.sendMessage(getMsg("usage_add"));
                return true;
            }
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack item = player.getInventory().getItemInMainHand();

                if (item.getType() != Material.WRITTEN_BOOK && item.getType() != Material.WRITABLE_BOOK) {
                    player.sendMessage(getMsg("hold_book"));
                    return true;
                }

                String bookId = args[1].toLowerCase();
                if (getConfig().contains("books." + bookId)) {
                    player.sendMessage(getMsg("already_exists"));
                    return true;
                }

                BookMeta meta = (BookMeta) item.getItemMeta();
                String title = bookId;
                if (meta.hasTitle()) {
                    title = meta.getTitle();
                } else if (meta.hasDisplayName()) {
                    title = ChatColor.stripColor(meta.getDisplayName());
                }
                
                int targetSlot = -1;
                if (args.length >= 3) {
                    try { targetSlot = Integer.parseInt(args[2]); } catch (Exception ignored) {}
                }

                int guiSize = getConfig().getInt("gui.size", 27);
                ConfigurationSection booksSection = getConfig().getConfigurationSection("books");

                if (targetSlot <= 0 || targetSlot > guiSize) {
                    int nextSlot = 1;
                    if (booksSection != null) {
                        for (int i = 1; i <= guiSize; i++) {
                            boolean occupied = false;
                            for (String key : booksSection.getKeys(false)) {
                                if (getConfig().getInt("books." + key + ".slot") == i) {
                                    occupied = true;
                                    break;
                                }
                            }
                            if (!occupied) {
                                nextSlot = i;
                                break;
                            }
                        }
                    }
                    targetSlot = nextSlot;
                }

                getConfig().set("books." + bookId + ".slot", targetSlot);
                getConfig().set("books." + bookId + ".icon", "BOOK");
                getConfig().set("books." + bookId + ".name", "&#FFD700" + title); 
                getConfig().set("books." + bookId + ".permission", ""); 
                
                List<String> lore = new ArrayList<>();
                lore.add(getMsg("click_read"));
                getConfig().set("books." + bookId + ".lore", lore);
                getConfig().set("books." + bookId + ".title", title);
                getConfig().set("books." + bookId + ".author", "Адміністрація");
                
                List<String> pages = new ArrayList<>();
                if (meta.getPages() != null) {
                    for (String page : meta.getPages()) pages.add(page.replace("§", "&"));
                }
                getConfig().set("books." + bookId + ".pages", pages);
                
                saveConfig();
                openIconSelector(player, bookId);
            }
            return true;
        }
        return true;
    }

    private void openGui(Player player) {
        int size = getConfig().getInt("gui.size", 27);
        Inventory gui = Bukkit.createInventory(null, size, guiTitle);

        ConfigurationSection booksSection = getConfig().getConfigurationSection("books");
        if (booksSection != null) {
            for (String key : booksSection.getKeys(false)) {
                String perm = getConfig().getString("books." + key + ".permission", "");
                if (!perm.isEmpty() && !player.hasPermission(perm) && !player.isOp()) continue; 

                int slot = getConfig().getInt("books." + key + ".slot") - 1; 
                String matName = getConfig().getString("books." + key + ".icon", "BOOK");
                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.BOOK;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null) {
                    meta.setDisplayName(colorize(getConfig().getString("books." + key + ".name", key)));
                    List<String> lore = getConfig().getStringList("books." + key + ".lore");
                    List<String> coloredLore = new ArrayList<>();
                    for (String l : lore) coloredLore.add(colorize(l));
                    meta.setLore(coloredLore);
                    item.setItemMeta(meta);
                }
                if (slot >= 0 && slot < size) gui.setItem(slot, item);
            }
        }
        
        if (getConfig().getBoolean("settings.sounds", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        }
        player.openInventory(gui);
    }

    private void openIconSelector(Player player, String bookId) {
        Inventory inv = Bukkit.createInventory(null, 54, iconSelectorPrefix + bookId);
        
        // 54 найпопулярніших іконок для гайдів!
        Material[] icons = {
            Material.BOOK, Material.WRITTEN_BOOK, Material.ENCHANTED_BOOK, Material.NAME_TAG, Material.PAPER, Material.MAP,
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT, Material.COPPER_INGOT, Material.NETHERITE_INGOT,
            Material.DIAMOND_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.BOW, Material.CROSSBOW, Material.SHIELD,
            Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.FISHING_ROD, Material.TRIDENT, Material.SPYGLASS, Material.RECOVERY_COMPASS,
            Material.COMPASS, Material.CLOCK, Material.ENDER_PEARL, Material.ENDER_EYE, Material.TOTEM_OF_UNDYING, Material.EXPERIENCE_BOTTLE,
            Material.GRASS_BLOCK, Material.DIRT, Material.COBBLESTONE, Material.STONE, Material.OBSIDIAN, Material.BEDROCK,
            Material.OAK_LOG, Material.OAK_PLANKS, Material.OAK_SIGN, Material.CHEST, Material.ENDER_CHEST, Material.CRAFTING_TABLE,
            Material.FURNACE, Material.CAMPFIRE, Material.TORCH, Material.LANTERN, Material.SLIME_BALL, Material.MAGMA_CREAM,
            Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.BREAD, Material.COOKED_BEEF, Material.CAKE
        };
        
        for (Material mat : icons) {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(getMsg("select") + mat.name());
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.equals(guiTitle)) {
            event.setCancelled(true); 
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;

            Player player = (Player) event.getWhoClicked();
            int clickedSlot = event.getSlot();
            
            ConfigurationSection booksSection = getConfig().getConfigurationSection("books");
            if (booksSection != null) {
                for (String key : booksSection.getKeys(false)) {
                    int configSlot = getConfig().getInt("books." + key + ".slot") - 1;
                    if (clickedSlot == configSlot) {
                        ItemStack book = getBookItemFromConfig(key);
                        if (getConfig().getBoolean("settings.sounds", true)) {
                            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                        }
                        
                        player.closeInventory();
                        player.openBook(book);
                        break;
                    }
                }
            }
        } 
        else if (title.startsWith(iconSelectorPrefix)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;

            Player player = (Player) event.getWhoClicked();
            String bookId = title.replace(iconSelectorPrefix, "");
            getConfig().set("books." + bookId + ".icon", event.getCurrentItem().getType().name());
            saveConfig();
            player.closeInventory();
            player.sendMessage(getMsg("icon_set"));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("settings.open_on_first_join", false)) {
            if (!event.getPlayer().hasPlayedBefore()) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    openGui(event.getPlayer());
                }, 20L); 
            }
        }
    }

    private ItemStack getBookItemFromConfig(String bookKey) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(colorize(getConfig().getString("books." + bookKey + ".title", "Довідник")));
            meta.setAuthor(colorize(getConfig().getString("books." + bookKey + ".author", "Адміністрація")));
            List<String> pages = getConfig().getStringList("books." + bookKey + ".pages");
            for (String page : pages) meta.spigot().addPage(parsePage(page));
            book.setItemMeta(meta);
        }
        return book;
    }
}