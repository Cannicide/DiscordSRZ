# ScavengerLink
Bukkit/Spigot plugin inspired by DiscordSRV. A light plugin that transfers data of your choice from a minecraft server to a webserver, allowing you to implement DiscordSRV-like features such as role synchronization on your own NodeJS discord bot. Formerly known as DiscordSRZ.

## Not Just Another DiscordSRV
Though inspired by DiscordSRV, ScavengerLink is not a fork or clone of DiscordSRV. ScavengerLink implements a code-based system to link minecraft accounts to discord accounts by submitting JSON data (including a unique code, player UUID, and placeholder data) to a specified URL in a POST request.
By only implementing this code and data POST submission process, ScavengerLink allows you to employ the same features as DiscordSRV using code on your webserver, instead of consuming more minecraft server resources on the plugin's end in order to gain those same features. Role synchronization and more
can be done on the webserver's end as opposed to the plugin's end, making ScavengerLink a MUCH lighter alternative to DiscordSRV for knowledgeable web and (javascript) discord bot developers.

As of **v1.2.0**, DiscordSRZ (now named ScavengerLink) can now also send PlaceholderAPI placeholder data in the POST requests made while synchronizing and linking. This functionality goes beyond the current featureset of DiscordSRV, allowing you to transfer much more information between minecraft and discord (anything available in PlaceholderAPI, including but not limited to: player IP, op status, anti-cheat violations, economy balance data, factions data, custom placeholder data).

As of **v3.1.2**, ScavengerLink now supports the latest versions of minecraft (1.16.4 and 1.16.5). It should also be able to support most versions from 1.8 to 1.16 as it was initially designed for 1.8, but the latest version of ScavengerLink has only been tested in 1.16.4.

DiscordSRZ is not affiliated with DiscordSRV, and does not use any code from the DiscordSRV plugin.

## How Much Lighter?
ScavengerLink is a very light plugin. Here's a direct size comparison.

- DiscordSRV 1.21.3 --> **8.11 MB** (8110 KB)
- ScavengerLink 3.2.1 --> **0.015 MB** (15 KB)

By exporting the actual role-assigning and other discord-related features to your own discord bot instead of including a pre-written discord bot within this plugin, ScavengerLink is able to minimize its size tremendously. All the plugin needs to do is manage numeric codes (creating a new one when a player attempts to link), send a POST request to your bot (with the minecraft user's UUID, generated code, any configured permission groups, and any configured placeholder data), and your bot will handle the rest.

## How Should My Bot Receive and Use the Data?
ScavengerLink sends a POST request to the URL specified in the configuration. Your bot will therefore require some sort of webserver that can handle HTTP requests. It will need to listen for a POST request at a specific URL, receive the JSON data from the request body, and use that data to do what you need to. If you want to use a system similar to DiscordSRV, your bot will need to save all of the JSON data in a database, and listen for a DM from a user that contains a saved generated code. After that, you will be able to use the placeholder data and/or permission groups (ranks) from the saved JSON data to synchronize the user's Discord roles with their Minecraft ranks or such. Every time an already linked user joins or leaves the Minecraft server, a POST request is also sent containing updated permission and placeholder data (and is distinguishable from the initial sync POST). Handle those separate POST requests to update a user's roles and data after they are initially linked. When a linked user decides to unlink, another POST request is additionally sent (distinguishable from both the initial and update sync POSTS) containing all of the same data, allowing you to cycle through the permission groups and remove any that the user has in the Discord server since they are unlinking.

Here is an example of the POST request body:
```
{ 
    user: 'UUID',
    code: (xxxxx = CODE) / (0 = SYNC) / (-1 = UNSYNC),
    data: { 
        sync: [ 'VIP', 'MVP', 'DonorRank' ],
        placeholders: [ 'value of %placeholder1%', 'value of %placeholder2%' ] 
    } 
}
```

The code is an integer that changes depending on whether this is an initial sync, update sync, or unsync POST request. On initial sync (when a user begins linking), `code` is the 5-digit generated code for the user. On update sync (when a linked user joins/leaves MC server), `code` is 0. On unsync (when a user unlinks), `code` is -1.

If you want a working example of some bot-side code that works with ScavengerLink, I use ScavengerLink in my [scav-bot](https://github.com/Cannicide/scav-bot/blob/master/discordsrz.js) discord bot (written in NodeJS and uses discord.js) and have fully functional role synchronization code written there. Note that my bot code is heavily dependent on my custom storage and DM-interpreter systems, so you will need to tweak the code to work with your own storage and message-handling systems.

## Can I Modify the Code?
Feel free to modify the code or make improvements, both with the ScavengerLink plugin and the scav-bot discord bot. Both are completely open-source, and all I ask is that you give credit where credit is due if you create your own fork or modification. It took me several weeks to create the plugin and several more to perfect it, and I have been working on the discord bot regularly for over a year now.

Created by **Cannicide#2753**
