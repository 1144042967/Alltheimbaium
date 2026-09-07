package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.InstantInscriberConnection;
import cn.sd.jrz.alltheimbaium.gui.InstantInscriberMenu;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
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
 * ATI 零刻压印器（AE2 大数版，配方参考 AE2 压印机 Inscriber）。
 * <p>
 * 输入区最多 {@link #INPUT_MAX_TYPES} 行、输出区最多 {@link #OUTPUT_MAX_TYPES} 行（AE 大数存储）。
 * 两档模式（GUI 可切换、NBT 保存）：
 * <ul>
 *     <li>{@link #MODE_INSCRIBE 压板}：读取 AE2 <code>mode:inscribe</code> 配方。模板(top/bottom)不消耗，
 *         每消耗 1 份原料(middle 命中物)，同时生成它支持的所有配方各一份产物；</li>
 *     <li>{@link #MODE_ASSEMBLY 组装}：读取 AE2 <code>mode:press</code> 配方，消耗其全部输入材料，
 *         优先生成需要 3 种材料的配方，其次 2 种材料的配方。</li>
 * </ul>
 * 生成无耗时，仅在"输入变化 / 能量注入后 ≥ 单产物耗能 / 切换模式"时计算一次；每生成一个产物扣
 * {@link #ENERGY_PER_OP} FE（上限 {@link #MAX_ENERGY}）。输出行向启用"推送"的面转给相邻机器。
 */
public class InstantInscriberEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberEntity.class);

    public static final int MAX_ENERGY = 200_000_000;
    public static final int ENERGY_PER_OP = 1000;
    public static final int INPUT_MAX_TYPES = 18;
    public static final int OUTPUT_MAX_TYPES = 9;

    // 模式
    /** 压板：AE2 mode:inscribe（模板不消耗，耗 1 原料产出全部命中结果） */
    public static final int MODE_INSCRIBE = 0;
    /** 组装：AE2 mode:press（消耗全部材料，先 3 后 2） */
    public static final int MODE_ASSEMBLY = 1;
    public static final int MODE_COUNT = 2;

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

    /** AE2 inscribe（压板）配方：middle=消耗物，output=结果 */
    private static final class InscribeEntry {
        final Ingredient material;
        final ItemStack output;
        final int outCount;

        InscribeEntry(Ingredient material, ItemStack output) {
            this.material = material;
            this.output = output;
            this.outCount = Math.max(1, output.getCount());
        }
    }

    /** AE2 press（组装）配方：top/middle/bottom 材料数组，nonEmpty=所需材料种类数 */
    private static final class AssemblyEntry {
        final Ingredient[] mats; // [top, middle, bottom]
        final int nonEmpty;
        final ItemStack output;
        final int outCount;

        AssemblyEntry(Ingredient[] mats, ItemStack output) {
            this.mats = mats;
            int n = 0;
            for (Ingredient ing : mats) {
                if (ing != null && !ing.isEmpty()) {
                    n++;
                }
            }
            this.nonEmpty = n;
            this.output = output;
            this.outCount = Math.max(1, output.getCount());
        }
    }

    public final int[] directionState = new int[6];
    public int mode = MODE_INSCRIBE;
    private boolean working = false; // 计算中护栏（防自触发递归）
    public int findIndex = 0;

    public final List<Row> inputRows = new ArrayList<>();
    public final List<Row> outputRows = new ArrayList<>();

    public final SmeltEnergy energy = new SmeltEnergy(this);
    private final LazyOptional<InstantInscriberConnection> itemOptional =
            LazyOptional.of(() -> new InstantInscriberConnection(this));
    private final LazyOptional<EnergyStorage> energyOptional = LazyOptional.of(() -> energy);

    public InstantInscriberEntity(BlockPos pos, BlockState state) {
        super(Registration.INSTANT_INSCRIBER_ENTITY.get(), pos, state);
    }

    // ==================== 行读取 ====================

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

    // ==================== 模式 ====================

    public int getMode() {
        return mode;
    }

    public boolean isAssembly() {
        return mode == MODE_ASSEMBLY;
    }

    /**
     * 切换模式（压板 ↔ 组装），切换后尝试计算一次
     */
    public void cycleMode() {
        mode = (mode + 1) % MODE_COUNT;
        setChanged();
        requestCompute();
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

    @Nonnull
    public ItemStack insertInput(@Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = stack.getItem();
        Row row = findRow(inputRows, item);
        boolean canAccept = row != null || inputRows.size() < INPUT_MAX_TYPES;
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
            requestCompute();
        }
        return ItemStack.EMPTY;
    }

    public long addOutputProduct(@Nonnull Item item, long count) {
        if (count <= 0) {
            return 0;
        }
        Row row = findRow(outputRows, item);
        if (row == null) {
            if (outputRows.size() >= OUTPUT_MAX_TYPES) {
                return count;
            }
            row = new Row(item);
            outputRows.add(row);
        }
        row.stock = Tool.suit(row.stock + count);
        return 0;
    }

    public long extractInputItems(int index, long max) {
        return extractRow(inputRows, index, max);
    }

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
            log.error("InstantInscriberEntity.extractRow error", e);
        }
        return 0;
    }

    // ==================== 生成 ====================

    /**
     * 请求一次生成计算（服务端）。调用点：输入变化、能量注入、切换模式。
     */
    public void requestCompute() {
        Level level = getLevel();
        if (level == null || level.isClientSide || working) {
            return;
        }
        working = true;
        try {
            if (mode == MODE_ASSEMBLY) {
                doAssembly(level);
            } else {
                doInscribe(level);
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.requestCompute error", e);
        } finally {
            working = false;
        }
    }

    /**
     * 压板模式：每消耗 1 份原料，同时生成它支持的所有 inscribe 配方各一份。
     * 能量/输出行种类不足时整批跳过（不部分消耗材料）。
     */
    private void doInscribe(Level level) {
        List<InscribeEntry> recipes = readInscribe(level);
        if (recipes.isEmpty()) {
            return;
        }
        boolean anyMoved = false;
        for (Row row : new ArrayList<>(inputRows)) {
            if (row.stock <= 0) {
                continue;
            }
            // 该原料命中的所有压板配方
            List<InscribeEntry> hits = new ArrayList<>();
            ItemStack probe = new ItemStack(row.item, 1);
            for (InscribeEntry entry : recipes) {
                if (entry.material.test(probe)) {
                    hits.add(entry);
                }
            }
            if (hits.isEmpty()) {
                continue;
            }
            // 一批成本与输出占位（种类）
            long cost = 0;
            for (InscribeEntry entry : hits) {
                cost += (long) entry.outCount * ENERGY_PER_OP;
            }
            // 每批消耗 1 份原料
            while (row.stock > 0 && energy.getEnergyStored() >= cost && canFitOutputTypes(hits)) {
                row.stock--;
                energy.spendEnergy((int) cost);
                for (InscribeEntry entry : hits) {
                    addOutputProduct(entry.output.getItem(), entry.outCount);
                }
                anyMoved = true;
            }
        }
        cleanEmptyRows(inputRows);
        if (anyMoved) {
            setChanged();
        }
    }

    /**
     * 组装模式：消耗配方全部输入材料生成结果，优先生成 3 材料配方，其次 2 材料配方。
     */
    private void doAssembly(Level level) {
        List<AssemblyEntry> recipes = readAssembly(level);
        if (recipes.isEmpty()) {
            return;
        }
        List<AssemblyEntry> three = new ArrayList<>();
        List<AssemblyEntry> two = new ArrayList<>();
        for (AssemblyEntry entry : recipes) {
            if (entry.nonEmpty == 3) {
                three.add(entry);
            } else if (entry.nonEmpty == 2) {
                two.add(entry);
            }
        }
        boolean anyMoved = false;
        int guard = 0;
        while (guard++ < 4096) {
            boolean anyThree = false;
            for (AssemblyEntry entry : three) {
                while (tryAssemble(entry)) {
                    anyThree = true;
                    anyMoved = true;
                }
            }
            if (anyThree) {
                continue; // 3 材料仍可行则始终优先
            }
            boolean anyTwo = false;
            for (AssemblyEntry entry : two) {
                while (tryAssemble(entry)) {
                    anyTwo = true;
                    anyMoved = true;
                }
            }
            if (!anyTwo) {
                break;
            }
        }
        cleanEmptyRows(inputRows);
        if (anyMoved) {
            setChanged();
        }
    }

    /**
     * 尝试按配方组装一次：成功则扣材料、扣能量、加产物。
     */
    private boolean tryAssemble(AssemblyEntry entry) {
        List<Row> needed = new ArrayList<>();
        for (Ingredient ing : entry.mats) {
            if (ing == null || ing.isEmpty()) {
                continue;
            }
            Row row = null;
            for (Row r : inputRows) {
                if (r.stock > 0 && ing.test(new ItemStack(r.item, 1))) {
                    row = r;
                    break;
                }
            }
            if (row == null) {
                return false;
            }
            needed.add(row);
        }
        if (needed.isEmpty()) {
            return false;
        }
        int cost = entry.outCount * ENERGY_PER_OP;
        if (energy.getEnergyStored() < cost) {
            return false;
        }
        // 输出种类容量
        if (!canFitOutputTypes(entry.output.getItem())) {
            return false;
        }
        for (Row row : needed) {
            row.stock--;
        }
        energy.spendEnergy(cost);
        addOutputProduct(entry.output.getItem(), entry.outCount);
        return true;
    }

    /** 输出区能否容纳这批结果（每个结果必须命中已有输出行，或还有空闲行） */
    private boolean canFitOutputTypes(List<InscribeEntry> entries) {
        int needNew = 0;
        for (InscribeEntry entry : entries) {
            Item item = entry.output.getItem();
            Row row = findRow(outputRows, item);
            if (row == null) {
                needNew++;
            }
        }
        return outputRows.size() + needNew <= OUTPUT_MAX_TYPES;
    }

    private boolean canFitOutputTypes(Item item) {
        return findRow(outputRows, item) != null || outputRows.size() < OUTPUT_MAX_TYPES;
    }

    // ==================== AE2 配方读取（可选联动，无 AE2 时不产出） ====================

    @Nullable
    private static RecipeType<?> findInscriberType(Level level) {
        try {
            var registry = level.registryAccess().registry(Registries.RECIPE_TYPE).orElse(null);
            if (registry == null) {
                return null;
            }
            for (String id : new String[]{"ae2:inscriber", "appliedenergistics2:inscriber"}) {
                RecipeType<?> type = registry.get(new ResourceLocation(id));
                if (type != null) {
                    return type;
                }
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.findInscriberType error", e);
        }
        return null;
    }

    /** 反射读取 Inscriber 配方 processType 名（INSCRIBE / PRESS），读不到返回 null */
    @Nullable
    private static String processName(Recipe<?> recipe) {
        try {
            Object value = recipe.getClass().getMethod("getProcessType").invoke(recipe);
            if (value instanceof Enum<?> en) {
                return en.name();
            }
        } catch (Throwable e) {
            // 忽略：非 AE2 压印配方或无该方法
        }
        return null;
    }

    /** 读取全部 inscribe(压板) 配方：middle=消耗原料、输出=结果 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<InscribeEntry> readInscribe(Level level) {
        List<InscribeEntry> result = new ArrayList<>();
        RecipeType<?> type = findInscriberType(level);
        if (type == null) {
            return result;
        }
        try {
            Collection<?> all = level.getRecipeManager().getAllRecipesFor((RecipeType) type);
            for (Object obj : all) {
                if (!(obj instanceof Recipe<?> recipe)) {
                    continue;
                }
                if (!"INSCRIBE".equals(processName(recipe))) {
                    continue;
                }
                ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
                if (output.isEmpty()) {
                    continue;
                }
                List<Ingredient> ings = recipe.getIngredients();
                if (ings == null || ings.size() < 3) {
                    continue;
                }
                // AE2 getIngredients() 返回 [top, middle, bottom]
                Ingredient middle = ings.get(1);
                if (middle == null || middle.isEmpty()) {
                    continue;
                }
                result.add(new InscribeEntry(middle, output));
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.readInscribe error", e);
        }
        return result;
    }

    /** 读取全部 press(组装) 配方：消耗 top/middle/bottom 全部非空材料 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<AssemblyEntry> readAssembly(Level level) {
        List<AssemblyEntry> result = new ArrayList<>();
        RecipeType<?> type = findInscriberType(level);
        if (type == null) {
            return result;
        }
        try {
            Collection<?> all = level.getRecipeManager().getAllRecipesFor((RecipeType) type);
            for (Object obj : all) {
                if (!(obj instanceof Recipe<?> recipe)) {
                    continue;
                }
                if (!"PRESS".equals(processName(recipe))) {
                    continue;
                }
                ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
                if (output.isEmpty()) {
                    continue;
                }
                List<Ingredient> ings = recipe.getIngredients();
                if (ings == null || ings.size() < 3) {
                    continue;
                }
                Ingredient[] mats = {ings.get(0), ings.get(1), ings.get(2)};
                result.add(new AssemblyEntry(mats, output));
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.readAssembly error", e);
        }
        return result;
    }

    // ==================== 输出推送 ====================

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            pushOutput();
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.serverTick error", e);
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
        for (Row row : new ArrayList<>(outputRows)) {
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

    // ==================== 方向/能量 ====================

    public int getDirectionState(Direction direction) {
        return directionState[direction.ordinal()];
    }

    public void cycleDirection(Direction direction) {
        int idx = direction.ordinal();
        directionState[idx] = (directionState[idx] + 1) % STATE_COUNT;
        setChanged();
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    private static class SmeltEnergy extends EnergyStorage {
        private final InstantInscriberEntity owner;

        SmeltEnergy(InstantInscriberEntity owner) {
            super(MAX_ENERGY, MAX_ENERGY, 0);
            this.owner = owner;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                owner.requestCompute();
            }
            return received;
        }

        public void setEnergyStored(int amount) {
            this.energy = Math.max(0, Math.min(MAX_ENERGY, amount));
        }

        public int spendEnergy(int amount) {
            int deducted = Math.min(this.energy, Math.max(0, amount));
            this.energy -= deducted;
            return deducted;
        }
    }

    // ==================== 菜单/能力 ====================

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.instant_inscriber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new InstantInscriberMenu(id, inv, worldPosition);
    }

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
            log.error("InstantInscriberEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    // ==================== NBT ====================

    private static final String KEY_INPUT = "input";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_DIR = "directionState";
    private static final String KEY_MODE = "mode";

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.put(KEY_INPUT, saveRows(inputRows));
            nbt.put(KEY_OUTPUT, saveRows(outputRows));
            nbt.putInt(KEY_ENERGY, energy.getEnergyStored());
            nbt.putIntArray(KEY_DIR, directionState);
            nbt.putInt(KEY_MODE, mode);
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            boolean oldWorking = working;
            working = true;
            try {
                if (nbt.contains(KEY_INPUT, Tag.TAG_LIST)) {
                    loadRows(inputRows, (ListTag) nbt.get(KEY_INPUT));
                }
                if (nbt.contains(KEY_OUTPUT, Tag.TAG_LIST)) {
                    loadRows(outputRows, (ListTag) nbt.get(KEY_OUTPUT));
                }
            } finally {
                working = oldWorking;
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
            if (nbt.contains(KEY_MODE, Tag.TAG_INT)) {
                mode = Math.max(0, Math.min(MODE_COUNT - 1, nbt.getInt(KEY_MODE)));
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.load error", e);
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
                log.warn("InstantInscriberEntity.loadRows entry error", e);
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
