package com.nightbeam.tbos.item;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.network.chat.Component;

/**
 * Ordered page text for the Archivist's Journal. Common code so the page order is
 * defined once; the client renders it through the vanilla book screen.
 */
public final class ArchivistJournalPages {
    public static final int PAGE_COUNT = 9;

    private ArchivistJournalPages() {
    }

    public static List<Component> pages() {
        return IntStream.rangeClosed(1, PAGE_COUNT)
                .mapToObj(page -> (Component) Component.translatable("journal.tbos.page." + page))
                .toList();
    }
}
