package cn.sd.jrz.alltheimbaium.block;

import net.minecraft.world.ItemInteractionResult;
import cn.sd.jrz.alltheimbaium.gui.SupplyCrateMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.SupplyData;
import cn.sd.jrz.alltheimbaium.setup.SupplyRoll;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * ATI 补给箱：右键（无额外条件）打开补给 GUI。
 * <p>
 * 打开时按当前世界种子、世界游戏小时（取整）与玩家已用补给点随机生成 10 个分类物品，
 * 一次计算后同时用于服务端菜单与写入开屏数据（两端一致）。
 * 方块本身不保存任何数据（数据都在玩家身上）。
 */
public class SupplyCrateBlock extends Block {
    private static final Logger log = LoggerFactory.getLogger(SupplyCrateBlock.class);

    public SupplyCrateBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            // 打开时按当前状态随机生成一组（世界种子 | 游戏小时取整 | 已用补给点 作为随机种子）
            ItemStack[] rolls = level instanceof ServerLevel serverLevel
                    ? SupplyRoll.roll(serverLevel, serverPlayer) : new ItemStack[10];
            int max = SupplyData.getMax(serverPlayer);
            int used = SupplyData.getUsed(serverPlayer);

            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, owner) -> SupplyCrateMenu.createServer(id, inv, pos, rolls, -1, max, used, serverPlayer),
                    Component.translatable("block.alltheimbaium.supply_crate").withStyle(Tip.rarityColor(Registration.SUPPLY_CRATE_ITEM.get())));
            // 开屏数据与服务端菜单使用同一组随机结果，保证客户端显示一致
            serverPlayer.openMenu(provider, buf -> writeOpenData(buf, pos, rolls, max, used));
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("SupplyCrateBlock.use error", e);
        }
        return InteractionResult.PASS;
    }

    /**
     * 1.21：原版的 Block#use 拆成了空手的 useWithoutItem 与持物的 useItemOn，
     * 这里两个都覆写并统一转到 doUse，行为与 1.20.1 保持一致。
     */
    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return doUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        InteractionResult result = doUse(state, level, pos, player, handIn, hit);
        if (result == InteractionResult.SUCCESS) {
            return ItemInteractionResult.SUCCESS;
        }
        if (result == InteractionResult.FAIL) {
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }


    /**
     * 写入菜单初始数据：方块坐标、10 个完整物品（含 NBT）、选中索引(无=-1 编码为0)、最大/已用补给点。
     * <p>
     * 物品按 NBT 写而不是只写注册 id：药水 / 附魔书 / 带 EntityTag 的刷怪蛋这类物品的信息全在 NBT 上，
     * 只发 id 客户端会显示（并兑换）成默认版本。
     */
    private static void writeOpenData(RegistryFriendlyByteBuf buf, BlockPos pos, ItemStack[] rolls, int max, int used) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < SupplyCrateMenu.ROLL_SLOTS; i++) {
            ItemStack stack = (rolls != null && i < rolls.length && rolls[i] != null) ? rolls[i] : ItemStack.EMPTY;
            // 1.21：物品一律走 STREAM_CODEC（自带数据组件与注册表访问），不再手工 save/readNbt
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
        buf.writeVarInt(0);
        buf.writeVarInt(max);
        buf.writeVarInt(used);
    }
}
