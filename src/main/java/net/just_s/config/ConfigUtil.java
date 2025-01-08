package net.just_s.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.just_s.PTLMod;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigUtil {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;
    private PTLConfig config;

    private final Logger LOGGER;

    public ConfigUtil(Path path, Logger logger) {
        LOGGER = logger;
        configFile = path.toFile();
    }

    public PTLConfig getData() {
        if (config == null) {
            config = new PTLConfig();
        }
        return config;
    }

    public synchronized void setData(PTLConfig newConfig) {
        config = newConfig;
    }

    protected JsonElement serialize() {
        return PTLConfig.CODEC
                .encode(getData(), JsonOps.INSTANCE, JsonOps.INSTANCE.empty())
                .getOrThrow(false, LOGGER::error);
    }

    protected void deserialize(JsonElement element) {
        config = PTLConfig.CODEC.decode(JsonOps.INSTANCE, element).getOrThrow(false, LOGGER::error).getFirst();
    }

    public void save() {
        JsonElement json = serialize();
        try (FileWriter w = new FileWriter(configFile)) {
            final JsonWriter jw = GSON.newJsonWriter(w);
            jw.setIndent("\t");
            GSON.toJson(json, jw);
            PTLMod.LOGGER.info("Saved new config file.");
        } catch (IOException e) {LOGGER.error("Error while saving:{}", e.getMessage());}
    }

    public void loadOrCreate() {
        if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (!(parent.exists() || parent.mkdirs())) {
                LOGGER.warn("Can't create config: {}", configFile.getAbsolutePath());
                return;
            }
            save();
        } else {
            load();
        }
    }

    public void load() {
        try (FileReader f = new FileReader(configFile)) {
            deserialize(JsonParser.parseReader(f));
        } catch (Exception e) {
            LOGGER.warn("Exception occurred while reading config. ", e);
            save();
        }
    }
}
