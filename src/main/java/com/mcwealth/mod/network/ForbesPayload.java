package com.mcwealth.mod.network;

import com.mcwealth.mod.MinecraftWealthMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ForbesPayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<ForbesPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MinecraftWealthMod.MOD_ID, "forbes_board"));

    public static final PacketCodec<RegistryByteBuf, ForbesPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ForbesPayload::json,
            ForbesPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}