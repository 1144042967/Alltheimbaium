package cn.sd.jrz.alltheimbaium.setup;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.block.*;
import cn.sd.jrz.alltheimbaium.entity.*;
import cn.sd.jrz.alltheimbaium.gui.AutoFarmlandMenu;
import cn.sd.jrz.alltheimbaium.gui.ClockMenu;
import cn.sd.jrz.alltheimbaium.gui.EternalSwordMenu;
import cn.sd.jrz.alltheimbaium.gui.EternalTotemMenu;
import cn.sd.jrz.alltheimbaium.gui.LiquidFountainMenu;
import cn.sd.jrz.alltheimbaium.gui.MobFarmMenu;
import cn.sd.jrz.alltheimbaium.gui.ResourceFarmMenu;
import cn.sd.jrz.alltheimbaium.gui.PlatformMenu;
import cn.sd.jrz.alltheimbaium.gui.StorageFountainMenu;
import cn.sd.jrz.alltheimbaium.gui.SupplyCrateMenu;
import cn.sd.jrz.alltheimbaium.item.*;
import cn.sd.jrz.alltheimbaium.recipe.BrewingCraftRecipe;
import cn.sd.jrz.alltheimbaium.recipe.PotionCombineRecipe;
import cn.sd.jrz.alltheimbaium.recipe.SmeltingCraftRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("DataFlowIssue")
public class Registration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Alltheimbaium.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Alltheimbaium.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Alltheimbaium.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Alltheimbaium.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Alltheimbaium.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alltheimbaium.MODID);

    public static void init(FMLJavaModLoadingContext context) {
        BLOCKS.register(context.getModEventBus());
        ITEMS.register(context.getModEventBus());
        ENTITIES.register(context.getModEventBus());
        RECIPE_SERIALIZERS.register(context.getModEventBus());
        MENUS.register(context.getModEventBus());
        CREATIVE_MODE_TABS.register(Alltheimbaium.MODID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Alltheimbaium.MODID))
                .icon(() -> new ItemStack(Registration.FARMLAND_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(Registration.FARMLAND_ITEM.get());
                    output.accept(Registration.AUTO_FARMLAND_ITEM.get());
                    output.accept(Registration.STORAGE_FOUNTAIN_ITEM.get());
                    output.accept(Registration.LIQUID_FOUNTAIN_ITEM.get());
                    output.accept(Registration.MOB_FARM_ITEM.get());//生物农场
                    output.accept(Registration.RESOURCE_FARM_ITEM.get());//资源农场
                    output.accept(Registration.ETERNAL_TOTEM.get());
                    output.accept(Registration.ETERNAL_SWORD.get());
                    output.accept(Registration.CLOCK_ITEM.get());
                    output.accept(Registration.PLATFORM_ITEM.get());
                    output.accept(Registration.SUPPLY_CRATE_ITEM.get());//补给箱






                    output.accept(Registration.PACKAGE_MATERIAL_X1.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X2.get());
                    output.accept(Registration.PACKAGE_MATERIAL_X3.get());
                    output.accept(Registration.BLOCK_DIAMOND_X8.get());
                    output.accept(Registration.BLOCK_GOLD_X8.get());
                    output.accept(Registration.BLOCK_SILICON_X8.get());
                    output.accept(Registration.BLOCK_QUANTUM_ALLOY_X8.get());
                    output.accept(Registration.BLOCK_SKY_STEEL_X8.get());
                })
                .build()
        );
        CREATIVE_MODE_TABS.register(context.getModEventBus());
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 0.5f);

    // 方块

    public static final RegistryObject<FarmlandBlock> FARMLAND_BLOCK = BLOCKS.register("farmland", FarmlandBlock::new);
    public static final RegistryObject<AutoFarmlandBlock> AUTO_FARMLAND_BLOCK = BLOCKS.register("auto_farmland", () -> new AutoFarmlandBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(DyeColor.BLUE)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)));
    public static final RegistryObject<ClockBlock> CLOCK_BLOCK = BLOCKS.register("clock", () -> new ClockBlock(BLOCK_PROPERTIES));
    public static final RegistryObject<PlatformBlock> PLATFORM_BLOCK = BLOCKS.register("platform", () -> new PlatformBlock(BLOCK_PROPERTIES));
    public static final RegistryObject<SupplyCrateBlock> SUPPLY_CRATE_BLOCK = BLOCKS.register("supply_crate", () -> new SupplyCrateBlock(BLOCK_PROPERTIES));
    // 液体机：玻璃罐体，复制玻璃方块属性（音效等）+ noOcclusion 使其不 cull 相邻方块的面，透过罐体能正常看到后面的地面/物品
    public static final RegistryObject<LiquidFountainBlock> LIQUID_FOUNTAIN_BLOCK = BLOCKS.register("liquid_fountain", () -> new LiquidFountainBlock(
            BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .mapColor(DyeColor.BLUE)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)
                    .noOcclusion()));
    public static final RegistryObject<StorageFountainBlock> STORAGE_FOUNTAIN_BLOCK = BLOCKS.register("storage_fountain", () -> new StorageFountainBlock(BLOCK_PROPERTIES));
    // 生物农场：玻璃罐体（noOcclusion 使罐内生物/后方可见）
    public static final RegistryObject<MobFarmBlock> MOB_FARM_BLOCK = BLOCKS.register("mob_farm", () -> new MobFarmBlock(
            BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .mapColor(DyeColor.BLUE)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)
                    .noOcclusion()));
    // 通用资源农场：玻璃罐体风格
    public static final RegistryObject<ResourceFarmBlock> RESOURCE_FARM_BLOCK = BLOCKS.register("resource_farm", () -> new ResourceFarmBlock(
            BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .mapColor(DyeColor.GREEN)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)
                    .noOcclusion()));

    // 物品
    public static final RegistryObject<BlockItem> FARMLAND_ITEM = ITEMS.register("farmland", () -> new FarmlandItem(FARMLAND_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> AUTO_FARMLAND_ITEM = ITEMS.register("auto_farmland", () -> new AutoFarmlandItem(AUTO_FARMLAND_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<ClockItem> CLOCK_ITEM = ITEMS.register("clock", () -> new ClockItem(CLOCK_BLOCK.get(), "block.alltheimbaium.clock.description"));
    public static final RegistryObject<PlatformItem> PLATFORM_ITEM = ITEMS.register("platform", () -> new PlatformItem(PLATFORM_BLOCK.get()));
    public static final RegistryObject<SupplyCrateItem> SUPPLY_CRATE_ITEM = ITEMS.register("supply_crate", () -> new SupplyCrateItem(SUPPLY_CRATE_BLOCK.get()));
    public static final RegistryObject<BlockItem> LIQUID_FOUNTAIN_ITEM = ITEMS.register("liquid_fountain", () -> new LiquidFountainItem(LIQUID_FOUNTAIN_BLOCK.get()));
    public static final RegistryObject<BlockItem> STORAGE_FOUNTAIN_ITEM = ITEMS.register("storage_fountain", () -> new StorageFountainItem(STORAGE_FOUNTAIN_BLOCK.get()));
    public static final RegistryObject<BlockItem> MOB_FARM_ITEM = ITEMS.register("mob_farm", () -> new MobFarmItem(MOB_FARM_BLOCK.get()));
    public static final RegistryObject<BlockItem> RESOURCE_FARM_ITEM = ITEMS.register("resource_farm", () -> new ResourceFarmItem(RESOURCE_FARM_BLOCK.get()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X1 = ITEMS.register("package_material_x1", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X2 = ITEMS.register("package_material_x2", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PACKAGE_MATERIAL_X3 = ITEMS.register("package_material_x3", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_DIAMOND_X8 = ITEMS.register("block_diamond_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_GOLD_X8 = ITEMS.register("block_gold_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_SILICON_X8 = ITEMS.register("block_silicon_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_QUANTUM_ALLOY_X8 = ITEMS.register("block_quantum_alloy_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_SKY_STEEL_X8 = ITEMS.register("block_sky_steel_x8", () -> new Item(new Item.Properties()));
    public static final RegistryObject<EternalTotemItem> ETERNAL_TOTEM = ITEMS.register("eternal_totem", EternalTotemItem::new);
    public static final RegistryObject<EternalSwordItem> ETERNAL_SWORD = ITEMS.register("eternal_sword", EternalSwordItem::new);

    // 菜单类型
    public static final RegistryObject<MenuType<EternalSwordMenu>> ETERNAL_SWORD_MENU =
            MENUS.register("eternal_sword", () -> IForgeMenuType.create((id, inv, data) -> new EternalSwordMenu(id, inv)));
    public static final RegistryObject<MenuType<EternalTotemMenu>> ETERNAL_TOTEM_MENU =
            MENUS.register("eternal_totem", () -> IForgeMenuType.create((id, inv, data) -> new EternalTotemMenu(id, inv)));
    public static final RegistryObject<MenuType<LiquidFountainMenu>> LIQUID_FOUNTAIN_MENU =
            MENUS.register("liquid_fountain", () -> IForgeMenuType.create((id, inv, data) -> new LiquidFountainMenu(id, inv, data.readBlockPos())));
    public static final RegistryObject<MenuType<AutoFarmlandMenu>> AUTO_FARMLAND_MENU =
            MENUS.register("auto_farmland", () -> IForgeMenuType.create((id, inv, data) -> new AutoFarmlandMenu(id, inv, data.readBlockPos())));
    public static final RegistryObject<MenuType<ClockMenu>> CLOCK_MENU =
            MENUS.register("clock", () -> IForgeMenuType.create((id, inv, data) -> new ClockMenu(id, inv, data.readBlockPos())));
    public static final RegistryObject<MenuType<StorageFountainMenu>> STORAGE_FOUNTAIN_MENU =
            MENUS.register("storage_fountain", () -> IForgeMenuType.create((id, inv, data) -> new StorageFountainMenu(id, inv, data.readBlockPos())));
    public static final RegistryObject<MenuType<MobFarmMenu>> MOB_FARM_MENU =
            MENUS.register("mob_farm", () -> IForgeMenuType.create((id, inv, data) -> new MobFarmMenu(id, inv, data)));
    public static final RegistryObject<MenuType<ResourceFarmMenu>> RESOURCE_FARM_MENU =
            MENUS.register("resource_farm", () -> IForgeMenuType.create((id, inv, data) -> new ResourceFarmMenu(id, inv, data)));
    public static final RegistryObject<MenuType<PlatformMenu>> PLATFORM_MENU =
            MENUS.register("platform", () -> IForgeMenuType.create((id, inv, data) -> new PlatformMenu(id, inv, data.readBlockPos())));
    public static final RegistryObject<MenuType<SupplyCrateMenu>> SUPPLY_CRATE_MENU =
            MENUS.register("supply_crate", () -> IForgeMenuType.create((id, inv, data) -> new SupplyCrateMenu(id, inv, data)));

    // 配方序列化器
    public static final RegistryObject<RecipeSerializer<SmeltingCraftRecipe>> SMELTING_CRAFT_SERIALIZER = RECIPE_SERIALIZERS.register("smelting_craft", () -> SmeltingCraftRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<BrewingCraftRecipe>> BREWING_CRAFT_SERIALIZER = RECIPE_SERIALIZERS.register("brewing_craft", () -> BrewingCraftRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<PotionCombineRecipe>> POTION_COMBINE_SERIALIZER = RECIPE_SERIALIZERS.register("potion_combine", () -> PotionCombineRecipe.SERIALIZER);


    // 实体
    public static final RegistryObject<BlockEntityType<CommonEntity>> FARMLAND_ENTITY = ENTITIES.register("farmland", () -> BlockEntityType.Builder.of((pos, state) -> new CommonEntity(pos, state, Registration.FARMLAND_ENTITY::get), FARMLAND_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<AutoFarmlandEntity>> AUTO_FARMLAND_ENTITY = ENTITIES.register("auto_farmland", () -> BlockEntityType.Builder.of(AutoFarmlandEntity::new, AUTO_FARMLAND_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<ClockEntity>> CLOCK_ENTITY = ENTITIES.register("clock", () -> BlockEntityType.Builder.of(ClockEntity::new, CLOCK_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<LiquidFountainEntity>> LIQUID_FOUNTAIN_ENTITY = ENTITIES.register("liquid_fountain", () -> BlockEntityType.Builder.of(LiquidFountainEntity::new, LIQUID_FOUNTAIN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<StorageFountainEntity>> STORAGE_FOUNTAIN_ENTITY = ENTITIES.register("storage_fountain", () -> BlockEntityType.Builder.of(StorageFountainEntity::new, STORAGE_FOUNTAIN_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<MobFarmEntity>> MOB_FARM_ENTITY = ENTITIES.register("mob_farm", () -> BlockEntityType.Builder.of(MobFarmEntity::new, MOB_FARM_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<ResourceFarmEntity>> RESOURCE_FARM_ENTITY = ENTITIES.register("resource_farm", () -> BlockEntityType.Builder.of(ResourceFarmEntity::new, RESOURCE_FARM_BLOCK.get()).build(null));
}
