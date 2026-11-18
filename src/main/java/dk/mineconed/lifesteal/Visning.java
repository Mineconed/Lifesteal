package dk.mineconed.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Made by Mineconed
 *
 * Title, actionbar, bossbar, sounds and clickable chat that work from 1.8 to 1.21.
 */
public final class Visning {

    private Object bar;

    @SuppressWarnings("deprecation")
    public static void titel(Player p, String t, String u, int ind, int bliv, int ud) {
        try {
            Method m = p.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            m.invoke(p, t, u, ind, bliv, ud);
        } catch (Throwable nyt) {
            try {
                p.sendTitle(t, u);
            } catch (Throwable ignore) {
                // no titles
            }
        }
    }

    /** Returns false if this version has no actionbar API (1.8). */
    public static boolean actionbar(Player p, String tekst) {
        try {
            Class<?> type = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object komp = komponenter(tekst);
            Object spigot = p.getClass().getMethod("spigot").invoke(p);
            Method send = spigot.getClass().getMethod("sendMessage", type, komp.getClass());
            send.setAccessible(true);
            send.invoke(spigot, type.getField("ACTION_BAR").get(null), komp);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static Object komponenter(String tekst) throws Exception {
        Class<?> tc = Class.forName("net.md_5.bungee.api.chat.TextComponent");
        return tc.getMethod("fromLegacyText", String.class).invoke(null, tekst);
    }

    /**
     * Sends chat you can click. handling: url, command or suggest. Falls back to normal chat.
     */
    public static void klik(Player p, String tekst, String hover, String handling, String vaerdi) {
        if ((vaerdi == null || vaerdi.isEmpty()) && (hover == null || hover.isEmpty())) {
            p.sendMessage(tekst);
            return;
        }
        try {
            Object komp = komponenter(tekst);
            Class<?> ce = Class.forName("net.md_5.bungee.api.chat.ClickEvent");
            Class<?> cea = Class.forName("net.md_5.bungee.api.chat.ClickEvent$Action");
            Class<?> he = Class.forName("net.md_5.bungee.api.chat.HoverEvent");
            Class<?> hea = Class.forName("net.md_5.bungee.api.chat.HoverEvent$Action");
            Class<?> base = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Object klikEvent = null;
            if (vaerdi != null && !vaerdi.isEmpty()) {
                String a = "url".equalsIgnoreCase(handling) ? "OPEN_URL" : "suggest".equalsIgnoreCase(handling) ? "SUGGEST_COMMAND" : "RUN_COMMAND";
                klikEvent = ce.getConstructor(cea, String.class).newInstance(cea.getField(a).get(null), vaerdi);
            }
            Object hoverEvent = null;
            if (hover != null && !hover.isEmpty()) {
                hoverEvent = he.getConstructor(hea, komp.getClass()).newInstance(hea.getField("SHOW_TEXT").get(null), komponenter(hover));
            }
            int n = Array.getLength(komp);
            for (int i = 0; i < n; i++) {
                Object k = Array.get(komp, i);
                if (klikEvent != null) {
                    base.getMethod("setClickEvent", ce).invoke(k, klikEvent);
                }
                if (hoverEvent != null) {
                    base.getMethod("setHoverEvent", he).invoke(k, hoverEvent);
                }
            }
            Object spigot = p.getClass().getMethod("spigot").invoke(p);
            Method send = spigot.getClass().getMethod("sendMessage", komp.getClass());
            send.setAccessible(true);
            send.invoke(spigot, komp);
        } catch (Throwable e) {
            p.sendMessage(tekst);
        }
    }

    public static void lyd(Player p, String... navne) {
        for (String n : navne) {
            if (n == null || n.isEmpty()) {
                continue;
            }
            try {
                p.playSound(p.getLocation(), Sound.valueOf(n.toUpperCase()), 1f, 1f);
                return;
            } catch (Throwable ignore) {
                // try next name
            }
        }
    }

    /** PlaceholderAPI if it is installed. */
    public static String papi(Player p, String s) {
        if (p == null || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return s;
        }
        try {
            Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) c.getMethod("setPlaceholders", Player.class, String.class).invoke(null, p, s);
        } catch (Throwable e) {
            return s;
        }
    }

    /** Shows or updates the bossbar for these players (1.9+). progress 0 to 1. */
    public boolean bossbar(Iterable<? extends Player> spillere, String tekst, double progress, String farveNavn) {
        try {
            Class<?> farve = Class.forName("org.bukkit.boss.BarColor");
            if (bar == null) {
                Class<?> stil = Class.forName("org.bukkit.boss.BarStyle");
                Class<?> flag = Class.forName("org.bukkit.boss.BarFlag");
                Method lav = Bukkit.class.getMethod("createBossBar", String.class, farve, stil, Array.newInstance(flag, 0).getClass());
                bar = lav.invoke(null, tekst, farve.getField("WHITE").get(null), stil.getField("SOLID").get(null), Array.newInstance(flag, 0));
            }
            try {
                bar.getClass().getMethod("setColor", farve).invoke(bar, farve.getField(farveNavn.toUpperCase()).get(null));
            } catch (Throwable ignore) {
                // keep color
            }
            bar.getClass().getMethod("setTitle", String.class).invoke(bar, tekst);
            bar.getClass().getMethod("setProgress", double.class).invoke(bar, Math.max(0.0, Math.min(1.0, progress)));
            Method add = bar.getClass().getMethod("addPlayer", Player.class);
            for (Player p : spillere) {
                add.invoke(bar, p);
            }
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public void fjernBossbar() {
        if (bar == null) {
            return;
        }
        try {
            bar.getClass().getMethod("removeAll").invoke(bar);
        } catch (Throwable ignore) {
            // ignore
        }
        bar = null;
    }
}
