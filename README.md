# DiscordSRZ
Bukkit/Spigot plugin inspired by DiscordSRV. A light plugin that transfers data of your choice from a minecraft server to a webserver, allowing you to implement DiscordSRV-like features such as role synchronization on your own NodeJS discord bot.

Though inspired by DiscordSRV, DiscordSRZ is not a fork or clone of DiscordSRV. DiscordSRZ implements a code-based system to link minecraft accounts to discord accounts by submitting JSON data (including a unique code, player UUID, and placeholder data) to a specified URL in a POST request.
By only implementing this code and data POST submission process, DiscordSRZ allows you to employ the same features as DiscordSRV using code on your webserver, instead of consuming more minecraft server resources on the plugin's end in order to gain those same features. Role synchronization and more
can be done on the webserver's end as opposed to the plugin's end, making DiscordSRZ a lighter alternative to DiscordSRV for knowledgeable web and (javascript) discord bot developers.

As of **v1.2.0**, DiscordSRZ can now also send PlaceholderAPI placeholder data in the POST requests made while synchronizing and linking. This functionality goes beyond the current featureset of DiscordSRV, allowing you to transfer much more information between minecraft and discord (anything available in PlaceholderAPI, including but not limited to: player IP, op status, anti-cheat violations, economy balance data, factions data, custom placeholder data).

DiscordSRZ is not affiliated with DiscordSRV, and does not use any code from the DiscordSRV plugin.
