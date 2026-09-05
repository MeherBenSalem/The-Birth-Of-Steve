package com.nightbeam.tbos.neoforge;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.command.YesterglassCommands;
import com.nightbeam.tbos.neoforge.platform.NeoForgeRegistryHelper;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge 1.20.1 entry point: loads the common mod, commits the registrars, then
 * adapts Forge's event bus onto the loader-neutral handlers.
 */
@Mod(Yesterglass.MOD_ID)
public final class TbosNeoForge {
    // Forge 1.20.1 constructs the @Mod class through its no-argument
    // constructor and offers the mod bus from FMLJavaModLoadingContext instead;
    // only NeoForge 1.20.4+ injects the bus as a constructor parameter.
    public TbosNeoForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Yesterglass.init();
        NeoForgeRegistryHelper.attachAll(modBus);
        // Forge 1.20.1's @Mod has no dist attribute: the dedicated server classloads
        // this constructor, so the client bootstrap must be dist-guarded.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TbosNeoForgeClient::init);

        modBus.addListener(TbosNeoForge::registerGameTests);
        modBus.addListener(TbosNeoForge::createAttributes);

        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onBreakBlock);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onPlaceBlock);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onExplosion);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onChunkLoaded);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onLivingDeath);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::memoryIncoming);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, false,
                net.minecraftforge.event.entity.living.LivingDamageEvent.class, TbosNeoForge::memoryDamaged);
        MinecraftForge.EVENT_BUS.addListener(TbosNeoForge::onLivingHeal);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(TbosNeoForgeGameTests.class);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        ModEntities.forEachAttribute((type, builder) -> event.put(type, builder.build()));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        YesterglassCommands.register(event.getDispatcher());
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TemporalSiteEvents.onServerTick(event.getServer());
        ArchiveRunEvents.onServerTick(event.getServer());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        TemporalSiteEvents.onServerStarted(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TemporalSiteEvents.onServerStopped(event.getServer());
        ArchiveRunEvents.onServerStopped(event.getServer());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TemporalSiteEvents.onPlayerLoggedIn(player);
            ArchiveRunEvents.onPlayerLoggedIn(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TemporalSiteEvents.onPlayerChangedDimension(player);
            ArchiveRunEvents.onPlayerChangedDimension(player);
        }
    }

    private static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        boolean allowed = TemporalSiteEvents.allowBreak(level, event.getPos(), player)
                && ArchiveRunEvents.allowBreak(level, event.getPos(), player);
        if (!allowed) {
            event.setCanceled(true);
        }
    }

    private static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean allowed = TemporalSiteEvents.allowPlace(level, event.getPos(), event.getEntity())
                && ArchiveRunEvents.allowPlace(level, event.getPos(), event.getEntity());
        if (!allowed) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TemporalSiteEvents.filterExplosion(level, event.getAffectedBlocks());
            ArchiveRunEvents.filterExplosion(level, event.getAffectedBlocks());
        }
    }

    private static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TemporalSiteEvents.onChunkLoaded(level, event.getChunk().getPos());
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!ArchiveRunEvents.allowDeath(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onLivingHeal(LivingHealEvent event) {
        event.setAmount(ArchiveRunEvents.scaleHeal(event.getEntity(), event.getAmount()));
    }

    private static void memoryIncoming(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            float amount=com.nightbeam.tbos.memory.MemoryCombat.incoming(player,event.getSource(),event.getAmount());
            if(amount<=0)event.setCanceled(true);else event.setAmount(amount);
        }
    }
    private static void memoryDamaged(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if(event.getSource().getEntity() instanceof ServerPlayer player)
            com.nightbeam.tbos.memory.MemoryCombat.successfulAttack(player,event.getEntity(),event.getAmount());
    }
}
