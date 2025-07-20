package dev.xpple.netherbedrockcracker.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.xpple.clientarguments.arguments.CDimensionArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class CustomClientCommandSource extends ClientSuggestionProvider implements FabricClientCommandSource {

    private static final DynamicCommandExceptionType UNKNOWN_DIMENSION_EXCEPTION = new DynamicCommandExceptionType(dimension -> Component.translatableEscape("argument.dimension.invalid", dimension));

    private final Minecraft client;
    private final Entity entity;
    private final Vec3 position;
    private final Vec2 rotation;
    private final ClientLevel world;
    private final Map<String, Object> meta;

    public CustomClientCommandSource(ClientPacketListener listener, Minecraft minecraft, Entity entity, Vec3 position, Vec2 rotation, ClientLevel world, Map<String, Object> meta) {
        super(listener, minecraft);

        this.client = minecraft;
        this.entity = entity;
        this.position = position;
        this.rotation = rotation;
        this.world = world;
        this.meta = meta;
    }

    public static CustomClientCommandSource of(FabricClientCommandSource source) {
        if (source instanceof CustomClientCommandSource custom) {
            return custom;
        }
        return new CustomClientCommandSource(source.getClient().getConnection(), source.getClient(), source.getEntity(), source.getPosition(), source.getRotation(), source.getWorld(), new HashMap<>());
    }

    @Override
    public void sendFeedback(Component message) {
        this.client.gui.getChat().addMessage(message);
        this.client.getNarrator().sayNow(message);
    }

    @Override
    public void sendError(Component message) {
        this.sendFeedback(Component.empty().append(message).withStyle(ChatFormatting.RED));
    }

    @Override
    public Minecraft getClient() {
        return this.client;
    }

    @Override
    public LocalPlayer getPlayer() {
        return this.getClient().player;
    }

    @Override
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public Vec3 getPosition() {
        return this.position;
    }

    @Override
    public Vec2 getRotation() {
        return this.rotation;
    }

    @Override
    public ClientLevel getWorld() {
        return this.world;
    }

    @Override
    public Object getMeta(String key) {
        return this.meta.get(key);
    }

    public CustomClientCommandSource withEntity(Entity entity) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, entity, this.position, this.rotation, this.world, this.meta);
    }

    public CustomClientCommandSource withPosition(Vec3 position) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, this.entity, position, this.rotation, this.world, this.meta);
    }

    public CustomClientCommandSource withRotation(Vec2 rotation) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, this.entity, this.position, rotation, this.world, this.meta);
    }

    public CustomClientCommandSource withWorld(ClientLevel world) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, this.entity, this.position, this.rotation, world, this.meta);
    }

    public CustomClientCommandSource withMeta(String key, Object value) {
        this.meta.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<Level> getDimension() throws CommandSyntaxException {
        Object dimensionMeta = this.getMeta("dimension");
        if (dimensionMeta != null) {
            return (ResourceKey<Level>) dimensionMeta;
        }
        String dimensionString = this.getWorld().dimension().location().getPath();
        ResourceLocation location = CDimensionArgument.dimension().parse(new StringReader(dimensionString));
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, location);
        return this.levels().stream()
            .filter(key -> key.registry().equals(resourceKey.registry()) && key.location().equals(resourceKey.location()))
            .findAny().orElseThrow(() -> UNKNOWN_DIMENSION_EXCEPTION.create(location));
    }
}
