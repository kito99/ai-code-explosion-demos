package com.example.dukeguess.telemetry;

/// Tag keys shared by the game's spans and meters.
///
/// Every value these carry is drawn from a fixed set, which keeps cardinality low and
/// keeps player names, which NFR-9 keeps out of telemetry, from ever becoming a tag.
public enum TelemetryTag {

    OUTCOME("outcome"),
    VERDICT("verdict");

    private final String key;

    TelemetryTag(final String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
