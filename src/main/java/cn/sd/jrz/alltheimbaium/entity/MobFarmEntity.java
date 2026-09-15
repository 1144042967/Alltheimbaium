package cn.sd.jrz.alltheimbaium.entity;

import static cn.sd.jrz.alltheimbaium.setup.Registration.MOB_FARM_ENTITY;
import static cn.sd.jrz.alltheimbaium.setup.Registration.MOB_FARM_ITEM;
import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.connection.MobFarmConnection;
import cn.sd.jrz.alltheimbaium.gui.MobFarmMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.KillLootEstimator;
import cn.sd.jrz.alltheimbaium.setup.MobFarmCatalog;
import cn.sd.jrz.alltheimbaium.setup.MobFarmInteraction;
import cn.sd.jrz.alltheimbaium.setup.MobFarmMarkerIndex;
import cn.sd.jrz.alltheimbaium.setup.MobFarmWhitelist;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 生物农场方块实体。
 * <p>
 * 仿照 StorageFountainEntity + FarmEntity 的混合：
 * 收容一只生物（存完整实体 NBT + 懒加载客户端缓存实体用于渲染），
 * 收容时建立"产物表"（每行 物品+权重），每 tick 按 权重×等级 平滑累计、按 CARRY 进位成整数物品；
 * 产物行存量长期保存、可六面输出、GUI 内取物。
 * 标记槽放入刷怪蛋/特征掉落物收容生物；使用槽放入物品自动模拟"右击收容物"产出。
 */
