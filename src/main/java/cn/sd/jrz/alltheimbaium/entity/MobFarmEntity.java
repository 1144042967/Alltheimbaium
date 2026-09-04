package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.connection.MobFarmConnection;
import cn.sd.jrz.alltheimbaium.gui.MobFarmMenu;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.KillLootEstimator;
import cn.sd.jrz.alltheimbaium.setup.MobFarmCatalog;
import cn.sd.jrz.alltheimbaium.setup.MobFarmInteraction;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
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
import java.util.function.Function;

/**
 * 生物农场方块实体。
 * <p>
 * 仿照 StorageFountainEntity + FarmEntity 的混合：
 * 收容一只生物（存完整实体 NBT + 懒加载客户端缓存实体用于渲染），
 * 收容时建立"产物表"（每行 物品+权重），每 tick 按 权重×等级 平滑累计、按 CARRY 进位成整数物品；
 * 产物行存量长期保存、可六面输出、GUI 内取物。
 * 标记槽放入刷怪蛋/特征掉落物收容生物；使用槽放入物品自动模拟"右击收容物"产出。
 */
public class MobFarmEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
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
    /** 使用槽冷却节拍 */
    private int useCooldown = 0;

    /** 六面 + 无方向能力缓存（同 StorageFountainEntity） */
    @SuppressWarnings("unchecked")
    private final LazyOptional<MobFarmConnection>[] fecOptionals = createDirectionalOptionals();

    private LazyOptional<MobFarmConnection>[] createDirectionalOptionals() {
        LazyOptional<MobFarmConnection>[] optionals = new LazyOptional[7];
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            final Direction direction = directions[i];
            optionals[i] = LazyOptional.of(() -> new MobFarmConnection(this, direction));
        }
        optionals[6] = LazyOptional.of(() -> new MobFarmConnection(this, null));
        return optionals;
    }

    public MobFarmEntity(BlockPos pos, BlockState state) {
        super(Registration.MOB_FARM_ENTITY.get(), pos, state);
        this.level = Math.max(1, MobFarmBlock.getInitialLevel());
        this.useCooldown = MobFarmBlock.getUseIntervalTicks();
    }

    // ==================== 收容生物查询 ====================

    public boolean hasContained() {
        return entityTag != null && entityTag.contains("id", Tag.TAG_STRING);
    }

    @Nullable
    public EntityType<?> getContainedType() {
        if (entityTag == null || !entityTag.contains("id", Tag.TAG_STRING)) {
            return null;
        }
        Optional<EntityType<?>> type = EntityType.byString(entityTag.getString("id"));
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
                cachedEntity = EntityType.loadEntityRecursive(entityTag.copy(), level, Function.identity());
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
            if (level == null || level.isClientSide) {
                return false;
            }
            CompoundTag tag = new CompoundTag();
            if (!entity.save(tag) || !tag.contains("id", Tag.TAG_STRING)) {
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
            if (level != null && !level.isClientSide) {
                try {
                    Entity probe = type.create(level);
                    if (probe != null && probe.save(tag)) {
                        entityTag = tag;
                    }
                } catch (Throwable e) {
                    log.warn("MobFarmEntity.captureByType create fail for {}", type, e);
                }
            }
            if (entityTag == null || !entityTag.contains("id", Tag.TAG_STRING)) {
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

    /**
     * 清空收容物，保留等级与存量。方向指向的越界槽复位为随机。
     */
    public void clearContained() {
        try {
            entityTag = null;
            cachedEntity = null;
            useCooldown = MobFarmBlock.getUseIntervalTicks();
            // 清掉各行的权重，避免残留产物表（存量行保留，仍可取出/输出）
            for (Row row : rows) {
                row.weight = 0;
                row.acc = 0;
            }
            cleanEmptyRows();
            // 指向不存在行的方向状态复位为随机，防止错指
            for (int i = 0; i < directionState.length; i++) {
                if (directionState[i] >= STATE_SLOT_BASE + rows.size()) {
                    directionState[i] = STATE_RANDOM;
                }
            }
            setChanged();
            sendUpdatePacket();
        } catch (Throwable e) {
            log.error("MobFarmEntity.clearContained error", e);
        }
    }

    /** 标记槽能否把物品放入并确定收容目标类型（null=不允许） */
    @Nullable
    public EntityType<?> resolveMarkerTarget(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof SpawnEggItem egg) {
            //noinspection deprecation
            return egg.getType(null);
        }
        return MobFarmCatalog.typeOfSignature(stack.getItem());
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
            if (level == null || level.isClientSide) {
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
     * 构建产物表：白名单 Config 额外掉落（为主） + 击杀掉落补充（去重） + 刷怪蛋兜底。
     */
    private List<Weighted> buildDropTable(ServerLevel serverLevel, EntityType<?> type) {
        Map<Item, Long> map = new HashMap<>();
        DataConfig cfg = MobFarmCatalog.configFor(type);
        if (cfg != null) {
            // 白名单：以现有农场 Config 产物的权重为主
            for (DataConfig.ItemProduct p : cfg.getProductList()) {
                if (p.item != null) {
                    map.put(p.item, Math.max(map.getOrDefault(p.item, 0L), Math.max(1L, p.count)));
                }
            }
        }
        // 击杀掉落：仅补充 Config 没有的物品种（避免同一物品双重计权）
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
        int idx = direction.ordinal();
        int count = getStateCount();
        directionState[idx] = (directionState[idx] + 1) % count;
        setChanged();
    }

    // ==================== 标记槽 / 使用槽 ====================

    public final ItemStackHandler markerSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return resolveMarkerTarget(stack) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            Level level = getLevel();
            if (level == null) {
                return;
            }
            if (level.isClientSide) {
                if (!markerSlot.getStackInSlot(0).isEmpty()) {
                    markerSlot.setStackInSlot(0, ItemStack.EMPTY);
                }
                return;
            }
            processMarkerSlot();
        }
    };

    public final ItemStackHandler useSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /**
     * 处理标记槽物品（服务端）：同生物→清空；否则收容/替换该生物。处理完清槽。
     */
    private void processMarkerSlot() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            ItemStack stack = markerSlot.getStackInSlot(0);
            if (stack.isEmpty()) {
                return;
            }
            EntityType<?> target = resolveMarkerTarget(stack);
            if (target == null) {
                markerSlot.setStackInSlot(0, ItemStack.EMPTY);
                return;
            }
            EntityType<?> current = getContainedType();
            boolean clearing = current != null && current == target;
            if (clearing) {
                clearContained();
            } else {
                captureByType(target);
            }
            markerSlot.setStackInSlot(0, ItemStack.EMPTY);
            setChanged();
            sendUpdatePacket();
            String name = Component.translatable(target.getDescriptionId()).getString();
            sendMessageToNearbyPlayer(clearing
                    ? "chat.alltheimbaium.mob_farm.clear"
                    : (current == null ? "chat.alltheimbaium.mob_farm.mark" : "chat.alltheimbaium.mob_farm.replace"), name);
        } catch (Throwable e) {
            log.error("MobFarmEntity.processMarkerSlot error", e);
        }
    }

    // ==================== 服务端 tick ====================

    /**
     * 服务端主循环：等级增长 → 被动累计 → 使用槽通道 → 剪毛恢复 → 六面输出。
     */
    public void tickServer() {
        Level world = getLevel();
        if (world == null || world.isClientSide) {
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
            // 2) 被动确定性累计：每行 权重×等级
            if (contained) {
                for (Row row : rows) {
                    if (row.weight <= 0) {
                        continue;
                    }
                    row.acc = Tool.suit(row.acc + row.weight * this.level);
                    if (row.acc >= carry) {
                        row.stock = Tool.suit(row.stock + row.acc / carry);
                        row.acc = row.acc % carry;
                    }
                }
            }
            // 3) 剪毛/挤奶后的恢复计时（仅收容时）
            boolean regrowDone = false;
            if (contained && entityTag != null) {
                EntityType<?> type = getContainedType();
                if (type != null) {
                    regrowDone = MobFarmInteraction.tickRegrow(type, entityTag);
                }
            }
            // 4) 使用槽自动模拟右击
            if (contained && !useSlot.getStackInSlot(0).isEmpty()) {
                if (--useCooldown <= 0) {
                    useCooldown = Math.max(1, MobFarmBlock.getUseIntervalTicks());
                    tickUseSlot();
                }
            }
            // 5) 主动输出（受总开关控制）
            if (outputEnabled) {
                outputToNeighbors();
            }
            setChanged();
            if (regrowDone) {
                sendUpdatePacket();
            }
        } catch (Throwable e) {
            log.error("MobFarmEntity.tickServer error", e);
        }
    }

    /** 用使用槽物品对收容物模拟右击一次（服务端） */
    private void tickUseSlot() {
        try {
            Level level = getLevel();
            if (level == null || level.isClientSide || entityTag == null) {
                return;
            }
            EntityType<?> type = getContainedType();
            if (type == null) {
                return;
            }
            ItemStack inSlot = useSlot.getStackInSlot(0);
            if (inSlot.isEmpty()) {
                return;
            }
            MobFarmInteraction.UseResult result = MobFarmInteraction.simulateUse(type, entityTag, inSlot);
            boolean changed = false;
            if (!result.isEmpty()) {
                for (ItemStack stack : result.produced()) {
                    addProduct(stack);
                }
                changed = true;
            }
            if (result.durabilityUsed() > 0 && inSlot.isDamageableItem()) {
                int dmg = inSlot.getDamageValue() + result.durabilityUsed();
                int max = inSlot.getMaxDamage();
                if (max > 0 && dmg >= max) {
                    inSlot.shrink(1);
                } else {
                    inSlot.setDamageValue(dmg);
                }
                changed = true;
            }
            if (result.consumeInput()) {
                inSlot.shrink(1);
                changed = true;
            }
            if (changed) {
                useSlot.setStackInSlot(0, inSlot);
                setChanged();
                sendUpdatePacket();
            }
        } catch (Throwable e) {
            log.error("MobFarmEntity.tickUseSlot error", e);
        }
    }

    /** 向相邻方块主动输出可输出行的整件物品 */
    private void outputToNeighbors() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
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
        var optional = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
        var handler = optional.resolve().orElse(null);
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

    private void pushRow(net.minecraftforge.items.IItemHandler handler, int index) {
        Row row = rows.get(index);
        int maxStack = new ItemStack(row.item).getMaxStackSize();
        if (maxStack <= 0) {
            maxStack = 1;
        }
        long remaining = row.stock;
        while (remaining > 0) {
            int amount = (int) Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(row.item, amount);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            int inserted = amount - leftover.getCount();
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
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                int idx = direction == null ? 6 : direction.ordinal();
                if (idx >= 0 && idx < fecOptionals.length) {
                    return fecOptionals[idx].cast();
                }
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("MobFarmEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.mob_farm");
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
    private static final String KEY_MARKER = "markerSlot";
    private static final String KEY_USE = "useSlot";

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            if (entityTag != null) {
                nbt.put(KEY_ENTITY_TAG, entityTag);
            }
            nbt.putLong(KEY_LEVEL, level);
            nbt.putLong(KEY_TICK, tickCount);
            nbt.put(KEY_ROWS, saveRows());
            nbt.putIntArray(KEY_DIR, directionState);
            nbt.putBoolean(KEY_OUTPUT, outputEnabled);
            nbt.put(KEY_MARKER, markerSlot.serializeNBT());
            nbt.put(KEY_USE, useSlot.serializeNBT());
        } catch (Throwable e) {
            log.error("MobFarmEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        cachedEntity = null;
        try {
            entityTag = nbt.contains(KEY_ENTITY_TAG, Tag.TAG_COMPOUND) ? nbt.getCompound(KEY_ENTITY_TAG) : null;
            if (nbt.contains(KEY_LEVEL, Tag.TAG_LONG)) {
                level = Tool.suit(nbt.getLong(KEY_LEVEL));
            }
            if (nbt.contains(KEY_TICK, Tag.TAG_LONG)) {
                tickCount = Tool.suit(nbt.getLong(KEY_TICK));
            }
            if (nbt.contains(KEY_ROWS, Tag.TAG_LIST)) {
                loadRows((ListTag) nbt.get(KEY_ROWS));
            }
            if (nbt.contains(KEY_DIR)) {
                int[] arr = nbt.getIntArray(KEY_DIR);
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionState[i] = Math.max(0, Math.min(getStateCount() - 1, arr[i]));
                }
            }
            if (nbt.contains(KEY_OUTPUT, Tag.TAG_BYTE)) {
                outputEnabled = nbt.getBoolean(KEY_OUTPUT);
            }
            if (nbt.contains(KEY_MARKER, Tag.TAG_COMPOUND)) {
                markerSlot.deserializeNBT(nbt.getCompound(KEY_MARKER));
            }
            if (nbt.contains(KEY_USE, Tag.TAG_COMPOUND)) {
                useSlot.deserializeNBT(nbt.getCompound(KEY_USE));
            }
        } catch (Throwable e) {
            log.error("MobFarmEntity.load error", e);
        }
    }

    private ListTag saveRows() {
        ListTag list = new ListTag();
        for (Row row : rows) {
            CompoundTag c = new CompoundTag();
            new ItemStack(row.item, 1).save(c);
            c.putLong("Stock", row.stock);
            c.putLong("Weight", row.weight);
            list.add(c);
        }
        return list;
    }

    private void loadRows(ListTag list) {
        rows.clear();
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag c = list.getCompound(i);
                ItemStack stack = ItemStack.of(c);
                if (stack.isEmpty()) {
                    continue;
                }
                Row row = new Row(stack.getItem(), 0);
                row.stock = c.contains("Stock", Tag.TAG_LONG) ? Tool.suit(c.getLong("Stock")) : 0;
                row.weight = c.contains("Weight", Tag.TAG_LONG) ? Tool.suit(c.getLong("Weight")) : 0;
                rows.add(row);
            } catch (Throwable e) {
                log.warn("MobFarmEntity.loadRows entry error", e);
            }
        }
        cleanEmptyRows();
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

    public void sendUpdatePacket() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
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
