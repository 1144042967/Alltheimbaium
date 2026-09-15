package cn.sd.jrz.alltheimbaium.entity;

import static cn.sd.jrz.alltheimbaium.setup.Registration.INSTANT_FURNACE_ENTITY;
import static cn.sd.jrz.alltheimbaium.setup.Registration.INSTANT_FURNACE_ITEM;
import cn.sd.jrz.alltheimbaium.connection.InstantFurnaceConnection;
import cn.sd.jrz.alltheimbaium.gui.InstantFurnaceMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ATI 零刻熔炉实体（大数版，参考方块生成机/自动耕地）。
 * <p>
 * 输入区与输出区各为最多 {@link #MAX_TYPES} 行"物品种类行"：每行 = 一种物品 + {@code long} 大数存量，
 * 数量无 64 上限、可保存大数；GUI 以缩写数字展示。熔炼无耗时：
 * <ul>
 *     <li>每 tick 服务端先做一轮批量熔炼（SMELTING→BLASTING→SMOKING 三级兜底查配方，
 *         每种输入行按当前电量整批一次算完），合成完成后执行输出推送；</li>
 *     <li>被动输入/输出只做收发货（并入输入行 / 从输出行抽取），不触发配方计算；
 *         "能否按配方生效"由每 tick 合成环节统一判断；</li>
 *     <li>交换按钮仅交换输入/输出内容，不触发熔炼。</li>
 * </ul>
 * 输出行产物每 tick 向启用"推送"的面转给相邻机器/箱子；任意面开放 IItemHandler 供管道插入/抽取。
 */
public class InstantFurnaceEntity extends BlockEntity implements MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(InstantFurnaceEntity.class);

    /**
     * FE 能量上限：20 亿。仍在 int 范围内，但**超过数据槽的 16 位**，同步时必须拆成高低两块
     * （见 {@code InstantFurnaceMenu} 的 chunk 工具）。
     */
    public static final int MAX_ENERGY = 2_000_000_000;
    /** 每熔炼一个物品消耗的 FE */
    public static final int ENERGY_PER_SMELT = 1000;
    /**
     * 查表优先级：熔炉 → 高炉 → 烟熏（同一种原料只取最先命中的那一条）。
     * 机器与 JEI 都读这一份，保证两边展示与结算的顺序一致。
     */
    public static final List<RecipeType<? extends AbstractCookingRecipe>> FURNACE_TYPES =
            List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING);
    /** 输入 / 输出区各可容纳的最大物品种类数（"槽位数"） */
    public static final int MAX_TYPES = 18;

    // 六面输出状态
    public static final int STATE_PUSH = 0;
    public static final int STATE_DISABLED = 1;
    public static final int STATE_COUNT = 2;

    /** 一种物品 + 大数存量 */
    public static final class Row {
        @Nonnull
        public final Item item;
        public long stock;

        public Row(@Nonnull Item item) {
            this.item = item;
        }
    }

    /** 六面推送状态，索引与 Direction.values() 顺序一致 */
    public final int[] directionState = new int[6];
    /** 输出推送轮询游标 */
    public int findIndex = 0;

    /** 输入行（等待熔炼的原料，种类 ≤ MAX_TYPES） */
    public final List<Row> inputRows = new ArrayList<>();
    /** 输出行（熔炼成品，种类 ≤ MAX_TYPES） */
    public final List<Row> outputRows = new ArrayList<>();

    /** FE 能量存储：上限 2 亿、只接收不放出；合成由每 tick 按当前电量批量执行。 */
    public final SmeltEnergy energy = new SmeltEnergy(this);


    /**
     * 对外 IItemHandler 能力（行为方向无关：插入进输入区、抽取自输出区）。
     * NeoForge 不再实现 ICapabilityProvider，由 {@code registerCapabilities} 拉取。
     */
    private final InstantFurnaceConnection itemHandler = new InstantFurnaceConnection(this);

    /** 26.x：{@link Capabilities.Item#BLOCK} 要求的是 {@code ResourceHandler<ItemResource>} */
    @Nonnull
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    /** 对外能量能力（任意方向均返回同一存储）；26.x 的能力类型是 {@link EnergyHandler} */
    @Nonnull
    public EnergyHandler getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    public InstantFurnaceEntity(BlockPos pos, BlockState state) {
        super(INSTANT_FURNACE_ENTITY.get(), pos, state);
        // 六面输出默认全关：刚放下时不该把产物主动推给相邻方块，要玩家在界面里逐面打开
        Arrays.fill(directionState, STATE_DISABLED);
    }

    // ==================== 行读取（菜单/数据同步用） ====================

    public int getInputCount() {
        return inputRows.size();
    }

    public int getOutputCount() {
        return outputRows.size();
    }

    @Nonnull
    public ItemStack getInputStack(int index) {
        if (index < 0 || index >= inputRows.size()) {
            return ItemStack.EMPTY;
        }
        Row row = inputRows.get(index);
        return row.stock > 0 ? new ItemStack(row.item, 1) : ItemStack.EMPTY;
    }

    @Nonnull
    public ItemStack getOutputStack(int index) {
        if (index < 0 || index >= outputRows.size()) {
            return ItemStack.EMPTY;
        }
        Row row = outputRows.get(index);
        return row.stock > 0 ? new ItemStack(row.item, 1) : ItemStack.EMPTY;
    }

    public long getInputStock(int index) {
        return index >= 0 && index < inputRows.size() ? inputRows.get(index).stock : 0;
    }

    public long getOutputStock(int index) {
        return index >= 0 && index < outputRows.size() ? outputRows.get(index).stock : 0;
    }

    public int getInputItemId(int index) {
        if (index < 0 || index >= inputRows.size() || inputRows.get(index).stock <= 0) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.getId(inputRows.get(index).item);
    }

    public int getOutputItemId(int index) {
        if (index < 0 || index >= outputRows.size() || outputRows.get(index).stock <= 0) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.getId(outputRows.get(index).item);
    }

    // ==================== 行增删 ====================

    @Nullable
    private Row findRow(List<Row> list, Item item) {
        for (Row row : list) {
            if (row.item == item) {
                return row;
            }
        }
        return null;
    }

    private void cleanEmptyRows(List<Row> list) {
        list.removeIf(row -> row.stock <= 0);
    }

    /**
     * 把一组物品并入输入行（同种类累加 / 无则新建行），供管道、手动投料调用。
     *
     * @return 无法存入的剩余物品（输入行种类已满且该种类不存在时原样返回），空表示全部接收
     */
    @Nonnull
    public ItemStack insertInput(@Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = stack.getItem();
        Row row = findRow(inputRows, item);
        boolean canAccept = row != null || inputRows.size() < MAX_TYPES;
        if (!canAccept) {
            return stack;
        }
        if (!simulate) {
            if (row == null) {
                row = new Row(item);
                inputRows.add(row);
            }
            row.stock = Tool.suit(row.stock + stack.getCount());
            setChanged();
            // 被动输入只收发货；配方合成交由每 tick 统一执行
        }
        return ItemStack.EMPTY;
    }

    /**
     * 把一个产物的 count 件并入输出行（熔炼用）。返回未能存入的数量（种类满且无该种类时）。
     */
    public long addOutputProduct(@Nonnull Item item, long count) {
        if (count <= 0) {
            return 0;
        }
        Row row = findRow(outputRows, item);
        if (row == null) {
            if (outputRows.size() >= MAX_TYPES) {
                return count;
            }
            row = new Row(item);
            outputRows.add(row);
        }
        row.stock = Tool.suit(row.stock + count);
        return 0;
    }

    /**
     * 从输入行取出最多 max 件（GUI 手动取回原料），返回实际取出数
     */
    public long extractInputItems(int index, long max) {
        return extractRow(inputRows, index, max);
    }

    /**
     * 从输出行取出最多 max 件（GUI/管道取成品），返回实际取出数
     */
    public long extractOutputItems(int index, long max) {
        return extractRow(outputRows, index, max);
    }

    private long extractRow(List<Row> list, int index, long max) {
        try {
            if (index < 0 || index >= list.size() || max <= 0) {
                return 0;
            }
            Row row = list.get(index);
            if (row.stock <= 0) {
                return 0;
            }
            long toExtract = Math.min(row.stock, max);
            row.stock -= toExtract;
            cleanEmptyRows(list);
            setChanged();
            return toExtract;
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.extractRow error", e);
        }
        return 0;
    }

    // ==================== 熔炼（每 tick 批量） ====================

    /** 烧炼配方结果缓存：Item → 产物（EMPTY = 不可烧哨兵）；随 RecipeManager 实例变化重建，避免每 tick 全表扫配方 */
    private RecipeManager cookCacheManager;
    private final Map<Item, ItemStack> cookCache = new HashMap<>();

    private void ensureCookCache(@Nonnull Level level) {
        RecipeManager rm = recipeManager(level);
        if (rm != cookCacheManager) {
            cookCacheManager = rm;
            cookCache.clear();
        }
    }

    /**
     * 取当前可用的配方管理器。
     * <p>
     * 26.x：{@code Level#getRecipeManager()} 已删除，改走 {@code Level#recipeAccess()}；而 26.x 起
     * <b>客户端不再同步完整配方表</b>（{@code ClientLevel.recipeAccess()} 只有属性集与切石机配方），
     * 因此这里做类型判定，取不到时按"没有配方"处理。熔炉的产物查询本来就只在服务端生效。
     */
    @Nullable
    private static RecipeManager recipeManager(@Nonnull Level level) {
        if (level.isClientSide()) {
            return null;
        }
        return level.recipeAccess() instanceof RecipeManager manager ? manager : null;
    }

    /**
     * 每 tick 单遍批量熔炼（服务端）：把输入行中可烧炼物品整批转为输出行产物（大数，无每行 64 上限）。
     * 对每个输入行一次算完：n = min(行存量, 电量 / 单件耗能)，扣 n、扣能 n×1000、加 n×产物数量，不逐件循环。
     *
     * @return 是否有改动（供 serverTick 门控 setChanged）
     */
    private boolean smeltOnce(@Nonnull Level level) {
        if (inputRows.isEmpty() || energy.getEnergyStored() < ENERGY_PER_SMELT) {
            return false;
        }
        ensureCookCache(level);
        boolean moved = false;
        // 快照遍历：处理中可能因取空移除行
        for (Row row : new ArrayList<>(inputRows)) {
            if (row.stock <= 0) {
                continue;
            }
            if (energy.getEnergyStored() < ENERGY_PER_SMELT) {
                break; // 电量不足：其余行同样无法熔炼
            }
            ItemStack result = cookCache.computeIfAbsent(row.item, item -> findCookResult(item, level));
            if (result.isEmpty()) {
                continue; // 不可烧（含 EMPTY 哨兵）：快速跳过
            }
            Item outItem = result.getItem();
            long perOut = Math.max(1, result.getCount());
            Row outRow = findRow(outputRows, outItem);
            if (outRow == null && outputRows.size() >= MAX_TYPES) {
                continue; // 输出行种类已满且该产物不在其中：暂无法熔炼（扣料前预判）
            }
            long n = Math.min(row.stock, (long) energy.getEnergyStored() / ENERGY_PER_SMELT);
            if (n <= 0) {
                break;
            }
            row.stock -= n;
            energy.spendEnergy((int) (n * ENERGY_PER_SMELT));
            addOutputProduct(outItem, n * perOut);
            moved = true;
        }
        cleanEmptyRows(inputRows);
        return moved;
    }

    /**
     * 通配 {@code RecipeType<? extends AbstractCookingRecipe>} 下的配方表读取（消除类型捕获带来的编译错误）。
     * <p>
     * 26.x：{@code RecipeManager#getAllRecipesFor} 已删除，改成从全表里按配方类型筛。
     */
    @SuppressWarnings("unchecked")
    public static List<RecipeHolder<? extends AbstractCookingRecipe>> getAllCookingRecipes(
            @Nonnull RecipeManager manager, @Nonnull RecipeType<? extends AbstractCookingRecipe> type) {
        List<RecipeHolder<? extends AbstractCookingRecipe>> list = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.value() instanceof AbstractCookingRecipe cooking && cooking.getType() == type) {
                list.add((RecipeHolder<? extends AbstractCookingRecipe>) holder);
            }
        }
        return list;
    }

    /**
     * 查询物品的烧炼配方结果（仅在缓存 miss 时调用一次）：按 SMELTING→BLASTING→SMOKING 三级兜底。
     * 不可烧返回 EMPTY（同样入缓存作哨兵，避免反复查表）；客户端不查。
     * <p>
     * 26.x：{@code Recipe#getIngredients}/{@code getResultItem} 已删除，改用
     * {@code RecipeManager#getRecipeFor(RecipeType, RecipeInput, Level)} 直接按输入查表，
     * 产物走 {@code Recipe#assemble(RecipeInput)}。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nonnull
    private static ItemStack findCookResult(@Nonnull Item item, @Nonnull Level level) {
        if (level.isClientSide()) {
            return ItemStack.EMPTY;
        }
        try {
            RecipeManager manager = recipeManager(level);
            if (manager == null) {
                return ItemStack.EMPTY;
            }
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(item, 1));
            for (RecipeType<? extends AbstractCookingRecipe> type : FURNACE_TYPES) {
                // 通配类型下泛型没法直接调用，用原始类型摊平（具体类型由 FURNACE_TYPES 保证）
                Object found = manager.getRecipeFor((RecipeType) type, input, level).orElse(null);
                if (found instanceof RecipeHolder<?> holder && holder.value() instanceof AbstractCookingRecipe recipe) {
                    return recipe.assemble(input).copy();
                }
            }
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.findCookResult error", e);
        }
        return ItemStack.EMPTY;
    }

    // ==================== 输出推送 ====================

    /** 单次大批量塞入上限（件），避免对无限容量邻居逐 64 组循环导致卡顿 */
    private static final long PUSH_BATCH = 1_000_000;

    /**
     * 服务端 tick：每 tick 先完成一轮配方合成，合成运算后执行输出推送。
     * 只在确有改动（合成动行/扣能、推送推货）时才 setChanged，避免闲置机器持续脏标记。
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        try {
            boolean changed = smeltOnce(level); // 每 tick 先合成
            changed |= pushOutput();            // 合成后输出
            if (changed) {
                setChanged();
            }
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.serverTick error", e);
        }
    }

    /**
     * 把输出行产物转给启用"推送"的相邻面。每 tick 从轮询游标起探测，
     * 本 tick 最多成功推给一面；选中面无目标/满时顺延探测其余启用面，避免空等。
     *
     * @return 是否实际推出了物品
     */
    private boolean pushOutput() {
        Level level = getLevel();
        if (level == null || outputRows.isEmpty()) {
            return false;
        }
        boolean pushed = false;
        for (int attempt = 0; attempt < 6; attempt++) {
            findIndex = (findIndex + 1) % 6;
            Direction direction = Direction.values()[findIndex];
            if (directionState[findIndex] != STATE_PUSH) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, neighbor.getBlockPos(), direction.getOpposite());
            if (handler == null) {
                continue;
            }
            for (Row row : new ArrayList<>(outputRows)) {
                if (row.stock <= 0) {
                    continue;
                }
                ItemResource resource = ItemResource.of(row.item);
                long remaining = row.stock;
                while (remaining > 0) {
                    int amount = (int) Math.min(remaining, PUSH_BATCH);
                    if (amount <= 0) {
                        break;
                    }
                    // 26.x：insertStacking 内部会开一个根事务并在结束时提交，等价于旧的 ItemHandlerHelper.insertItemStacked
                    int inserted = ResourceHandlerUtil.insertStacking(handler, resource, amount, null);
                    if (inserted <= 0) {
                        break; // 该面已满：顺延下一面
                    }
                    row.stock -= inserted;
                    remaining -= inserted;
                    pushed = true;
                }
            }
            cleanEmptyRows(outputRows);
            if (pushed) {
                return true; // 本 tick 服务一面即可，其余面留待后续轮转
            }
        }
        return false;
    }

    // ==================== 交换输入/输出 ====================

    /**
     * 把输入行与输出行的内容整体互换（仅交换物品，不触发熔炼；合成由每 tick 统一执行）。
     */
    public void swapSlots() {
        try {
            List<Row> tmp = new ArrayList<>(inputRows);
            inputRows.clear();
            inputRows.addAll(outputRows);
            outputRows.clear();
            outputRows.addAll(tmp);
            cleanEmptyRows(inputRows);
            cleanEmptyRows(outputRows);
            setChanged();
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.swapSlots error", e);
        }
    }

    // ==================== 方向状态 ====================

    public int getDirectionState(Direction direction) {
        return directionState[direction.ordinal()];
    }

    /**
     * 切换指定面的推送状态（推送 ↔ 禁用）
     */
    public void cycleDirection(Direction direction) {
        int idx = direction.ordinal();
        directionState[idx] = (directionState[idx] + 1) % STATE_COUNT;
        setChanged();
    }

    // ==================== 能量读取 ====================

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    // ==================== 能量存储实现 ====================

    /**
     * 可被机器直控的 FE 能量存储（上限 2 亿、只接收不放出）。
     * <p>
     * 26.x：能力类型是 {@link EnergyHandler}，基类因此从 {@code EnergyStorage} 换成
     * {@code SimpleEnergyHandler}——外部注入走它自带的事务日志（回滚/提交由框架保证），
     * {@code onEnergyChanged} 里再补脏标记；spendEnergy / setEnergyStored 直接操作
     * protected 的 energy 字段完成"扣电/写存量"，不走 insert/extract 的限额与事务，
     * 防止熔炼扣电时自触发递归熔炼。
     */
    private static class SmeltEnergy extends SimpleEnergyHandler {
        private final InstantFurnaceEntity owner;

        SmeltEnergy(InstantFurnaceEntity owner) {
            super(MAX_ENERGY, MAX_ENERGY, 0);
            this.owner = owner;
        }

        /** 注入能量后脏标记持久化（事务提交时回调）；合成由每 tick 统一执行 */
        @Override
        protected void onEnergyChanged(int previousAmount) {
            owner.setChanged();
        }

        public int getEnergyStored() {
            return this.energy;
        }

        public int getMaxEnergyStored() {
            return this.capacity;
        }

        /** 直接写入存量（加载/恢复用），自动裁剪到 [0, 上限] */
        public void setEnergyStored(int amount) {
            this.energy = Math.max(0, Math.min(MAX_ENERGY, amount));
        }

        /** 直接扣减存量（熔炼用），不走 extract 的 maxExtract 限制与事务 */
        public int spendEnergy(int amount) {
            int deducted = Math.min(this.energy, Math.max(0, amount));
            this.energy -= deducted;
            return deducted;
        }
    }

    // ==================== 菜单提供 ====================

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.instant_furnace").withStyle(Tip.rarityColor(INSTANT_FURNACE_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new InstantFurnaceMenu(id, inv, worldPosition);
    }

    // ==================== Capability ====================


    // ==================== NBT 存取 ====================

    private static final String KEY_INPUT = "input";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_DIR = "directionState";

    @Override
    protected void saveAdditional(@Nonnull ValueOutput output) {
        super.saveAdditional(output);
        try {
            // 26.x：产物行改由 ValueOutput 列表托管，物品组件自动按当前注册表访问器编解码
            Tool.writeRows(output, KEY_INPUT, toStockRows(inputRows), Tool.StockRow.CODEC);
            Tool.writeRows(output, KEY_OUTPUT, toStockRows(outputRows), Tool.StockRow.CODEC);
            output.putInt(KEY_ENERGY, energy.getEnergyStored());
            output.putIntArray(KEY_DIR, directionState);
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.saveAdditional error", e);
        }
    }

    @Override
    protected void loadAdditional(@Nonnull ValueInput input) {
        super.loadAdditional(input);
        try {
            // 反序列化只恢复状态；合成由每 tick 统一执行
            loadRows(inputRows, Tool.readRows(input, KEY_INPUT, Tool.StockRow.CODEC));
            loadRows(outputRows, Tool.readRows(input, KEY_OUTPUT, Tool.StockRow.CODEC));
            energy.setEnergyStored(input.getIntOr(KEY_ENERGY, energy.getEnergyStored()));
            int[] arr = input.getIntArray(KEY_DIR).orElse(null);
            if (arr != null) {
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionState[i] = Math.max(0, Math.min(STATE_COUNT - 1, arr[i]));
                }
            }
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.loadAdditional error", e);
        }
    }

    /** 内部可变行 → 持久化记录 */
    @Nonnull
    private static List<Tool.StockRow> toStockRows(@Nonnull List<Row> rows) {
        List<Tool.StockRow> list = new ArrayList<>(rows.size());
        for (Row row : rows) {
            list.add(new Tool.StockRow(Tool.oneOf(new ItemStack(row.item)), row.stock));
        }
        return list;
    }

    private static void loadRows(@Nonnull List<Row> target, @Nonnull List<Tool.StockRow> list) {
        target.clear();
        for (Tool.StockRow record : list) {
            try {
                ItemStack stack = record.item();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Row row = new Row(stack.getItem());
                row.stock = Tool.suit(record.count());
                target.add(row);
            } catch (Throwable e) {
                log.warn("InstantFurnaceEntity.loadRows entry error", e);
            }
        }
        target.removeIf(row -> row.stock <= 0);
    }

    // ==================== 客户端同步 ====================
    // 26.x：handleUpdateTag / onDataPacket 的覆写已删除，改由原版 loadWithComponents(ValueInput) 默认接管。

    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
