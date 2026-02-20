package io.bountysmp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BountyCommands {
    private BountyCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("bounty")
                .then(CommandManager.literal("gui")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                    BountyData data = BountyManager.getData(ctx.getSource().getServer());
                                    BountyPlayerState state = data.getOrCreate(player.getUuid());
                                    state.setGuiEnabled(enabled);
                                    data.markDirty();

                                    player.sendMessage(Text.literal("[Bounty] GUI " + (enabled ? "attivata" : "disattivata")).formatted(Formatting.YELLOW), false);
                                    return 1;
                                })))
                .then(CommandManager.literal("view")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            BountyData data = BountyManager.getData(ctx.getSource().getServer());
                            BountyPlayerState state = data.getOrCreate(player.getUuid());

                            String targetName = "Nessuno";
                            if (state.getCurrentTarget() != null) {
                                ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(state.getCurrentTarget());
                                targetName = target != null ? target.getGameProfile().getName() : "Offline";
                            }

                            player.sendMessage(Text.literal("[Bounty] Vite: " + state.getHearts() + " | Target: " + targetName)
                                    .formatted(Formatting.GOLD), false);
                            return 1;
                        }))
        );

        dispatcher.register(CommandManager.literal("resuscita")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            ServerPlayerEntity donor = ctx.getSource().getPlayerOrThrow();
                            ServerPlayerEntity receiver = EntityArgumentType.getPlayer(ctx, "player");

                            if (donor.getUuid().equals(receiver.getUuid())) {
                                donor.sendMessage(Text.literal("[Bounty] Non puoi resuscitare te stesso.").formatted(Formatting.RED), false);
                                return 0;
                            }

                            BountyData data = BountyManager.getData(ctx.getSource().getServer());
                            BountyPlayerState donorState = data.getOrCreate(donor.getUuid());
                            BountyPlayerState receiverState = data.getOrCreate(receiver.getUuid());

                            int heartsToDonate = donorState.getHearts() / 2;
                            if (heartsToDonate <= 0) {
                                donor.sendMessage(Text.literal("[Bounty] Non hai cuori sufficienti da donare.").formatted(Formatting.RED), false);
                                return 0;
                            }

                            donorState.setHearts(donorState.getHearts() - heartsToDonate);
                            receiverState.setHearts(receiverState.getHearts() + heartsToDonate);
                            data.markDirty();

                            donor.sendMessage(Text.literal("[Bounty] Hai donato " + heartsToDonate + " cuori a " + receiver.getGameProfile().getName())
                                    .formatted(Formatting.GREEN), false);
                            receiver.sendMessage(Text.literal("[Bounty] Hai ricevuto " + heartsToDonate + " cuori da " + donor.getGameProfile().getName())
                                    .formatted(Formatting.GREEN), false);
                            return 1;
                        }))
        );
    }
}
