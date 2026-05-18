package dev.xpple.netherbedrockcracker.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CustomClientCommandSource extends ClientSuggestionProvider implements FabricClientCommandSource {

    private final Minecraft client;
    private final Entity entity;
    private final Vec3 position;
    private final Vec2 rotation;
    private final ClientLevel level;
    private final Map<String, Object> meta;

    public CustomClientCommandSource(ClientPacketListener listener, Minecraft minecraft, Entity entity, Vec3 position, Vec2 rotation, ClientLevel level, PermissionSet permissionSet, Map<String, Object> meta) {
        super(listener, minecraft, permissionSet);

        this.client = minecraft;
        this.entity = entity;
        this.position = position;
        this.rotation = rotation;
        this.level = level;
        this.meta = meta;
    }

    public static CustomClientCommandSource of(FabricClientCommandSource source) {
        if (source instanceof CustomClientCommandSource custom) {
            return custom;
        }
        return new CustomClientCommandSource(source.getClient().getConnection(), source.getClient(), source.getEntity(), source.getPosition(), source.getRotation(), source.getLevel(), source.permissions(), new HashMap<>());
    }

    @Override
    public void sendFeedback(Component message) {
        this.client.gui.getChat().addClientSystemMessage(message);
        this.client.getNarrator().saySystemChatQueued(message);
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
    public ClientLevel getLevel() {
        return this.level;
    }

    @Override
    public @Nullable Object getMeta(String key) {
        return this.meta.get(key);
    }

    public CustomClientCommandSource withEntity(Entity entity) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, entity, this.position, this.rotation, this.level, this.permissions(), this.meta);
    }

    public CustomClientCommandSource withPosition(Vec3 position) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, this.entity, position, this.rotation, this.level, this.permissions(), this.meta);
    }

    public CustomClientCommandSource withRotation(Vec2 rotation) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, this.entity, this.position, rotation, this.level, this.permissions(), this.meta);
    }

    public CustomClientCommandSource withLevel(ClientLevel level) {
        return new CustomClientCommandSource(this.client.getConnection(), this.client, this.entity, this.position, this.rotation, level, this.permissions(), this.meta);
    }

    public CustomClientCommandSource withMeta(String key, Object value) {
        this.meta.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<Level> getDimension() {
        Object dimensionMeta = this.getMeta("dimension");
        if (dimensionMeta != null) {
            return (ResourceKey<Level>) dimensionMeta;
        }
        return inferDimension(this.level.dimensionType());
    }

    private static ResourceKey<Level> inferDimension(DimensionType dimensionType) {
        return switch (dimensionType.skybox()) {
            case NONE -> Level.NETHER;
            case OVERWORLD -> Level.OVERWORLD;
            case END -> Level.END;
        };
    }
}
