package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.InstantFurnaceConnection;
import cn.sd.jrz.alltheimbaium.gui.InstantFurnaceMenu;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ATI 零刻熔炉实体（AE 大数版，参考方块生成机/自动耕地）。
 * <p>
 * 输入区与输出区各为最多 {@link #MAX_TYPES} 行"物品种类行"：每行 = 一种物品 + {@code long} 大数存量，
 * 数量无 64 上限、可保存大数；GUI 以 AE 风格数字展示。熔炼无耗时：
 * <ul>
 *     <li>物品被加入输入行 → 立即尝试查询烧炼配方（SMELTING→BLASTING→SMOKING 三级兜底），
 *         每件扣 {@link #ENERGY_PER_SMELT} FE，产物并入对应输出行；</li>
 *     <li>触发时机 = 输入行变化 / 能量注入后存量 ≥ 单件耗能 / 交换完成后；</li>
 *     <li>不逐 tick 计算。</li>
 * </ul>
 * 输出行产物每 tick 向启用"推送"的面转给相邻机器/箱子；任意面开放 IItemHandler 供管道插入/抽取。
 */
public class InstantFurnaceEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(InstantFurnaceEntity.class);

    /** FE 能量上限：2 亿（小于 int 上限，可用单个数据槽同步） */
    public static final int MAX_ENERGY = 200_000_000;
    /** 每熔炼一个物品消耗的 FE */
    public static final int ENERGY_PER_SMELT = 1000;
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
    /** 熔炼进行中护栏（防止 onContentsChanged / 能量注入回调自触发递归） */
    private boolean smelting = false;
    /** 输出推送轮询游标 */
    public int findIndex = 0;

    /** 输入行（等待熔炼的原料，种类 ≤ MAX_TYPES） */
    public final List<Row> inputRows = new ArrayList<>();
    /** 输出行（熔炼成品，种类 ≤ MAX_TYPES） */
    public final List<Row> outputRows = new ArrayList<>();

    /** FE 能量存储：上限 2 亿、只接收不放出；注入能量后存量 ≥ 单件耗能时触发一次熔炼。 */
    public final SmeltEnergy energy = new SmeltEnergy(this);

    /** 对外 IItemHandler 能力（行为方向无关：插入进输入区、抽取自输出区） */
    private final LazyOptional<InstantFurnaceConnection> itemOptional =
            LazyOptional.of(() -> new InstantFurnaceConnection(this));
    /** 对外能量能力（任意方向均返回同一存储） */
    private final LazyOptional<EnergyStorage> energyOptional = LazyOptional.of(() -> energy);

    public InstantFurnaceEntity(BlockPos pos, BlockState state) {
        super(Registration.INSTANT_FURNACE_ENTITY.get(), pos, state);
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
            requestSmelt();
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

    // ==================== 熔炼 ====================

    /**
     * 请求一次熔炼计算（服务端）。调用点：输入行变化、能量注入、交换完成后。
     */
    public void requestSmelt() {
        Level level = getLevel();
        if (level == null || level.isClientSide || smelting) {
            return;
        }
        smelting = true;
        try {
            doSmeltPass();
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.requestSmelt error", e);
        } finally {
            smelting = false;
        }
    }

    /**
     * 执行一轮熔炼：尽量把输入行中的可烧炼物品转为输出行产物（AE 大数，无每行 64 上限）。
     * 每行一次性熔炼到"该行空 / 电量不足 / 输出行种类已满"为止，减少配方查询次数。
     */
    private void doSmeltPass() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        boolean anyMoved = false;
        boolean movedThisRound;
        int guard = 0;
        do {
            movedThisRound = false;
            // 快照遍历：处理中可能因取空移除行
            List<Row> snapshot = new ArrayList<>(inputRows);
            for (Row row : snapshot) {
                if (row.stock <= 0) {
                    continue;
                }
                if (energy.getEnergyStored() < ENERGY_PER_SMELT) {
                    break; // 电量不足：其余行同样无法熔炼
                }
                ItemStack result = findCookResult(new ItemStack(row.item, 1), level);
                if (result.isEmpty()) {
                    continue;
                }
                Item outItem = result.getItem();
                long perOut = Math.max(1, result.getCount());
                Row outRow = findRow(outputRows, outItem);
                if (outRow == null) {
                    if (outputRows.size() >= MAX_TYPES) {
                        continue; // 输出行种类已满且该产物不在其中：暂无法熔炼
                    }
                    outRow = new Row(outItem);
                    outputRows.add(outRow);
                }
                long n = Math.min(row.stock, energy.getEnergyStored() / ENERGY_PER_SMELT);
                if (n <= 0) {
                    break;
                }
                row.stock -= n;
                energy.spendEnergy((int) (n * ENERGY_PER_SMELT));
                outRow.stock = Tool.suit(outRow.stock + n * perOut);
                anyMoved = true;
                movedThisRound = true;
            }
            cleanEmptyRows(inputRows);
            cleanEmptyRows(outputRows);
            if (++guard > 4096) {
                break; // 安全上限，防止异常导致死循环
            }
        } while (movedThisRound);
        if (anyMoved) {
            setChanged();
        }
    }

    /**
     * 查询物品的烧炼配方结果：按 SMELTING→BLASTING→SMOKING 三级兜底（客户端不查，返回 null）。
     */
    @Nullable
    private static ItemStack findCookResult(@Nonnull ItemStack input, @Nonnull Level level) {
        if (level.isClientSide) {
            return null;
        }
        try {
            RecipeManager recipeManager = level.getRecipeManager();
            ItemStack single = input.copyWithCount(1);
            List<RecipeType<? extends AbstractCookingRecipe>> types = List.of(
                    RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING);
            for (RecipeType<? extends AbstractCookingRecipe> type : types) {
                Collection<? extends AbstractCookingRecipe> recipes = recipeManager.getAllRecipesFor(type);
                for (AbstractCookingRecipe recipe : recipes) {
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
        return null;
    }

    // ==================== 输出推送 ====================

    /**
     * 服务端 tick：把输出行产物转给启用"推送"的相邻面（每 tick 轮询一个面）。
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            pushOutput();
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.serverTick error", e);
        }
        setChanged();
    }

    private void pushOutput() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        findIndex = (findIndex + 1) % 6;
        Direction direction = Direction.values()[findIndex];
        if (directionState[findIndex] != STATE_PUSH) {
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
        if (neighbor == null) {
            return;
        }
        IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
        if (handler == null) {
            return;
        }
        List<Row> snapshot = new ArrayList<>(outputRows);
        for (Row row : snapshot) {
            if (row.stock <= 0) {
                continue;
            }
            int maxStack = Math.max(1, new ItemStack(row.item).getMaxStackSize());
            long remaining = row.stock;
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, (long) maxStack);
                ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(row.item, amount), false);
                int inserted = amount - leftover.getCount();
                if (inserted <= 0) {
                    break;
                }
                row.stock -= inserted;
                remaining -= inserted;
            }
        }
        cleanEmptyRows(outputRows);
    }

    // ==================== 交换输入/输出 ====================

    /**
     * 把输入行与输出行的内容整体互换，完成后请求一次熔炼（先换位再计算）。
     */
    public void swapSlots() {
        if (smelting) {
            return;
        }
        smelting = true;
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
        } finally {
            smelting = false;
        }
        requestSmelt();
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
                owner.requestSmelt();
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
        return Component.translatable("block.alltheimbaium.instant_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new InstantFurnaceMenu(id, inv, worldPosition);
    }

    // ==================== Capability ====================

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                return itemOptional.cast();
            }
            if (capability == ForgeCapabilities.ENERGY) {
                return energyOptional.cast();
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    // ==================== NBT 存取 ====================

    private static final String KEY_INPUT = "input";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_DIR = "directionState";

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.put(KEY_INPUT, saveRows(inputRows));
            nbt.put(KEY_OUTPUT, saveRows(outputRows));
            nbt.putInt(KEY_ENERGY, energy.getEnergyStored());
            nbt.putIntArray(KEY_DIR, directionState);
        } catch (Throwable e) {
            log.error("InstantFurnaceEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            // 反序列化不触发熔炼（熔炼只在输入/能量/交换变化时计算）
            boolean oldSmelting = smelting;
            smelting = true;
            try {
                if (nbt.contains(KEY_INPUT, Tag.TAG_LIST)) {
                    loadRows(inputRows, (ListTag) nbt.get(KEY_INPUT));
                }
                if (nbt.contains(KEY_OUTPUT, Tag.TAG_LIST)) {
                    loadRows(outputRows, (ListTag) nbt.get(KEY_OUTPUT));
                }
            } finally {
                smelting = oldSmelting;
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

    private static ListTag saveRows(List<Row> rows) {
        ListTag list = new ListTag();
        for (Row row : rows) {
            CompoundTag c = new CompoundTag();
            new ItemStack(row.item, 1).save(c);
            c.putLong("Stock", row.stock);
            list.add(c);
        }
        return list;
    }

    private static void loadRows(List<Row> target, ListTag list) {
        target.clear();
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag c = list.getCompound(i);
                ItemStack stack = ItemStack.of(c);
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
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        this.load(tag);
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }
}
