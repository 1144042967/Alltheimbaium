package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nonnull;

public class HeatBallItem extends Item {
    public HeatBallItem() {
        super(new Item.Properties());
    }

    @Override
    public @Nonnull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(clickedPos);
        if (!CampfireBlock.canLight(blockstate) && !CandleBlock.canLight(blockstate) && !CandleCakeBlock.canLight(blockstate)) {
            BlockPos targetPos = clickedPos.relative(context.getClickedFace());
            if (BaseFireBlock.canBePlacedAt(level, targetPos, context.getHorizontalDirection())) {
                level.playSound(player, targetPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
                BlockState targetState = BaseFireBlock.getState(level, targetPos);
                level.setBlock(targetPos, targetState, 11);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, clickedPos);
                ItemStack itemstack = context.getItemInHand();
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, targetPos, itemstack);
                    itemstack.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            level.playSound(player, clickedPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setBlock(clickedPos, blockstate.setValue(BlockStateProperties.LIT, Boolean.TRUE), 11);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, clickedPos);
            if (player != null) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
    }
}
