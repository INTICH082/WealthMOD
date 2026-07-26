package com.mcwealth.mod.network;

import com.mcwealth.mod.MinecraftWealthMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WealthChartsPayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<WealthChartsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MinecraftWealthMod.MOD_ID, "wealth_charts"));

    public static final PacketCodec<RegistryByteBuf, WealthChartsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, WealthChartsPayload::json,
            WealthChartsPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}