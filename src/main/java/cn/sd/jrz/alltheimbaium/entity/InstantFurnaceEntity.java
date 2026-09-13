package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.InstantFurnaceConnection;
import cn.sd.jrz.alltheimbaium.gui.InstantFurnaceMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
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
     * NeoForge 不再实现 ICapabilityProvider，由 {@code Registration.registerCapabilities} 拉取。
     */
    private final InstantFurnaceConnection itemHandler = new InstantFurnaceConnection(this);

    @Nonnull
    public InstantFurnaceConnection getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    /** 对外能量能力（任意方向均返回同一存储） */
    @Nonnull
    public EnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    public InstantFurnaceEntity(BlockPos pos, BlockState state) {
        super(Registration.INSTANT_FURNACE_ENTITY.get(), pos, state);
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
        RecipeManager rm = level.getRecipeManager();
        if (rm != cookCacheManager) {
            cookCacheManager = rm;
            cookCache.clear();
        }
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

    /** 通配 {@code RecipeType<? extends AbstractCookingRecipe>} 下的配方表读取（消除类型捕获带来的编译错误） */
    @SuppressWarnings("unchecked")
    public static List<RecipeHolder<? extends AbstractCookingRecipe>> getAllCookingRecipes(
            @Nonnull RecipeManager manager, @Nonnull RecipeType<? extends AbstractCookingRecipe> type) {
        return (List<RecipeHolder<? extends AbstractCookingRecipe>>) (List<?>) manager.getAllRecipesFor((RecipeType) type);
    }

    /**
     * 查询物品的烧炼配方结果（仅在缓存 miss 时调用一次）：按 SMELTING→BLASTING→SMOKING 三级兜底。
     * 不可烧返回 EMPTY（同样入缓存作哨兵，避免反复查表）；客户端不查。
     */
    @Nonnull
    private static ItemStack findCookResult(@Nonnull Item item, @Nonnull Level level) {
        if (level.isClientSide) {
            return ItemStack.EMPTY;
        }
        try {
            RecipeManager recipeManager = level.getRecipeManager();
            ItemStack single = new ItemStack(item, 1);
            for (RecipeType<? extends AbstractCookingRecipe> type : FURNACE_TYPES) {
                // 1.21：getAllRecipesFor 的返回类型与 RecipeType 的类型参数绑定，
                // 通配类型下用 RecipeHolder<? extends AbstractCookingRecipe> 接住
                for (RecipeHolder<? extends AbstractCookingRecipe> __htype : getAllCookingRecipes(recipeManager, type)) {
                    AbstractCookingRecipe recipe = __htype.value();
                    for (Ingredient ingredient : recipe.getIngredients()) {
                        if (ingredient.test(single)) {
                            return recipe.getResultItem(level.registryAccess()).copy();
                        }
                    }
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
        if (level == null || level.isClientSide) {
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
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor.getBlockPos(), direction.getOpposite());
            if (handler == null) {
                continue;
            }
            for (Row row : new ArrayList<>(outputRows)) {
                if (row.stock <= 0) {
                    continue;
                }
                long remaining = row.stock;
                while (remaining > 0) {
                    int amount = (int) Math.min(remaining, PUSH_BATCH);
                    if (amount <= 0) {
                        break;
                    }
                    ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(row.item, amount), false);
                    int inserted = amount - leftover.getCount();
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
     * receiveEnergy 注入能量后回调机器尝试熔炼；spendEnergy / setEnergyStored 直接操作
     * EnergyStorage 的 protected energy 字段完成"扣电/写存量"，避免走 receiveEnergy/extractEnergy
     * 的开关限制与回调，防止熔炼扣电时自触发递归熔炼。
     */
    private static class SmeltEnergy extends EnergyStorage {
        private final InstantFurnaceEntity owner;

        SmeltEnergy(InstantFurnaceEntity owner) {
            super(MAX_ENERGY, MAX_ENERGY, 0);
            this.owner = owner;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                owner.setChanged(); // 能量须脏标记持久化；合成由每 tick 统一执行
            }
            return received;
        }

        /** 直接写入存量（加载/恢复用），自动裁剪到 [0, 上限] */
        public void setEnergyStored(int amount) {
            this.energy = Math.max(0, Math.min(MAX_ENERGY, amount));
        }

        /** 直接扣减存量（熔炼用），不走 extractEnergy 的 canExtract 限制与回调 */
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
        return Component.translatable("block.alltheimbaium.instant_furnace").withStyle(Tip.rarityColor(Registration.INSTANT_FURNACE_ITEM.get()));
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
    public void saveAdditional(@Nonnull CompoundTag nbt, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        try {
            nbt.put(KEY_INPUT, saveRows(inputRows, registries));
            nbt.put(KEY_OUTPUT, saveRows(outputRows, registries));
            nbt.putInt(KEY_ENERGY, energy.getEnergyStored());
            nbt.putIntArray(KEY_DIR, directionState);
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.saveAdditional error", e);
        }
    }

    @Override
    public void loadAdditional(@Nonnull CompoundTag nbt, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        try {
            // 反序列化只恢复状态；合成由每 tick 统一执行
            if (nbt.contains(KEY_INPUT, Tag.TAG_LIST)) {
                loadRows(inputRows, (ListTag) nbt.get(KEY_INPUT), registries);
            }
            if (nbt.contains(KEY_OUTPUT, Tag.TAG_LIST)) {
                loadRows(outputRows, (ListTag) nbt.get(KEY_OUTPUT), registries);
            }
            if (nbt.contains(KEY_ENERGY, Tag.TAG_INT)) {
                energy.setEnergyStored(nbt.getInt(KEY_ENERGY));
            }
            if (nbt.contains(KEY_DIR)) {
                int[] arr = nbt.getIntArray(KEY_DIR);
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionState[i] = Math.max(0, Math.min(STATE_COUNT - 1, arr[i]));
                }
            }
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.load error", e);
        }
    }

    private static ListTag saveRows(List<Row> rows, @Nonnull HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Row row : rows) {
            // 1.21：save 需要注册表访问器，且一律以返回值为准
            CompoundTag c = (CompoundTag) new ItemStack(row.item, 1).save(registries, new CompoundTag());
            c.putLong("Stock", row.stock);
            list.add(c);
        }
        return list;
    }

    private static void loadRows(List<Row> target, ListTag list, @Nonnull HolderLookup.Provider registries) {
        target.clear();
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag c = list.getCompound(i);
                ItemStack stack = ItemStack.parseOptional(registries, c);
                if (stack.isEmpty()) {
                    continue;
                }
                Row row = new Row(stack.getItem());
                row.stock = c.contains("Stock", Tag.TAG_LONG) ? Tool.suit(c.getLong("Stock")) : 0;
                target.add(row);
            } catch (Throwable e) {
                log.warn("InstantFurnaceEntity.loadRows entry error", e);
            }
        }
        target.removeIf(row -> row.stock <= 0);
    }

    // ==================== 客户端同步 ====================

    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        this.loadAdditional(tag, registries);
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt, @Nonnull HolderLookup.Provider registries) {
        // 1.21：改走 NeoForge 扩展的 IBlockEntityExtension#onDataPacket(Connection, Packet, Provider)
        this.loadAdditional(pkt.getTag(), registries);
    }
}
