package net.just_s.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PTLConfig(
        String spreadsheet_id,
        String title,
        String title_total,
        String title_logs,
        boolean is_generated
) {
    public static final Codec<PTLConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.fieldOf("spreadsheet_id").forGetter(PTLConfig::spreadsheet_id),
                    Codec.STRING.fieldOf("title").forGetter(PTLConfig::title),
                    Codec.STRING.fieldOf("title_total").forGetter(PTLConfig::title_total),
                    Codec.STRING.fieldOf("title_logs").forGetter(PTLConfig::title_logs),
                    Codec.BOOL.fieldOf("is_generated").forGetter(PTLConfig::is_generated)
            ).apply(instance, PTLConfig::new)
    );

    public PTLConfig() {
        this("this value will be replaced automatically", "Playtime Logger", "Total Playtime", "Logs", false);
    }

    public PTLConfig withSpreadSheetID(String spreadSheetID) {
        return new PTLConfig(spreadSheetID, this.title(), this.title_total(), this.title_logs(), this.is_generated());
    }

    public PTLConfig withTitle(String title) {
        return new PTLConfig(this.spreadsheet_id(), title, this.title_total(), this.title_logs(), this.is_generated());
    }

    public PTLConfig withTitleTotal(String titleTotal) {
        return new PTLConfig(this.spreadsheet_id(), this.title(), titleTotal, this.title_logs(), this.is_generated());
    }

    public PTLConfig withTitleLogs(String titleLogs) {
        return new PTLConfig(this.spreadsheet_id(), this.title(), this.title_total(), titleLogs, this.is_generated());
    }

    public PTLConfig withGenerated(boolean isGenerated) {
        return new PTLConfig(this.spreadsheet_id(), this.title(), this.title_total(), this.title_logs(), isGenerated);
    }
}
