package com.nightbeam.tbos.platform;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.platform.services.INetworkHelper;
import com.nightbeam.tbos.platform.services.IPlatformHelper;
import com.nightbeam.tbos.platform.services.IRegistryHelper;
import java.util.ServiceLoader;

/**
 * The one seam between the loader-agnostic mod and the loader running it.
 *
 * <p>Common code is compiled against vanilla only, so anything that needs
 * NeoForge or Fabric has to come through here. Each loader project ships a
 * {@code META-INF/services} file naming its implementation, and the JDK's
 * service loader picks the right one at runtime.
 */
public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IRegistryHelper REGISTRIES = load(IRegistryHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        T service = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No platform implementation for " + clazz.getName()
                                + "; the loader project is missing its META-INF/services entry"));
        Yesterglass.LOGGER.debug("Loaded {} for service {}", service.getClass().getName(), clazz.getName());
        return service;
    }
}
