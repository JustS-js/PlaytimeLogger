package net.just_s;

import net.fabricmc.api.DedicatedServerModInitializer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.just_s.config.ConfigUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class PTLMod implements DedicatedServerModInitializer {
	public static final String MOD_ID = "ptl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final String DIR = "playtimelogger";
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(DIR).resolve("playtimelogger.json");
	public static final ConfigUtil CONFIG = new ConfigUtil(CONFIG_PATH, LOGGER);

	@Override
	public void onInitializeServer() {
		ServerPlayConnectionEvents.JOIN.register(
				(serverPlayNetworkHandler, packetSender, minecraftServer) -> logPlayerConnection(serverPlayNetworkHandler.getPlayer())
		);
		ServerPlayConnectionEvents.DISCONNECT.register(
				(serverPlayNetworkHandler, minecraftServer) -> logPlayerDisconnection(serverPlayNetworkHandler.getPlayer())
		);

		CONFIG.loadOrCreate();

		Path credentials = FabricLoader.getInstance().getConfigDir().resolve(DIR).resolve("credentials.json");
		if (!Files.exists(credentials)) {
			LOGGER.warn("No credentials.json found in \"{}\"", FabricLoader.getInstance().getConfigDir().resolve(DIR));
			LOGGER.warn("If you have not setup your config - do that now please!! {}", CONFIG_PATH);
		} else {
			GoogleSheetsUtil.init(credentials, FabricLoader.getInstance().getConfigDir().resolve(DIR), MOD_ID, CONFIG, LOGGER);
		}
		LOGGER.info("playtime logger is ready.");
	}

	public static void logPlayerConnection(ServerPlayerEntity player) {
		logPlayer(player, false);
	}

	public static void logPlayerDisconnection(ServerPlayerEntity player) {
		logPlayer(player, true);
	}

	private static void logPlayer(ServerPlayerEntity player, boolean disconnect) {
		GoogleSheetsUtil.logAsync(
				player.getEntityName(),
				player.getUuid(),
				(disconnect) ? GoogleSheetsUtil.Event.LOGOUT : GoogleSheetsUtil.Event.LOGIN
		);
	}
}