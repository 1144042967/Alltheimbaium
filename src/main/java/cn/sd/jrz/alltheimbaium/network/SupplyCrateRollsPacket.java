package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.gui.SupplyCrateMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 补给箱：把服务端重掷出来的一组物品发给客户端（服务端 → 客户端）。
 * <p>
 * 不能像以前那样只发物品注册 id：一是药水 / 附魔书 / 带 EntityTag 的刷怪蛋这类物品的信息全在 NBT 上，
 * 只发 id 客户端会显示成默认版本；二是数据槽只有 16 位，注册 id 超过 32767 时图标会整个消失。
 * 因此这里直接传完整 ItemStack（NBT 形式，与 BlockEntity 存档用的是同一套读写）。
 */
public class SupplyCrateRollsPacket {
    private final ItemStack[] rolls;

    public SupplyCrateRollsPacket(ItemStack[] rolls) {
        this.rolls = rolls;
    }

    public static void encode(SupplyCrateRollsPacket msg, FriendlyByteBuf buf) {
        for (int i = 0; i < SupplyCrateMenu.ROLL_SLOTS; i++) {
            ItemStack stack = (msg.rolls != null && i < msg.rolls.length) ? msg.rolls[i] : ItemStack.EMPTY;
            buf.writeNbt(stack.isEmpty() ? null : stack.save(new CompoundTag()));
        }
    }

    public static SupplyCrateRollsPacket decode(FriendlyByteBuf buf) {
        ItemStack[] rolls = new ItemStack[SupplyCrateMenu.ROLL_SLOTS];
        for (int i = 0; i < SupplyCrateMenu.ROLL_SLOTS; i++) {
            CompoundTag tag = buf.readNbt();
            rolls[i] = tag == null ? ItemStack.EMPTY : ItemStack.of(tag);
        }
        return new SupplyCrateRollsPacket(rolls);
    }

    public static void handle(SupplyCrateRollsPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> applyOnClient(msg)));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyOnClient(SupplyCrateRollsPacket msg) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof SupplyCrateMenu menu) {
            menu.applyRolls(msg.rolls);
        }
    }
}
