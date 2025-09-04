package top.bearcabbage.itemsastotem;

import java.util.*;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;

public class RandomInventorySelector {

    /**
     * Selects a random non-empty ItemStack from the player's inventory
     *
     * @param inventory The PlayerInventory to select from
     * @param mode 0 = equally random by slots, 1 = weighted by stack count
     * @param random Random instance for selection
     * @return A SelectionResult containing the ItemStack and slot index, or null if no non-empty stacks exist
     */
    public static SelectionResult selectRandomStack(PlayerInventory inventory, int mode, Random random) {
        List<SlotStackPair> nonEmptyStacks = collectNonEmptyStacks(inventory);

        if (nonEmptyStacks.isEmpty()) {
            return null;
        }

        if (mode == 0) {
            // Mode 0: Equally random by slots
            return selectEquallyRandom(nonEmptyStacks, random);
        } else if (mode == 1) {
            // Mode 1: Weighted by stack count
            return selectWeightedRandom(nonEmptyStacks, random);
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode + ". Use 0 for equal probability or 1 for weighted by count.");
        }
    }

    /**
     * Collects all non-empty stacks from the inventory with their slot information
     */
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
        for (Map.Entry<Integer, EquipmentSlot> entry : PlayerInventory.EQUIPMENT_SLOTS.entrySet()) {
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

    /**
     * Selects randomly with equal probability for each slot
     */
    private static SelectionResult selectEquallyRandom(List<SlotStackPair> nonEmptyStacks, Random random) {
        int randomIndex = random.nextInt(nonEmptyStacks.size());
        SlotStackPair selected = nonEmptyStacks.get(randomIndex);
        return new SelectionResult(selected.stack, selected.slotIndex, selected.slotType);
    }

    /**
     * Selects randomly weighted by stack count
     */
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

    /**
     * Maps EquipmentSlot to SlotType for better categorization
     */
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

    /**
     * Helper class to store slot information with the stack
     */
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

    /**
     * Enum to categorize different types of slots
     */
    public enum SlotType {
        MAIN,      // Main inventory slots (0-35)
        OFFHAND,   // Offhand slot (40)
        ARMOR,     // Armor slots (head, chest, legs, feet)
        BODY,      // Body slot (41)
        SADDLE,    // Saddle slot (42)
        EQUIPMENT  // Other equipment slots
    }

    /**
     * Convenience method with default Random instance
     */
    public static SelectionResult selectRandomStack(PlayerInventory inventory, int mode) {
        return selectRandomStack(inventory, mode, new Random());
    }

    /**
     * Result class containing the selected ItemStack and its slot information
     */
    public static class SelectionResult {
        private final ItemStack itemStack;
        private final int slotIndex;
        private final SlotType slotType;

        public SelectionResult(ItemStack itemStack, int slotIndex, SlotType slotType) {
            this.itemStack = itemStack;
            this.slotIndex = slotIndex;
            this.slotType = slotType;
        }

        /**
         * @return The selected ItemStack (original reference, not a copy)
         */
        public ItemStack getItemStack() {
            return itemStack;
        }

        /**
         * @return The original slot index where this stack was found
         */
        public int getSlotIndex() {
            return slotIndex;
        }

        /**
         * @return The type of slot where this stack was found
         */
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