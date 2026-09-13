package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.gui.SupplyCrateMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * 补给箱：把服务端重掷出来的一组物品发给客户端（服务端 → 客户端）。
 * <p>
 * 不能像以前那样只发物品注册 id：一是药水 / 附魔书 / 带 EntityTag 的刷怪蛋这类物品的信息全在数据组件上，
 * 只发 id 客户端会显示成默认版本；二是数据槽只有 16 位，注册 id 超过 32767 时图标会整个消失。
 * <p>
 * 1.21.1：改用 {@code ItemStack.OPTIONAL_STREAM_CODEC}（自带数据组件与注册表访问），
 * 不再自己拿 NBT 序列化。
 */
public record SupplyCrateRollsPacket(@Nonnull ItemStack[] rolls) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SupplyCrateRollsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Alltheimbaium.MODID, "supply_crate_rolls"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SupplyCrateRollsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                for (int i = 0; i < SupplyCrateMenu.ROLL_SLOTS; i++) {
                    ItemStack stack = (packet.rolls() != null && i < packet.rolls().length)
                            ? packet.rolls()[i] : ItemStack.EMPTY;
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                }
            },
            buf -> {
                ItemStack[] rolls = new ItemStack[SupplyCrateMenu.ROLL_SLOTS];
                for (int i = 0; i < SupplyCrateMenu.ROLL_SLOTS; i++) {
                    rolls[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                }
                return new SupplyCrateRollsPacket(rolls);
            });

    @Override
    @Nonnull
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SupplyCrateRollsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // NeoForge 没有 DistExecutor，用 FMLEnvironment 判断；客户端分支只在客户端执行，不会在服务端加载客户端类
            if (FMLEnvironment.dist == Dist.CLIENT) {
                applyOnClient(payload);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyOnClient(SupplyCrateRollsPacket payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof SupplyCrateMenu menu) {
            menu.applyRolls(payload.rolls());
        }
    }
}
