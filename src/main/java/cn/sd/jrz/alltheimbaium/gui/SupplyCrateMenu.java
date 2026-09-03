package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.SupplyCrateBlock;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.SupplyData;
import cn.sd.jrz.alltheimbaium.setup.SupplyRoll;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * ATI 补给箱容器。
 * <p>
 * 服务端在打开时按 世界种子|游戏小时(取整)|已用补给点 随机 10 个分类物品，同时写入开屏包。
 * 随机小时按整点取整：GUI 保持打开不动时，每到新的整点自动重掷一次（免费）。
 * 数据槽同步 10 个物品 id、选中索引、最大/已用补给点。
 * 按钮：0~9 选择某分类物品；10 兑换（消耗 3 点，给 1 件选中物品）；11 刷新（消耗 1 点，重新随机 10 件）。
 */
public class SupplyCrateMenu extends AbstractContainerMenu {
    public static final int BUTTON_SELECT_BASE = 0;   // 0~9：选择某分类物品
    public static final int BUTTON_REDEEM = 10;        // 兑换选中物品
    public static final int BUTTON_REFRESH = 11;       // 刷新

    /** 补给箱位置（仍有效判断用） */
    public final BlockPos pos;
    /** 服务端打开该菜单的玩家（客户端为 null） */
    @Nullable
    private final ServerPlayer ownerPlayer;
    /** 当前 10 个物品所对应的世界游戏小时（整点）；整点变化时自动重掷 */
    private int rollHour = -1;

    /** 10 个分类的随机物品 id（0 = 无可用） */
    private final int[] itemIds = new int[10];
    /** 当前选中的分类索引，-1 表示未选择 */
    private int selectedIndex = -1;
    private int clientMax;
    private int clientUsed;

    public SupplyCrateMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, data.readBlockPos(), null, -1,
                readIds(data), data.readVarInt() - 1, data.readVarInt(), data.readVarInt());
    }

    /**
     * 服务端构造（由 SupplyCrateBlock 提供已生成的随机结果与打开玩家）
     */
    public static SupplyCrateMenu createServer(int id, Inventory playerInventory, BlockPos pos,
                                               ItemStack[] rolls, int selectedIndex, int max, int used,
                                               ServerPlayer owner) {
        int[] ids = new int[10];
        for (int i = 0; i < 10; i++) {
            ids[i] = (i < rolls.length && !rolls[i].isEmpty())
                    ? BuiltInRegistries.ITEM.getId(rolls[i].getItem()) : 0;
        }
        int hour = owner.level() instanceof ServerLevel sl ? currentHour(sl) : -1;
        return new SupplyCrateMenu(id, playerInventory, pos, owner, hour, ids, selectedIndex, max, used);
    }

    private SupplyCrateMenu(int id, Inventory playerInventory, BlockPos pos,
                            @Nullable ServerPlayer owner, int rollHour, int[] ids,
                            int initialSelected, int initialMax, int initialUsed) {
        super(Registration.SUPPLY_CRATE_MENU.get(), id);
        this.pos = pos;
        this.ownerPlayer = owner;
        this.rollHour = rollHour;
        System.arraycopy(ids, 0, this.itemIds, 0, Math.min(10, ids.length));
        this.selectedIndex = initialSelected;
        this.clientMax = initialMax;
        this.clientUsed = initialUsed;
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> itemIds[idx], v -> itemIds[idx] = v));
        }
        addDataSlot(makeDataSlot(() -> selectedIndex + 1, v -> selectedIndex = v - 1));
        addDataSlot(makeDataSlot(() -> clientMax, v -> clientMax = v));
        addDataSlot(makeDataSlot(() -> clientUsed, v -> clientUsed = v));
    }

    private static int[] readIds(FriendlyByteBuf data) {
        int[] ids = new int[10];
        for (int i = 0; i < 10; i++) {
            ids[i] = data.readVarInt();
        }
        return ids;
    }

    /** 世界累计真实小时数（72000 tick = 1 真实小时） */
    private static int currentHour(Level level) {
        return (int) (level.getGameTime() / (20L * 60 * 60));
    }

    /**
     * 每 tick 广播前检查：世界游戏小时取整后变化则自动免费重掷一组（GUI 不动时每小时刷新一次）。
     */
    @Override
    public void broadcastChanges() {
        try {
            if (ownerPlayer != null && ownerPlayer.level() instanceof ServerLevel serverLevel) {
                int hour = currentHour(serverLevel);
                if (hour != rollHour) {
                    rollItems(serverLevel, ownerPlayer);
                    // 整点自动刷新：物品已变化，清空选择避免误换到新物品
                    this.selectedIndex = -1;
                }
            }
        } catch (Throwable e) {
            // 不阻断容器同步
        }
        super.broadcastChanges();
    }

    // ==================== 读取 ====================

    public int getItemId(int index) {
        return index >= 0 && index < 10 ? itemIds[index] : 0;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getMax() {
        return clientMax;
    }

    public int getUsed() {
        return clientUsed;
    }

    public int getRemaining() {
        return Math.max(0, clientMax - clientUsed);
    }

    /**
     * 某分类当前随机物品的栈（数量 1），空返回 EMPTY
     */
    public ItemStack getRolledStack(int index) {
        int id = getItemId(index);
        if (id <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(id), 1);
    }

    // ==================== 按钮处理 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (player.level().isClientSide) {
            return false;
        }
        if (id >= BUTTON_SELECT_BASE && id < BUTTON_SELECT_BASE + 10) {
            if (getItemId(id) != 0) {
                this.selectedIndex = id;
            }
            return true;
        }
        int remaining = SupplyData.getMax(player) - SupplyData.getUsed(player);
        if (id == BUTTON_REDEEM) {
            if (remaining < SupplyData.COST_REDEEM) {
                return false;
            }
            if (this.selectedIndex < 0 || getItemId(this.selectedIndex) == 0) {
                return false;
            }
            // 给玩家 1 件选中的物品，放不下则丢到脚边
            ItemStack give = getRolledStack(this.selectedIndex).copy();
            player.getInventory().add(give);
            if (!give.isEmpty()) {
                player.drop(give, false);
            }
            SupplyData.addUsed(player, SupplyData.COST_REDEEM);
            if (player.level() instanceof ServerLevel serverLevel) {
                rollItems(serverLevel, player);
            }
            this.selectedIndex = -1;
            return true;
        }
        if (id == BUTTON_REFRESH) {
            if (remaining < SupplyData.COST_REFRESH) {
                return false;
            }
            SupplyData.addUsed(player, SupplyData.COST_REFRESH);
            if (player.level() instanceof ServerLevel serverLevel) {
                rollItems(serverLevel, player);
            }
            this.selectedIndex = -1;
            return true;
        }
        return false;
    }

    /**
     * 按当前状态重掷一组物品并记录对应的整点小时。
     */
    private void rollItems(ServerLevel level, Player player) {
        ItemStack[] rolls = SupplyRoll.roll(level, player);
        for (int i = 0; i < rolls.length && i < 10; i++) {
            itemIds[i] = rolls[i].isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(rolls[i].getItem());
        }
        this.rollHour = currentHour(level);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        BlockState state = player.level().getBlockState(pos);
        return state.getBlock() instanceof SupplyCrateBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static DataSlot makeDataSlot(IntSupplier getter, IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() {
                return getter.getAsInt();
            }

            @Override
            public void set(int value) {
                setter.accept(value);
            }
        };
    }
}
