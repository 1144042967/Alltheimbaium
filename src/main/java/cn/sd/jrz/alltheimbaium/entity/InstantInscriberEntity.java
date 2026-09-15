package cn.sd.jrz.alltheimbaium.entity;

import static cn.sd.jrz.alltheimbaium.setup.Registration.INSTANT_INSCRIBER_ENTITY;
import static cn.sd.jrz.alltheimbaium.setup.Registration.INSTANT_INSCRIBER_ITEM;
import cn.sd.jrz.alltheimbaium.connection.InstantInscriberConnection;
import cn.sd.jrz.alltheimbaium.gui.InstantInscriberMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
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

import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ATI 零刻压印器（大数版，配方参考 AE2 压印机 Inscriber）。
 * <p>
 * 输入区最多 {@link #INPUT_MAX_TYPES} 行、输出区最多 {@link #OUTPUT_MAX_TYPES} 行（大数存储）。
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
public class InstantInscriberEntity extends BlockEntity implements MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberEntity.class);

    /**
     * FE 能量上限：20 亿。仍在 int 范围内，但**超过数据槽的 16 位**，同步时必须拆成高低两块
     * （见 {@code InstantInscriberMenu} 的 chunk 工具）。
     */
    public static final int MAX_ENERGY = 2_000_000_000;
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

    /**
     * 对外 IItemHandler 能力（行为方向无关：插入进输入区、抽取自输出区）。
     * NeoForge 不再实现 ICapabilityProvider，由 {@code registerCapabilities} 拉取。
     */
    private final InstantInscriberConnection itemHandler = new InstantInscriberConnection(this);

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

    public InstantInscriberEntity(BlockPos pos, BlockState state) {
        super(INSTANT_INSCRIBER_ENTITY.get(), pos, state);
        // 六面输出默认全关：刚放下时不该把产物主动推给相邻方块，要玩家在界面里逐面打开
        Arrays.fill(directionState, STATE_DISABLED);
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
        RecipeManager rm = recipeManager(level);
        if (rm != recipeCacheManager) {
            recipeCacheManager = rm;
            inscribeCache = readInscribe(level);
            assemblyCache = readAssembly(level);
        }
    }

    /**
     * 取当前可用的配方管理器；取不到（客户端 / 未就绪）返回 null。
     * <p>
     * 26.x：{@code Level#getRecipeManager()} 已删除，改走 {@code Level#recipeAccess()}；且 26.x 起
     * <b>客户端不再同步完整配方表</b>（{@code ClientLevel.recipeAccess()} 只有属性集与切石机配方），
     * 因此客户端的帮助卡拿不到任何配方，会走空态文案。
     */
    @Nullable
    private static RecipeManager recipeManager(@Nonnull Level level) {
        if (level.isClientSide()) {
            return null;
        }
        return level.recipeAccess() instanceof RecipeManager manager ? manager : null;
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
        for (String id : new String[]{"ae2:inscriber", "appliedenergistics2:inscriber"}) {
            try {
                Identifier key = Identifier.tryParse(id);
                if (key == null) {
                    continue;
                }
                // 直接查内置注册表：RECIPE_TYPE 不是"带默认值"的注册表，未注册时 getValue 返回 null
                RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(key);
                if (type != null) {
                    return type;
                }
                // 兜底：部分环境下 RECIPE_TYPE 只出现在 level 的 registryAccess 里
                var registry = level.registryAccess().lookupOrThrow(Registries.RECIPE_TYPE);
                if (registry != null) {
                    type = registry.getValue(key);
                    if (type != null) {
                        return type;
                    }
                }
            } catch (Throwable e) {
                log.error("InstantInscriberEntity.findInscriberType error for {}", id, e);
            }
        }
        return null;
    }

    /**
     * 取出配方本体。
     * <p>
     * 1.21 起 {@code RecipeManager#getAllRecipesFor} 返回的是 {@code RecipeHolder} 列表，
     * 元素本身不是 {@code Recipe}——直接 {@code instanceof Recipe} 判类型会把整表跳过，
     * 表现为"装了 AE2 却读不到任何配方"。
     */
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

    /**
     * 把配方的材料还原到"上 / 中 / 下"三个固定槽位。
     * <p>
     * 26.x：{@code Recipe#getIngredients()} 已删除，改读 {@link PlacementInfo}——
     * 它给的是"紧凑后的材料表 + 槽位到材料下标的映射"，按映射还原即可拿回原始槽位顺序。
     */
    @Nonnull
    private static Ingredient[] positionalIngredients(@Nonnull Recipe<?> recipe) {
        Ingredient[] slots = new Ingredient[3];
        try {
            PlacementInfo info = recipe.placementInfo();
            List<Ingredient> ingredients = info.ingredients();
            IntList mapping = info.slotsToIngredientIndex();
            for (int slot = 0; slot < mapping.size() && slot < slots.length; slot++) {
                int index = mapping.getInt(slot);
                if (index >= 0 && index < ingredients.size()) {
                    slots[slot] = ingredients.get(index);
                }
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.positionalIngredients error", e);
        }
        return slots;
    }

    /**
     * 取配方的首个产物。
     * <p>
     * 26.x：{@code Recipe#getResultItem(...)} 已删除，改从 {@code Recipe#display()} 里取
     * {@code RecipeDisplay#result()} 并解析成物品栈。
     */
    @Nonnull
    private static ItemStack firstResult(@Nonnull Recipe<?> recipe, @Nonnull Level level) {
        try {
            for (RecipeDisplay display : recipe.display()) {
                ItemStack stack = display.result().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
                if (!stack.isEmpty()) {
                    return stack.copy();
                }
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.firstResult error", e);
        }
        return ItemStack.EMPTY;
    }

    /** 读取全部 inscribe(压板) 配方：middle=消耗原料、输出=结果 */
    private static List<InscribeEntry> readInscribe(Level level) {
        List<InscribeEntry> result = new ArrayList<>();
        RecipeType<?> type = findInscriberType(level);
        RecipeManager manager = recipeManager(level);
        if (type == null || manager == null) {
            return result;
        }
        try {
            for (RecipeHolder<?> holder : manager.getRecipes()) {
                try {
                    Recipe<?> recipe = holder.value();
                    if (recipe.getType() != type || !"INSCRIBE".equals(processName(recipe))) {
                        continue;
                    }
                    ItemStack output = firstResult(recipe, level);
                    if (output.isEmpty()) {
                        continue;
                    }
                    Ingredient[] slots = positionalIngredients(recipe);
                    // AE2 的材料布局是 [top, middle, bottom]
                    Ingredient middle = slots[1];
                    if (middle == null || middle.isEmpty()) {
                        continue;
                    }
                    result.add(new InscribeEntry(middle, output));
                } catch (Throwable e) {
                    log.warn("InstantInscriberEntity.readInscribe entry error", e);
                }
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.readInscribe error", e);
        }
        return result;
    }

    /** 读取全部 press(组装) 配方：消耗 top/middle/bottom 全部非空材料 */
    private static List<AssemblyEntry> readAssembly(Level level) {
        List<AssemblyEntry> result = new ArrayList<>();
        RecipeType<?> type = findInscriberType(level);
        RecipeManager manager = recipeManager(level);
        if (type == null || manager == null) {
            return result;
        }
        try {
            for (RecipeHolder<?> holder : manager.getRecipes()) {
                try {
                    Recipe<?> recipe = holder.value();
                    if (recipe.getType() != type || !"PRESS".equals(processName(recipe))) {
                        continue;
                    }
                    ItemStack output = firstResult(recipe, level);
                    if (output.isEmpty()) {
                        continue;
                    }
                    Ingredient[] mats = positionalIngredients(recipe);
                    if (mats[0] == null && mats[1] == null && mats[2] == null) {
                        continue;
                    }
                    result.add(new AssemblyEntry(mats, output));
                } catch (Throwable e) {
                    log.warn("InstantInscriberEntity.readAssembly entry error", e);
                }
            }
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.readAssembly error", e);
        }
        return result;
    }

    // ==================== GUI 帮助卡数据 ====================

    /**
     * GUI 帮助卡用的一条配方摘要。
     *
     * @param output 产物
     * @param inputs 消耗的材料，每个 Ingredient 取其第一个候选物品
     */
    public record RecipeSummary(@Nonnull ItemStack output, @Nonnull List<ItemStack> inputs) {
    }

    /**
     * 压板帮助卡用的一条摘要：**同一份原料 + 它支持的全部压板产物**。
     * <p>
     * 与 {@link RecipeSummary} 的区别在于聚合方向——压板是"1 份原料吃出多种压板"，
     * 按产物逐条列会把同一份原料重复很多遍，所以这里按<b>输入</b>聚合。
     */
    public record PressSummary(@Nonnull List<ItemStack> inputs, @Nonnull List<ItemStack> outputs) {
    }

    /**
     * 压板模式（INSCRIBE）支持的配方摘要，供 GUI 帮助卡使用：1 份中间原料 → 它支持的全部压板。
     * <p>
     * AE2 的压印配方是 (上, 中, 下) → 产物，压板模式只认中间那格，因此多套模板可能落在同一份原料上；
     * 这里做两件事：
     * <ol>
     *     <li><b>去重</b>：计算后 (输入, 产物) 完全一致的组合只保留一份；</li>
     *     <li><b>按输入聚合</b>：输入的注册名相同的产物并进同一条，界面上就是一行。</li>
     * </ol>
     * <b>26.x 注意</b>：客户端不再同步完整配方表（{@code ClientLevel.recipeAccess()} 只有属性集与切石机配方），
     * 因此这个方法在客户端会返回空列表，界面走空态文案；未装 AE2 时同样为空。
     */
    @Nonnull
    public static List<PressSummary> inscribeSummaries(@Nonnull Level level) {
        // 输入注册名 → 该输入支持的产物（保持首次出现顺序，最后再统一排序）
        Map<String, List<ItemStack>> outputsByInput = new LinkedHashMap<>();
        Map<String, ItemStack> inputById = new LinkedHashMap<>();
        Set<String> seenPair = new HashSet<>();
        for (InscribeEntry entry : readInscribe(level)) {
            List<ItemStack> inputs = firstOf(entry.material);
            if (inputs.isEmpty()) {
                continue;
            }
            ItemStack input = inputs.get(0);
            String inputKey = itemKey(input);
            String pairKey = inputKey + " -> " + itemKey(entry.output) + " x" + entry.output.getCount();
            if (!seenPair.add(pairKey)) {
                continue;
            }
            inputById.putIfAbsent(inputKey, input);
            outputsByInput.computeIfAbsent(inputKey, key -> new ArrayList<>()).add(entry.output);
        }
        List<PressSummary> result = new ArrayList<>();
        for (Map.Entry<String, List<ItemStack>> e : outputsByInput.entrySet()) {
            result.add(new PressSummary(List.of(inputById.get(e.getKey())), List.copyOf(e.getValue())));
        }
        // 按输入注册名排序，保证分页顺序稳定
        result.sort(Comparator.comparing(summary -> itemKey(summary.inputs().get(0))));
        return result;
    }

    /** 物品的注册名，用于去重与排序 */
    @Nonnull
    private static String itemKey(@Nonnull ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    /**
     * 组装模式（PRESS）支持的配方摘要，供 GUI 帮助卡使用：消耗 top/middle/bottom 的全部非空材料
     */
    @Nonnull
    public static List<RecipeSummary> assemblySummaries(@Nonnull Level level) {
        List<RecipeSummary> result = new ArrayList<>();
        Set<String> seenPair = new HashSet<>();
        for (AssemblyEntry entry : readAssembly(level)) {
            List<ItemStack> inputs = new ArrayList<>();
            for (Ingredient ing : entry.mats) {
                inputs.addAll(firstOf(ing));
            }
            // 计算后 (产物, 材料) 完全一致的组合只保留一份
            StringBuilder key = new StringBuilder(itemKey(entry.output)).append('x').append(entry.output.getCount());
            for (ItemStack input : inputs) {
                key.append(" + ").append(itemKey(input));
            }
            if (!seenPair.add(key.toString())) {
                continue;
            }
            result.add(new RecipeSummary(entry.output, inputs));
        }
        sortByOutput(result);
        return result;
    }

    /**
     * 取 Ingredient 的第一个候选物品（AE2 压印配方的材料实际都是单一物品）。
     * 26.x：{@code Ingredient#getItems()} 已删除，改从 {@code items()} 流里取首个候选。
     */
    @Nonnull
    private static List<ItemStack> firstOf(@Nullable Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return List.of();
        }
        Holder<Item> item = ingredient.items().findFirst().orElse(null);
        return item == null ? List.of() : List.of(new ItemStack(item.value()));
    }

    /**
     * 按产物注册名排序。{@code getAllRecipesFor} 返回的顺序取决于 Map 迭代，虽然同一会话内稳定，
     * 但排序后分页顺序才是可预期的。
     */
    private static void sortByOutput(@Nonnull List<RecipeSummary> summaries) {
        summaries.sort(Comparator.comparing(s -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(s.output().getItem());
            return id == null ? "" : id.toString();
        }));
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
        if (level == null || level.isClientSide()) {
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

    /**
     * 可被机器直控的 FE 能量存储（上限 2 亿、只接收不放出）。
     * <p>
     * 26.x：能力类型是 {@link EnergyHandler}，基类因此从 {@code EnergyStorage} 换成
     * {@code SimpleEnergyHandler}（外部注入走它自带的事务日志），{@code onEnergyChanged} 里补脏标记。
     */
    private static class SmeltEnergy extends SimpleEnergyHandler {
        private final InstantInscriberEntity owner;

        SmeltEnergy(InstantInscriberEntity owner) {
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

        /** 直接扣减存量（合成用），不走 extract 的 maxExtract 限制与事务 */
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
        return Component.translatable("block.alltheimbaium.instant_inscriber").withStyle(Tip.rarityColor(INSTANT_INSCRIBER_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new InstantInscriberMenu(id, inv, worldPosition);
    }


    // ==================== NBT ====================

    private static final String KEY_INPUT = "input";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_DIR = "directionState";
    private static final String KEY_MODE = "mode";

    @Override
    protected void saveAdditional(@Nonnull ValueOutput output) {
        super.saveAdditional(output);
        try {
            // 26.x：产物行改由 ValueOutput 列表托管，物品组件自动按当前注册表访问器编解码
            Tool.writeRows(output, KEY_INPUT, toStockRows(inputRows), Tool.StockRow.CODEC);
            Tool.writeRows(output, KEY_OUTPUT, toStockRows(outputRows), Tool.StockRow.CODEC);
            output.putInt(KEY_ENERGY, energy.getEnergyStored());
            output.putIntArray(KEY_DIR, directionState);
            output.putInt(KEY_MODE, mode);
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.saveAdditional error", e);
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
            mode = Math.max(0, Math.min(MODE_COUNT - 1, input.getIntOr(KEY_MODE, mode)));
        } catch (Throwable e) {
            log.error("InstantInscriberEntity.loadAdditional error", e);
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
                log.warn("InstantInscriberEntity.loadRows entry error", e);
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
