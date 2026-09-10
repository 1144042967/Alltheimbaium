package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.InstantInscriberConnection;
import cn.sd.jrz.alltheimbaium.gui.InstantInscriberMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ATI 零刻压印器（AE2 大数版，配方参考 AE2 压印机 Inscriber）。
 * <p>
 * 输入区最多 {@link #INPUT_MAX_TYPES} 行、输出区最多 {@link #OUTPUT_MAX_TYPES} 行（AE 大数存储）。
 * 两档模式（GUI 可切换、NBT 保存）：
 * <ul>
 *     <li>{@link #MODE_INSCRIBE 压板}：读取 AE2 <code>mode:inscribe</code> 配方。模板(top/bottom)不消耗，
 *         每消耗 1 份原料(middle 命中物)，同时生成它支持的所有配方各一份产物；</li>
 *     <li>{@link #MODE_ASSEMBLY 组装}：读取 AE2 <code>mode:press</code> 配方，消耗其全部输入材料，
 *         优先级 = 3 种材料配方先于 2 种材料配方。</li>
 * </ul>
 * 生成无耗时：每 tick 服务端按当前模式对输入做一轮批量合成（按优先级直到无配方可做），每生成一件扣
 * {@link #ENERGY_PER_OP} FE（上限 {@link #MAX_ENERGY}），合成完成后执行输出推送；
 * 被动输入/输出只收发货，不触发配方计算。
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
    public int findIndex = 0;

    /** AE2 inscribe/press 配方缓存，随 RecipeManager 实例变化重建，避免每 tick 全表扫配方（AE2 未装为空列表） */
    private RecipeManager recipeCacheManager;
    private List<InscribeEntry> inscribeCache = new ArrayList<>();
    private List<AssemblyEntry> assemblyCache = new ArrayList<>();

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
     * 切换模式（压板 ↔ 组装）；合成由每 tick 统一执行
     */
    public void cycleMode() {
        mode = (mode + 1) % MODE_COUNT;
        setChanged();
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
            // 被动输入只收发货；配方合成交由每 tick 统一执行
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

    // ==================== 生成（每 tick 批量） ====================

    /**
     * 服务端每 tick 合成入口：先刷新 AE2 配方缓存，再按当前模式对输入做一轮批量合成。
     * 空输入 / 电量不足一个最低操作时提前短路。
     *
     * @return 是否有改动
     */
    public boolean computeOnce(Level level) {
        if (inputRows.isEmpty() || energy.getEnergyStored() < ENERGY_PER_OP) {
            return false;
        }
        ensureRecipeCache(level);
        return mode == MODE_ASSEMBLY ? runAssembly() : runInscribe();
    }

    /** 缓存失效重建：RecipeManager 引用变化即重建（其余 tick / 切模式不再重扫配方表） */
    private void ensureRecipeCache(Level level) {
        RecipeManager rm = level.getRecipeManager();
        if (rm != recipeCacheManager) {
            recipeCacheManager = rm;
            inscribeCache = readInscribe(level);
            assemblyCache = readAssembly(level);
        }
    }

    /**
     * 压板模式批量合成：对每个输入行，1 份原料 = 命中全部 inscribe 配方各产 outCount。
     * n = min(行存量, 电量 / 每批成本)，整批一次扣料/扣能/加产物，不逐件循环。
     * 能量/输出行种类不足时整批跳过（不部分消耗材料）。
     */
    private boolean runInscribe() {
        if (inscribeCache.isEmpty()) {
            return false;
        }
        boolean moved = false;
        for (Row row : new ArrayList<>(inputRows)) {
            if (row.stock <= 0) {
                continue;
            }
            // 该原料命中的所有压板配方
            List<InscribeEntry> hits = new ArrayList<>();
            ItemStack probe = new ItemStack(row.item, 1);
            for (InscribeEntry entry : inscribeCache) {
                if (entry.material.test(probe)) {
                    hits.add(entry);
                }
            }
            if (hits.isEmpty()) {
                continue;
            }
            // 一批成本与输出占位（种类）
            long perRound = 0;
            for (InscribeEntry entry : hits) {
                perRound += (long) entry.outCount * ENERGY_PER_OP;
            }
            if (!canFitOutputTypes(hits)) {
                continue; // 输出区类型容纳不下整批产物（扣料前预判）
            }
            long n = Math.min(row.stock, (long) energy.getEnergyStored() / perRound);
            if (n <= 0) {
                continue;
            }
            row.stock -= n;
            energy.spendEnergy((int) (n * perRound));
            for (InscribeEntry entry : hits) {
                addOutputProduct(entry.output.getItem(), n * entry.outCount);
            }
            moved = true;
        }
        cleanEmptyRows(inputRows);
        return moved;
    }

    /**
     * 组装模式批量合成：消耗配方全部输入材料生成结果。
     * 优先级 = 3 材料配方先于 2 材料配方；每个配方按整批算完，外层 do-while 直到某轮无配方可执行
     * （guard 仅防未来回归）。
     */
    private boolean runAssembly() {
        if (assemblyCache.isEmpty()) {
            return false;
        }
        List<AssemblyEntry> three = new ArrayList<>();
        List<AssemblyEntry> two = new ArrayList<>();
        for (AssemblyEntry entry : assemblyCache) {
            if (entry.nonEmpty == 3) {
                three.add(entry);
            } else if (entry.nonEmpty == 2) {
                two.add(entry);
            }
        }
        boolean moved = false;
        int guard = 0;
        boolean progressed;
        do {
            progressed = false;
            progressed |= runExecutables(three); // 3 材料优先，直到本轮该组无配方可执行
            progressed |= runExecutables(two);
            if (progressed) {
                moved = true;
            }
        } while (progressed && ++guard < 1024);
        cleanEmptyRows(inputRows);
        return moved;
    }

    /** 对列表内每个配方各整批执行直至无法执行，返回是否有任一执行成功 */
    private boolean runExecutables(List<AssemblyEntry> entries) {
        boolean any = false;
        for (AssemblyEntry entry : entries) {
            while (executeAssemblyBatch(entry) > 0) {
                any = true;
            }
        }
        return any;
    }

    /**
     * 尝试按配方整批合成一次：解析各材料槽命中行（含 multiplicity：同一行被多个槽命中时按次数扣），
     * n = min(电量 / 每件成本, 各材料行可用量 / 被用次数)；能量/输出种类容量不足则跳过；
     * 成功则整批原子扣料/扣能/加产物。
     *
     * @return 实际合成次数
     */
    private long executeAssemblyBatch(AssemblyEntry entry) {
        List<Row> rows = new ArrayList<>();
        List<Long> need = new ArrayList<>();
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
                return 0; // 某材料槽无命中：本次无法执行
            }
            int idx = rows.indexOf(row);
            if (idx < 0) {
                rows.add(row);
                need.add(1L);
            } else {
                need.set(idx, need.get(idx) + 1);
            }
        }
        if (rows.isEmpty()) {
            return 0;
        }
        long perOp = (long) entry.outCount * ENERGY_PER_OP;
        long n = (long) energy.getEnergyStored() / perOp;
        for (int i = 0; i < rows.size(); i++) {
            n = Math.min(n, rows.get(i).stock / need.get(i));
        }
        if (n <= 0) {
            return 0;
        }
        // 输出种类容量（扣料前预判）
        if (!canFitOutputTypes(entry.output.getItem())) {
            return 0;
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).stock -= n * need.get(i);
        }
        energy.spendEnergy((int) (n * perOp));
        addOutputProduct(entry.output.getItem(), n * entry.outCount);
        return n;
    }

    /** 输出区能否容纳这批结果（每个结果必须命中已有输出行，或还有空闲行；同类去重） */
    private boolean canFitOutputTypes(List<InscribeEntry> entries) {
        Set<Item> needNew = new HashSet<>();
        for (InscribeEntry entry : entries) {
            if (findRow(outputRows, entry.output.getItem()) == null) {
                needNew.add(entry.output.getItem());
            }
        }
        return outputRows.size() + needNew.size() <= OUTPUT_MAX_TYPES;
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

    /** 单次大批量塞入上限（件），避免对无限容量邻居逐 64 组循环导致卡顿 */
    private static final long PUSH_BATCH = 1_000_000;

    /**
     * 服务端 tick：每 tick 先按当前模式做一轮批量合成（合成完成后），再执行输出推送。
     * 只在确有改动（合成动行/扣能、推送推货）时才 setChanged，避免闲置机器持续脏标记。
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            boolean changed = computeOnce(level); // 每 tick 先合成
            changed |= pushOutput();              // 合成后输出
            if (changed) {
                setChanged();
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.serverTick error", e);
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
            IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
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
                owner.setChanged(); // 能量须脏标记持久化；合成由每 tick 统一执行
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
        return Component.translatable("block.alltheimbaium.instant_inscriber").withStyle(Tip.rarityColor(Registration.INSTANT_INSCRIBER_ITEM.get()));
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
            // 反序列化只恢复状态；合成由每 tick 统一执行
            if (nbt.contains(KEY_INPUT, Tag.TAG_LIST)) {
                loadRows(inputRows, (ListTag) nbt.get(KEY_INPUT));
            }
            if (nbt.contains(KEY_OUTPUT, Tag.TAG_LIST)) {
                loadRows(outputRows, (ListTag) nbt.get(KEY_OUTPUT));
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
