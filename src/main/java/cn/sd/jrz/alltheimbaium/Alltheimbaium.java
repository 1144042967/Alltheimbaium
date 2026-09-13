package cn.sd.jrz.alltheimbaium;

import cn.sd.jrz.alltheimbaium.network.Network;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Alltheimbaium.MODID)
public class Alltheimbaium {
    public static final String MODID = "alltheimbaium";

    public Alltheimbaium(IEventBus modBus, ModContainer container) {
        // SERVER 配置由 NeoForge 在存档加载时读取；ModConfigEvent 监听见 Config
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        Registration.init(modBus);
        modBus.addListener(Registration::registerCapabilities);
        Network.register(modBus);
    }
}
