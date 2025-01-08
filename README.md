# Playtime Logger Mod

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
### License

This project is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
