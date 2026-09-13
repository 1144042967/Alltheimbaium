package cn.sd.jrz.alltheimbaium.setup;

import javax.annotation.Nonnull;
import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.block.*;
import cn.sd.jrz.alltheimbaium.entity.*;
import cn.sd.jrz.alltheimbaium.gui.AutoFarmlandMenu;
import cn.sd.jrz.alltheimbaium.gui.ClockMenu;
import cn.sd.jrz.alltheimbaium.gui.CreativeTransmuterMenu;
import cn.sd.jrz.alltheimbaium.gui.EternalSwordMenu;
import cn.sd.jrz.alltheimbaium.gui.EternalTotemMenu;
import cn.sd.jrz.alltheimbaium.gui.InstantFurnaceMenu;
import cn.sd.jrz.alltheimbaium.gui.InstantInscriberMenu;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 中心注册文件。
 * <p>
 * 声明顺序即物品的注册顺序，也是创造模式标签页的排列顺序，两者保持一致：
 * 按品级升序（材料级 → 便利级 → 高效级 → 破坏平衡），同级内按用途排列，
 * 因此自上而下扫一遍标签页，名称颜色梯度就是能力梯度。
 * 品级与名称颜色的对应关系见 {@code item/Tip.java}。
 */
@SuppressWarnings("DataFlowIssue")
public class Registration {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Alltheimbaium.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alltheimbaium.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Alltheimbaium.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Alltheimbaium.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Alltheimbaium.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alltheimbaium.MODID);

    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        MENUS.register(bus);
        CREATIVE_MODE_TABS.register(Alltheimbaium.MODID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Alltheimbaium.MODID))
                .icon(() -> new ItemStack(Registration.ETERNAL_SWORD.get()))
                .displayItems((parameters, output) -> {
                    // 顺序与下方声明顺序一致，按品级升序排列
                    output.accept(Registration.PACKAGE_MATERIAL.get());
                    output.accept(Registration.FARMLAND_ITEM.get());
                    output.accept(Registration.PLATFORM_ITEM.get());
                    output.accept(Registration.EXTRACTION_INTERFACE_ITEM.get());
                    output.accept(Registration.CLOCK_ITEM.get());
                    output.accept(Registration.SUPPLY_CRATE_ITEM.get());
                    output.accept(Registration.AUTO_FARMLAND_ITEM.get());
                    output.accept(Registration.INSTANT_FURNACE_ITEM.get());
                    output.accept(Registration.INSTANT_INSCRIBER_ITEM.get());
                    output.accept(Registration.MOB_FARM_ITEM.get());
                    output.accept(Registration.RESOURCE_FARM_ITEM.get());
                    output.accept(Registration.STORAGE_FOUNTAIN_ITEM.get());
                    output.accept(Registration.LIQUID_FOUNTAIN_ITEM.get());
                    output.accept(Registration.CREATIVE_TRANSMUTER_ITEM.get());
                    output.accept(Registration.ETERNAL_TOTEM.get());
                    output.accept(Registration.ETERNAL_SWORD.get());
                })
                .build()
        );
        CREATIVE_MODE_TABS.register(bus);
    }

    private static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(DyeColor.BLUE)
            .pushReaction(PushReaction.DESTROY)
            .strength(0.5f, 0.5f);

    // ==================== 材料级 ====================

    public static final DeferredItem<PackageMaterialItem> PACKAGE_MATERIAL = ITEMS.register("package_material", PackageMaterialItem::new);

    // ==================== 便利级 ====================

    public static final DeferredBlock<FarmlandBlock> FARMLAND_BLOCK = BLOCKS.register("farmland", FarmlandBlock::new);
    public static final DeferredItem<BlockItem> FARMLAND_ITEM = ITEMS.register("farmland", () -> new FarmlandItem(FARMLAND_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<PlatformBlock> PLATFORM_BLOCK = BLOCKS.register("platform", () -> new PlatformBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<PlatformItem> PLATFORM_ITEM = ITEMS.register("platform", () -> new PlatformItem(PLATFORM_BLOCK.get()));

    // 取出接口：聚合相邻本MOD产物/流体机器，供管道被动抽取（无GUI、无主动输出）
    public static final DeferredBlock<ExtractionInterfaceBlock> EXTRACTION_INTERFACE_BLOCK = BLOCKS.register("extraction_interface", () -> new ExtractionInterfaceBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<ExtractionInterfaceItem> EXTRACTION_INTERFACE_ITEM = ITEMS.register("extraction_interface", () -> new ExtractionInterfaceItem(EXTRACTION_INTERFACE_BLOCK.get()));

    public static final DeferredBlock<ClockBlock> CLOCK_BLOCK = BLOCKS.register("clock", () -> new ClockBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<ClockItem> CLOCK_ITEM = ITEMS.register("clock", () -> new ClockItem(CLOCK_BLOCK.get()));

    public static final DeferredBlock<SupplyCrateBlock> SUPPLY_CRATE_BLOCK = BLOCKS.register("supply_crate", () -> new SupplyCrateBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<SupplyCrateItem> SUPPLY_CRATE_ITEM = ITEMS.register("supply_crate", () -> new SupplyCrateItem(SUPPLY_CRATE_BLOCK.get()));

    // ==================== 高效级 ====================

    public static final DeferredBlock<AutoFarmlandBlock> AUTO_FARMLAND_BLOCK = BLOCKS.register("auto_farmland", () -> new AutoFarmlandBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(DyeColor.BLUE)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)));
    public static final DeferredItem<BlockItem> AUTO_FARMLAND_ITEM = ITEMS.register("auto_farmland", () -> new AutoFarmlandItem(AUTO_FARMLAND_BLOCK.get(), new Item.Properties()));

    // 零刻熔炉：18 输入槽 + 18 输出槽，即时熔炼消耗 FE
    public static final DeferredBlock<InstantFurnaceBlock> INSTANT_FURNACE_BLOCK = BLOCKS.register("instant_furnace", () -> new InstantFurnaceBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<BlockItem> INSTANT_FURNACE_ITEM = ITEMS.register("instant_furnace", () -> new InstantFurnaceItem(INSTANT_FURNACE_BLOCK.get()));

    // 零刻压印器：读 AE2 压印机配方，压板/组装双模式即时生成（消耗 FE）
    public static final DeferredBlock<InstantInscriberBlock> INSTANT_INSCRIBER_BLOCK = BLOCKS.register("instant_inscriber", () -> new InstantInscriberBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<BlockItem> INSTANT_INSCRIBER_ITEM = ITEMS.register("instant_inscriber", () -> new InstantInscriberItem(INSTANT_INSCRIBER_BLOCK.get()));

    // 生物农场：玻璃罐体（noOcclusion 使罐内生物/后方可见）
    public static final DeferredBlock<MobFarmBlock> MOB_FARM_BLOCK = BLOCKS.register("mob_farm", () -> new MobFarmBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(DyeColor.BLUE)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> MOB_FARM_ITEM = ITEMS.register("mob_farm", () -> new MobFarmItem(MOB_FARM_BLOCK.get()));

    // 通用资源农场：玻璃罐体风格
    public static final DeferredBlock<ResourceFarmBlock> RESOURCE_FARM_BLOCK = BLOCKS.register("resource_farm", () -> new ResourceFarmBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(DyeColor.GREEN)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> RESOURCE_FARM_ITEM = ITEMS.register("resource_farm", () -> new ResourceFarmItem(RESOURCE_FARM_BLOCK.get()));

    // ==================== 破坏平衡 ====================

    public static final DeferredBlock<StorageFountainBlock> STORAGE_FOUNTAIN_BLOCK = BLOCKS.register("storage_fountain", () -> new StorageFountainBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<BlockItem> STORAGE_FOUNTAIN_ITEM = ITEMS.register("storage_fountain", () -> new StorageFountainItem(STORAGE_FOUNTAIN_BLOCK.get()));

    // 液体机：玻璃罐体，复制玻璃方块属性（音效等）+ noOcclusion 使其不 cull 相邻方块的面，透过罐体能正常看到后面的地面/物品
    public static final DeferredBlock<LiquidFountainBlock> LIQUID_FOUNTAIN_BLOCK = BLOCKS.register("liquid_fountain", () -> new LiquidFountainBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(DyeColor.BLUE)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.5f, 0.5f)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> LIQUID_FOUNTAIN_ITEM = ITEMS.register("liquid_fountain", () -> new LiquidFountainItem(LIQUID_FOUNTAIN_BLOCK.get()));

    // 质变器：形状是普通实心方块，不沿用玻璃罐体的 noOcclusion
    public static final DeferredBlock<CreativeTransmuterBlock> CREATIVE_TRANSMUTER_BLOCK = BLOCKS.register("creative_transmuter", () -> new CreativeTransmuterBlock(BLOCK_PROPERTIES));
    public static final DeferredItem<BlockItem> CREATIVE_TRANSMUTER_ITEM = ITEMS.register("creative_transmuter", () -> new CreativeTransmuterItem(CREATIVE_TRANSMUTER_BLOCK.get()));

    public static final DeferredItem<EternalTotemItem> ETERNAL_TOTEM = ITEMS.register("eternal_totem", EternalTotemItem::new);
    public static final DeferredItem<EternalSwordItem> ETERNAL_SWORD = ITEMS.register("eternal_sword", EternalSwordItem::new);

    // ==================== 菜单类型 ====================

    public static final DeferredHolder<MenuType<?>, MenuType<EternalSwordMenu>> ETERNAL_SWORD_MENU =
            MENUS.register("eternal_sword", () -> IMenuTypeExtension.create((id, inv, data) -> new EternalSwordMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<EternalTotemMenu>> ETERNAL_TOTEM_MENU =
            MENUS.register("eternal_totem", () -> IMenuTypeExtension.create((id, inv, data) -> new EternalTotemMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<LiquidFountainMenu>> LIQUID_FOUNTAIN_MENU =
            MENUS.register("liquid_fountain", () -> IMenuTypeExtension.create((id, inv, data) -> new LiquidFountainMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<CreativeTransmuterMenu>> CREATIVE_TRANSMUTER_MENU =
            MENUS.register("creative_transmuter", () -> IMenuTypeExtension.create((id, inv, data) -> new CreativeTransmuterMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<AutoFarmlandMenu>> AUTO_FARMLAND_MENU =
            MENUS.register("auto_farmland", () -> IMenuTypeExtension.create((id, inv, data) -> new AutoFarmlandMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<ClockMenu>> CLOCK_MENU =
            MENUS.register("clock", () -> IMenuTypeExtension.create((id, inv, data) -> new ClockMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageFountainMenu>> STORAGE_FOUNTAIN_MENU =
            MENUS.register("storage_fountain", () -> IMenuTypeExtension.create((id, inv, data) -> new StorageFountainMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<InstantFurnaceMenu>> INSTANT_FURNACE_MENU =
            MENUS.register("instant_furnace", () -> IMenuTypeExtension.create((id, inv, data) -> new InstantFurnaceMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<InstantInscriberMenu>> INSTANT_INSCRIBER_MENU =
            MENUS.register("instant_inscriber", () -> IMenuTypeExtension.create((id, inv, data) -> new InstantInscriberMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<MobFarmMenu>> MOB_FARM_MENU =
            MENUS.register("mob_farm", () -> IMenuTypeExtension.create((id, inv, data) -> new MobFarmMenu(id, inv, data)));
    public static final DeferredHolder<MenuType<?>, MenuType<ResourceFarmMenu>> RESOURCE_FARM_MENU =
            MENUS.register("resource_farm", () -> IMenuTypeExtension.create((id, inv, data) -> new ResourceFarmMenu(id, inv, data)));
    public static final DeferredHolder<MenuType<?>, MenuType<PlatformMenu>> PLATFORM_MENU =
            MENUS.register("platform", () -> IMenuTypeExtension.create((id, inv, data) -> new PlatformMenu(id, inv, data.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<SupplyCrateMenu>> SUPPLY_CRATE_MENU =
            MENUS.register("supply_crate", () -> IMenuTypeExtension.create((id, inv, data) -> new SupplyCrateMenu(id, inv, data)));

    // ==================== 配方序列化器 ====================

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SmeltingCraftRecipe>> SMELTING_CRAFT_SERIALIZER = RECIPE_SERIALIZERS.register("smelting_craft", () -> SmeltingCraftRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BrewingCraftRecipe>> BREWING_CRAFT_SERIALIZER = RECIPE_SERIALIZERS.register("brewing_craft", () -> BrewingCraftRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PotionCombineRecipe>> POTION_COMBINE_SERIALIZER = RECIPE_SERIALIZERS.register("potion_combine", () -> PotionCombineRecipe.SERIALIZER);

    // ==================== 实体 ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CommonEntity>> FARMLAND_ENTITY = ENTITIES.register("farmland", () -> BlockEntityType.Builder.of((pos, state) -> new CommonEntity(pos, state, Registration.FARMLAND_ENTITY::get), FARMLAND_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoFarmlandEntity>> AUTO_FARMLAND_ENTITY = ENTITIES.register("auto_farmland", () -> BlockEntityType.Builder.of(AutoFarmlandEntity::new, AUTO_FARMLAND_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClockEntity>> CLOCK_ENTITY = ENTITIES.register("clock", () -> BlockEntityType.Builder.of(ClockEntity::new, CLOCK_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidFountainEntity>> LIQUID_FOUNTAIN_ENTITY = ENTITIES.register("liquid_fountain", () -> BlockEntityType.Builder.of(LiquidFountainEntity::new, LIQUID_FOUNTAIN_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeTransmuterEntity>> CREATIVE_TRANSMUTER_ENTITY = ENTITIES.register("creative_transmuter", () -> BlockEntityType.Builder.of(CreativeTransmuterEntity::new, CREATIVE_TRANSMUTER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageFountainEntity>> STORAGE_FOUNTAIN_ENTITY = ENTITIES.register("storage_fountain", () -> BlockEntityType.Builder.of(StorageFountainEntity::new, STORAGE_FOUNTAIN_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstantFurnaceEntity>> INSTANT_FURNACE_ENTITY = ENTITIES.register("instant_furnace", () -> BlockEntityType.Builder.of(InstantFurnaceEntity::new, INSTANT_FURNACE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstantInscriberEntity>> INSTANT_INSCRIBER_ENTITY = ENTITIES.register("instant_inscriber", () -> BlockEntityType.Builder.of(InstantInscriberEntity::new, INSTANT_INSCRIBER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractionInterfaceEntity>> EXTRACTION_INTERFACE_ENTITY = ENTITIES.register("extraction_interface", () -> BlockEntityType.Builder.of(ExtractionInterfaceEntity::new, EXTRACTION_INTERFACE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobFarmEntity>> MOB_FARM_ENTITY = ENTITIES.register("mob_farm", () -> BlockEntityType.Builder.of(MobFarmEntity::new, MOB_FARM_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceFarmEntity>> RESOURCE_FARM_ENTITY = ENTITIES.register("resource_farm", () -> BlockEntityType.Builder.of(ResourceFarmEntity::new, RESOURCE_FARM_BLOCK.get()).build(null));

    /**
     * 能力集中注册。NeoForge 里方块实体**不再实现 {@code ICapabilityProvider}**、也没有 {@code LazyOptional}，
     * 而是由 mod 总线的 {@link RegisterCapabilitiesEvent} 统一告诉游戏"某个方块实体能提供什么"。
     * 每台机器在实体侧暴露一个 getter（见各 Entity 的 {@code getItemHandler} 等）。
     */
    public static void registerCapabilities(@Nonnull RegisterCapabilitiesEvent event) {
        // 注：耕地（CommonEntity）与时钟（ClockEntity）在 1.20.1 就没有对外暴露任何能力，
        // 这里不注册，与旧版行为保持一致。
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AUTO_FARMLAND_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STORAGE_FOUNTAIN_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LIQUID_FOUNTAIN_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LIQUID_FOUNTAIN_ENTITY.get(), (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CREATIVE_TRANSMUTER_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MOB_FARM_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RESOURCE_FARM_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INSTANT_FURNACE_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INSTANT_FURNACE_ENTITY.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INSTANT_INSCRIBER_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INSTANT_INSCRIBER_ENTITY.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EXTRACTION_INTERFACE_ENTITY.get(), (be, side) -> be.getHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EXTRACTION_INTERFACE_ENTITY.get(), (be, side) -> be.getHandler(side));
    }
}
