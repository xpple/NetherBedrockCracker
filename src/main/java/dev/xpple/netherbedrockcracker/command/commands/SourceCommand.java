package dev.xpple.netherbedrockcracker.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.xpple.netherbedrockcracker.command.CustomClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static dev.xpple.clientarguments.arguments.CEntityArgument.*;
import static dev.xpple.clientarguments.arguments.CRotationArgument.*;
import static dev.xpple.clientarguments.arguments.CVec3Argument.*;
import static dev.xpple.netherbedrockcracker.command.arguments.DimensionArgument.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class SourceCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> rootLiteral = literal("nbc:source");
        LiteralCommandNode<FabricClientCommandSource> root = dispatcher.register(rootLiteral);
        dispatcher.register(rootLiteral
            .then(literal("run")
                .redirect(dispatcher.getRoot(), CommandContext::getSource))
            .then(literal("as")
                .then(argument("entity", entity())
                    .redirect(root, ctx -> CustomClientCommandSource.of(ctx.getSource()).withEntity(getEntity(ctx, "entity")))))
            .then(literal("positioned")
                .then(argument("pos", vec3())
                    .redirect(root, ctx -> CustomClientCommandSource.of(ctx.getSource()).withPosition(getVec3(ctx, "pos")))))
            .then(literal("rotated")
                .then(argument("rot", rotation())
                    .redirect(root, ctx -> CustomClientCommandSource.of(ctx.getSource()).withRotation(getRotation(ctx, "rot").getRotation(ctx.getSource())))))
            .then(literal("in")
                .then(argument("dimension", dimension())
                    .redirect(root, ctx -> CustomClientCommandSource.of(ctx.getSource()).withMeta("dimension", getDimension(ctx, "dimension"))))));
    }
}
