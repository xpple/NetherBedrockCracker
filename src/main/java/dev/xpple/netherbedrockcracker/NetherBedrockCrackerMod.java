package dev.xpple.netherbedrockcracker;

import com.mojang.brigadier.CommandDispatcher;
import dev.xpple.netherbedrockcracker.command.commands.CrackCommand;
import dev.xpple.netherbedrockcracker.command.commands.SourceCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandBuildContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NetherBedrockCrackerMod implements ClientModInitializer {

    public static final String MOD_ID = "netherbedrockcracker";

    static {
        String libraryName = System.mapLibraryName("bedrockcracker");
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        Path tempFile;
        try {
            tempFile = Files.createTempFile(libraryName, "");
            Files.copy(modContainer.findPath(libraryName).orElseThrow(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.load(tempFile.toAbsolutePath().toString());
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(NetherBedrockCrackerMod::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        CrackCommand.register(dispatcher);
        SourceCommand.register(dispatcher);
    }
}
