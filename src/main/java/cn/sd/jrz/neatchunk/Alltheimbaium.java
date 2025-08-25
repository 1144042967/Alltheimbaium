package cn.sd.jrz.neatchunk;

import cn.sd.jrz.neatchunk.setup.Registration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Alltheimbaium.MODID)
public class Alltheimbaium {
    public static final String MODID = "alltheimbaium";

    public Alltheimbaium(FMLJavaModLoadingContext context) {
        Registration.init(context);
    }
}
