package top.bearcabbage.itemsastotem.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method="createPlayerAttributes" ,at = @At("HEAD"), cancellable = true)
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(LivingEntity.createLivingAttributes().add(EntityAttributes.ATTACK_DAMAGE, 1.0).add(EntityAttributes.MOVEMENT_SPEED, 0.10000000149011612).add(EntityAttributes.ATTACK_SPEED).add(EntityAttributes.LUCK).add(EntityAttributes.BLOCK_INTERACTION_RANGE, 4.5).add(EntityAttributes.ENTITY_INTERACTION_RANGE, 3.0).add(EntityAttributes.BLOCK_BREAK_SPEED).add(EntityAttributes.SUBMERGED_MINING_SPEED).add(EntityAttributes.SNEAKING_SPEED).add(EntityAttributes.MINING_EFFICIENCY).add(EntityAttributes.SWEEPING_DAMAGE_RATIO).add(EntityAttributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7).add(EntityAttributes.WAYPOINT_RECEIVE_RANGE, 6.0E7).add(EntityAttributes.MAX_HEALTH, 2.0));
    }
}
