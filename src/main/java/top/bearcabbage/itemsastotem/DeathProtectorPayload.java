package top.bearcabbage.itemsastotem;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static top.bearcabbage.itemsastotem.ItemsAsTotem.MOD_ID;

public record DeathProtectorPayload(int index) implements CustomPayload {
   public static final Id<DeathProtectorPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID,"death_protector_index"));
   public static final PacketCodec<PacketByteBuf, DeathProtectorPayload> CODEC = PacketCodec.of((value, buf) -> buf.writeInt(value.index),
           buf -> new DeathProtectorPayload(buf.readInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
