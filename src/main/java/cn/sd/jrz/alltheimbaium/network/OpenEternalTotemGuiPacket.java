package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.gui.EternalTotemMenu;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

/**
 * 客户端请求打开永恒图腾配置界面的数据包（ALT+右击时发送）。
 */
public class OpenEternalTotemGuiPacket {

    public OpenEternalTotemGuiPacket() {
    }

    public static void encode(OpenEternalTotemGuiPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenEternalTotemGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenEternalTotemGuiPacket();
    }

    public static void handle(OpenEternalTotemGuiPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            // 主手或副手持有永恒图腾时打开配置界面
            ItemStack totem = player.getMainHandItem();
            if (!totem.is(Registration.ETERNAL_TOTEM.get())) {
                totem = player.getOffhandItem();
            }
            if (!totem.is(Registration.ETERNAL_TOTEM.get())) return;
            ItemStack finalTotem = totem;
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.alltheimbaium.eternal_totem.title");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new EternalTotemMenu(containerId, inventory, player, finalTotem);
                }
            });
        });
        context.setPacketHandled(true);
    }
}
