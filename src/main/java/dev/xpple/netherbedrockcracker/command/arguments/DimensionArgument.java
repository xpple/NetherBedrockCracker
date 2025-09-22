package dev.xpple.netherbedrockcracker.command.arguments;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DimensionArgument implements ArgumentType<ResourceKey<Level>> {

    private static final Collection<String> EXAMPLES = List.of("overworld", "the_nether", "the_end");

    private static final DynamicCommandExceptionType UNKNOWN_DIMENSION_EXCEPTION = new DynamicCommandExceptionType(dimension -> Component.translatableEscape("argument.dimension.invalid", dimension));

    private static final Map<ResourceLocation, ResourceKey<Level>> DIMENSIONS = ImmutableMap.<ResourceLocation, ResourceKey<Level>>builder()
        .put(Level.OVERWORLD.location(), Level.OVERWORLD)
        .put(Level.NETHER.location(), Level.NETHER)
        .put(Level.END.location(), Level.END)
        .build();

    public static DimensionArgument dimension() {
        return new DimensionArgument();
    }

    @SuppressWarnings("unchecked")
    public static ResourceKey<Level> getDimension(CommandContext<FabricClientCommandSource> context, String name) {
        return (ResourceKey<Level>) context.getArgument(name, ResourceKey.class);
    }

    @Override
    public ResourceKey<Level> parse(StringReader reader) throws CommandSyntaxException {
        ResourceLocation resourceLocation = ResourceLocation.read(reader);
        ResourceKey<Level> dimension = DIMENSIONS.get(resourceLocation);
        if (dimension == null) {
            throw UNKNOWN_DIMENSION_EXCEPTION.create(resourceLocation);
        }
        return dimension;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(DIMENSIONS.keySet(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
