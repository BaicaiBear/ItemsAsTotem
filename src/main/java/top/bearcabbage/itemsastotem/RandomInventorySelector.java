package top.bearcabbage.itemsastotem;

import java.util.*;

import eu.pb4.playerdata.api.PlayerDataApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import static top.bearcabbage.itemsastotem.ItemsAsTotem.DPData;
import static top.bearcabbage.itemsastotem.ItemsAsTotem.activateEquip;

public class RandomInventorySelector {


    public static SelectionResult selectRandomStack(PlayerInventory inventory, int mode, Random random) {
        List<SlotStackPair> nonEmptyStacks = collectNonEmptyStacks(inventory);

        if (nonEmptyStacks.isEmpty()) {
            return null;
        }


        String lastDrawnItem = lastDrawing(inventory);


        if (lastDrawnItem != null) {
            nonEmptyStacks = nonEmptyStacks.stream()
                    .filter(pair -> !pair.stack.getItem().toString().equals(lastDrawnItem))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }


        if (nonEmptyStacks.isEmpty()) {
            return null;
        }

        SelectionResult result;
        if (mode == 0) {
            // Mode 0: Equally random by slots
            result = selectEquallyRandom(nonEmptyStacks, random);
        } else if (mode == 1) {
            // Mode 1: Weighted by stack count
            result = selectWeightedRandom(nonEmptyStacks, random);
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode + ". Use 0 for equal probability or 1 for weighted by count.");
        }

        if (result != null) {
            saveThisDrawing(inventory, result.getItemStack().getItem());
        }

        return result;
    }


    public static String lastDrawing(PlayerInventory inventory) {
        PlayerEntity player = inventory.player;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound component = PlayerDataApi.getCustomDataFor(serverPlayer, DPData);
            if (component != null && component.contains("LastDrawnItem")) {
                return component.getString("LastDrawnItem").get();
            }
        }
        return null;
    }


    public static void saveThisDrawing(PlayerInventory inventory, Item item) {
        PlayerEntity player = inventory.player;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound component = PlayerDataApi.getCustomDataFor(serverPlayer, DPData);
            if (component != null) {
                component.putString("LastDrawnItem", String.valueOf(item.toString()));
                PlayerDataApi.setCustomDataFor(serverPlayer, DPData, component);
            }
            else {
                NbtCompound newComponent = new NbtCompound();
                newComponent.putString("LastDrawnItem", String.valueOf(item.toString()));
                PlayerDataApi.setCustomDataFor(serverPlayer, DPData, newComponent);
            }
        }
    }


    private static List<SlotStackPair> collectNonEmptyStacks(PlayerInventory inventory) {
        List<SlotStackPair> nonEmptyStacks = new ArrayList<>();

        // Collect from main inventory (slots 0-35)
        for (int i = 0; i < inventory.getMainStacks().size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                nonEmptyStacks.add(new SlotStackPair(i, stack, SlotType.MAIN));
            }
        }

        // Collect from equipment slots
        // Using the EQUIPMENT_SLOTS mapping from PlayerInventory
        if(activateEquip) for (Map.Entry<Integer, EquipmentSlot> entry : PlayerInventory.EQUIPMENT_SLOTS.entrySet()) {
            int slotIndex = entry.getKey();
            EquipmentSlot equipmentSlot = entry.getValue();
            ItemStack stack = inventory.getStack(slotIndex);
            if (!stack.isEmpty()) {
                SlotType slotType = getSlotTypeFromEquipmentSlot(equipmentSlot);
                nonEmptyStacks.add(new SlotStackPair(slotIndex, stack, slotType));
            }
        }

        return nonEmptyStacks;
    }


    private static SelectionResult selectEquallyRandom(List<SlotStackPair> nonEmptyStacks, Random random) {
        int randomIndex = random.nextInt(nonEmptyStacks.size());
        SlotStackPair selected = nonEmptyStacks.get(randomIndex);
        return new SelectionResult(selected.stack, selected.slotIndex, selected.slotType);
    }


    private static SelectionResult selectWeightedRandom(List<SlotStackPair> nonEmptyStacks, Random random) {
        // Calculate total weight (sum of all stack counts)
        int totalWeight = nonEmptyStacks.stream()
                .mapToInt(pair -> pair.stack.getCount())
                .sum();

        // Generate random number between 0 and totalWeight-1
        int randomWeight = random.nextInt(totalWeight);

        // Find the stack corresponding to this weight
        int currentWeight = 0;
        for (SlotStackPair pair : nonEmptyStacks) {
            currentWeight += pair.stack.getCount();
            if (randomWeight < currentWeight) {
                return new SelectionResult(pair.stack, pair.slotIndex, pair.slotType);
            }
        }

        // Fallback (should never reach here)
        SlotStackPair lastPair = nonEmptyStacks.get(nonEmptyStacks.size() - 1);
        return new SelectionResult(lastPair.stack, lastPair.slotIndex, lastPair.slotType);
    }


    private static SlotType getSlotTypeFromEquipmentSlot(EquipmentSlot equipmentSlot) {
        switch (equipmentSlot) {
            case OFFHAND:
                return SlotType.OFFHAND;
            case HEAD:
            case CHEST:
            case LEGS:
            case FEET:
                return SlotType.ARMOR;
            case BODY:
                return SlotType.BODY;
            case SADDLE:
                return SlotType.SADDLE;
            default:
                return SlotType.EQUIPMENT;
        }
    }


    private static class SlotStackPair {
        final int slotIndex;
        final ItemStack stack;
        final SlotType slotType;

        SlotStackPair(int slotIndex, ItemStack stack, SlotType slotType) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.slotType = slotType;
        }
    }


    public enum SlotType {
        MAIN,      // Main inventory slots (0-35)
        OFFHAND,   // Offhand slot (40)
        ARMOR,     // Armor slots (head, chest, legs, feet)
        BODY,      // Body slot (41)
        SADDLE,    // Saddle slot (42)
        EQUIPMENT  // Other equipment slots
    }


    public static SelectionResult selectRandomStack(PlayerInventory inventory, int mode) {
        return selectRandomStack(inventory, mode, new Random());
    }


    public static class SelectionResult {
        private final ItemStack itemStack;
        private final int slotIndex;
        private final SlotType slotType;

        public SelectionResult(ItemStack itemStack, int slotIndex, SlotType slotType) {
            this.itemStack = itemStack;
            this.slotIndex = slotIndex;
            this.slotType = slotType;
        }


        public ItemStack getItemStack() {
            return itemStack;
        }


        public int getSlotIndex() {
            return slotIndex;
        }


        public SlotType getSlotType() {
            return slotType;
        }

        @Override
        public String toString() {
            return String.format("SelectionResult{slot=%d, type=%s, item=%s, count=%d}",
                    slotIndex, slotType, itemStack.getItem().toString(), itemStack.getCount());
        }
    }
}