package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.block.AlltheimbaiumFarmlandBlock;
import cn.sd.jrz.alltheimbaium.block.FarmBlock;
import cn.sd.jrz.alltheimbaium.entity.CommonEntity;
import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.item.FarmItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Alltheimbaium.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Alltheimbaium.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Alltheimbaium.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alltheimbaium.MODID);

    public static void init(FMLJavaModLoadingContext context) {
        BLOCKS.register(context.getModEventBus());
        ITEMS.register(context.getModEventBus());
        ENTITIES.register(context.getModEventBus());
        CREATIVE_MODE_TABS.register(Alltheimbaium.MODID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Alltheimbaium.MODID))
                .icon(() -> new ItemStack(Registration.FARMLAND_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(Registration.FARMLAND_ITEM.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X8.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X64.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X512.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X4K.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X32K.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X256K.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X2M.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X16M.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X128M.get());
                    output.accept(Registration.PACKAGE_MATERIAL_XG.get());
                    output.accept(Registration.FARM_IRON_GOLEM_ITEM.get());
                })
                .build()
        );
        CREATIVE_MODE_TABS.register(context.getModEventBus());
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 0.5f);

    //BLOCK

    public static final RegistryObject<AlltheimbaiumFarmlandBlock> FARMLAND_BLOCK = BLOCKS.register("farmland", AlltheimbaiumFarmlandBlock::new);
    public static final RegistryObject<FarmBlock> FARM_IRON_GOLEM_BLOCK = BLOCKS.register("farm_iron_golem", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_IRON_GOLEM));

    //ITEM
    public static final RegistryObject<BlockItem> FARMLAND_ITEM = ITEMS.register("farmland", () -> new BlockItem(FARMLAND_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X8 = ITEMS.register("package_material_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X64 = ITEMS.register("package_material_x64", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X512 = ITEMS.register("package_material_x512", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X4K = ITEMS.register("package_material_x4k", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X32K = ITEMS.register("package_material_x32k", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X256K = ITEMS.register("package_material_x256k", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X2M = ITEMS.register("package_material_x2m", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X16M = ITEMS.register("package_material_x16m", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X128M = ITEMS.register("package_material_x128m", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_XG = ITEMS.register("package_material_xg", () -> new Item(new Item.Properties()));
    public static final RegistryObject<FarmItem> FARM_IRON_GOLEM_ITEM = ITEMS.register("farm_iron_golem", () -> new FarmItem(FARM_IRON_GOLEM_BLOCK.get(), DataConfig.FARM_IRON_GOLEM));

    //Entities

    public static final RegistryObject<BlockEntityType<CommonEntity>> FARMLAND_ENTITY = ENTITIES.register("farmland", () -> BlockEntityType.Builder.of((pos, state) -> new CommonEntity(pos, state, Registration.FARMLAND_ENTITY::get), FARMLAND_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_IRON_GOLEM_ENTITY = ENTITIES.register("farm_iron_golem", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_IRON_GOLEM), FARM_IRON_GOLEM_BLOCK.get()).build(null));
}
