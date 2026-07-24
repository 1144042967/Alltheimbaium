package cn.sd.jrz.alltheimbaium;

import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Alltheimbaium.MODID)
public class Alltheimbaium {
    public static final String MODID = "alltheimbaium";

    public Alltheimbaium(IEventBus bus, ModContainer container) {
        Config.init(container);
        Registration.init(bus);
    }
}
