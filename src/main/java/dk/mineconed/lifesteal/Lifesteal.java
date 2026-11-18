package dk.mineconed.lifesteal;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Made by Mineconed
 *
 * Lifesteal: kill a player to steal a heart, lose all hearts and you are out.
 */
public final class Lifesteal extends JavaPlugin implements Listener {

    private Beskeder msg;
    private File fil;
    private YamlConfiguration data;
    private final Map<String, Long> sidsteDrab = new HashMap<String, Long>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        msg = new Beskeder(this);
        fil = new File(getDataFolder(), "hearts.yml");
        data = fil.exists() ? YamlConfiguration.loadConfiguration(fil) : new YamlConfiguration();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player p : Bukkit.getOnlinePlayers()) {
            anvend(p);
        }
        getLogger().info("Lifesteal is enabled. Made by Mineconed.");
    }

    @Override
    public void onDisable() {
        gem();
    }

    private void gem() {
        try {
            data.save(fil);
        } catch (IOException e) {
            getLogger().warning("Could not save hearts.yml");
        }
    }

    // ═══════════════ HJERTER ═══════════════

    private int start() {
        return getConfig().getInt("start_hearts", 10);
    }

    private int maks() {
        return getConfig().getInt("max_hearts", 20);
    }

    int hjerter(UUID u) {
        return data.getInt("players." + u + ".hearts", start());
    }

    private void saetHjerter(UUID u, int n) {
        data.set("players." + u + ".hearts", Math.max(0, n));
        gem();
        Player p = Bukkit.getPlayer(u);
        if (p != null) {
            anvend(p);
        }
    }

    @SuppressWarnings("deprecation")
    private void anvend(Player p) {
        if (deaktiveret(p)) {
            return;
        }
        double max = Math.max(1, hjerter(p.getUniqueId())) * 2.0;
        boolean ok = false;
        try {
            Class<?> attr = Class.forName("org.bukkit.attribute.Attribute");
            Object a;
            try {
                a = attr.getField("MAX_HEALTH").get(null);
            } catch (NoSuchFieldException gammel) {
                a = attr.getField("GENERIC_MAX_HEALTH").get(null);
            }
            Method get = p.getClass().getMethod("getAttribute", attr);
            Object inst = get.invoke(p, a);
            inst.getClass().getMethod("setBaseValue", double.class).invoke(inst, max);
            ok = true;
        } catch (Throwable old) {
            ok = false;
        }
        if (!ok) {
            p.setMaxHealth(max);
        }
        if (p.getHealth() > max) {
            p.setHealth(max);
        }
    }

    private boolean deaktiveret(Player p) {
        return getConfig().getStringList("disabled_worlds").contains(p.getWorld().getName());
    }

    // ═══════════════ HJERTE ITEM ═══════════════

    @SuppressWarnings("deprecation")
    ItemStack hjerteItem(int antal) {
        Material m = Material.matchMaterial(getConfig().getString("heart_item.material", "NETHER_STAR"));
        ItemStack i = new ItemStack(m == null ? Material.NETHER_STAR : m, Math.max(1, antal));
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(msg.c("heart_name"));
        im.setLore(msg.linjer("heart_lore"));
        i.setItemMeta(im);
        return i;
    }

    private boolean erHjerte(ItemStack i) {
        if (i == null || !i.hasItemMeta() || !i.getItemMeta().hasDisplayName()) {
            return false;
        }
        ItemStack h = hjerteItem(1);
        return i.getType() == h.getType() && i.getItemMeta().getDisplayName().equals(h.getItemMeta().getDisplayName());
    }

    // ═══════════════ EVENTS ═══════════════

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String s = "players." + p.getUniqueId();
        if (data.getBoolean(s + ".eliminated", false)) {
            data.set(s + ".eliminated", null);
            data.set(s + ".hearts", getConfig().getInt("revive_hearts", 3));
            gem();
            if (p.getGameMode().name().equals("SPECTATOR") && getConfig().getString("elimination.action", "ban").equalsIgnoreCase("spectator")) {
                return;
            }
            msg.send(p, "back");
        }
        data.set(s + ".name", p.getName());
        anvend(p);
    }

    private String ip(Player p) {
        return p.getAddress() == null ? "" : p.getAddress().getAddress().getHostAddress();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        final Player v = e.getEntity();
        if (deaktiveret(v)) {
            return;
        }
        Player k = v.getKiller();
        int tab = Math.max(1, getConfig().getInt("hearts_per_kill", 1));
        if (k != null && k != v) {
            if (getConfig().getBoolean("anti_farm.ignore_same_ip", true) && ip(k).equals(ip(v)) && !ip(k).isEmpty()) {
                msg.send(k, "same_ip");
                return;
            }
            String noegle = k.getUniqueId() + ">" + v.getUniqueId();
            long nu = System.currentTimeMillis();
            Long sidst = sidsteDrab.get(noegle);
            long cd = getConfig().getInt("anti_farm.same_victim_minutes", 10) * 60000L;
            if (sidst != null && Math.subtractExact(nu, sidst) < cd) {
                msg.send(k, "farm_cooldown", "{player}", v.getName());
                return;
            }
            sidsteDrab.put(noegle, nu);
            int kh = hjerter(k.getUniqueId());
            if (kh < maks()) {
                saetHjerter(k.getUniqueId(), Math.min(maks(), kh + tab));
                msg.send(k, "stole", "{player}", v.getName(), "{hearts}", String.valueOf(hjerter(k.getUniqueId())));
                Visning.lyd(k, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
            } else if (getConfig().getBoolean("drop_heart_when_full", true)) {
                v.getWorld().dropItemNaturally(v.getLocation(), hjerteItem(tab));
                msg.send(k, "full_dropped");
            } else {
                msg.send(k, "full");
            }
        } else if (!getConfig().getBoolean("lose_heart_on_natural_death", false)) {
            return;
        }
        int ny = Math.subtractExact(hjerter(v.getUniqueId()), tab);
        saetHjerter(v.getUniqueId(), ny);
        if (ny > 0) {
            msg.send(v, "lost", "{hearts}", String.valueOf(ny), "{killer}", k == null ? msg.c("nature") : k.getName());
            return;
        }
        eliminer(v, k == null ? msg.c("nature") : k.getName());
    }

    private void eliminer(final Player v, String hvem) {
        data.set("players." + v.getUniqueId() + ".eliminated", true);
        gem();
        Bukkit.broadcastMessage(msg.c("prefix") + msg.c("eliminated_broadcast", "{player}", v.getName(), "{killer}", hvem));
        final String handling = getConfig().getString("elimination.action", "ban").toLowerCase();
        final int min = getConfig().getInt("elimination.ban_minutes", 1440);
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (handling.equals("spectator")) {
                    if (v.isOnline()) {
                        v.setGameMode(GameMode.valueOf("SPECTATOR"));
                        msg.send(v, "eliminated_spectator");
                    }
                    return;
                }
                Date slut = min > 0 ? new Date(System.currentTimeMillis() + min * 60000L) : null;
                String grund = msg.c("ban_reason", "{time}", min > 0 ? tid(min) : msg.c("forever"));
                Bukkit.getBanList(BanList.Type.NAME).addBan(v.getName(), grund, slut, "Lifesteal");
                if (v.isOnline()) {
                    v.kickPlayer(grund);
                }
            }
        }, 2L);
    }

    private String tid(int min) {
        if (min >= 1440) {
            return (min / 1440) + msg.c("unit_d") + (min % 1440 >= 60 ? " " + ((min % 1440) / 60) + msg.c("unit_h") : "");
        }
        if (min >= 60) {
            return (min / 60) + msg.c("unit_h") + (min % 60 > 0 ? " " + (min % 60) + msg.c("unit_m") : "");
        }
        return min + msg.c("unit_m");
    }

    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("deprecation")
    public void onBrug(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack i = p.getItemInHand();
        if (!erHjerte(i)) {
            return;
        }
        e.setCancelled(true);
        int h = hjerter(p.getUniqueId());
        if (h >= maks()) {
            msg.send(p, "max_reached", "{max}", String.valueOf(maks()));
            return;
        }
        if (i.getAmount() > 1) {
            i.setAmount(Math.subtractExact(i.getAmount(), 1));
        } else {
            p.setItemInHand(null);
        }
        saetHjerter(p.getUniqueId(), h + 1);
        msg.send(p, "used_heart", "{hearts}", String.valueOf(h + 1));
        Visning.lyd(p, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
    }

    // ═══════════════ KOMMANDOER ═══════════════

    @SuppressWarnings("deprecation")
    private OfflinePlayer find(String navn) {
        Player p = Bukkit.getPlayerExact(navn);
        return p != null ? p : Bukkit.getOfflinePlayer(navn);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String c = cmd.getName().toLowerCase();
        if (c.equals("hearts")) {
            if (a.length > 0) {
                OfflinePlayer o = find(a[0]);
                msg.send(s, "hearts_other", "{player}", o.getName() == null ? a[0] : o.getName(), "{hearts}", String.valueOf(hjerter(o.getUniqueId())));
            } else if (s instanceof Player) {
                msg.send(s, "hearts_self", "{hearts}", String.valueOf(hjerter(((Player) s).getUniqueId())), "{max}", String.valueOf(maks()));
            }
            return true;
        }
        if (c.equals("withdraw")) {
            if (!(s instanceof Player) || !s.hasPermission("lifesteal.withdraw")) {
                msg.send(s, "no_permission");
                return true;
            }
            Player p = (Player) s;
            int n = 1;
            if (a.length > 0) {
                try {
                    n = Math.max(1, Integer.parseInt(a[0]));
                } catch (NumberFormatException ex) {
                    msg.send(p, "not_number");
                    return true;
                }
            }
            int h = hjerter(p.getUniqueId());
            if (Math.subtractExact(h, n) < 1) {
                msg.send(p, "withdraw_too_many");
                return true;
            }
            saetHjerter(p.getUniqueId(), Math.subtractExact(h, n));
            for (ItemStack rest : p.getInventory().addItem(hjerteItem(n)).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
            msg.send(p, "withdrew", "{amount}", String.valueOf(n), "{hearts}", String.valueOf(Math.subtractExact(h, n)));
            return true;
        }
        if (!s.hasPermission("lifesteal.admin")) {
            msg.send(s, "no_permission");
            return true;
        }
        String a1 = a.length > 0 ? a[0].toLowerCase() : "";
        if (a1.equals("reload")) {
            reloadConfig();
            msg.laes();
            for (Player p : Bukkit.getOnlinePlayers()) {
                anvend(p);
            }
            msg.send(s, "reloaded");
            return true;
        }
        if ((a1.equals("set") || a1.equals("add") || a1.equals("remove")) && a.length > 2) {
            OfflinePlayer o = find(a[1]);
            int n;
            try {
                n = Integer.parseInt(a[2]);
            } catch (NumberFormatException ex) {
                msg.send(s, "not_number");
                return true;
            }
            int h = hjerter(o.getUniqueId());
            int ny = a1.equals("set") ? n : a1.equals("add") ? h + n : Math.subtractExact(h, n);
            ny = Math.max(1, Math.min(ny, Math.max(maks(), n)));
            saetHjerter(o.getUniqueId(), ny);
            msg.send(s, "admin_set", "{player}", a[1], "{hearts}", String.valueOf(ny));
            return true;
        }
        if (a1.equals("giveheart") && a.length > 1) {
            Player p = Bukkit.getPlayerExact(a[1]);
            if (p == null) {
                msg.send(s, "not_online");
                return true;
            }
            int n = 1;
            if (a.length > 2) {
                try {
                    n = Math.max(1, Integer.parseInt(a[2]));
                } catch (NumberFormatException ex) {
                    n = 1;
                }
            }
            p.getInventory().addItem(hjerteItem(n));
            msg.send(s, "gave_heart", "{player}", p.getName(), "{amount}", String.valueOf(n));
            return true;
        }
        if (a1.equals("revive") && a.length > 1) {
            OfflinePlayer o = find(a[1]);
            String navn = o.getName() == null ? a[1] : o.getName();
            Bukkit.getBanList(BanList.Type.NAME).pardon(navn);
            data.set("players." + o.getUniqueId() + ".eliminated", null);
            data.set("players." + o.getUniqueId() + ".hearts", getConfig().getInt("revive_hearts", 3));
            gem();
            Player p = Bukkit.getPlayer(o.getUniqueId());
            if (p != null) {
                if (p.getGameMode().name().equals("SPECTATOR")) {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.teleport(p.getWorld().getSpawnLocation());
                }
                anvend(p);
                msg.send(p, "revived");
            }
            Bukkit.broadcastMessage(msg.c("prefix") + msg.c("revived_broadcast", "{player}", navn));
            return true;
        }
        msg.send(s, "usage", "{label}", label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> valg = new ArrayList<String>();
        String c = cmd.getName().toLowerCase();
        if (c.equals("lifesteal")) {
            if (a.length == 1) {
                valg.addAll(Arrays.asList("set", "add", "remove", "giveheart", "revive", "reload"));
            } else if (a.length == 2) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    valg.add(p.getName());
                }
            }
        } else if (c.equals("hearts") && a.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                valg.add(p.getName());
            }
        }
        List<String> ud = new ArrayList<String>();
        String sidste = a.length == 0 ? "" : a[Math.subtractExact(a.length, 1)].toLowerCase();
        for (String v : valg) {
            if (v.toLowerCase().startsWith(sidste)) {
                ud.add(v);
            }
        }
        return ud;
    }
}
