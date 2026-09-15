package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.gui.SupplyCrateMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * 补给箱：把服务端重掷出来的一组物品发给客户端（服务端 → 客户端）。
 * <p>
 * 不能像以前那样只发物品注册 id：一是药水 / 附魔书 / 带 EntityTag 的刷怪蛋这类物品的信息全在数据组件上，
 * 只发 id 客户端会显示成默认版本；二是数据槽只有 16 位，注册 id 超过 32767 时图标会整个消失。
 * <p>
 * 收发一律走 {@link SupplyCrateMenu#writeRolls} / {@link SupplyCrateMenu#readRolls}（NBT，按注册名），
 * <b>不要</b>改用 {@code ItemStack.OPTIONAL_STREAM_CODEC}——它按注册表整数 id 同步附魔等组件，
 * 注册表实例对不上时会在 Netty 线程抛 {@code Can't find id for '…/minecraft:impaling'} 直接掐断连接。
 */
public record SupplyCrateRollsPacket(@Nonnull ItemStack[] rolls) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SupplyCrateRollsPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Alltheimbaium.MODID, "supply_crate_rolls"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SupplyCrateRollsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> SupplyCrateMenu.writeRolls(buf, packet.rolls()),
            buf -> new SupplyCrateRollsPacket(SupplyCrateMenu.readRolls(buf)));

    @Override
    @Nonnull
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SupplyCrateRollsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // NeoForge 没有 DistExecutor，用 FMLEnvironment 判断；客户端分支只在客户端执行，不会在服务端加载客户端类
            // 26.x：FMLEnvironment.dist 字段已删，改用 FMLEnvironment.getDist()
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                applyOnClient(payload);
            }
        });
    }

    // 26.x：@OnlyIn 会触发 NeoForge 的 OnlyInWarningsHandler 报错，已删除该注解；
    // 本方法只在客户端执行（上面的 getDist() 分支保证），服务端不会解析到 Minecraft 类
    private static void applyOnClient(SupplyCrateRollsPacket payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof SupplyCrateMenu menu) {
            menu.applyRolls(payload.rolls());
        }
    }
}
