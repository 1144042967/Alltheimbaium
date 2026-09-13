package cn.sd.jrz.alltheimbaium.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络通道。用于客户端请求打开永恒之剑 / 永恒图腾配置界面，
 * 以及补给箱把重掷结果（含 NBT 的完整物品）发给客户端。
 * <p>
 * NeoForge 1.21.1 不再有 Forge 的 {@code SimpleChannel}/{@code NetworkEvent}：
 * 每个包实现 {@code CustomPacketPayload} 并自带 {@code StreamCodec}，
 * 在 mod 总线的 {@link RegisterPayloadHandlersEvent} 里按方向注册（这里统一用协议版本 "1"）。
 */
public final class Network {

    /** 协议版本：两端不一致时 NeoForge 会拒绝连接 */
    public static final String PROTOCOL_VERSION = "1";

    private Network() {
    }

    /**
     * 注册所有数据包。须在 mod 构造阶段调用一次。
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(Network::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // 客户端 → 服务端：请求打开配置界面
        registrar.playToServer(OpenEternalSwordGuiPacket.TYPE, OpenEternalSwordGuiPacket.STREAM_CODEC,
                OpenEternalSwordGuiPacket::handle);
        registrar.playToServer(OpenEternalTotemGuiPacket.TYPE, OpenEternalTotemGuiPacket.STREAM_CODEC,
                OpenEternalTotemGuiPacket::handle);
        // 服务端 → 客户端：补给箱重掷结果
        registrar.playToClient(SupplyCrateRollsPacket.TYPE, SupplyCrateRollsPacket.STREAM_CODEC,
                SupplyCrateRollsPacket::handle);
    }
}
