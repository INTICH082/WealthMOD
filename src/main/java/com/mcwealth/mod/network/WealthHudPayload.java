package com.mcwealth.mod.network;

import com.mcwealth.mod.MinecraftWealthMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WealthHudPayload(double total) implements CustomPayload {

    public static final CustomPayload.Id<WealthHudPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MinecraftWealthMod.MOD_ID, "wealth_hud"));

    public static final PacketCodec<RegistryByteBuf, WealthHudPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, WealthHudPayload::total,
            WealthHudPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}