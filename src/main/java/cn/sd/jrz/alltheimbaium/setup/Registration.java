package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.block.AlltheimbaiumFarmlandBlock;
import cn.sd.jrz.alltheimbaium.block.FarmBlock;
import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.CommonEntity;
import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.item.FarmItem;
import cn.sd.jrz.alltheimbaium.item.StorageFountainItem;
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

@SuppressWarnings("DataFlowIssue")
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
                    output.accept(Registration.STORAGE_FOUNTAIN_ITEM.get());

                    output.accept(Registration.FARM_COBBLESTONE_ITEM.get());//圆石 1
                    output.accept(Registration.FARM_WOOD_ITEM.get());//树 1
                    output.accept(Registration.FARM_BAMBOO_ITEM.get());//竹子 1
                    output.accept(Registration.FARM_SUGAR_CANES_ITEM.get());//甘蔗 1
                    output.accept(Registration.FARM_BONE_MEAL_ITEM.get());//骨粉 1
                    output.accept(Registration.FARM_ICE_ITEM.get());//冰 1
                    output.accept(Registration.FARM_BEE_ITEM.get());//蜜蜂 1
                    output.accept(Registration.FARM_FROG_ITEM.get());//青蛙 1
                    output.accept(Registration.FARM_SQUID_ITEM.get());//鱿鱼 1

                    output.accept(Registration.FARM_CHICKEN_ITEM.get());//鸡 2
                    output.accept(Registration.FARM_RABBIT_ITEM.get());//兔子 2
                    output.accept(Registration.FARM_PIG_ITEM.get());//猪 2
                    output.accept(Registration.FARM_SHEEP_ITEM.get());//羊 2
                    output.accept(Registration.FARM_COW_ITEM.get());//牛 2

                    output.accept(Registration.FARM_ZOMBIE_ITEM.get());//僵尸 3
                    output.accept(Registration.FARM_SKELETON_ITEM.get());//骷髅 3
                    output.accept(Registration.FARM_CREEPER_ITEM.get());//苦力怕 3
                    output.accept(Registration.FARM_SPIDER_ITEM.get());//蜘蛛 3
                    output.accept(Registration.FARM_SLIME_ITEM.get());//史莱姆 3
                    output.accept(Registration.FARM_WITCH_ITEM.get());//女巫 3
                    output.accept(Registration.FARM_DROWNED_ITEM.get());//溺尸 3
                    output.accept(Registration.FARM_ENDERMAN_ITEM.get());//末影人 3
                    output.accept(Registration.FARM_GUARDIAN_ITEM.get());//守卫者 3
                    output.accept(Registration.FARM_PHANTOM_ITEM.get());//幻翼 3
                    output.accept(Registration.FARM_SHULKER_ITEM.get());//潜影贝 4
                    output.accept(Registration.FARM_GHAST_ITEM.get());//恶魂 4
                    output.accept(Registration.FARM_MAGMA_CUBE_ITEM.get());//岩浆怪 3
                    output.accept(Registration.FARM_HOGLIN_ITEM.get());//疣猪兽 3
                    output.accept(Registration.FARM_ZOMBIFIED_PIGLIN_ITEM.get());//僵尸猪灵 3
                    output.accept(Registration.FARM_BLAZE_ITEM.get());//烈焰人 4
                    output.accept(Registration.FARM_WITHER_SKELETON_ITEM.get());//凋零骷髅 4

                    output.accept(Registration.FARM_VILLAGER_ITEM.get());//村民 2
                    output.accept(Registration.FARM_IRON_GOLEM_ITEM.get());//铁傀儡 3
                    output.accept(Registration.FARM_PILLAGER_ITEM.get());//掠夺者 5
                    output.accept(Registration.FARM_EVOKER_ITEM.get());//唤魔者 5
                    output.accept(Registration.FARM_RAVAGER_ITEM.get());//劫掠兽 5

                    output.accept(Registration.FARM_WARDEN_ITEM.get());//监守者 6
                    output.accept(Registration.FARM_WITHER_ITEM.get());//凋零 6
                    output.accept(Registration.FARM_ENDER_DRAGON_ITEM.get());//末影龙 6

                    output.accept(Registration.PACKAGE_MATERIAL_X8.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X64.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X512.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X4K.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X32K.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X256K.get());
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
    public static final RegistryObject<StorageFountainBlock> STORAGE_FOUNTAIN_BLOCK = BLOCKS.register("storage_fountain", () -> new StorageFountainBlock(BLOCK_PROPERTIES));
    public static final RegistryObject<FarmBlock> FARM_BAMBOO_BLOCK = BLOCKS.register("farm_bamboo", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_BAMBOO));
    public static final RegistryObject<FarmBlock> FARM_BEE_BLOCK = BLOCKS.register("farm_bee", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_BEE));
    public static final RegistryObject<FarmBlock> FARM_BLAZE_BLOCK = BLOCKS.register("farm_blaze", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_BLAZE));
    public static final RegistryObject<FarmBlock> FARM_BONE_MEAL_BLOCK = BLOCKS.register("farm_bone_meal", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_BONE_MEAL));
    public static final RegistryObject<FarmBlock> FARM_CHICKEN_BLOCK = BLOCKS.register("farm_chicken", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_CHICKEN));
    public static final RegistryObject<FarmBlock> FARM_COBBLESTONE_BLOCK = BLOCKS.register("farm_cobblestone", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_COBBLESTONE));
    public static final RegistryObject<FarmBlock> FARM_COW_BLOCK = BLOCKS.register("farm_cow", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_COW));
    public static final RegistryObject<FarmBlock> FARM_CREEPER_BLOCK = BLOCKS.register("farm_creeper", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_CREEPER));
    public static final RegistryObject<FarmBlock> FARM_DROWNED_BLOCK = BLOCKS.register("farm_drowned", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_DROWNED));
    public static final RegistryObject<FarmBlock> FARM_ENDERMAN_BLOCK = BLOCKS.register("farm_enderman", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_ENDERMAN));
    public static final RegistryObject<FarmBlock> FARM_ENDER_DRAGON_BLOCK = BLOCKS.register("farm_ender_dragon", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_ENDER_DRAGON));
    public static final RegistryObject<FarmBlock> FARM_EVOKER_BLOCK = BLOCKS.register("farm_evoker", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_EVOKER));
    public static final RegistryObject<FarmBlock> FARM_FROG_BLOCK = BLOCKS.register("farm_frog", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_FROG));
    public static final RegistryObject<FarmBlock> FARM_GHAST_BLOCK = BLOCKS.register("farm_ghast", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_GHAST));
    public static final RegistryObject<FarmBlock> FARM_GUARDIAN_BLOCK = BLOCKS.register("farm_guardian", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_GUARDIAN));
    public static final RegistryObject<FarmBlock> FARM_HOGLIN_BLOCK = BLOCKS.register("farm_hoglin", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_HOGLIN));
    public static final RegistryObject<FarmBlock> FARM_ICE_BLOCK = BLOCKS.register("farm_ice", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_ICE));
    public static final RegistryObject<FarmBlock> FARM_IRON_GOLEM_BLOCK = BLOCKS.register("farm_iron_golem", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_IRON_GOLEM));
    public static final RegistryObject<FarmBlock> FARM_MAGMA_CUBE_BLOCK = BLOCKS.register("farm_magma_cube", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_MAGMA_CUBE));
    public static final RegistryObject<FarmBlock> FARM_PHANTOM_BLOCK = BLOCKS.register("farm_phantom", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_PHANTOM));
    public static final RegistryObject<FarmBlock> FARM_PIG_BLOCK = BLOCKS.register("farm_pig", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_PIG));
    public static final RegistryObject<FarmBlock> FARM_PILLAGER_BLOCK = BLOCKS.register("farm_pillager", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_PILLAGER));
    public static final RegistryObject<FarmBlock> FARM_RABBIT_BLOCK = BLOCKS.register("farm_rabbit", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_RABBIT));
    public static final RegistryObject<FarmBlock> FARM_RAVAGER_BLOCK = BLOCKS.register("farm_ravager", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_RAVAGER));
    public static final RegistryObject<FarmBlock> FARM_SHEEP_BLOCK = BLOCKS.register("farm_sheep", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SHEEP));
    public static final RegistryObject<FarmBlock> FARM_SHULKER_BLOCK = BLOCKS.register("farm_shulker", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SHULKER));
    public static final RegistryObject<FarmBlock> FARM_SKELETON_BLOCK = BLOCKS.register("farm_skeleton", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SKELETON));
    public static final RegistryObject<FarmBlock> FARM_SLIME_BLOCK = BLOCKS.register("farm_slime", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SLIME));
    public static final RegistryObject<FarmBlock> FARM_SPIDER_BLOCK = BLOCKS.register("farm_spider", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SPIDER));
    public static final RegistryObject<FarmBlock> FARM_SQUID_BLOCK = BLOCKS.register("farm_squid", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SQUID));
    public static final RegistryObject<FarmBlock> FARM_SUGAR_CANES_BLOCK = BLOCKS.register("farm_sugar_canes", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_SUGAR_CANES));
    public static final RegistryObject<FarmBlock> FARM_VILLAGER_BLOCK = BLOCKS.register("farm_villager", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_VILLAGER));
    public static final RegistryObject<FarmBlock> FARM_WARDEN_BLOCK = BLOCKS.register("farm_warden", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_WARDEN));
    public static final RegistryObject<FarmBlock> FARM_WITCH_BLOCK = BLOCKS.register("farm_witch", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_WITCH));
    public static final RegistryObject<FarmBlock> FARM_WITHER_BLOCK = BLOCKS.register("farm_wither", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_WITHER));
    public static final RegistryObject<FarmBlock> FARM_WITHER_SKELETON_BLOCK = BLOCKS.register("farm_wither_skeleton", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_WITHER_SKELETON));
    public static final RegistryObject<FarmBlock> FARM_WOOD_BLOCK = BLOCKS.register("farm_wood", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_WOOD));
    public static final RegistryObject<FarmBlock> FARM_ZOMBIE_BLOCK = BLOCKS.register("farm_zombie", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_ZOMBIE));
    public static final RegistryObject<FarmBlock> FARM_ZOMBIFIED_PIGLIN_BLOCK = BLOCKS.register("farm_zombified_piglin", () -> new FarmBlock(BLOCK_PROPERTIES, DataConfig.FARM_ZOMBIFIED_PIGLIN));

    //ITEM
    public static final RegistryObject<BlockItem> FARMLAND_ITEM = ITEMS.register("farmland", () -> new BlockItem(FARMLAND_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> STORAGE_FOUNTAIN_ITEM = ITEMS.register("storage_fountain", () -> new StorageFountainItem(STORAGE_FOUNTAIN_BLOCK.get()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X8 = ITEMS.register("package_material_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X64 = ITEMS.register("package_material_x64", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X512 = ITEMS.register("package_material_x512", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X4K = ITEMS.register("package_material_x4k", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X32K = ITEMS.register("package_material_x32k", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X256K = ITEMS.register("package_material_x256k", () -> new Item(new Item.Properties()));
    public static final RegistryObject<FarmItem> FARM_BAMBOO_ITEM = ITEMS.register("farm_bamboo", () -> new FarmItem(FARM_BAMBOO_BLOCK.get(), DataConfig.FARM_BAMBOO));
    public static final RegistryObject<FarmItem> FARM_BEE_ITEM = ITEMS.register("farm_bee", () -> new FarmItem(FARM_BEE_BLOCK.get(), DataConfig.FARM_BEE));
    public static final RegistryObject<FarmItem> FARM_BLAZE_ITEM = ITEMS.register("farm_blaze", () -> new FarmItem(FARM_BLAZE_BLOCK.get(), DataConfig.FARM_BLAZE));
    public static final RegistryObject<FarmItem> FARM_BONE_MEAL_ITEM = ITEMS.register("farm_bone_meal", () -> new FarmItem(FARM_BONE_MEAL_BLOCK.get(), DataConfig.FARM_BONE_MEAL));
    public static final RegistryObject<FarmItem> FARM_CHICKEN_ITEM = ITEMS.register("farm_chicken", () -> new FarmItem(FARM_CHICKEN_BLOCK.get(), DataConfig.FARM_CHICKEN));
    public static final RegistryObject<FarmItem> FARM_COBBLESTONE_ITEM = ITEMS.register("farm_cobblestone", () -> new FarmItem(FARM_COBBLESTONE_BLOCK.get(), DataConfig.FARM_COBBLESTONE));
    public static final RegistryObject<FarmItem> FARM_COW_ITEM = ITEMS.register("farm_cow", () -> new FarmItem(FARM_COW_BLOCK.get(), DataConfig.FARM_COW));
    public static final RegistryObject<FarmItem> FARM_CREEPER_ITEM = ITEMS.register("farm_creeper", () -> new FarmItem(FARM_CREEPER_BLOCK.get(), DataConfig.FARM_CREEPER));
    public static final RegistryObject<FarmItem> FARM_DROWNED_ITEM = ITEMS.register("farm_drowned", () -> new FarmItem(FARM_DROWNED_BLOCK.get(), DataConfig.FARM_DROWNED));
    public static final RegistryObject<FarmItem> FARM_ENDERMAN_ITEM = ITEMS.register("farm_enderman", () -> new FarmItem(FARM_ENDERMAN_BLOCK.get(), DataConfig.FARM_ENDERMAN));
    public static final RegistryObject<FarmItem> FARM_ENDER_DRAGON_ITEM = ITEMS.register("farm_ender_dragon", () -> new FarmItem(FARM_ENDER_DRAGON_BLOCK.get(), DataConfig.FARM_ENDER_DRAGON));
    public static final RegistryObject<FarmItem> FARM_EVOKER_ITEM = ITEMS.register("farm_evoker", () -> new FarmItem(FARM_EVOKER_BLOCK.get(), DataConfig.FARM_EVOKER));
    public static final RegistryObject<FarmItem> FARM_FROG_ITEM = ITEMS.register("farm_frog", () -> new FarmItem(FARM_FROG_BLOCK.get(), DataConfig.FARM_FROG));
    public static final RegistryObject<FarmItem> FARM_GHAST_ITEM = ITEMS.register("farm_ghast", () -> new FarmItem(FARM_GHAST_BLOCK.get(), DataConfig.FARM_GHAST));
    public static final RegistryObject<FarmItem> FARM_GUARDIAN_ITEM = ITEMS.register("farm_guardian", () -> new FarmItem(FARM_GUARDIAN_BLOCK.get(), DataConfig.FARM_GUARDIAN));
    public static final RegistryObject<FarmItem> FARM_HOGLIN_ITEM = ITEMS.register("farm_hoglin", () -> new FarmItem(FARM_HOGLIN_BLOCK.get(), DataConfig.FARM_HOGLIN));
    public static final RegistryObject<FarmItem> FARM_ICE_ITEM = ITEMS.register("farm_ice", () -> new FarmItem(FARM_ICE_BLOCK.get(), DataConfig.FARM_ICE));
    public static final RegistryObject<FarmItem> FARM_IRON_GOLEM_ITEM = ITEMS.register("farm_iron_golem", () -> new FarmItem(FARM_IRON_GOLEM_BLOCK.get(), DataConfig.FARM_IRON_GOLEM));
    public static final RegistryObject<FarmItem> FARM_MAGMA_CUBE_ITEM = ITEMS.register("farm_magma_cube", () -> new FarmItem(FARM_MAGMA_CUBE_BLOCK.get(), DataConfig.FARM_MAGMA_CUBE));
    public static final RegistryObject<FarmItem> FARM_PHANTOM_ITEM = ITEMS.register("farm_phantom", () -> new FarmItem(FARM_PHANTOM_BLOCK.get(), DataConfig.FARM_PHANTOM));
    public static final RegistryObject<FarmItem> FARM_PIG_ITEM = ITEMS.register("farm_pig", () -> new FarmItem(FARM_PIG_BLOCK.get(), DataConfig.FARM_PIG));
    public static final RegistryObject<FarmItem> FARM_PILLAGER_ITEM = ITEMS.register("farm_pillager", () -> new FarmItem(FARM_PILLAGER_BLOCK.get(), DataConfig.FARM_PILLAGER));
    public static final RegistryObject<FarmItem> FARM_RABBIT_ITEM = ITEMS.register("farm_rabbit", () -> new FarmItem(FARM_RABBIT_BLOCK.get(), DataConfig.FARM_RABBIT));
    public static final RegistryObject<FarmItem> FARM_RAVAGER_ITEM = ITEMS.register("farm_ravager", () -> new FarmItem(FARM_RAVAGER_BLOCK.get(), DataConfig.FARM_RAVAGER));
    public static final RegistryObject<FarmItem> FARM_SHEEP_ITEM = ITEMS.register("farm_sheep", () -> new FarmItem(FARM_SHEEP_BLOCK.get(), DataConfig.FARM_SHEEP));
    public static final RegistryObject<FarmItem> FARM_SHULKER_ITEM = ITEMS.register("farm_shulker", () -> new FarmItem(FARM_SHULKER_BLOCK.get(), DataConfig.FARM_SHULKER));
    public static final RegistryObject<FarmItem> FARM_SKELETON_ITEM = ITEMS.register("farm_skeleton", () -> new FarmItem(FARM_SKELETON_BLOCK.get(), DataConfig.FARM_SKELETON));
    public static final RegistryObject<FarmItem> FARM_SLIME_ITEM = ITEMS.register("farm_slime", () -> new FarmItem(FARM_SLIME_BLOCK.get(), DataConfig.FARM_SLIME));
    public static final RegistryObject<FarmItem> FARM_SPIDER_ITEM = ITEMS.register("farm_spider", () -> new FarmItem(FARM_SPIDER_BLOCK.get(), DataConfig.FARM_SPIDER));
    public static final RegistryObject<FarmItem> FARM_SQUID_ITEM = ITEMS.register("farm_squid", () -> new FarmItem(FARM_SQUID_BLOCK.get(), DataConfig.FARM_SQUID));
    public static final RegistryObject<FarmItem> FARM_SUGAR_CANES_ITEM = ITEMS.register("farm_sugar_canes", () -> new FarmItem(FARM_SUGAR_CANES_BLOCK.get(), DataConfig.FARM_SUGAR_CANES));
    public static final RegistryObject<FarmItem> FARM_VILLAGER_ITEM = ITEMS.register("farm_villager", () -> new FarmItem(FARM_VILLAGER_BLOCK.get(), DataConfig.FARM_VILLAGER));
    public static final RegistryObject<FarmItem> FARM_WARDEN_ITEM = ITEMS.register("farm_warden", () -> new FarmItem(FARM_WARDEN_BLOCK.get(), DataConfig.FARM_WARDEN));
    public static final RegistryObject<FarmItem> FARM_WITCH_ITEM = ITEMS.register("farm_witch", () -> new FarmItem(FARM_WITCH_BLOCK.get(), DataConfig.FARM_WITCH));
    public static final RegistryObject<FarmItem> FARM_WITHER_ITEM = ITEMS.register("farm_wither", () -> new FarmItem(FARM_WITHER_BLOCK.get(), DataConfig.FARM_WITHER));
    public static final RegistryObject<FarmItem> FARM_WITHER_SKELETON_ITEM = ITEMS.register("farm_wither_skeleton", () -> new FarmItem(FARM_WITHER_SKELETON_BLOCK.get(), DataConfig.FARM_WITHER_SKELETON));
    public static final RegistryObject<FarmItem> FARM_WOOD_ITEM = ITEMS.register("farm_wood", () -> new FarmItem(FARM_WOOD_BLOCK.get(), DataConfig.FARM_WOOD));
    public static final RegistryObject<FarmItem> FARM_ZOMBIE_ITEM = ITEMS.register("farm_zombie", () -> new FarmItem(FARM_ZOMBIE_BLOCK.get(), DataConfig.FARM_ZOMBIE));
    public static final RegistryObject<FarmItem> FARM_ZOMBIFIED_PIGLIN_ITEM = ITEMS.register("farm_zombified_piglin", () -> new FarmItem(FARM_ZOMBIFIED_PIGLIN_BLOCK.get(), DataConfig.FARM_ZOMBIFIED_PIGLIN));

    //Entities
    public static final RegistryObject<BlockEntityType<CommonEntity>> FARMLAND_ENTITY = ENTITIES.register("farmland", () -> BlockEntityType.Builder.of((pos, state) -> new CommonEntity(pos, state, Registration.FARMLAND_ENTITY::get), FARMLAND_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<StorageFountainEntity>> STORAGE_FOUNTAIN_ENTITY = ENTITIES.register("storage_fountain", () -> BlockEntityType.Builder.of(StorageFountainEntity::new, STORAGE_FOUNTAIN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_BAMBOO_ENTITY = ENTITIES.register("farm_bamboo", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_BAMBOO), FARM_BAMBOO_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_BEE_ENTITY = ENTITIES.register("farm_bee", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_BEE), FARM_BEE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_BLAZE_ENTITY = ENTITIES.register("farm_blaze", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_BLAZE), FARM_BLAZE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_BONE_MEAL_ENTITY = ENTITIES.register("farm_bone_meal", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_BONE_MEAL), FARM_BONE_MEAL_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_CHICKEN_ENTITY = ENTITIES.register("farm_chicken", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_CHICKEN), FARM_CHICKEN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_COBBLESTONE_ENTITY = ENTITIES.register("farm_cobblestone", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_COBBLESTONE), FARM_COBBLESTONE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_COW_ENTITY = ENTITIES.register("farm_cow", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_COW), FARM_COW_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_CREEPER_ENTITY = ENTITIES.register("farm_creeper", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_CREEPER), FARM_CREEPER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_DROWNED_ENTITY = ENTITIES.register("farm_drowned", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_DROWNED), FARM_DROWNED_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_ENDERMAN_ENTITY = ENTITIES.register("farm_enderman", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_ENDERMAN), FARM_ENDERMAN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_ENDER_DRAGON_ENTITY = ENTITIES.register("farm_ender_dragon", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_ENDER_DRAGON), FARM_ENDER_DRAGON_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_EVOKER_ENTITY = ENTITIES.register("farm_evoker", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_EVOKER), FARM_EVOKER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_FROG_ENTITY = ENTITIES.register("farm_frog", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_FROG), FARM_FROG_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_GHAST_ENTITY = ENTITIES.register("farm_ghast", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_GHAST), FARM_GHAST_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_GUARDIAN_ENTITY = ENTITIES.register("farm_guardian", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_GUARDIAN), FARM_GUARDIAN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_HOGLIN_ENTITY = ENTITIES.register("farm_hoglin", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_HOGLIN), FARM_HOGLIN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_ICE_ENTITY = ENTITIES.register("farm_ice", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_ICE), FARM_ICE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_IRON_GOLEM_ENTITY = ENTITIES.register("farm_iron_golem", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_IRON_GOLEM), FARM_IRON_GOLEM_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_MAGMA_CUBE_ENTITY = ENTITIES.register("farm_magma_cube", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_MAGMA_CUBE), FARM_MAGMA_CUBE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_PHANTOM_ENTITY = ENTITIES.register("farm_phantom", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_PHANTOM), FARM_PHANTOM_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_PIG_ENTITY = ENTITIES.register("farm_pig", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_PIG), FARM_PIG_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_PILLAGER_ENTITY = ENTITIES.register("farm_pillager", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_PILLAGER), FARM_PILLAGER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_RABBIT_ENTITY = ENTITIES.register("farm_rabbit", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_RABBIT), FARM_RABBIT_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_RAVAGER_ENTITY = ENTITIES.register("farm_ravager", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_RAVAGER), FARM_RAVAGER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SHEEP_ENTITY = ENTITIES.register("farm_sheep", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SHEEP), FARM_SHEEP_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SHULKER_ENTITY = ENTITIES.register("farm_shulker", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SHULKER), FARM_SHULKER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SKELETON_ENTITY = ENTITIES.register("farm_skeleton", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SKELETON), FARM_SKELETON_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SLIME_ENTITY = ENTITIES.register("farm_slime", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SLIME), FARM_SLIME_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SPIDER_ENTITY = ENTITIES.register("farm_spider", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SPIDER), FARM_SPIDER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SQUID_ENTITY = ENTITIES.register("farm_squid", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SQUID), FARM_SQUID_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_SUGAR_CANES_ENTITY = ENTITIES.register("farm_sugar_canes", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_SUGAR_CANES), FARM_SUGAR_CANES_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_VILLAGER_ENTITY = ENTITIES.register("farm_villager", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_VILLAGER), FARM_VILLAGER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_WARDEN_ENTITY = ENTITIES.register("farm_warden", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_WARDEN), FARM_WARDEN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_WITCH_ENTITY = ENTITIES.register("farm_witch", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_WITCH), FARM_WITCH_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_WITHER_ENTITY = ENTITIES.register("farm_wither", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_WITHER), FARM_WITHER_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_WITHER_SKELETON_ENTITY = ENTITIES.register("farm_wither_skeleton", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_WITHER_SKELETON), FARM_WITHER_SKELETON_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_WOOD_ENTITY = ENTITIES.register("farm_wood", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_WOOD), FARM_WOOD_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_ZOMBIE_ENTITY = ENTITIES.register("farm_zombie", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_ZOMBIE), FARM_ZOMBIE_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FarmEntity>> FARM_ZOMBIFIED_PIGLIN_ENTITY = ENTITIES.register("farm_zombified_piglin", () -> BlockEntityType.Builder.of((pos, state) -> new FarmEntity(pos, state, DataConfig.FARM_ZOMBIFIED_PIGLIN), FARM_ZOMBIFIED_PIGLIN_BLOCK.get()).build(null));
}
