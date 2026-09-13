package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 简易网络通道。用于客户端请求打开永恒之剑 / 永恒图腾配置界面，
 * 以及补给箱把重掷结果（含 NBT 的完整物品）发给客户端。
 */
public class Network {

    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(Alltheimbaium.MODID, "main");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_NAME,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /**
     * 注册所有数据包。须在 mod 构造阶段调用一次
     */
    public static void register() {
        CHANNEL.registerMessage(0, OpenEternalSwordGuiPacket.class,
                OpenEternalSwordGuiPacket::encode,
                OpenEternalSwordGuiPacket::decode,
                OpenEternalSwordGuiPacket::handle);
        CHANNEL.registerMessage(1, OpenEternalTotemGuiPacket.class,
                OpenEternalTotemGuiPacket::encode,
                OpenEternalTotemGuiPacket::decode,
                OpenEternalTotemGuiPacket::handle);
        CHANNEL.registerMessage(2, SupplyCrateRollsPacket.class,
                SupplyCrateRollsPacket::encode,
                SupplyCrateRollsPacket::decode,
                SupplyCrateRollsPacket::handle);
    }
}
