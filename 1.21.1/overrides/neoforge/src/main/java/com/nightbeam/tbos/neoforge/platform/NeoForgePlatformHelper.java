package com.nightbeam.tbos.neoforge.platform;

import com.nightbeam.tbos.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public final class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
