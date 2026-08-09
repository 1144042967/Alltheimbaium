package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.gui.EternalSwordMenu;
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
 * 客户端请求打开永恒之剑配置界面的数据包（ALT+右击时发送）。
 */
public class OpenEternalSwordGuiPacket {

    public OpenEternalSwordGuiPacket() {
    }

    public static void encode(OpenEternalSwordGuiPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenEternalSwordGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenEternalSwordGuiPacket();
    }

    public static void handle(OpenEternalSwordGuiPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            // 主手或副手持有永恒之剑时打开配置界面
            ItemStack sword = player.getMainHandItem();
            if (!sword.is(Registration.ETERNAL_SWORD.get())) {
                sword = player.getOffhandItem();
            }
            if (!sword.is(Registration.ETERNAL_SWORD.get())) return;
            ItemStack finalSword = sword;
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.alltheimbaium.eternal_sword.title");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new EternalSwordMenu(containerId, inventory, player, finalSword);
                }
            });
        });
        context.setPacketHandled(true);
    }
}
