package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.ClockBlock;
import cn.sd.jrz.alltheimbaium.block.FarmlandBlock;
import cn.sd.jrz.alltheimbaium.gui.ClockMenu;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClockEntity extends BlockEntity implements MenuProvider {
    // ==================== 全局开关 ====================
    // 所有时钟共享的全局开关，初始值由 loadConfig() 从配置文件读取（每次进游戏重新加载）。
    // 非 NBT 持久化：GUI 中切换仅本次会话生效。
    private static boolean globalActive = true;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用，设置时钟全局开关初始状态
     */
    public static void loadConfig() {
        globalActive = Config.CLOCK_DEFAULT_ACTIVE.get();
    }

    public static boolean isGlobalActive() {
        return globalActive;
    }

    public static void setGlobalActive(boolean active) {
        globalActive = active;
    }

    // ==================== 可调速度 ====================
    /** 可选的倍速档位 */
    public static final int[] SPEEDS = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};

    /** 单独开关（当前时钟总开关），NBT 持久化 */
    public boolean enabled = true;
    /** 六方向开关，索引与 Direction.values() 顺序一致，默认全开，NBT 持久化 */
    public final boolean[] directionEnabled = new boolean[6];
    /** 当前倍速，NBT 持久化 */
    public int speed = 2;

    public ClockEntity(BlockPos pos, BlockState state) {
        super(Registration.CLOCK_ENTITY.get(), pos, state);
        // 初始化六方向开关为全开
        java.util.Arrays.fill(directionEnabled, true);
    }

    // ==================== 状态控制 ====================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    public boolean isDirectionEnabled(Direction direction) {
        return directionEnabled[direction.ordinal()];
    }

    public void toggleDirection(Direction direction) {
        int idx = direction.ordinal();
        directionEnabled[idx] = !directionEnabled[idx];
        setChanged();
    }

    public int getSpeed() {
        return speed;
    }

    /**
     * 直接设置倍速档位（非法值回退到最低档）
     */
    public void setSpeed(int value) {
        speed = SPEEDS[indexOfSpeed(value)];
        setChanged();
    }

    /**
     * 循环切换到下一个倍速档位
     */
    public void cycleSpeed() {
        speed = SPEEDS[(indexOfSpeed(speed) + 1) % SPEEDS.length];
        setChanged();
    }

    /**
     * 查找倍速在 SPEEDS 中的索引，不在列表中则返回 0（最低档）
     */
    private static int indexOfSpeed(int value) {
        for (int i = 0; i < SPEEDS.length; i++) {
            if (SPEEDS[i] == value) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 指定方向的相邻方块注册 id（用于 GUI 展示实际相邻方块的物品图标）
     */
    public int getNeighborBlockId(Direction direction) {
        Level level = getLevel();
        if (level == null) {
            return 0;
        }
        Block block = level.getBlockState(worldPosition.relative(direction)).getBlock();
        //noinspection deprecation
        return BuiltInRegistries.BLOCK.getId(block);
    }

    // ==================== 加速逻辑 ====================

    public void tick(Level level) {
        if (!globalActive || !enabled || level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.values()) {
            // 该方向开关关闭则跳过
            if (!directionEnabled[direction.ordinal()]) {
                continue;
            }
            BlockPos pos = getBlockPos().relative(direction);
            BlockState blockState = level.getBlockState(pos);
            Block block = blockState.getBlock();
            if (level instanceof ServerLevel && block.isRandomlyTicking(blockState)) {
                blockState.randomTick((ServerLevel) level, pos, level.getRandom());
            }
            if (block instanceof ClockBlock || block instanceof FarmlandBlock) {
                continue;
            }
            if (!(block instanceof EntityBlock entityBlock)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                //noinspection unchecked
                BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) entityBlock.getTicker(level, blockState, blockEntity.getType());
                if (blockEntity.isRemoved() || ticker == null) {
                    continue;
                }
                for (int i = 1; i < speed; i++) {
                    if (blockEntity.isRemoved()) {
                        break;
                    }
                    ticker.tick(level, pos, blockState, blockEntity);
                }
            }
            BlockEntity aboveEntity = level.getBlockEntity(pos);
            if (aboveEntity != null) {
                aboveEntity.setChanged();
            }
        }
    }

    // ==================== 菜单提供 ====================

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.clock");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new ClockMenu(id, inv, worldPosition);
    }

    // ==================== NBT 持久化 ====================

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.putBoolean("enabled", enabled);
            int[] dirArr = new int[6];
            for (int i = 0; i < 6; i++) {
                dirArr[i] = directionEnabled[i] ? 1 : 0;
            }
            nbt.putIntArray("directionEnabled", dirArr);
            nbt.putInt("speed", speed);
        } catch (Throwable e) {
            log.error("ClockEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            if (nbt.contains("enabled")) {
                enabled = nbt.getBoolean("enabled");
            }
            if (nbt.contains("directionEnabled")) {
                int[] arr = nbt.getIntArray("directionEnabled");
                for (int i = 0; i < Math.min(6, arr.length); i++) {
                    directionEnabled[i] = arr[i] != 0;
                }
            }
            if (nbt.contains("speed")) {
                speed = SPEEDS[indexOfSpeed(nbt.getInt("speed"))];
            }
        } catch (Throwable e) {
            log.error("ClockEntity.load error", e);
        }
    }

    // ==================== 客户端同步（供 BER 渲染倍速数值） ====================

    /**
     * 区块加载/方块放置时同步全部数据到客户端
     */
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

    /**
     * 状态变化时向附近客户端发送更新包（刷新方块侧面的倍速数值渲染）
     */
    public void sendUpdatePacket() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClockEntity.class);
}
