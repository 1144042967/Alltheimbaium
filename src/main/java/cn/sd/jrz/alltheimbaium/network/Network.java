package cn.sd.jrz.alltheimbaium.network;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 简易网络通道。目前仅用于客户端请求打开永恒之剑配置界面。
 */
public class Network {

    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(Alltheimbaium.MODID, "main");

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
    }
}