public class MobFarmEntity extends BlockEntity implements MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(MobFarmEntity.class);

    // ==================== 方向输出状态 ====================
    /** 随机：输出任意有存量的行 */
    public static final int STATE_RANDOM = 0;
    /** 禁用 */
    public static final int STATE_DISABLED = 1;
    /** 槽N 起始：state = STATE_SLOT_BASE + 行索引(0~26) */
    public static final int STATE_SLOT_BASE = 2;

    /** 六面输出状态，索引与 Direction.values() 顺序一致 */
    public final int[] directionState = new int[6];
    /** 主动输出总开关 */
    public boolean outputEnabled = true;
    /** 逐面轮询游标 */
    public int findIndex = 0;

    // ==================== 等级 / 状态 ====================
    public long level;
    public long tickCount = 0;

    /** 产物行：item + 存量(整件) + 权重 + 分数进位。行序即 GUI 27 槽顺序 */
    public static final class Row {
        @Nonnull
        public final Item item;
        public long stock;
        public long weight;
        public long acc;
        /** 由使用槽产出的行（权重记刷怪蛋速度，仅用于展示/排序，不参与被动累计，避免双倍产出） */
        public boolean fromTool = false;

        public Row(@Nonnull Item item, long weight) {
            this.item = item;
            this.weight = weight;
        }
    }

    /** 产物行（≤ maxProducts） */
    public final List<Row> rows = new ArrayList<>();

    /** 收容生物的完整实体 NBT（含 id），null = 未收容 */
    @Nullable
    public CompoundTag entityTag;
    /** 客户端懒加载渲染实体缓存（不属于世界） */
    @Nullable
    public Entity cachedEntity;
    /** 载入含收容生物但无产物表时，首个服务端 tick 需要补建产物表 */
    private boolean needRebuild = false;

    /** 六面 + 无方向能力缓存（同 StorageFountainEntity） */
    /**
     * 对外物品能力：按面懒建并缓存。NeoForge 不再实现 ICapabilityProvider，
     * 由 {@code registerCapabilities} 在 RegisterCapabilitiesEvent 里拉取（下标 6 = 无方向查询）。
     */
    private final MobFarmConnection[] itemHandlers = new MobFarmConnection[7];

    /** 26.x：{@link Capabilities.Item#BLOCK} 要求的是 {@code ResourceHandler<ItemResource>}，实现类见 MobFarmConnection */
    @Nullable
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        int idx = side == null ? 6 : side.ordinal();
        if (itemHandlers[idx] == null) {
            itemHandlers[idx] = new MobFarmConnection(this, side);
        }
        return itemHandlers[idx];
    }

    public MobFarmEntity(BlockPos pos, BlockState state) {
        super(MOB_FARM_ENTITY.get(), pos, state);
        this.level = Math.max(1, MobFarmBlock.getInitialLevel());
    }

    // ==================== 收容生物查询 ====================

    /** 实体 NBT 是否带合法的 {@code id} 字段（26.x：CompoundTag 的取值一律返回 Optional） */
    private static boolean hasEntityId(@Nullable CompoundTag tag) {
        return tag != null && tag.getString("id").isPresent();
    }

    public boolean hasContained() {
        return hasEntityId(entityTag);
    }

    @Nullable
    public EntityType<?> getContainedType() {
        if (!hasEntityId(entityTag)) {
            return null;
        }
        Optional<EntityType<?>> type = EntityType.byString(entityTag.getStringOr("id", ""));
        return type.orElse(null);
    }

    /** 收容生物实体类型注册 id（客户端同步用，未收容返回 0） */
    public int getContainedEntityId() {
        EntityType<?> type = getContainedType();
        //noinspection deprecation
        return type == null ? 0 : BuiltInRegistries.ENTITY_TYPE.getId(type);
    }

    /**
     * 客户端渲染用：从收容 NBT 懒加载一个不属于世界的实体实例
     */
    @Nullable
    public Entity getClientRenderedEntity() {
        try {
            Level level = getLevel();
            if (level == null || !hasContained()) {
                return null;
            }
            if (cachedEntity == null && entityTag != null) {
                // 26.x：loadEntityRecursive 的旧 3 参重载已删除，必须显式给 EntitySpawnReason，
                // 这里只是客户端渲染用的模板实体，用 LOAD 最贴合语义
                cachedEntity = EntityType.loadEntityRecursive(entityTag.copy(), level, EntitySpawnReason.LOAD, entity -> entity);
                if (cachedEntity == null) {
                    return null;
                }
                cachedEntity.setBoundingBox(new AABB(0, 0, 0, 0, 0, 0));
                cachedEntity.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5);
            }
            return cachedEntity;
        } catch (Throwable e) {
            log.error("MobFarmEntity.getClientRenderedEntity error", e);
        }
        return null;
    }

    // ==================== 收容 ====================

    /**
     * 把一只真实生物收容进机器（服务端）。返回是否成功。
     */
    public boolean captureEntity(@Nonnull LivingEntity entity) {
        try {
            Level level = getLevel();
            if (level == null || level.isClientSide()) {
                return false;
            }
            // 26.x：Entity#save(CompoundTag) 已删除，改成写进 ValueOutput（返回是否写成功），
            // 由 TagValueOutput 负责补 id 等字段
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            entity.save(output);
            CompoundTag tag = output.buildResult();
            if (!hasEntityId(tag)) {
                tag = new CompoundTag();
                tag.putString("id", EntityType.getKey(entity.getType()).toString());
            }
            entityTag = tag;
            cachedEntity = null;
            rebuildProducts();
            setChanged();
            sendUpdatePacket();
            return true;
        } catch (Throwable e) {
            log.error("MobFarmEntity.captureEntity error", e);
        }
        return false;
    }

    /**
     * 用刷怪蛋/特征物收容一个没有真实个体的生物（服务端），生成模板 NBT。
     */
    public void captureByType(@Nonnull EntityType<?> type) {
        try {
            Level level = getLevel();
            CompoundTag tag = new CompoundTag();
            if (level != null && !level.isClientSide()) {
                try {
                    // 26.x：EntityType#create(Level) 已删除，探测实例用 COMMAND 生成原因
                    Entity probe = level instanceof net.minecraft.server.level.ServerLevel serverLevel
                            ? type.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                            : null;
                    if (probe != null) {
                        // 26.x：同上，Entity#save 改收 ValueOutput
                        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                        probe.save(output);
                        tag = output.buildResult();
                        if (hasEntityId(tag)) {
                            entityTag = tag;
                        }
                    }
                } catch (Throwable e) {
                    log.warn("MobFarmEntity.captureByType create fail for {}", type, e);
                }
            }
            if (!hasEntityId(entityTag)) {
                entityTag = new CompoundTag();
                entityTag.putString("id", EntityType.getKey(type).toString());
            }
            cachedEntity = null;
            rebuildProducts();
            setChanged();
            sendUpdatePacket();
        } catch (Throwable e) {
            log.error("MobFarmEntity.captureByType error", e);
        }
    }

    /** 标记槽能否把物品放入并确定收容目标类型（null=不允许） */
    @Nullable
    public EntityType<?> resolveMarkerTarget(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        try {
            if (stack.getItem() instanceof SpawnEggItem egg) {
                // 1.21：getType(ItemStack) 不接受 null（会 NPE），必须传手上这一份
                return egg.getType(stack);
            }
            EntityType<?> marker = MobFarmWhitelist.markerTypeOf(stack.getItem());
            if (marker != null) {
                return marker;
            }
            // 动态掉落物表只在服务端构建：任意"掉落物 → 收容生物"（白名单之外）
            if (getLevel() instanceof ServerLevel serverLevel) {
                MobFarmMarkerIndex.ensureBuilt(serverLevel);
                return MobFarmMarkerIndex.lookup(stack.getItem());
            }
        } catch (Throwable e) {
            log.error("MobFarmEntity.resolveMarkerTarget error", e);
        }
        return null;
    }

    // ==================== 产物表 ====================

    /**
     * 单一产物来源：物品 + 权重（权重尺度与现有农场一致，500 ≈ 1 件/s @ Lv1）
     */
    private record Weighted(Item item, long weight) {
    }

    /**
     * 按当前收容生物重建产物表并合并进 rows（存量按物品保留）。服务端调用。
     */
    public void rebuildProducts() {
        try {
            Level level = getLevel();
            if (level == null || level.isClientSide()) {
                return;
            }
            EntityType<?> type = getContainedType();
            if (type == null) {
                return;
            }
            List<Weighted> table = buildDropTable((ServerLevel) level, type);
            // 建立旧行的索引
            Map<Item, Row> oldByItem = new HashMap<>();
            for (Row row : rows) {
                oldByItem.put(row.item, row);
            }
            List<Row> newRows = new ArrayList<>();
            int max = MobFarmBlock.getMaxProducts();
            for (Weighted w : table) {
                Row old = oldByItem.remove(w.item);
                if (old != null) {
                    old.weight = w.weight;
                    newRows.add(old);
                } else if (newRows.size() < max) {
                    newRows.add(new Row(w.item, w.weight));
                }
            }
            // 旧的存量行（不在新表内）尽量保留（权重清 0，不再生产，但仍可取出）
            for (Row old : oldByItem.values()) {
                if (newRows.size() < max) {
                    old.weight = 0;
                    old.acc = 0;
                    newRows.add(old);
                }
            }
            // 超出 27 时优先丢弃无存量的低权重行
            while (newRows.size() > max) {
                newRows.sort(Comparator.comparingLong((Row r) -> r.weight).thenComparingLong(r -> r.stock));
                Row drop = newRows.get(0);
                if (drop.stock > 0) {
                    break; // 存量行不静默丢弃
                }
                newRows.remove(0);
            }
            rows.clear();
            rows.addAll(newRows);
            for (int i = 0; i < directionState.length; i++) {
                if (directionState[i] >= STATE_SLOT_BASE + rows.size()) {
                    directionState[i] = STATE_RANDOM;
                }
            }
            setChanged();
        } catch (Throwable e) {
            log.error("MobFarmEntity.rebuildProducts error", e);
        }
    }

    /**
     * 构建产物表：白名单产物（专用产物白名单，为主） + 击杀掉落补充（去重） + 刷怪蛋兜底。
     */
    private List<Weighted> buildDropTable(ServerLevel serverLevel, EntityType<?> type) {
        Map<Item, Long> map = new HashMap<>();
        for (MobFarmWhitelist.Product p : MobFarmWhitelist.productsFor(type)) {
            map.put(p.item(), Math.max(map.getOrDefault(p.item(), 0L), Math.max(1L, p.weight())));
        }
        // 击杀掉落：仅补充白名单没有的物品种（避免同一物品双重计权）
        try {
            for (KillLootEstimator.SampledDrop drop : KillLootEstimator.estimate(serverLevel, type)) {
                if (!map.containsKey(drop.item())) {
                    long w = Math.max(1L, Math.min(10000L, Math.round(drop.avgPerKill() * 500.0)));
                    map.put(drop.item(), w);
                }
            }
        } catch (Throwable e) {
            log.warn("MobFarmEntity sample drop error for {}", type, e);
        }
        // 刷怪蛋兜底（白名单 Config 通常已包含，无需重复）
        Item egg = MobFarmCatalog.spawnEggOf(type);
        if (egg != null && !map.containsKey(egg)) {
            map.put(egg, 1L);
        }
        List<Weighted> list = new ArrayList<>();
        map.forEach((item, weight) -> list.add(new Weighted(item, weight)));
        list.sort((a, b) -> {
            if (a.weight() != b.weight()) {
                return Long.compare(b.weight(), a.weight());
            }
            return BuiltInRegistries.ITEM.getKey(a.item()).compareTo(BuiltInRegistries.ITEM.getKey(b.item()));
        });
        return list;
    }

    /** 加入一笔产物（使用槽等来源），并入行存量 */
    public void addProduct(@Nonnull ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            int max = MobFarmBlock.getMaxProducts();
            Row row = findRow(stack.getItem());
            if (row == null) {
                if (rows.size() < max) {
                    row = new Row(stack.getItem(), 0);
                    rows.add(row);
                } else {
                    // 满 27 时：丢弃一个无存量、无权重（手动来源）的行腾位
                    Row victim = null;
                    for (Row r : rows) {
                        if (r.stock == 0 && r.weight == 0) {
                            victim = r;
                            break;
                        }
                    }
                    if (victim != null) {
                        rows.remove(victim);
                        row = new Row(stack.getItem(), 0);
                        rows.add(row);
                    } else {
                        log.warn("MobFarmEntity.addProduct: 输出槽已满，丢弃 {}", stack);
                        return;
                    }
                }
            }
            row.stock = Tool.suit(row.stock + stack.getCount());
            for (int i = 0; i < directionState.length; i++) {
                if (directionState[i] >= STATE_SLOT_BASE + rows.size()) {
                    directionState[i] = STATE_RANDOM;
                }
            }
            setChanged();
        } catch (Throwable e) {
            log.error("MobFarmEntity.addProduct error", e);
        }
    }

    @Nullable
    private Row findRow(Item item) {
        for (Row row : rows) {
            if (row.item == item) {
                return row;
            }
        }
        return null;
    }

    /** 清理既无存量又无权重的空行 */
    private void cleanEmptyRows() {
        rows.removeIf(row -> row.stock <= 0 && row.weight <= 0);
    }

    // ==================== 产物行读取（菜单/连接/数据同步） ====================

    public int getProductCount() {
        return rows.size();
    }

    @Nonnull
    public ItemStack getProductStack(int index) {
        if (index < 0 || index >= rows.size()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(rows.get(index).item, 1);
    }

    public int getProductItemId(int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.getId(rows.get(index).item);
    }

    public long getProductStock(int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        return rows.get(index).stock;
    }

    public long getProductWeight(int index) {
        if (index < 0 || index >= rows.size()) {
            return 0;
        }
        return rows.get(index).weight;
    }

    /** 指定行的展示模式：0=被动产物行，1=使用槽正在产出的工具行，2=使用槽工具行但当前不产出 */
    public int getRowMode(int index) {
        try {
            if (index < 0 || index >= rows.size()) {
                return 0;
            }
            Row row = rows.get(index);
            if (!row.fromTool) {
                return 0;
            }
            ItemStack tool = specialSlot.getStackInSlot(0);
            EntityType<?> type = getContainedType();
            if (type == null || tool.isEmpty() || entityTag == null) {
                return 2;
            }
            Item prod = MobFarmInteraction.produceItem(type, entityTag, tool);
            return prod == row.item ? 1 : 2;
        } catch (Throwable e) {
            log.error("MobFarmEntity.getRowMode error", e);
        }
        return 0;
    }

    /** 使用槽全局状态：0=未收容/无工具需求，1=缺少使用工具，2=使用工具不符，3=正常 */
    public int getToolStatus() {
        try {
            if (!hasContained()) {
                return 0;
            }
            ItemStack tool = specialSlot.getStackInSlot(0);
            if (tool.isEmpty()) {
                return 1;
            }
            EntityType<?> type = getContainedType();
            if (type == null || entityTag == null) {
                return 0;
            }
            return MobFarmInteraction.produceItem(type, entityTag, tool) != null ? 3 : 2;
        } catch (Throwable e) {
            log.error("MobFarmEntity.getToolStatus error", e);
        }
        return 0;
    }

    /** 指定行索引的物品（供连接抽取） */
    @Nullable
    public Item getProductItem(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index).item;
    }

    /**
     * 从指定行提取最多 maxItems 件（扣减存量），返回实际提取数
     */
    public long extractItems(int index, long maxItems) {
        try {
            if (index < 0 || index >= rows.size() || maxItems <= 0) {
                return 0;
            }
            Row row = rows.get(index);
            long available = row.stock;
            if (available <= 0) {
                return 0;
            }
            long toExtract = Math.min(available, maxItems);
            row.stock = available - toExtract;
            cleanEmptyRows();
            setChanged();
            return toExtract;
        } catch (Throwable e) {
            log.error("MobFarmEntity.extractItems error", e);
        }
        return 0;
    }

    // ==================== 方向状态 ====================

    public static int getStateCount() {
        return STATE_SLOT_BASE + MobFarmBlock.getMaxProducts();
    }

    public int getDirectionState(Direction direction) {
        return directionState[direction.ordinal()];
    }

    /** 指定面状态是否为槽 N（返回行索引，否则 -1） */
    public int getDirectionSlot(Direction direction) {
        int state = getDirectionState(direction);
        return state >= STATE_SLOT_BASE ? state - STATE_SLOT_BASE : -1;
    }

    public void cycleDirectionState(Direction direction) {
        cycleDirectionState(direction, true);
    }

    /**
     * 循环切换某面的输出状态
     *
     * @param forward true 正向（左键），false 反向（右键）
     */
    public void cycleDirectionState(Direction direction, boolean forward) {
        int idx = direction.ordinal();
        int count = getStateCount();
        directionState[idx] = forward
                ? (directionState[idx] + 1) % count
                : (directionState[idx] + count - 1) % count;
        setChanged();
    }

    // ==================== 收容 / 使用 合一槽 ====================

    /**
     * 收容与使用合一槽：未收容时是"标记槽"（放入刷怪蛋/特征掉落物即收容该生物，处理完清空）；
     * 已收容后是"使用槽"（放入对收容物右击能产出掉落物的物品，由机器自动模拟使用）。
     * 每台机器只能收容一次：不允许取消收容、不允许再次收容。
     */
    public final ItemStackHandler specialSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (hasContained()) {
                return true; // 使用槽：任意物品
            }
            Level lvl = getLevel();
            if (lvl != null && lvl.isClientSide()) {
                return true; // 客户端算不出动态掉落物表：一律放行，由服务端权威判定收容/清退
            }
            return resolveMarkerTarget(stack) != null; // 服务端标记槽：刷怪蛋/静态特征物/动态掉落物
        }

        @Override
        protected void onContentsChanged(int slot) {
            Level level = getLevel();
            if (level == null) {
                return;
            }
            if (level.isClientSide()) {
                if (!hasContained() && !specialSlot.getStackInSlot(0).isEmpty()) {
                    specialSlot.setStackInSlot(0, ItemStack.EMPTY);
                }
                return;
            }
            if (!hasContained()) {
                processSpecialSlotMarker();
            }
        }
    };

    /**
     * 未收容时处理合一槽中的标记物品：收容对应生物，只收容一次，不允许取消/替换。
     */
    private void processSpecialSlotMarker() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        try {
            ItemStack stack = specialSlot.getStackInSlot(0);
            if (stack.isEmpty() || hasContained()) {
                return;
            }
            EntityType<?> target = resolveMarkerTarget(stack);
            if (target == null) {
                specialSlot.setStackInSlot(0, ItemStack.EMPTY);
                return;
            }
            captureByType(target);
            specialSlot.setStackInSlot(0, ItemStack.EMPTY);
            setChanged();
            sendUpdatePacket();
            String name = Component.translatable(target.getDescriptionId()).getString();
            sendMessageToNearbyPlayer("chat.alltheimbaium.mob_farm.mark", name);
        } catch (Throwable e) {
            log.error("MobFarmEntity.processSpecialSlotMarker error", e);
        }
    }

    // ==================== 服务端 tick ====================

    /**
     * 服务端主循环：等级增长 → 被动累计 → 使用槽通道 → 六面输出。
     */
    public void tickServer() {
        Level world = getLevel();
        if (world == null || world.isClientSide()) {
            return;
        }
        try {
            // 1) 等级随时间增长（到上限停止）
            if (this.level < MobFarmBlock.getMaxLevel()) {
                tickCount++;
                if (tickCount >= 20L * MobFarmBlock.getLevelUpIntervalSeconds()) {
                    this.level++;
                    tickCount = 0;
                }
            }
            boolean contained = hasContained();
            long carry = MobFarmBlock.getCarry();
            // 0) 放置/载入后首次 tick 若收容了生物但尚无产物表，则补建（手持收容后放下的情形）
            if (needRebuild) {
                needRebuild = false;
                if (contained) {
                    rebuildProducts();
                }
            }
            // 2) 被动确定性累计：每行 权重×等级
            if (contained) {
                for (Row row : rows) {
                    if (row.weight <= 0 || row.fromTool) {
                        continue;
                    }
                    row.acc = Tool.suit(row.acc + row.weight * this.level);
                    if (row.acc >= carry) {
                        row.stock = Tool.suit(row.stock + row.acc / carry);
                        row.acc = row.acc % carry;
                    }
                }
            }
            // 3) 使用槽：工具对收容物可产出的物品种，按"该生物刷怪蛋权重×等级"累计，攒够 carry 出 1 件，
            //    速度与刷怪蛋完全一致；工具缺失/不符时该工具行自动停止（速度 0），也不消耗工具。
            if (contained && entityTag != null) {
                EntityType<?> t = getContainedType();
                ItemStack tool = specialSlot.getStackInSlot(0);
                if (t != null && !tool.isEmpty()) {
                    Item prod = MobFarmInteraction.produceItem(t, entityTag, tool);
                    if (prod != null) {
                        Row r = findRow(prod);
                        if (r == null) {
                            if (rows.size() < MobFarmBlock.getMaxProducts()) {
                                r = new Row(prod, Math.max(1L, spawnEggWeight()));
                                r.fromTool = true;
                                rows.add(r);
                            }
                        } else if (!r.fromTool) {
                            r = null; // 该物品本身有被动产物行（如羊的羊毛），工具通道不重复生成
                        }
                        if (r != null) {
                            r.acc = Tool.suit(r.acc + r.weight * this.level);
                            if (r.acc >= carry) {
                                r.stock = Tool.suit(r.stock + r.acc / carry);
                                r.acc = r.acc % carry;
                            }
                        }
                    }
                }
            }
            // 4) 主动输出（受总开关控制）
            if (outputEnabled) {
                outputToNeighbors();
            }
            setChanged();
        } catch (Throwable e) {
            log.error("MobFarmEntity.tickServer error", e);
        }
    }

    /**
     * 当前收容生物刷怪蛋产物行的权重（作为使用槽产出的速度基准），默认 1
     */
    private long spawnEggWeight() {
        try {
            EntityType<?> type = getContainedType();
            if (type != null) {
                Item egg = MobFarmCatalog.spawnEggOf(type);
                if (egg != null) {
                    for (Row row : rows) {
                        if (row.item == egg && row.weight > 0) {
                            return row.weight;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 1L;
    }

    /** 向相邻方块主动输出可输出行的整件物品 */
    private void outputToNeighbors() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        Direction[] directions = Direction.values();
        findIndex = (findIndex + 1) % directions.length;
        Direction direction = directions[findIndex];
        int state = getDirectionState(direction);
        if (state == STATE_DISABLED) {
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
        if (neighbor == null) {
            return;
        }
        var handler = level.getCapability(Capabilities.Item.BLOCK, neighbor.getBlockPos(), direction.getOpposite());
        if (handler == null) {
            return;
        }
        List<Integer> targets = new ArrayList<>();
        if (state == STATE_RANDOM) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).stock > 0) {
                    targets.add(i);
                }
            }
        } else {
            int idx = state - STATE_SLOT_BASE;
            if (idx < rows.size() && rows.get(idx).stock > 0) {
                targets.add(idx);
            }
        }
        for (int idx : targets) {
            pushRow(handler, idx);
        }
    }

    private void pushRow(@Nonnull ResourceHandler<ItemResource> handler, int index) {
        Row row = rows.get(index);
        int maxStack = new ItemStack(row.item).getMaxStackSize();
        if (maxStack <= 0) {
            maxStack = 1;
        }
        ItemResource resource = ItemResource.of(row.item);
        long remaining = row.stock;
        while (remaining > 0) {
            int amount = (int) Math.min(remaining, maxStack);
            // 26.x：insertStacking 内部会开一个根事务并在结束时提交，等价于旧的 ItemHandlerHelper.insertItemStacked
            int inserted = ResourceHandlerUtil.insertStacking(handler, resource, amount, null);
            if (inserted <= 0) {
                break;
            }
            row.stock -= inserted;
            remaining -= inserted;
        }
        cleanEmptyRows();
    }

    // ==================== capability / 菜单 ====================


    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.mob_farm").withStyle(Tip.rarityColor(MOB_FARM_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new MobFarmMenu(id, inv, worldPosition);
    }

    // ==================== NBT ====================

    private static final String KEY_ENTITY_TAG = "entityTag";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_TICK = "tickCount";
    private static final String KEY_ROWS = "rows";
    private static final String KEY_DIR = "directionState";
    private static final String KEY_OUTPUT = "outputEnabled";
    private static final String KEY_SPECIAL = "specialSlot";

    @Override
    protected void saveAdditional(@Nonnull ValueOutput output) {
        super.saveAdditional(output);
        try {
            if (entityTag != null) {
                output.store(KEY_ENTITY_TAG, CompoundTag.CODEC, entityTag);
            }
            output.putLong(KEY_LEVEL, level);
            output.putLong(KEY_TICK, tickCount);
            // 26.x：产物行改由 ValueOutput 列表托管，物品组件自动按当前注册表访问器编解码
            Tool.writeRows(output, KEY_ROWS, toWeightedToolRows(), Tool.WeightedToolRow.CODEC);
            output.putIntArray(KEY_DIR, directionState);
            output.putBoolean(KEY_OUTPUT, outputEnabled);
            output.putChild(KEY_SPECIAL, specialSlot);
        } catch (Throwable e) {
            log.error("MobFarmEntity.saveAdditional error", e);
        }
    }

    @Override
    protected void loadAdditional(@Nonnull ValueInput input) {
        super.loadAdditional(input);
        cachedEntity = null;
        try {
            entityTag = input.read(KEY_ENTITY_TAG, CompoundTag.CODEC).orElse(null);
            level = Tool.suit(input.getLongOr(KEY_LEVEL, level));
            tickCount = Tool.suit(input.getLongOr(KEY_TICK, tickCount));
            loadRows(Tool.readRows(input, KEY_ROWS, Tool.WeightedToolRow.CODEC));
            int[] arr = input.getIntArray(KEY_DIR).orElse(null);
            if (arr != null) {
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionState[i] = Math.max(0, Math.min(getStateCount() - 1, arr[i]));
                }
            }
            outputEnabled = input.getBooleanOr(KEY_OUTPUT, outputEnabled);
            input.readChild(KEY_SPECIAL, specialSlot);
            // 放置了含收容生物但尚无产物表的方块时，首个服务端 tick 补建产物表
            boolean hasWeight = false;
            for (Row row : rows) {
                if (row.weight > 0) {
                    hasWeight = true;
                    break;
                }
            }
            needRebuild = hasContained() && !hasWeight;
        } catch (Throwable e) {
            log.error("MobFarmEntity.loadAdditional error", e);
        }
    }

    /** 内部可变行 → 持久化记录（权重在记录类型里是 int，写入前统一裁剪） */
    @Nonnull
    private List<Tool.WeightedToolRow> toWeightedToolRows() {
        List<Tool.WeightedToolRow> list = new ArrayList<>(rows.size());
        for (Row row : rows) {
            list.add(new Tool.WeightedToolRow(Tool.oneOf(new ItemStack(row.item)), row.stock, Tool.suitInt(row.weight), row.fromTool));
        }
        return list;
    }

    private void loadRows(@Nonnull List<Tool.WeightedToolRow> list) {
        rows.clear();
        for (Tool.WeightedToolRow record : list) {
            try {
                ItemStack stack = record.item();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Row row = new Row(stack.getItem(), 0);
                row.stock = Tool.suit(record.count());
                row.weight = Math.max(0, record.weight());
                row.fromTool = record.tool();
                rows.add(row);
            } catch (Throwable e) {
                log.warn("MobFarmEntity.loadRows entry error", e);
            }
        }
        cleanEmptyRows();
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

    public void sendUpdatePacket() {
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void sendMessageToNearbyPlayer(String key, Object... args) {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = serverLevel.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 8.0, false);
        if (player != null) {
            player.sendSystemMessage(Component.translatable(key, args));
        }
    }
}
