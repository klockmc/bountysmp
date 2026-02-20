package io.bountysmp;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class BountyManager {
    private static final long ONE_WEEK_MS = 7L * 24L * 60L * 60L * 1000L;
    private static long tickCounter = 0L;

    private BountyManager() {
    }

    public static void onServerStarted(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return;
        }

        BountyData data = BountyData.get(overworld);
        server.getPlayerManager().getPlayerList().forEach(player -> data.getOrCreate(player.getUuid()));

        if (data.getNextWeeklyRollEpochMs() <= 0L) {
            data.setNextWeeklyRollEpochMs(System.currentTimeMillis() + ONE_WEEK_MS);
            rollWeeklyBounties(server, data, false);
        }

        data.markDirty();
    }

    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return;
        }

        BountyData data = BountyData.get(overworld);
        if (System.currentTimeMillis() >= data.getNextWeeklyRollEpochMs()) {
            rollWeeklyBounties(server, data, true);
            data.setNextWeeklyRollEpochMs(System.currentTimeMillis() + ONE_WEEK_MS);
            data.markDirty();
        }

        if (tickCounter % 100L == 0L) {
            sendHud(server, data);
        }
    }

    public static void onEntityKilled(ServerWorld world, Entity killer, Entity killed) {
        if (!(killer instanceof ServerPlayerEntity killerPlayer) || !(killed instanceof ServerPlayerEntity deadPlayer)) {
            return;
        }

        BountyData data = BountyData.get(world);
        BountyPlayerState killerState = data.getOrCreate(killerPlayer.getUuid());
        UUID expectedTarget = killerState.getCurrentTarget();

        if (expectedTarget != null && expectedTarget.equals(deadPlayer.getUuid())) {
            killerState.setCompletedWeeklyBounty(true);
            killerPlayer.sendMessage(Text.literal("[Bounty] Hai completato la taglia settimanale!").formatted(Formatting.GREEN), false);
            data.markDirty();
        }
    }

    public static BountyData getData(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            throw new IllegalStateException("Overworld non disponibile");
        }
        return BountyData.get(overworld);
    }

    public static void rollWeeklyBounties(MinecraftServer server, BountyData data, boolean applyPenalty) {
        List<ServerPlayerEntity> alivePlayers = server.getPlayerManager().getPlayerList().stream()
                .filter(player -> data.getOrCreate(player.getUuid()).getHearts() > 0)
                .sorted(Comparator.comparing(player -> player.getGameProfile().getName().toLowerCase()))
                .toList();

        if (alivePlayers.size() < 2) {
            server.getPlayerManager().broadcast(Text.literal("[Bounty] Servono almeno 2 player vivi per generare le taglie.").formatted(Formatting.RED), false);
            return;
        }

        if (applyPenalty) {
            for (ServerPlayerEntity player : alivePlayers) {
                BountyPlayerState state = data.getOrCreate(player.getUuid());
                if (!state.isCompletedWeeklyBounty()) {
                    state.setHearts(state.getHearts() - 1);
                    player.sendMessage(Text.literal("[Bounty] Non hai completato la taglia: -1 cuore.").formatted(Formatting.RED), false);
                    if (state.getHearts() <= 0) {
                        banZeroHearts(server, player);
                    }
                }
            }
        }

        List<ServerPlayerEntity> playersForAssignment = new ArrayList<>(alivePlayers.stream()
                .filter(player -> data.getOrCreate(player.getUuid()).getHearts() > 0)
                .toList());

        if (playersForAssignment.size() < 2) {
            return;
        }

        Collections.shuffle(playersForAssignment);
        int guard = 0;
        while (!isValidPermutation(playersForAssignment, data) && guard++ < 100) {
            Collections.shuffle(playersForAssignment);
        }

        for (int i = 0; i < playersForAssignment.size(); i++) {
            ServerPlayerEntity hunter = playersForAssignment.get(i);
            ServerPlayerEntity target = playersForAssignment.get((i + 1) % playersForAssignment.size());
            BountyPlayerState hunterState = data.getOrCreate(hunter.getUuid());
            hunterState.setLastTarget(hunterState.getCurrentTarget());
            hunterState.setCurrentTarget(target.getUuid());
            hunterState.setCompletedWeeklyBounty(false);
            hunter.sendMessage(Text.literal("[Bounty] Nuovo target: " + target.getGameProfile().getName()).formatted(Formatting.GOLD), false);
        }

        server.getPlayerManager().broadcast(Text.literal("[Bounty] Nuova rotazione settimanale completata.").formatted(Formatting.AQUA), false);
        data.markDirty();
    }

    private static boolean isValidPermutation(List<ServerPlayerEntity> players, BountyData data) {
        for (int i = 0; i < players.size(); i++) {
            ServerPlayerEntity hunter = players.get(i);
            ServerPlayerEntity target = players.get((i + 1) % players.size());

            if (hunter.getUuid().equals(target.getUuid())) {
                return false;
            }

            BountyPlayerState state = data.getOrCreate(hunter.getUuid());
            UUID lastTarget = state.getLastTarget();
            if (lastTarget != null && lastTarget.equals(target.getUuid())) {
                return false;
            }
        }
        return true;
    }

    private static void sendHud(MinecraftServer server, BountyData data) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BountyPlayerState state = data.getOrCreate(player.getUuid());
            if (!state.isGuiEnabled() || state.getHearts() <= 0) {
                continue;
            }

            String targetName = "Nessuno";
            if (state.getCurrentTarget() != null) {
                ServerPlayerEntity target = server.getPlayerManager().getPlayer(state.getCurrentTarget());
                targetName = target != null ? target.getGameProfile().getName() : "Offline";
            }

            Text hudText = Text.literal("❤ " + state.getHearts() + " | Target: " + targetName).formatted(Formatting.RED);
            player.sendMessage(hudText, true);
        }
    }

    private static void banZeroHearts(MinecraftServer server, ServerPlayerEntity player) {
        GameProfile profile = player.getGameProfile();
        server.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(profile, null, "BountySMP", null, "0 vite"));
        player.networkHandler.disconnect(Text.literal("Sei stato bannato: hai 0 vite."));
    }
}
