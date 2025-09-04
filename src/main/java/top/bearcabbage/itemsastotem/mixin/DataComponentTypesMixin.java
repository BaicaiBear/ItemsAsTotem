package top.bearcabbage.itemsastotem.mixin;

import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Rarity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static net.minecraft.component.DataComponentTypes.*;

@Mixin(DataComponentTypes.class)
public class DataComponentTypesMixin {

    @Mutable
    @Shadow
    @Final
    public static ComponentMap DEFAULT_ITEM_COMPONENTS;

    static {
        DEFAULT_ITEM_COMPONENTS = ComponentMap.builder()
                .add(MAX_STACK_SIZE, 64)
                .add(LORE, LoreComponent.DEFAULT)
                .add(ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
                .add(REPAIR_COST, 0)
                .add(ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .add(RARITY, Rarity.COMMON)
                .add(BREAK_SOUND, SoundEvents.ENTITY_ITEM_BREAK)
                .add(TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT)
                .add(DEATH_PROTECTION, new DeathProtectionComponent(List.of()))
                .build();
    }
}
