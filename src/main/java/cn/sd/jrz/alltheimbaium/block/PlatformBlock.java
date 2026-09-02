package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.gui.PlatformMenu;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 生成平台：右键（空手）打开配置 GUI，Shift+右键（空手）整片生成 3×3 区块平台。
 * <p>
 * 每块"区块平台"（16×16）的图案：四角是生成平台、四边是石砖、中间是平滑石头
 * （中间 10×10 区域四角保留荧光蛙明灯装饰）。
 * GUI 内：
 * - 伪装开关：默认关闭。开启后全局生效，所有生成平台的上表面贴图换成石砖（持久保存到配置，重进仍生效）。
 * - 3×3 九宫格按钮：每个格子 = 一个 16×16 区块（以所点平台所在区块为中心），
 *   该区块四个角是否都是生成平台 = 是否"已生成"（绿）；点击任意格子会尝试生成/重建该区块平台。
 * <p>
 * 伪装通过方块状态属性 {@link #DISGUISED} 决定模型（顶面平台贴图 or 石砖）。
 * 属性值随全局伪装开关变化：本机全局已放置的平台方块位置被追踪到 {@link #DISGUISE_POSITIONS}，
 * 切换开关时刷新当前已加载的所有追踪方块；未加载区块在加载时会按其角格列扫描纠正，保证持久一致。
 */
public class PlatformBlock extends Block {
    private static final Logger log = LoggerFactory.getLogger(PlatformBlock.class);
    /** 伪装状态属性：true 时模型顶面使用石砖贴图 */
    public static final BooleanProperty DISGUISED = BooleanProperty.create("disguised");
    private static final int CHUNK_SIZE = 16;
    private static final int PLATFORM_CHUNK_RADIUS = 1;

    /** 全局伪装开关（服务端权威；由配置加载与 GUI 切换维护，并保存到配置文件） */
    private static boolean disguiseActive = false;
    /** 已放置的平台方块位置（按世界维度分组），用于伪装开关切换时刷新当前已加载的平台方块 */
    private static final Map<ServerLevel, Set<BlockPos>> DISGUISE_POSITIONS = new HashMap<>();

    public PlatformBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISGUISED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISGUISED);
    }

    // ==================== 全局伪装 ====================

    /**
     * 由 Config.onConfigLoad() 在配置加载完成后调用
     */
    public static void loadConfig() {
        disguiseActive = Config.PLATFORM_DISGUISE_ENABLED.get();
    }

    /**
     * 当前全局伪装是否开启
     */
    public static boolean isDisguiseActive() {
        return disguiseActive;
    }

    /**
     * 服务端切换全局伪装：更新内存值并回写保存配置，然后刷新当前已加载的所有追踪平台方块。
     */
    public static void setDisguiseActive(MinecraftServer server, boolean on) {
        if (disguiseActive == on) {
            return;
        }
        disguiseActive = on;
        // 持久化到配置文件（所有世界共用的全局开关）
        Config.PLATFORM_DISGUISE_ENABLED.set(on);
        if (Config.SERVER_MOD_CONFIG != null) {
            Config.SERVER_MOD_CONFIG.save();
        }
        for (ServerLevel level : server.getAllLevels()) {
            Set<BlockPos> positions = DISGUISE_POSITIONS.get(level);
            if (positions == null || positions.isEmpty()) {
                continue;
            }
            for (BlockPos pos : positions) {
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof PlatformBlock && state.getValue(DISGUISED) != on) {
                    level.setBlock(pos, state.setValue(DISGUISED, on), 2);
                }
            }
        }
    }

    /**
     * 记录一个平台方块位置（用于伪装切换时刷新）
     */
    public static void trackPlatform(ServerLevel level, BlockPos pos) {
        DISGUISE_POSITIONS.computeIfAbsent(level, k -> new HashSet<>()).add(pos.immutable());
    }

    /**
     * 移除一个平台方块位置（方块被破坏时调用）
     */
    public static void untrackPlatform(ServerLevel level, BlockPos pos) {
        Set<BlockPos> set = DISGUISE_POSITIONS.get(level);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                DISGUISE_POSITIONS.remove(level);
            }
        }
    }

    /**
     * 服务端关闭时清空所有追踪（避免跨世界/会话残留）
     */
    public static void clearDisguisePositions() {
        DISGUISE_POSITIONS.clear();
    }

    /**
     * 平台区块加载时调用：按角格列扫描纠正伪装属性，并把这些平台方块记入追踪集合。
     */
    public static void correctChunkDisguise(ServerLevel level, LevelChunk chunk) {
        applyDisguiseToChunk(level, chunk, disguiseActive);
    }

    /**
     * 按 16×16 区块的四个角格列纵向扫描：发现生成平台即记入追踪，并在属性与目标不符时纠正。
     * 每块角格列只有 4 列，纵向扫描成本很低。
     */
    private static void applyDisguiseToChunk(ServerLevel level, LevelChunk chunk, boolean on) {
        ChunkPos pos = chunk.getPos();
        int baseX = pos.getMinBlockX();
        int baseZ = pos.getMinBlockZ();
        int[] corner = {0, CHUNK_SIZE - 1};
        for (int xv : corner) {
            for (int zv : corner) {
                applyDisguiseColumn(level, baseX + xv, baseZ + zv, on);
            }
        }
    }

    private static void applyDisguiseColumn(ServerLevel level, int x, int z, boolean on) {
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof PlatformBlock) {
                trackPlatform(level, p);
                if (state.getValue(DISGUISED) != on) {
                    level.setBlock(p, state.setValue(DISGUISED, on), 2);
                }
            }
        }
    }

    // ==================== 交互 ====================

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
                // 手持物品时不拦截（允许放置方块/使用物品）
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown()) {
                // Shift+右键（空手）：整片生成 3×3 区块平台
                generateRegion(level, pos);
                player.sendSystemMessage(Component.translatable("screen.alltheimbaium.platform.generated"));
            } else {
                // 右键（空手）：打开配置 GUI
                MenuProvider provider = new SimpleMenuProvider((id, inv, owner) -> new PlatformMenu(id, inv, pos),
                        Component.translatable("block.alltheimbaium.platform"));
                NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("PlatformBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    // ==================== 生成 ====================

    /**
     * 生成一块平台方块（角格用）时使用的带当前伪装属性的方块状态。
     * 通过已注册的单例取默认状态，避免静态方法无法访问实例。
     */
    private static BlockState platformState(boolean disguised) {
        return Registration.PLATFORM_BLOCK.get().defaultBlockState().setValue(DISGUISED, disguised);
    }

    /**
     * 整片生成：以所点平台所在区块为中心的 3×3 区块区域
     */
    private static void generateRegion(Level level, BlockPos anchor) {
        int chunkX = anchor.getX() >> 4;
        int chunkZ = anchor.getZ() >> 4;
        for (int dz = -PLATFORM_CHUNK_RADIUS; dz <= PLATFORM_CHUNK_RADIUS; dz++) {
            for (int dx = -PLATFORM_CHUNK_RADIUS; dx <= PLATFORM_CHUNK_RADIUS; dx++) {
                generateChunkCell(level, chunkX + dx, chunkZ + dz, anchor.getY());
            }
        }
    }

    /**
     * 按九宫格偏移(dx,dz ∈ {-1,0,1})生成对应那一个区块的平台
     */
    public static void generateCellAt(Level level, BlockPos anchor, int dx, int dz) {
        if (dx < -PLATFORM_CHUNK_RADIUS || dx > PLATFORM_CHUNK_RADIUS
                || dz < -PLATFORM_CHUNK_RADIUS || dz > PLATFORM_CHUNK_RADIUS) {
            return;
        }
        int chunkX = (anchor.getX() >> 4) + dx;
        int chunkZ = (anchor.getZ() >> 4) + dz;
        generateChunkCell(level, chunkX, chunkZ, anchor.getY());
    }

    /**
     * 生成单个区块（16×16）的平台平面：四角=生成平台、四边=石砖、中间=平滑石头，
     * 中间 10×10 区域四角保留荧光蛙明灯装饰。平台方块按当前全局伪装状态放置并记入追踪。
     */
    private static void generateChunkCell(Level level, int chunkX, int chunkZ, int y) {
        int chunkStartX = chunkX << 4;
        int chunkStartZ = chunkZ << 4;
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                BlockPos targetPos = new BlockPos(chunkStartX + x, y, chunkStartZ + z);
                BlockState current = level.getBlockState(targetPos);

                // 只替换空气、平滑石头、石砖、蛙明灯和自身
                Block currentBlock = current.getBlock();
                if (!current.isAir() && currentBlock != Blocks.SMOOTH_STONE
                        && currentBlock != Blocks.STONE_BRICKS
                        && currentBlock != Blocks.VERDANT_FROGLIGHT
                        && !(currentBlock instanceof PlatformBlock)) {
                    continue;
                }

                boolean isCorner = (x == 0 || x == CHUNK_SIZE - 1) && (z == 0 || z == CHUNK_SIZE - 1);
                boolean isEdge = x == 0 || x == CHUNK_SIZE - 1 || z == 0 || z == CHUNK_SIZE - 1;

                if (isCorner) {
                    level.setBlock(targetPos, platformState(disguiseActive), 3);
                    if (level instanceof ServerLevel serverLevel) {
                        trackPlatform(serverLevel, targetPos);
                    }
                } else if (isEdge) {
                    level.setBlock(targetPos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                } else if ((x == 3 || x == 12) && (z == 3 || z == 12)) {
                    // 中间 10×10 区域的四角：荧光蛙明灯装饰
                    level.setBlock(targetPos, Blocks.VERDANT_FROGLIGHT.defaultBlockState(), 3);
                } else {
                    level.setBlock(targetPos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * 判断某区块是否"已生成"：该区块（chunkX, chunkZ）的四个角（同一 Y 平面）是否都是生成平台。
     */
    public static boolean isChunkCellGenerated(Level level, int chunkX, int chunkZ, int y) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int[] corner = {0, CHUNK_SIZE - 1};
        for (int xv : corner) {
            for (int zv : corner) {
                BlockState state = level.getBlockState(new BlockPos(baseX + xv, y, baseZ + zv));
                if (!(state.getBlock() instanceof PlatformBlock)) {
                    return false;
                }
            }
        }
        return true;
    }
}
