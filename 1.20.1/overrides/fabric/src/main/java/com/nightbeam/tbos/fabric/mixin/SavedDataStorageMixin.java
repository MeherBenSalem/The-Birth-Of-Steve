package com.nightbeam.tbos.fabric.mixin;

/**
 * Intentionally empty for Minecraft 1.20.1.
 *
 * <p>This target stores its data through {@code SavedData.Factory}, whose
 * 1.20.1 implementation does not use the later {@code SavedDataStorage}
 * data-fixer path. The mixin remains a source shadow so the 26.x-only target
 * class is not compiled into this Java 21 build.
 */
final class SavedDataStorageMixin {
    private SavedDataStorageMixin() {
    }
}
