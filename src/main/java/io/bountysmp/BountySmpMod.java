package io.bountysmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BountySmpMod implements ModInitializer {
    public static final String MOD_ID = "bountysmp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(BountyManager::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(BountyManager::onServerTick);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(BountyManager::onEntityKilled);
        CommandRegistrationCallback.EVENT.register(BountyCommands::register);
    }
}
