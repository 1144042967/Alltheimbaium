package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.connection.ExtractionInterfaceConnection;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ATI 取出接口实体：向任意方向暴露 {@link ExtractionInterfaceConnection}
 * （只读 IItemHandler + IFluidHandler），方向无关。
 * <p>
 * 聚合范围不是"相邻六面"，而是从本方块出发、沿<b>本模组与联动模组的方块</b>连通搜索到的全部产出机器：
 * <ul>
 *     <li><b>导体</b>：任何 {@code alltheimbaium} 或 {@code autoresource} 命名空间的方块，
 *         搜索可以穿过它继续往外找。生成平台铺出的平滑石/石砖地面是原版方块，会自然断开连通。</li>
 *     <li><b>产出源</b>：存储方块制造机、生物农场、资源农场、自动耕地、液体无限制造机，
 *         以及 AutoResource 的全部机器（{@link #isLinkedSource}）。</li>
 *     <li><b>只传导不产出</b>：ATI 耕地、零刻熔炉、零刻压印器、AutoResource 水车马达，
 *         以及取出接口自身——它们让网络穿过去，但其中的物品不会被抽走。</li>
 * </ul>
 * 搜索仅限已加载的区块，结果缓存 {@link #RESCAN_INTERVAL} 刻后重算，因此新增机器最多 1 秒后生效。
 */
public class ExtractionInterfaceEntity extends BlockEntity implements ICapabilityProvider {
    private static final Logger log = LoggerFactory.getLogger(ExtractionInterfaceEntity.class);

    /** 连通范围的重算间隔（tick） */
    private static final int RESCAN_INTERVAL = 20;
    /** 单次搜索最多访问的方块数，防止超大建筑把服务端拖垮 */
    private static final int MAX_SCAN_BLOCKS = 4096;

    /**
     * 联动模组 AutoResource 的命名空间。该模组是可选依赖，这里只比对注册名字符串，不做任何编译期引用。
     */
    private static final String AUTORESOURCE_MODID = "autoresource";
    /**
     * AutoResource 里的水车马达：只向外输出旋转动力，不产出物品与流体，因此只传导不作为产出源
     */
    private static final String WATER_WHEEL_MOTOR = "water_wheel_motor";

    private final LazyOptional<ExtractionInterfaceConnection> capability =
            LazyOptional.of(() -> new ExtractionInterfaceConnection(this));

    /** 连通搜索得到的产出机器位置；只读视图，服务端维护 */
    private List<BlockPos> sources = List.of();
    /** 是否已经搜索过；未搜索过时由首次能力查询触发一次同步搜索 */
    private boolean scanned;
    /** 距下次重算的剩余刻数 */
    private int rescanCooldown;
    /** 上次搜索是否因超过 {@link #MAX_SCAN_BLOCKS} 被截断，用于只告警一次 */
    private boolean truncated;

    public ExtractionInterfaceEntity(BlockPos pos, BlockState state) {
        super(Registration.EXTRACTION_INTERFACE_ENTITY.get(), pos, state);
    }

    // ==================== 连通搜索 ====================

    /**
     * 当前连通范围内的产出机器位置。首次调用会同步搜索一次，之后由 {@link #serverTick()} 周期性重算。
     */
    @Nonnull
    public List<BlockPos> getSources() {
        if (!scanned) {
            scanned = true;
            rescan();
        }
        return sources;
    }

    /**
     * 服务端 tick：按 {@link #RESCAN_INTERVAL} 周期重算连通范围
     */
    public void serverTick() {
        if (--rescanCooldown <= 0) {
            rescanCooldown = RESCAN_INTERVAL;
            rescan();
        }
    }

    /**
     * 从自身出发沿本模组方块广度优先搜索，收集全部产出机器位置
     */
    private void rescan() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel)) {
            sources = List.of();
            return;
        }
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = getBlockPos();
        visited.add(origin);
        queue.add(origin);

        int scannedCount = 0;
        boolean cut = false;
        while (!queue.isEmpty()) {
            if (scannedCount++ >= MAX_SCAN_BLOCKS) {
                cut = true;
                break;
            }
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                // 已访问过的位置（无论是否导体）都不再重复处理
                if (!visited.add(next)) {
                    continue;
                }
                // 不加载未加载的区块：那些位置的机器本来也取不到
                if (!level.isLoaded(next) || !isConductor(level, next)) {
                    continue;
                }
                if (isSource(level.getBlockEntity(next))) {
                    found.add(next);
                }
                queue.add(next);
            }
        }

        sources = List.copyOf(found);
        if (cut != truncated) {
            truncated = cut;
            if (cut) {
                log.warn("取出接口 {} 的连通搜索超过 {} 个方块，已截断；仍有机器未被纳入", origin, MAX_SCAN_BLOCKS);
            }
        }
    }

    /**
     * 是否为可传导的方块——命名空间是 {@code alltheimbaium}（本模组）或 {@code autoresource}（联动模组）
     * 就能传导
     */
    private static boolean isConductor(@Nonnull Level level, @Nonnull BlockPos pos) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        return Alltheimbaium.MODID.equals(namespace) || AUTORESOURCE_MODID.equals(namespace);
    }

    /**
     * 是否为可抽取的产出机器。零刻熔炉与零刻压印器刻意不在此列：它们只传导，物品需另行抽取。
     */
    private static boolean isSource(@Nullable BlockEntity be) {
        if (be == null) {
            return false;
        }
        return be instanceof StorageFountainEntity
                || be instanceof MobFarmEntity
                || be instanceof ResourceFarmEntity
                || be instanceof AutoFarmlandEntity
                || be instanceof LiquidFountainEntity
                || isLinkedSource(be);
    }

    /**
     * 是否为联动模组 AutoResource 的产出机器：方块实体注册名落在 {@code autoresource} 命名空间下，
     * 且不是水车马达。
     * <p>
     * 用注册名判断而不是 {@code instanceof}，是因为本模组对 AutoResource 只做可选联动，不能有编译期依赖。
     * 至于具体能抽出物品还是流体，由各机器自己暴露的 Capability 决定——例如 FE 发电机只暴露能量，
     * 因此它虽然在此列，却抽不出任何物品与流体。
     */
    public static boolean isLinkedSource(@Nonnull BlockEntity be) {
        ResourceLocation id = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
        if (id == null || !AUTORESOURCE_MODID.equals(id.getNamespace())) {
            return false;
        }
        return !WATER_WHEEL_MOTOR.equals(id.getPath());
    }

    // ==================== 能力 ====================

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ITEM_HANDLER || capability == ForgeCapabilities.FLUID_HANDLER) {
                return this.capability.cast();
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("ExtractionInterfaceEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    // ==================== NBT（无持久字段） ====================

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        // 重载后连通范围需要重算
        scanned = false;
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
