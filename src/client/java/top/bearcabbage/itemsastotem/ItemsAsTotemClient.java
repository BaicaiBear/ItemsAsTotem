package top.bearcabbage.itemsastotem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public class ItemsAsTotemClient implements ClientModInitializer {

	public static int deathProtectorIndex = -1;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DeathProtectorPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				ItemsAsTotemClient.deathProtectorIndex = payload.index();
				boolean i = true;
				context.client().particleManager.addEmitter(context.player(), ParticleTypes.TOTEM_OF_UNDYING, 30);
				context.player().getWorld().playSoundClient(context.player().getX(), context.player().getY(), context.player().getZ(), SoundEvents.ITEM_TOTEM_USE, context.player().getSoundCategory(), 1.0F, 1.0F, false);
				if (context.player() == context.client().player) {
					context.client().gameRenderer.showFloatingItem(getActiveDeathProtector(context.player()));
				}
			});
		});
	}

	private static ItemStack getActiveDeathProtector(PlayerEntity player) {
		Hand[] var1 = Hand.values();
		int var2 = var1.length;

		for (Hand hand : var1) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (itemStack.getItem().equals(Items.TOTEM_OF_UNDYING)) {
				return itemStack;
			}
		}

		int index = top.bearcabbage.itemsastotem.ItemsAsTotemClient.deathProtectorIndex;
		if (index != -1) {
			ItemStack itemStack = player.getInventory().getStack(index);
			if (!itemStack.isEmpty() && itemStack.get(DataComponentTypes.DEATH_PROTECTION) != null) {
				return itemStack;
			}
		}

		return new ItemStack(Items.TOTEM_OF_UNDYING);
	}
}