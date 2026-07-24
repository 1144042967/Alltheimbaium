package cn.sd.jrz.alltheimbaium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PlatformBlock extends Block {
    private static final Logger log = LoggerFactory.getLogger(PlatformBlock.class);
    private static final int CHUNK_SIZE = 16;
    private static final int PLATFORM_CHUNK_RADIUS = 1;

    public PlatformBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            generatePlatform(level, pos);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.platform.generated"));
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("PlatformBlock.useWithoutItem error", e);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
        // 手持物品时不触发平台生成
        if (!stack.isEmpty() && !stack.is(Items.AIR)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        try {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            generatePlatform(level, pos);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.platform.generated"));
            return ItemInteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("PlatformBlock.useItemOn error", e);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /**
     * 判断是否为区块中心 10×10 区域的四个角（放置蛙明灯）。
     */
    private static boolean isCenterCorner(int x, int z) {
        return (x == 3 || x == 12) && (z == 3 || z == 12);
    }

    private void generatePlatform(Level level, BlockPos centerPos) {
        int chunkX = centerPos.getX() & -CHUNK_SIZE;
        int chunkZ = centerPos.getZ() & -CHUNK_SIZE;

        for (int chunkOffsetX = -PLATFORM_CHUNK_RADIUS; chunkOffsetX <= PLATFORM_CHUNK_RADIUS; chunkOffsetX++) {
            for (int chunkOffsetZ = -PLATFORM_CHUNK_RADIUS; chunkOffsetZ <= PLATFORM_CHUNK_RADIUS; chunkOffsetZ++) {
                int chunkStartX = chunkX + chunkOffsetX * CHUNK_SIZE;
                int chunkStartZ = chunkZ + chunkOffsetZ * CHUNK_SIZE;

                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        BlockPos targetPos = new BlockPos(chunkStartX + x, centerPos.getY(), chunkStartZ + z);
                        BlockState current = level.getBlockState(targetPos);

                        // 只替换空气、平滑石头、石砖和自身
                        Block currentBlock = current.getBlock();
                        if (!current.isAir() && currentBlock != Blocks.SMOOTH_STONE
                                && currentBlock != Blocks.STONE_BRICKS && currentBlock != this) {
                            continue;
                        }

                        boolean isCorner = (x == 0 || x == CHUNK_SIZE - 1) && (z == 0 || z == CHUNK_SIZE - 1);
                        boolean isEdge = x == 0 || x == CHUNK_SIZE - 1 || z == 0 || z == CHUNK_SIZE - 1;

                        if (isCorner) {
                            level.setBlock(targetPos, this.defaultBlockState(), 3);
                        } else if (isEdge) {
                            level.setBlock(targetPos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                        } else if (isCenterCorner(x, z)) {
                            level.setBlock(targetPos, Blocks.VERDANT_FROGLIGHT.defaultBlockState(), 3);
                        } else {
                            level.setBlock(targetPos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        try {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack(this));
            return drops;
        } catch (Throwable e) {
            log.error("PlatformBlock.getDrops error", e);
        }
        return super.getDrops(state, builder);
    }
}
