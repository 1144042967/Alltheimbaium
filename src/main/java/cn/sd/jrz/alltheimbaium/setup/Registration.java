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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 中心注册文件。
 * <p>
 * 声明顺序即物品的注册顺序，也是创造模式标签页的排列顺序，两者保持一致：
 * 按品级升序（材料级 → 便利级 → 高效级 → 破坏平衡），同级内按用途排列，
 * 因此自上而下扫一遍标签页，名称颜色梯度就是能力梯度。
 * 品级与名称颜色的对应关系见 {@code item/Tip.java}。
 * <p>
 * 26.x 起方块与物品都必须在属性里显式声明注册 id（{@code Properties#setId}），
 * 否则构造时直接抛错；这里统一走下面的 {@code blockProps}/{@code blockItemProps} 工厂，
 * 注册时把 NeoForge 传来的 key 灌进去。
 */
@SuppressWarnings("DataFlowIssue")
public class Registration {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Alltheimbaium.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alltheimbaium.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Alltheimbaium.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Alltheimbaium.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Alltheimbaium.MODID);
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

    // ==================== 属性工厂（26.x 必须带注册 id） ====================

    /** 普通机器方块属性：蓝色、活塞推动时销毁、硬度/抗性 0.5 */
    @Nonnull
    private static BlockBehaviour.Properties blockProps(@Nonnull Identifier key) {
        return BlockBehaviour.Properties.of()
                .mapColor(DyeColor.BLUE)
                .pushReaction(PushReaction.DESTROY)
                .strength(0.5f, 0.5f)
                .setId(ResourceKey.create(Registries.BLOCK, key));
    }

    /** 耕地类方块属性：复制原版耕地属性（音效等），不带原版耕地的 id */
    @Nonnull
    private static BlockBehaviour.Properties farmlandProps(@Nonnull Identifier key) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND)
                .setId(ResourceKey.create(Registries.BLOCK, key));
    }

    /** 玻璃罐体方块属性（存储 / 液体 / 生物 / 资源农场）：复制玻璃属性 + noOcclusion */
    @Nonnull
    private static BlockBehaviour.Properties glassBlockProps(@Nonnull Identifier key, @Nonnull DyeColor color) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                .mapColor(color)
                .pushReaction(PushReaction.DESTROY)
                .strength(0.5f, 0.5f)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, key));
    }

    /** 方块物品属性：26.x 的 BlockItem 默认用 item.&lt;mod&gt;.&lt;id&gt; 语言键，这里显式改用 block.&lt;mod&gt;.&lt;id&gt; */
    @Nonnull
    private static Item.Properties blockItemProps(@Nonnull Identifier key) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, key))
                .useBlockDescriptionPrefix();
    }

    /** 纯物品属性 */
    @Nonnull
    private static Item.Properties itemProps(@Nonnull Identifier key) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, key));
    }

    // ==================== 材料级 ====================

    public static final DeferredItem<PackageMaterialItem> PACKAGE_MATERIAL = ITEMS.register("package_material", key -> new PackageMaterialItem(itemProps(key)));

    // ==================== 便利级 ====================

    public static final DeferredBlock<FarmlandBlock> FARMLAND_BLOCK = BLOCKS.register("farmland", key -> new FarmlandBlock(farmlandProps(key)));
    public static final DeferredItem<BlockItem> FARMLAND_ITEM = ITEMS.register("farmland", key -> new FarmlandItem(FARMLAND_BLOCK.get(), blockItemProps(key)));

    public static final DeferredBlock<PlatformBlock> PLATFORM_BLOCK = BLOCKS.register("platform", key -> new PlatformBlock(blockProps(key)));
    public static final DeferredItem<PlatformItem> PLATFORM_ITEM = ITEMS.register("platform", key -> new PlatformItem(PLATFORM_BLOCK.get(), blockItemProps(key)));

    // 取出接口：聚合相邻本MOD产物/流体机器，供管道被动抽取（无GUI、无主动输出）
    public static final DeferredBlock<ExtractionInterfaceBlock> EXTRACTION_INTERFACE_BLOCK = BLOCKS.register("extraction_interface", key -> new ExtractionInterfaceBlock(blockProps(key)));
    public static final DeferredItem<ExtractionInterfaceItem> EXTRACTION_INTERFACE_ITEM = ITEMS.register("extraction_interface", key -> new ExtractionInterfaceItem(EXTRACTION_INTERFACE_BLOCK.get(), blockItemProps(key)));

    public static final DeferredBlock<ClockBlock> CLOCK_BLOCK = BLOCKS.register("clock", key -> new ClockBlock(blockProps(key)));
    public static final DeferredItem<ClockItem> CLOCK_ITEM = ITEMS.register("clock", key -> new ClockItem(CLOCK_BLOCK.get(), blockItemProps(key)));

    public static final DeferredBlock<SupplyCrateBlock> SUPPLY_CRATE_BLOCK = BLOCKS.register("supply_crate", key -> new SupplyCrateBlock(blockProps(key)));
    public static final DeferredItem<SupplyCrateItem> SUPPLY_CRATE_ITEM = ITEMS.register("supply_crate", key -> new SupplyCrateItem(SUPPLY_CRATE_BLOCK.get(), blockItemProps(key)));

    // ==================== 高效级 ====================

    public static final DeferredBlock<AutoFarmlandBlock> AUTO_FARMLAND_BLOCK = BLOCKS.register("auto_farmland", key -> new AutoFarmlandBlock(blockProps(key)));
    public static final DeferredItem<BlockItem> AUTO_FARMLAND_ITEM = ITEMS.register("auto_farmland", key -> new AutoFarmlandItem(AUTO_FARMLAND_BLOCK.get(), blockItemProps(key)));

    // 零刻熔炉：18 输入槽 + 18 输出槽，即时熔炼消耗 FE
    public static final DeferredBlock<InstantFurnaceBlock> INSTANT_FURNACE_BLOCK = BLOCKS.register("instant_furnace", key -> new InstantFurnaceBlock(blockProps(key)));
    public static final DeferredItem<BlockItem> INSTANT_FURNACE_ITEM = ITEMS.register("instant_furnace", key -> new InstantFurnaceItem(INSTANT_FURNACE_BLOCK.get(), blockItemProps(key)));

    // 零刻压印器：读 AE2 压印机配方，压板/组装双模式即时生成（消耗 FE）
    public static final DeferredBlock<InstantInscriberBlock> INSTANT_INSCRIBER_BLOCK = BLOCKS.register("instant_inscriber", key -> new InstantInscriberBlock(blockProps(key)));
    public static final DeferredItem<BlockItem> INSTANT_INSCRIBER_ITEM = ITEMS.register("instant_inscriber", key -> new InstantInscriberItem(INSTANT_INSCRIBER_BLOCK.get(), blockItemProps(key)));

    // 生物农场：玻璃罐体（noOcclusion 使罐内生物/后方可见）
    public static final DeferredBlock<MobFarmBlock> MOB_FARM_BLOCK = BLOCKS.register("mob_farm", key -> new MobFarmBlock(glassBlockProps(key, DyeColor.BLUE)));
    public static final DeferredItem<BlockItem> MOB_FARM_ITEM = ITEMS.register("mob_farm", key -> new MobFarmItem(MOB_FARM_BLOCK.get(), blockItemProps(key)));

    // 通用资源农场：玻璃罐体风格
    public static final DeferredBlock<ResourceFarmBlock> RESOURCE_FARM_BLOCK = BLOCKS.register("resource_farm", key -> new ResourceFarmBlock(glassBlockProps(key, DyeColor.GREEN)));
    public static final DeferredItem<BlockItem> RESOURCE_FARM_ITEM = ITEMS.register("resource_farm", key -> new ResourceFarmItem(RESOURCE_FARM_BLOCK.get(), blockItemProps(key)));

    // ==================== 破坏平衡 ====================

    public static final DeferredBlock<StorageFountainBlock> STORAGE_FOUNTAIN_BLOCK = BLOCKS.register("storage_fountain", key -> new StorageFountainBlock(blockProps(key)));
    public static final DeferredItem<BlockItem> STORAGE_FOUNTAIN_ITEM = ITEMS.register("storage_fountain", key -> new StorageFountainItem(STORAGE_FOUNTAIN_BLOCK.get(), blockItemProps(key)));

    // 液体机：玻璃罐体，复制玻璃方块属性（音效等）+ noOcclusion 使其不 cull 相邻方块的面，透过罐体能正常看到后面的地面/物品
    public static final DeferredBlock<LiquidFountainBlock> LIQUID_FOUNTAIN_BLOCK = BLOCKS.register("liquid_fountain", key -> new LiquidFountainBlock(glassBlockProps(key, DyeColor.BLUE)));
    public static final DeferredItem<BlockItem> LIQUID_FOUNTAIN_ITEM = ITEMS.register("liquid_fountain", key -> new LiquidFountainItem(LIQUID_FOUNTAIN_BLOCK.get(), blockItemProps(key)));

    // 质变器：形状是普通实心方块，不沿用玻璃罐体的 noOcclusion
    public static final DeferredBlock<CreativeTransmuterBlock> CREATIVE_TRANSMUTER_BLOCK = BLOCKS.register("creative_transmuter", key -> new CreativeTransmuterBlock(blockProps(key)));
    public static final DeferredItem<BlockItem> CREATIVE_TRANSMUTER_ITEM = ITEMS.register("creative_transmuter", key -> new CreativeTransmuterItem(CREATIVE_TRANSMUTER_BLOCK.get(), blockItemProps(key)));

    public static final DeferredItem<EternalTotemItem> ETERNAL_TOTEM = ITEMS.register("eternal_totem", key -> new EternalTotemItem(itemProps(key)));
    public static final DeferredItem<EternalSwordItem> ETERNAL_SWORD = ITEMS.register("eternal_sword", key -> new EternalSwordItem(itemProps(key)));

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

    // 26.x：BlockEntityType.Builder 已移除，直接用 NeoForge 补出来的公有构造器 new BlockEntityType<>(factory, blocks...)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CommonEntity>> FARMLAND_ENTITY = ENTITIES.register("farmland", () -> new BlockEntityType<>((pos, state) -> new CommonEntity(pos, state, Registration.FARMLAND_ENTITY::get), FARMLAND_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoFarmlandEntity>> AUTO_FARMLAND_ENTITY = ENTITIES.register("auto_farmland", () -> new BlockEntityType<>(AutoFarmlandEntity::new, AUTO_FARMLAND_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClockEntity>> CLOCK_ENTITY = ENTITIES.register("clock", () -> new BlockEntityType<>(ClockEntity::new, CLOCK_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidFountainEntity>> LIQUID_FOUNTAIN_ENTITY = ENTITIES.register("liquid_fountain", () -> new BlockEntityType<>(LiquidFountainEntity::new, LIQUID_FOUNTAIN_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeTransmuterEntity>> CREATIVE_TRANSMUTER_ENTITY = ENTITIES.register("creative_transmuter", () -> new BlockEntityType<>(CreativeTransmuterEntity::new, CREATIVE_TRANSMUTER_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageFountainEntity>> STORAGE_FOUNTAIN_ENTITY = ENTITIES.register("storage_fountain", () -> new BlockEntityType<>(StorageFountainEntity::new, STORAGE_FOUNTAIN_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstantFurnaceEntity>> INSTANT_FURNACE_ENTITY = ENTITIES.register("instant_furnace", () -> new BlockEntityType<>(InstantFurnaceEntity::new, INSTANT_FURNACE_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstantInscriberEntity>> INSTANT_INSCRIBER_ENTITY = ENTITIES.register("instant_inscriber", () -> new BlockEntityType<>(InstantInscriberEntity::new, INSTANT_INSCRIBER_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractionInterfaceEntity>> EXTRACTION_INTERFACE_ENTITY = ENTITIES.register("extraction_interface", () -> new BlockEntityType<>(ExtractionInterfaceEntity::new, EXTRACTION_INTERFACE_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobFarmEntity>> MOB_FARM_ENTITY = ENTITIES.register("mob_farm", () -> new BlockEntityType<>(MobFarmEntity::new, MOB_FARM_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceFarmEntity>> RESOURCE_FARM_ENTITY = ENTITIES.register("resource_farm", () -> new BlockEntityType<>(ResourceFarmEntity::new, RESOURCE_FARM_BLOCK.get()));

    /**
     * 能力集中注册。NeoForge 里方块实体**不再实现 {@code ICapabilityProvider}**、也没有 {@code LazyOptional}，
     * 而是由 mod 总线的 {@link RegisterCapabilitiesEvent} 统一告诉游戏"某个方块实体能提供什么"。
     * 每台机器在实体侧暴露一个 getter（见各 Entity 的 {@code getItemHandler} 等）。
     */
    public static void registerCapabilities(@Nonnull RegisterCapabilitiesEvent event) {
        // 注：耕地（CommonEntity）与时钟（ClockEntity）在 1.20.1 就没有对外暴露任何能力，
        // 这里不注册，与旧版行为保持一致。
        event.registerBlockEntity(Capabilities.Item.BLOCK, AUTO_FARMLAND_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, STORAGE_FOUNTAIN_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, LIQUID_FOUNTAIN_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, LIQUID_FOUNTAIN_ENTITY.get(), (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, CREATIVE_TRANSMUTER_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, MOB_FARM_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, RESOURCE_FARM_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, INSTANT_FURNACE_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, INSTANT_FURNACE_ENTITY.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, INSTANT_INSCRIBER_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, INSTANT_INSCRIBER_ENTITY.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, EXTRACTION_INTERFACE_ENTITY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, EXTRACTION_INTERFACE_ENTITY.get(), (be, side) -> be.getFluidHandler(side));
    }
}
