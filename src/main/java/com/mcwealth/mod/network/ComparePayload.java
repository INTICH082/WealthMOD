package com.mcwealth.mod.network;

import com.mcwealth.mod.MinecraftWealthMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ComparePayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<ComparePayload> ID = new CustomPayload.Id<>(Identifier.of(MinecraftWealthMod.MOD_ID, "compare"));

    public static final PacketCodec<RegistryByteBuf, ComparePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ComparePayload::json,ComparePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}