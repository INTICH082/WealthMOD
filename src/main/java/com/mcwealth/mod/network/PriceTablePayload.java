package com.mcwealth.mod.network;

import com.mcwealth.mod.MinecraftWealthMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PriceTablePayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<PriceTablePayload> ID =
            new CustomPayload.Id<>(Identifier.of(MinecraftWealthMod.MOD_ID, "price_table"));

    public static final PacketCodec<RegistryByteBuf, PriceTablePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PriceTablePayload::json,
            PriceTablePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}