package com.nightbeam.tbos.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
    /** Human-readable loader name, used only in logs. */
    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** The instance's {@code config/} directory. Both loaders guarantee it exists. */
    Path configDir();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
