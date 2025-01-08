# Playtime Logger Mod

### Purpose

If you ever encountered a problem with monitoring your server's activity, you might be interested in this modification.
This mod uses Google Spreadsheets API to automatically log every player's logins and logouts, calculates individual session timeframes and shows this information in an easy and comfort way.

### Setup

1. Download and install this mod on your dedicated server.
2. For now, you can follow [this tutorial](https://developers.google.com/sheets/api/quickstart/java#set-up-environment)
- Follow the [link](https://console.cloud.google.com/apis/dashboard) and create new Project
- Enable the API
- Create [OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent) and setup External user type. After that, you can use this project in the testing environment.
- Don't forget to add scope "./auth/spreadsheets"
3. Download your generated [OAuth client](https://console.cloud.google.com/apis/credentials) as a json file.
4. Rename it to "credentials.json" and put this file to "/config/playtimelogger/" (if you have started your server, this directory should already exist)
5. Restart your server. If everything works correctly, you will be prompted to follow the link in your server console. Follow the link and authorize with Google project that you created. Your credentials will be used by Playtime Logger to generate and update spreadsheet.

### Usage

After successful initialization you can start tracking your players' playtime activity. Go to the Google Spreadsheets, authorize with your Google account and search for "Playtime Logger" spreadsheet (the name might be different if you replaced it in the config file).
There you will see two sheets:
1. First one, by default, is called "Total Playtime". There you will see all unique players that visited your server starting from the moment you installed the mod.
There you will see players' names, UUIDs (for servers in offline mode), playtime for specified period and last login date.
2. The second sheet is called "Logs" by default. There you will see individual logins and logouts of every player. Those events are described by Epochs (in seconds), Dates (using =EPOCHTODATE formula), and session playtime in seconds.

### License

This project is available under the MIT license. Feel free to learn from it and incorporate it in your own projects.
