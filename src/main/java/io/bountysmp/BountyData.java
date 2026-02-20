package io.bountysmp;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BountyData extends PersistentState {
    private static final String DATA_NAME = "bountysmp_data";

    private final Map<UUID, BountyPlayerState> players = new HashMap<>();
    private long nextWeeklyRollEpochMs = 0L;

    public static BountyData get(ServerWorld world) {
        PersistentStateType<BountyData> type = new PersistentStateType<>(
                BountyData::new,
                BountyData::fromNbt,
                null
        );
        return world.getPersistentStateManager().getOrCreate(type, DATA_NAME);
    }

    public static BountyData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        BountyData data = new BountyData();
        data.nextWeeklyRollEpochMs = nbt.getLong("nextWeeklyRollEpochMs", 0L);

        NbtList list = nbt.getList("players", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound) element;
            UUID uuid = entry.getUuid("uuid");
            BountyPlayerState state = new BountyPlayerState();
            state.setHearts(entry.getInt("hearts", 10));
            state.setGuiEnabled(entry.getBoolean("guiEnabled", true));
            state.setCompletedWeeklyBounty(entry.getBoolean("completedWeeklyBounty", false));
            if (entry.containsUuid("currentTarget")) {
                state.setCurrentTarget(entry.getUuid("currentTarget"));
            }
            if (entry.containsUuid("lastTarget")) {
                state.setLastTarget(entry.getUuid("lastTarget"));
            }
            data.players.put(uuid, state);
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup wrapperLookup) {
        nbt.putLong("nextWeeklyRollEpochMs", nextWeeklyRollEpochMs);

        NbtList list = new NbtList();
        for (Map.Entry<UUID, BountyPlayerState> entry : players.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            playerNbt.putInt("hearts", entry.getValue().getHearts());
            playerNbt.putBoolean("guiEnabled", entry.getValue().isGuiEnabled());
            playerNbt.putBoolean("completedWeeklyBounty", entry.getValue().isCompletedWeeklyBounty());
            if (entry.getValue().getCurrentTarget() != null) {
                playerNbt.putUuid("currentTarget", entry.getValue().getCurrentTarget());
            }
            if (entry.getValue().getLastTarget() != null) {
                playerNbt.putUuid("lastTarget", entry.getValue().getLastTarget());
            }
            list.add(playerNbt);
        }
        nbt.put("players", list);
        return nbt;
    }

    public Map<UUID, BountyPlayerState> getPlayers() {
        return players;
    }

    public BountyPlayerState getOrCreate(UUID playerUuid) {
        return players.computeIfAbsent(playerUuid, ignored -> new BountyPlayerState());
    }

    public long getNextWeeklyRollEpochMs() {
        return nextWeeklyRollEpochMs;
    }

    public void setNextWeeklyRollEpochMs(long nextWeeklyRollEpochMs) {
        this.nextWeeklyRollEpochMs = nextWeeklyRollEpochMs;
    }
}
