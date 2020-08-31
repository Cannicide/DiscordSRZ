package us.cannicide.discordsrz;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiscordSRZ extends JavaPlugin {

    private static DiscordSRZ plugin;
    private static FileConfiguration config;

    public static DiscordSRZ getPlugin() {
        return DiscordSRZ.getPlugin(DiscordSRZ.class);
    }

    public static DiscordSRZ getInstance() {
        return plugin;
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info(ChatColor.GOLD + "Disabled DiscordSRZ");
    }

    @Override
    public void onEnable() {
        plugin = this;
        this.saveDefaultConfig();
        config = this.getConfig();

        getPlugin().getLogger().info(ChatColor.GOLD + "Started DiscordSRZ");
        this.getCommand("discord").setExecutor(new DiscordCommand());
        this.getCommand("discord").setTabCompleter(new DiscordCommand());
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        config.set("version", getPlugin().getDescription().getVersion());
        this.saveConfig();

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new DiscordExpansion(this).register();
        }
        else {
            getPlugin().getLogger().info(ChatColor.LIGHT_PURPLE + "Could not register DiscordSRZ Placeholders.");
        }
    }

    public static void syncRoles(CommandSender sender, int code) {
        List<String> groups = config.getStringList("groups");
        List<String> roles = config.getStringList("roles");
        List<String> placeholders = config.getStringList("placeholders");
        Player player = (Player) sender;

        String resp = "{\"user\":\"" + player.getUniqueId() + "\",\"code\":%code%,\"data\":{\"sync\":[%data%],\"placeholders\":[%placeholders%]}}";
        List<String> datalist = new ArrayList<>();
        List<String> plclist = new ArrayList<>();

        if (groups.size() != roles.size()) {
            sender.sendMessage("Not all groups have associated roles set in the config; please contact a System Administrator to fix this issue.");
            return;
        }

        for (int i = 0; i < groups.size(); i++) {
            if (player.hasPermission(groups.get(i))) {
                datalist.add("\"" + roles.get(i) + "\"");
            }
        }

        for (String placeholder : placeholders) {
            plclist.add("\"" + PlaceholderAPI.setPlaceholders(player, placeholder) + "\"");
        }

        String data = String.join(",", datalist);
        String plc = String.join(",", plclist);

        resp = resp.replace("%code%", "" + code).replace("%data%", data).replace("%placeholders%", plc);

        byte[] out = resp.getBytes(StandardCharsets.UTF_8);
        if (config.getBoolean("debug-mode")) sender.sendMessage("Sent request to url: " + config.getString("url"));
        new PostRequest(config.getString("url"), out);
    }

    public static void incrementCode() {
        config.set("code", config.getInt("code", 10000) + 1);
        getPlugin().saveConfig();
    }

    public static int getUniqueCode() {
        return config.getInt("code");
    }

    public static void initialSync(CommandSender sender) {
        DiscordSRZ.addLinkedPlayer(sender);
        DiscordSRZ.incrementCode();
        DiscordSRZ.syncRoles(sender, DiscordSRZ.getUniqueCode());
    }

    public static void unSync(CommandSender sender) {
        DiscordSRZ.syncRoles(sender, -1);
        DiscordSRZ.removeLinkedPlayer(sender);
    }

    public static void addLinkedPlayer(CommandSender sender) {
        List<String> linked = config.getStringList("linked");
        linked.add(((Player) sender).getUniqueId().toString());
        config.set("linked", linked);
        getPlugin().saveConfig();
    }

    public static void removeLinkedPlayer(CommandSender sender) {
        List<String> linked = config.getStringList("linked");
        linked.remove(((Player) sender).getUniqueId().toString());
        config.set("linked", linked);
        getPlugin().saveConfig();
    }

    public static boolean isLinkedPlayer(CommandSender sender) {
        List<String> linked = config.getStringList("linked");
        boolean containsPlayer = false;

        for (String item : linked) {
            if (item.equals(((Player) sender).getUniqueId().toString())) {
                containsPlayer = true;
            }
        }

        return containsPlayer;
    }

    public static String parseColors(String txt) {
        return txt.replaceAll("&([0-9a-f])", "\u00A7$1");
    }

    public static class PlayerJoinListener implements Listener {

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (DiscordSRZ.isLinkedPlayer(player)) DiscordSRZ.syncRoles(player, 0);
        }

    }

    public static class PostRequest {

        public PostRequest(String link, byte[] out) {
            BukkitRunnable r = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(link);
                        URLConnection con = url.openConnection();
                        HttpURLConnection http = (HttpURLConnection) con;
                        http.setRequestMethod("POST");
                        http.setDoOutput(true);

                        int length = out.length;

                        http.setFixedLengthStreamingMode(length);
                        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        http.connect();
                        try(OutputStream os = http.getOutputStream()) {
                            os.write(out);
                        }
                    }
                    catch (IOException e) {
                        getPlugin().getLogger().severe(ChatColor.RED + "Failed to send POST request.");
                    }
                }
            };

            r.runTaskAsynchronously(DiscordSRZ.getInstance());
        }

    }

    public static class DiscordCommand implements CommandExecutor, TabCompleter {

        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                if (sender.hasPermission("discordsrz.use")) {
                    if (args.length >= 1 && args[0] != null) {
                        if (args[0].equalsIgnoreCase("help")) {
                            //Send help command
                            sendHelpMessage(sender, args);
                        } else if (args[0].equalsIgnoreCase("link")) {
                            if (DiscordSRZ.isLinkedPlayer(sender)) {
                                sender.sendMessage(parseColors(config.getString("messages.alreadylinked")));
                                return true;
                            }

                            DiscordSRZ.initialSync(sender);
                            sender.sendMessage(parseColors(config.getString("messages.link.instructions").replace("{name}", DiscordSRZ.config.getString("name"))));
                            sender.sendMessage(parseColors(config.getString("messages.link.code").replace("{code}", ChatColor.AQUA + "" + DiscordSRZ.getUniqueCode() + ChatColor.RESET)));
                        } else if (args[0].equalsIgnoreCase("unlink")) {
                            if (!DiscordSRZ.isLinkedPlayer(sender)) {
                                sender.sendMessage(parseColors(config.getString("messages.notlinked")));
                                return true;
                            }

                            DiscordSRZ.unSync(sender);
                            sender.sendMessage(parseColors(config.getString("messages.unlink")));
                        } else if (args[0].equalsIgnoreCase("reload")) {
                            if (!sender.hasPermission("discordsrz.admin")) {
                                sender.sendMessage(config.getString("messages.noperms"));
                                return true;
                            }

                            DiscordSRZ.getInstance().reloadConfig();
                            config = DiscordSRZ.getPlugin().getConfig();
                            sender.sendMessage(ChatColor.GREEN + "Successfully reloaded the config.");

                        } else {
                            sender.sendMessage(parseColors(config.getString("messages.invalidarg")));
                        }
                    } else {
                        //Send help command
                        sendHelpMessage(sender, args);
                    }
                }
                else {
                    sender.sendMessage(config.getString("messages.noperms"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "You must be a player to use that command.");
            }

            return true;
        }

        public static void sendHelpMessage(CommandSender sender, String[] args) {
            String msg;

            if (args.length >= 2) {
                //Send help on a specific command
                if (args[1].equalsIgnoreCase("link")) {
                    msg = parseColors(config.getString("messages.help.link")).replace("{command}", "/discord link");
                    sender.sendMessage(msg);
                }
                else if (args[1].equalsIgnoreCase("unlink")) {
                    msg = parseColors(config.getString("messages.help.unlink")).replace("{command}", "/discord unlink");
                    sender.sendMessage(msg);
                }
                else if (args[1].equalsIgnoreCase("reload")) {
                    msg = sender.hasPermission("discordsrz.admin") ? parseColors(config.getString("messages.help.reload")).replace("{command}", "/discord reload") : ChatColor.AQUA + "[Admin Command]" + ChatColor.RESET + " - You do not have permission to use this command.";
                    sender.sendMessage(msg);
                }
                else {
                    msg = "The specified command does not exist.";
                    sender.sendMessage(msg);
                }
            }
            else {
                //Send help on all commands + info on DiscordSRZ
                msg = ChatColor.GREEN + "DiscordSRZ v" + DiscordSRZ.getPlugin().getDescription().getVersion() + " by " + DiscordSRZ.getPlugin().getDescription().getAuthors() + ChatColor.RESET;
                sender.sendMessage(msg);
                msg = parseColors(config.getString("messages.help.help")).replace("{command}", "/discord help");
                sender.sendMessage(msg);
                msg = parseColors(config.getString("messages.help.link")).replace("{command}", "/discord link");
                sender.sendMessage(msg);
                msg = parseColors(config.getString("messages.help.unlink")).replace("{command}", "/discord unlink");
                sender.sendMessage(msg);
                msg = sender.hasPermission("discordsrz.admin") ? parseColors(config.getString("messages.help.reload")).replace("{command}", "/discord reload") : ChatColor.AQUA + "[Admin Command]" + ChatColor.RESET + " - You do not have permission to use this command.";
                sender.sendMessage(msg);
            }

        }

        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if(command.getName().equalsIgnoreCase("discord")){
                List<String> l = new ArrayList<>();


                //Define the possibilities for arg1
                if (args.length == 1){
                    l.add("help");
                    l.add("link");
                    l.add("unlink");
                    l.add("reload");
                }

                //Define the possibilities for arg2
                else if (args.length == 2){
                    if (args[0].equalsIgnoreCase("help")) {
                        l.add("link");
                        l.add("unlink");
                        l.add("reload");
                    }
                    else {
                        return null;
                    }
                }

                return l; //returns the possibilities to the client

            }

            return null;

        }

    }

    public static class DiscordExpansion extends PlaceholderExpansion {

        private final DiscordSRZ plugin;

        /**
         * Since we register the expansion inside our own plugin, we
         * can simply use this method here to get an instance of our
         * plugin.
         *
         * @param plugin
         *        The instance of our plugin.
         */
        public DiscordExpansion(DiscordSRZ plugin){
            this.plugin = plugin;
        }

        /**
         * Because this is an internal class,
         * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
         * PlaceholderAPI is reloaded
         *
         * @return true to persist through reloads
         */
        @Override
        public boolean persist(){
            return true;
        }

        /**
         * Because this is a internal class, this check is not needed
         * and we can simply return {@code true}
         *
         * @return Always true since it's an internal class.
         */
        @Override
        public boolean canRegister(){
            return true;
        }

        /**
         * The name of the person who created this expansion should go here.
         * <br>For convenience do we return the author from the plugin.yml
         *
         * @return The name of the author as a String.
         */
        @Override
        public String getAuthor(){
            return plugin.getDescription().getAuthors().toString();
        }

        /**
         * The placeholder identifier should go here.
         * <br>This is what tells PlaceholderAPI to call our onRequest
         * method to obtain a value if a placeholder starts with our
         * identifier.
         * <br>The identifier has to be lowercase and can't contain _ or %
         *
         * @return The identifier in {@code %<identifier>_<value>%} as String.
         */
        @Override
        public String getIdentifier(){
            return "punishcmd";
        }

        /**
         * This is the version of the expansion.
         * <br>You don't have to use numbers, since it is set as a String.
         *
         * For convenience do we return the version from the plugin.yml
         *
         * @return The version as a String.
         */
        @Override
        public String getVersion(){
            return plugin.getDescription().getVersion();
        }

        /**
         * This is the method called when a placeholder with our identifier
         * is found and needs a value.
         * <br>We specify the value identifier in this method.
         * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
         *
         * @param  player - The player
         * @param  identifier
         *         A String containing the identifier/value.
         *
         * @return possibly-null String of the requested identifier.
         */
        @Override
        public String onPlaceholderRequest(Player player, String identifier){

            if(player == null){
                return "";
            }

            // %discord_linked%
            if(identifier.equals("linked")){
                return isLinkedPlayer(player) ? "yes" : "no";
            }

            return null;
        }
    }
}
