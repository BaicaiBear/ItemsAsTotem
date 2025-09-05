package top.bearcabbage.itemsastotem.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.bearcabbage.itemsastotem.DeathProtectorPayload;
import top.bearcabbage.itemsastotem.RandomInventorySelector;

import static top.bearcabbage.itemsastotem.ItemsAsTotem.randomMode;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract ItemStack getStackInHand(Hand hand);

    @Shadow
    public abstract void setHealth(float health);

    /**
     * @author BaicaiBear
     * @reason Allow all items randomly to act as totems of undying for players
     */
    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"), cancellable = true)
    private void tryUseDeathProtector(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.isPlayer()){
            if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                cir.setReturnValue(false);
            } else {
                boolean hasTotem = false;
                int index = -1;
                ItemStack itemStack = null;
                DeathProtectionComponent deathProtectionComponent = null;
                Hand[] var5 = Hand.values();

                for (Hand hand : var5) {
                    ItemStack itemStack2 = this.getStackInHand(hand);
                    deathProtectionComponent = (DeathProtectionComponent) itemStack2.get(DataComponentTypes.DEATH_PROTECTION);
                    if (deathProtectionComponent != null && deathProtectionComponent.equals(DeathProtectionComponent.TOTEM_OF_UNDYING)) {
                        itemStack = itemStack2.copy();
                        itemStack2.decrement(1);
                        hasTotem = true;
                        break;
                    }
                }

                if (itemStack == null) {
                    PlayerInventory inventory = ((PlayerEntity)(Object)this).getInventory();
                    RandomInventorySelector.SelectionResult result = RandomInventorySelector.selectRandomStack(inventory, randomMode);
                    if (result == null) {
                        cir.setReturnValue(false);
                        return;
                    }
                    ItemStack itemStack2 = result.getItemStack();
                    deathProtectionComponent = (DeathProtectionComponent) itemStack2.get(DataComponentTypes.DEATH_PROTECTION);
                    if (deathProtectionComponent != null) {
                        itemStack = itemStack2.copy();
                        index = result.getSlotIndex();
                        itemStack2.decrement(1);
                    }
                }

                if (itemStack != null) {
                    if ((Object) this instanceof ServerPlayerEntity serverPlayerEntity) {
                        serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
                        Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
                        this.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
                    }

                    this.setHealth(1.0F);
                    deathProtectionComponent.applyDeathEffects(itemStack, (LivingEntity)(Object)this);
                    if(hasTotem) this.getWorld().sendEntityStatus(this, (byte)35);
                    else ServerPlayNetworking.send(((ServerPlayerEntity)(Object)this), new DeathProtectorPayload(index));
                }

                cir.setReturnValue(deathProtectionComponent != null);
            }
        } else {
            if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                cir.setReturnValue(false);
            } else {
                ItemStack itemStack = null;
                DeathProtectionComponent deathProtectionComponent = null;
                Hand[] var5 = Hand.values();
                int var6 = var5.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    Hand hand = var5[var7];
                    ItemStack itemStack2 = this.getStackInHand(hand);
                    deathProtectionComponent = (DeathProtectionComponent)itemStack2.get(DataComponentTypes.DEATH_PROTECTION);
                    if (deathProtectionComponent != null && deathProtectionComponent.equals(DeathProtectionComponent.TOTEM_OF_UNDYING)) {
                        itemStack = itemStack2.copy();
                        itemStack2.decrement(1);
                        break;
                    }
                }

                if (itemStack != null) {
                    this.setHealth(1.0F);
                    deathProtectionComponent.applyDeathEffects(itemStack, (LivingEntity) (Object)this);
                    this.getWorld().sendEntityStatus(this, (byte)35);
                }
                cir.setReturnValue(deathProtectionComponent != null && deathProtectionComponent.equals(DeathProtectionComponent.TOTEM_OF_UNDYING));
            }
        }
    }

    @Inject(method = "setAbsorptionAmount", at = @At("HEAD"), cancellable = true)
    private void setAbsorptionAmount(float absorptionAmount, CallbackInfo ci) {
        if (this.isPlayer()){
            ci.cancel();
        }
    }
}
