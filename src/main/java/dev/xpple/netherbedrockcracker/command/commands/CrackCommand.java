package dev.xpple.netherbedrockcracker.command.commands;

import com.github.netherbedrockcracker.Block;
import com.github.netherbedrockcracker.NetherBedrockCracker;
import com.github.netherbedrockcracker.VecI64;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.seedfinding.mccore.util.math.NextLongReverser;
import dev.xpple.netherbedrockcracker.NetherBedrockCrackerMod;
import dev.xpple.netherbedrockcracker.command.CustomClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.mojang.brigadier.arguments.IntegerArgumentType.*;
import static dev.xpple.clientarguments.arguments.CEnumArgument.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;

public class CrackCommand {

    // leave one thread for the OS, and one to avoid blocking the client thread, but ensure at least one is used
    private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);

    private static final SimpleCommandExceptionType NOT_IN_NETHER_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.nbc:crack.notInNether"));
    private static final SimpleCommandExceptionType NOT_LOADED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.nbc:crack.notLoaded"));
    private static final SimpleCommandExceptionType ALREADY_CRACKING_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.nbc:crack.alreadyCracking"));

    private static final ExecutorService crackingExecutor = Executors.newCachedThreadPool();
    private static Future<?> currentTask = null;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("nbc:crack")
            .executes(ctx -> crack(CustomClientCommandSource.of(ctx.getSource())))
            .then(argument("threads", integer(1, MAX_THREADS))
                .executes(ctx -> crack(CustomClientCommandSource.of(ctx.getSource()), getInteger(ctx, "threads")))
                .then(argument("bedrockgeneration", enumArg(BedrockGeneration.class))
                    .executes(ctx -> crack(CustomClientCommandSource.of(ctx.getSource()), getInteger(ctx, "threads"), getEnum(ctx, "bedrockgeneration"))))));
    }

    private static int crack(CustomClientCommandSource source) throws CommandSyntaxException {
        return crack(source, MAX_THREADS);
    }

    private static int crack(CustomClientCommandSource source, int threads) throws CommandSyntaxException {
        return crack(source, threads, BedrockGeneration.NORMAL);
    }

    private static int crack(CustomClientCommandSource source, int threads, BedrockGeneration bedrockGen) throws CommandSyntaxException {
        ResourceKey<Level> dimension = source.getDimension();
        if (dimension != Level.NETHER) {
            throw NOT_IN_NETHER_EXCEPTION.create();
        }

        if (currentTask != null && !currentTask.isDone()) {
            throw ALREADY_CRACKING_EXCEPTION.create();
        }

        ClientChunkCache chunkSource = source.getLevel().getChunkSource();
        BlockPos position = BlockPos.containing(source.getPosition());
        ChunkPos centerChunkPos = ChunkPos.containing(position);

        List<BlockPos> bedrockPositions = new ArrayList<>();
        scanChunk(bedrockPositions, chunkSource.getChunk(centerChunkPos.x(), centerChunkPos.z(), ChunkStatus.FULL, false));
        scanChunk(bedrockPositions, chunkSource.getChunk(centerChunkPos.x() + 1, centerChunkPos.z(), ChunkStatus.FULL, false));
        scanChunk(bedrockPositions, chunkSource.getChunk(centerChunkPos.x(), centerChunkPos.z() + 1, ChunkStatus.FULL, false));
        scanChunk(bedrockPositions, chunkSource.getChunk(centerChunkPos.x() - 1, centerChunkPos.z(), ChunkStatus.FULL, false));
        scanChunk(bedrockPositions, chunkSource.getChunk(centerChunkPos.x(), centerChunkPos.z() - 1, ChunkStatus.FULL, false));

        currentTask = crackingExecutor.submit(() -> startCracking(source, bedrockPositions, threads, bedrockGen));
        return Command.SINGLE_SUCCESS;
    }

    private static void scanChunk(List<BlockPos> bedrockPositions, LevelChunk chunk) throws CommandSyntaxException {
        if (chunk == null) {
            throw NOT_LOADED_EXCEPTION.create();
        }

        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();

        // floor bedrock generates at 0 <= y <= 4
        // roof bedrock generates at 123 <= y <= 127
        // bedrock is rarest at y = 4 and y = 123, therefore yielding the most information
        LevelChunkSection floorSection = chunk.getSection(chunk.getSectionIndex(4));
        LevelChunkSection roofSection = chunk.getSection(chunk.getSectionIndex(123));

        for (int x = 0; x < LevelChunkSection.SECTION_WIDTH; x++) {
            for (int z = 0; z < LevelChunkSection.SECTION_HEIGHT; z++) {
                if (floorSection.getBlockState(x, 4 & 15, z).is(Blocks.BEDROCK)) {
                    bedrockPositions.add(new BlockPos(startX + x, 4, startZ + z));
                }
                if (roofSection.getBlockState(x, 123 & 15, z).is(Blocks.BEDROCK)) {
                    bedrockPositions.add(new BlockPos(startX + x, 123, startZ + z));
                }
            }
        }
    }

    private static void startCracking(CustomClientCommandSource source, List<BlockPos> bedrockPositions, int threads, BedrockGeneration bedrockGen) {
        Minecraft minecraft = source.getClient();

        int size = bedrockPositions.size();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment blockArray = Block.allocateArray(size, arena);
            for (int i = 0; i < size; i++) {
                MemorySegment block = Block.asSlice(blockArray, i);
                BlockPos pos = bedrockPositions.get(i);
                Block.x(block, pos.getX());
                Block.y(block, pos.getY());
                Block.z(block, pos.getZ());
                Block.block_type(block, NetherBedrockCracker.BEDROCK());
            }

            source.sendFeedback(Component.translatable("commands.nbc:crack.started", threads));
            MemorySegment vecI64 = null;
            try {
                vecI64 = NetherBedrockCracker.crack(arena, blockArray, size, threads, bedrockGen.num, NetherBedrockCracker.StructureSeed());
                long[] seeds = VecI64.ptr(vecI64).reinterpret(VecI64.len(vecI64) * NetherBedrockCracker.C_LONG_LONG.byteSize()).toArray(NetherBedrockCracker.C_LONG_LONG);

                if (seeds.length == 0) {
                    sendError(Component.translatable("commands.nbc:crack.noSeedFound"));
                    return;
                }
                sendFeedback(Component.translatable("commands.nbc:crack.foundStructureSeeds", seeds.length));
                for (long seed : seeds) {
                    sendFeedback(Component.translatable("commands.nbc:crack.entry", ComponentUtils.copyOnClickText(Long.toString(seed))));
                }
                for (long seed : seeds) {
                    for (long randomSeed : NextLongReverser.getSeeds(seed)) {
                        long ws = new Random(randomSeed ^ 0x5DEECE66DL).nextLong();
                        sendFeedback(Component.translatable("commands.nbc:crack.foundRandomWorldSeed", ComponentUtils.copyOnClickText(Long.toString(ws))));
                    }
                }
                if (minecraft.level == null) {
                    return;
                }
                final long biomeZoomSeed = minecraft.level.getBiomeManager().biomeZoomSeed;
                HashFunction sha256 = Hashing.sha256();
                for (long seed : seeds) {
                    for (long u = 0; u < 1L << 16; u++) {
                        long ws = (u << 48) | seed;
                        if (sha256.hashLong(ws).asLong() == biomeZoomSeed) {
                            sendFeedback(Component.translatable("commands.nbc:crack.foundWorldSeedByHash", ComponentUtils.copyOnClickText(Long.toString(ws))));
                        }
                    }
                }
            } finally {
                if (vecI64 != null) {
                    NetherBedrockCracker.free_vec(vecI64);
                }
            }
        }
    }

    private static void sendFeedback(MutableComponent component) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            NetherBedrockCrackerMod.LOGGER.info(component.getString());
            return;
        }
        Minecraft.getInstance().schedule(() -> player.sendSystemMessage(component));
    }

    private static void sendError(MutableComponent component) {
        sendFeedback(component.withStyle(ChatFormatting.RED));
    }

    private enum BedrockGeneration implements StringRepresentable {
        NORMAL(NetherBedrockCracker.Normal()),
        PAPER1_18(NetherBedrockCracker.Paper1_18());

        private final int num;

        BedrockGeneration(int num) {
            this.num = num;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name();
        }
    }
}
