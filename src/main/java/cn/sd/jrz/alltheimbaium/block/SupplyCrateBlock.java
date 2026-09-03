package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.gui.SupplyCrateMenu;
import cn.sd.jrz.alltheimbaium.setup.SupplyData;
import cn.sd.jrz.alltheimbaium.setup.SupplyRoll;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraftforge.network.NetworkHooks;
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
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
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
                    Component.translatable("block.alltheimbaium.supply_crate"));
            // 开屏数据与服务端菜单使用同一组随机结果，保证客户端显示一致
            NetworkHooks.openScreen(serverPlayer, provider, buf -> writeOpenData(buf, pos, rolls, max, used));
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("SupplyCrateBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    /**
     * 写入菜单初始数据：方块坐标、10 个物品 id、选中索引(无=-1 编码为0)、最大/已用补给点。
     */
    private static void writeOpenData(FriendlyByteBuf buf, BlockPos pos, ItemStack[] rolls, int max, int used) {
        buf.writeBlockPos(pos);
        for (int i = 0; i < 10; i++) {
            int id = (i < rolls.length && !rolls[i].isEmpty()) ? BuiltInRegistries.ITEM.getId(rolls[i].getItem()) : 0;
            buf.writeVarInt(id);
        }
        buf.writeVarInt(0);
        buf.writeVarInt(max);
        buf.writeVarInt(used);
    }
}
