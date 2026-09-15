package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.gui.EternalSwordMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * 客户端请求打开永恒之剑配置界面的数据包（ALT+右击时发送）。无字段，用 {@code StreamCodec.unit}。
 */
public record OpenEternalSwordGuiPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenEternalSwordGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Alltheimbaium.MODID, "open_eternal_sword_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEternalSwordGuiPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenEternalSwordGuiPacket());

    @Override
    @Nonnull
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEternalSwordGuiPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            // 主手或副手持有永恒之剑时打开配置界面
            ItemStack sword = player.getMainHandItem();
            if (!sword.is(Registration.ETERNAL_SWORD.get())) {
                sword = player.getOffhandItem();
            }
            if (!sword.is(Registration.ETERNAL_SWORD.get())) {
                return;
            }
            ItemStack finalSword = sword;
            // 1.21.1：NetworkHooks.openScreen 已移除，改用原版的 ServerPlayer.openMenu
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.alltheimbaium.eternal_sword.title")
                            .withStyle(Tip.rarityColor(Registration.ETERNAL_SWORD.get()));
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
                    return new EternalSwordMenu(containerId, inventory, player, finalSword);
                }
            });
        });
    }
}
