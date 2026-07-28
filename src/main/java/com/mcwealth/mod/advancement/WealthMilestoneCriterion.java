package com.mcwealth.mod.advancement;

import com.mcwealth.mod.MinecraftWealthMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class WealthMilestoneCriterion extends AbstractCriterion<WealthMilestoneCriterion.Conditions> {

    public static final Identifier ID = Identifier.of(MinecraftWealthMod.MOD_ID, "wealth_milestone");

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public void trigger(ServerPlayerEntity player, double totalWealth) {
        trigger(player, conditions -> conditions.test(totalWealth));
    }

    public record Conditions(Optional<LootContextPredicate> player, double amount) implements AbstractCriterion.Conditions {

        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootContextPredicate.OPTIONAL_CODEC.forGetter(Conditions::player),
                Codec.DOUBLE.fieldOf("amount").forGetter(Conditions::amount)
        ).apply(instance, Conditions::new));

        public boolean test(double actualAmount) {
            return actualAmount >= amount;
        }
    }
}