package net.just_s;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import net.just_s.config.ConfigUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;

public class GoogleSheetsUtil {
    public enum Event {
        LOGIN,
        LOGOUT
    }

    private static Sheets sheetsAPI = null;
    private static Logger LOGGER;
    private static ConfigUtil CONFIG;

    public static void init(Path credentialPath, Path tokensPath, String applicationName, ConfigUtil config, @Nullable Logger logger) {
        LOGGER = logger;
        CONFIG = config;
        try {
            sheetsAPI = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    authorizeWith(credentialPath, tokensPath)
            )
                    .setApplicationName(applicationName)
                    .build();

            if (!config.getData().is_generated()) {
                new Thread(GoogleSheetsUtil::generateSheets).start();
            }
        } catch (IOException | GeneralSecurityException e) {
            if (LOGGER != null) {
                LOGGER.error("Could not authorize google sheets API with \"{}\": {}", credentialPath, e);
            }
        }
    }

    private static Credential authorizeWith(Path credentialPath, Path tokensPath)
            throws IOException, GeneralSecurityException {

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(new FileInputStream(String.valueOf(credentialPath)))
        );

        GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                List.of(SheetsScopes.SPREADSHEETS)
        )
                .setDataStoreFactory(new FileDataStoreFactory(new File(String.valueOf(tokensPath))))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(
                googleAuthorizationCodeFlow, new LocalServerReceiver()
        )
                .authorize("user");
    }

    public static void logAsync(String playerName, UUID playerUUID, Event event) {
        if (sheetsAPI == null) {
            return;
        }
        new Thread(()->log(playerName, playerUUID, event)).start();
    }

    private static synchronized void log(String playerName, UUID playerUUID, Event event) {
        if (sheetsAPI == null) {
            return;
        }

        long epoch = System.currentTimeMillis() / 1000;

        try {
            // Add LOGIN or LOGOUT to Logs
            ValueRange responseLogs = sheetsAPI.spreadsheets()
                    .values()
                    .get(CONFIG.getData().spreadsheet_id(), CONFIG.getData().title_logs())
                    .execute();
            List<List<Object>> logEntries = responseLogs.getValues();

            int index = (logEntries != null) ? logEntries.size() + 1 : 2;

            ValueRange logBody = new ValueRange()
                    .setValues(
                            List.of(List.of(
                                    playerName,
                                    playerUUID.toString(),
                                    event.name(),
                                    epoch,
                                    "=EPOCHTODATE(" + epoch + ")",
                                    String.format("=IF(C%d=\"LOGOUT\"; IFERROR(D%d - MAXIFS(D$2:D%d; C$2:C%d; \"LOGIN\"; B$2:B%d; B%d); 0); \"\")", index, index, index, index, index, index)
                            ))
                    );
            sheetsAPI.spreadsheets()
                    .values()
                    .update(
                            CONFIG.getData().spreadsheet_id(),
                            String.format("%s!A%d:F%d", CONFIG.getData().title_logs(), index, index),
                            logBody
                    )
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            // Add formulas to Total if first time
            ValueRange responseTotal = sheetsAPI.spreadsheets()
                    .values()
                    .get(CONFIG.getData().spreadsheet_id(), CONFIG.getData().title_total())
                    .execute();
            List<List<Object>> rows = responseTotal.getValues();
            if (rows != null) {
                LOGGER.info("rows size: " + rows.size());
                for (List<Object> playerData : rows) {
                    LOGGER.warn(playerData.get(1) + " | " + playerUUID.toString());
                    if (playerData.get(1).equals(playerUUID.toString())) {
                        return;
                    }
                }
            }
            index = (rows == null) ? 2 : rows.size() + 1;

            ValueRange totalBody = new ValueRange()
                    .setValues(
                            List.of(List.of(
                                    playerName,
                                    playerUUID.toString(),
                                    String.format("=TEXT(SUMIFS(%s!F$2:F; %s!B$2:B; B%d; %s!E$2:E; \">\" & ((NOW() - 365))) / 86400; \"[hh]:mm:ss\")", CONFIG.getData().title_logs(), CONFIG.getData().title_logs(), index, CONFIG.getData().title_logs()),
                                    String.format("=TEXT(SUMIFS(%s!F$2:F; %s!B$2:B; B%d; %s!E$2:E; \">\" & (EOMONTH(NOW(); -1))) / 86400; \"[hh]:mm:ss\")", CONFIG.getData().title_logs(), CONFIG.getData().title_logs(), index, CONFIG.getData().title_logs()),
                                    String.format("=TEXT(SUMIFS(%s!F$2:F; %s!B$2:B; B%d; %s!E$2:E; \">\" & (NOW() - 7)) / 86400; \"[hh]:mm:ss\")", CONFIG.getData().title_logs(), CONFIG.getData().title_logs(), index, CONFIG.getData().title_logs()),
                                    String.format("=TEXT(SUMIFS(%s!F$2:F; %s!B$2:B; B%d; %s!E$2:E; \">\" & (NOW() - 1)) / 86400; \"[hh]:mm:ss\")", CONFIG.getData().title_logs(), CONFIG.getData().title_logs(), index, CONFIG.getData().title_logs()),
                                    String.format("=TEXT(MAXIFS(%s!E$2:E; %s!B$2:B; B%d; %s!C$2:C; \"LOGIN\"); \"DD:MM:YY hh:mm:ss\")", CONFIG.getData().title_logs(), CONFIG.getData().title_logs(), index, CONFIG.getData().title_logs())
                            ))
                    );
            sheetsAPI.spreadsheets()
                    .values()
                    .update(
                            CONFIG.getData().spreadsheet_id(),
                            String.format("%s!A%d:G%d", CONFIG.getData().title_total(), index, index),
                            totalBody
                    )
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (IOException e) {
            if (LOGGER != null) {
                LOGGER.error("Could not log {} at {} for {}: {}", event.name(), epoch, playerName, e.getMessage());
            }
        }
    }

    private static synchronized void generateSheets() {
        if (sheetsAPI == null) {
            return;
        }

        try {
            Spreadsheet spreadsheet = new Spreadsheet()
                    .setProperties(
                            new SpreadsheetProperties()
                                    .setTitle(CONFIG.getData().title())
                    );

            Sheets.Spreadsheets spreadsheets = sheetsAPI.spreadsheets();

            // Create spreadsheet
            spreadsheet = spreadsheets.create(spreadsheet)
                    .setFields("spreadsheetId")
                    .execute();

            CONFIG.setData(
                CONFIG.getData().withSpreadSheetID(spreadsheet.getSpreadsheetId())
            );
            CONFIG.save();

            // Init both sheets
            UpdateSheetPropertiesRequest updateSheetPropertiesRequest = new UpdateSheetPropertiesRequest()
                    .setProperties(
                            new SheetProperties()
                                    .setSheetId(0)
                                    .setTitle(CONFIG.getData().title_total())
                    )
                    .setFields("title");
            AddSheetRequest addSheetRequest = new AddSheetRequest()
                    .setProperties(
                            new SheetProperties()
                                    .setTitle(
                                            CONFIG.getData().title_logs()
                                    )
                    );

            BatchUpdateSpreadsheetRequest batchUpdateRequest =
                    new BatchUpdateSpreadsheetRequest()
                            .setRequests(
                                    List.of(
                                            new Request().setAddSheet(addSheetRequest),
                                            new Request().setUpdateSheetProperties(updateSheetPropertiesRequest)
                                    )
                            );
            spreadsheets.batchUpdate(
                    CONFIG.getData().spreadsheet_id(), batchUpdateRequest
            ).execute();

            // Create first sheet
            ValueRange appendBodyForTotalTab = new ValueRange()
                    .setValues(
                            List.of(List.of(
                                    "Player name",
                                    "Player UUID",
                                    "Playtime last YEAR",
                                    "Playtime last MONTH",
                                    "Playtime last WEEK",
                                    "Playtime last 24 HOURS",
                                    "Last Login Date"
                            ))
                    );

            spreadsheets.values().append(
                    CONFIG.getData().spreadsheet_id(), "%s!A1:G1".formatted(CONFIG.getData().title_total()), appendBodyForTotalTab
            )
                    .setValueInputOption("USER_ENTERED")
                    .execute();



            // Create second sheet
            ValueRange appendBodyForLogsTab = new ValueRange()
                    .setValues(
                            List.of(List.of(
                                    "Player Name",
                                    "Player UUID",
                                    "Event",
                                    "Epoch",
                                    "Date",
                                    "Session Playtime"
                            ))
                    );

            spreadsheets.values().append(
                            CONFIG.getData().spreadsheet_id(), "%s!A1:F1".formatted(CONFIG.getData().title_logs()), appendBodyForLogsTab
                    )
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            CONFIG.setData(
                    CONFIG.getData().withGenerated(true)
            );
            CONFIG.save();
        } catch (IOException e) {
            if (LOGGER != null) {
                LOGGER.error("Could not generate Sheets: {}", e.getMessage());
            }
        }
    }
}
