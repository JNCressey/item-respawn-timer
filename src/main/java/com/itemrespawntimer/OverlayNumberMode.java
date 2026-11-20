package com.itemrespawntimer;

public enum OverlayNumberMode {
    SECONDS("Seconds"),
    TICKS("Ticks");

    private final String displayName;

    OverlayNumberMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
