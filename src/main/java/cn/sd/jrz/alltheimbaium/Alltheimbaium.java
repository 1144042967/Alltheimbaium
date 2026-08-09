package cn.sd.jrz.alltheimbaium;

import cn.sd.jrz.alltheimbaium.network.Network;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Alltheimbaium.MODID)
public class Alltheimbaium {
    public static final String MODID = "alltheimbaium";

    public Alltheimbaium(FMLJavaModLoadingContext context) {
        Config.init(context);
        Registration.init(context);
        Network.register();
    }
}
