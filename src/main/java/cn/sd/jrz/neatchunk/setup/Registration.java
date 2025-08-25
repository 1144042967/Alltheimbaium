package cn.sd.jrz.neatchunk.setup;

import cn.sd.jrz.neatchunk.Alltheimbaium;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class Registration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Alltheimbaium.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Alltheimbaium.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Alltheimbaium.MODID);

    public static void init(FMLJavaModLoadingContext context) {
        BLOCKS.register(context.getModEventBus());
        ITEMS.register(context.getModEventBus());
        BLOCK_ENTITIES.register(context.getModEventBus());
    }
}
