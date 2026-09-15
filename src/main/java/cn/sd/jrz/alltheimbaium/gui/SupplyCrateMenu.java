package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.SupplyCrateBlock;
import cn.sd.jrz.alltheimbaium.network.Network;
import cn.sd.jrz.alltheimbaium.network.SupplyCrateRollsPacket;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.SupplyData;
import cn.sd.jrz.alltheimbaium.setup.SupplyRoll;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.serialization.DynamicOps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * ATI 补给箱容器。
 * <p>
 * 服务端在打开时按 世界种子|游戏小时(取整)|已用补给点 随机 10 个分类物品，同时写入开屏包。
 * 随机小时按整点取整：GUI 保持打开不动时，每到新的整点自动重掷一次（免费）。
 * <p>
 * 10 个物品按**完整 ItemStack**（含 NBT）经开屏包与 {@link SupplyCrateRollsPacket} 同步，
 * 不走数据槽——数据槽只有 16 位，装不下物品注册 id，更装不下 NBT。
 * 数据槽只同步选中索引与最大/已用补给点这三个小整数。
 */
public class SupplyCrateMenu extends AbstractContainerMenu {
    public static final int BUTTON_SELECT_BASE = 0;   // 0~9：选择某分类物品
    public static final int BUTTON_REDEEM = 10;        // 兑换选中物品
    public static final int BUTTON_REFRESH = 11;       // 刷新

    /** 一次随机生成的分类物品数 */
    public static final int ROLL_SLOTS = 10;

    /** 补给箱位置（仍有效判断用） */
    public final BlockPos pos;
    /** 服务端打开该菜单的玩家（客户端为 null） */
    @Nullable
    private final ServerPlayer ownerPlayer;
    /** 当前 10 个物品所对应的世界游戏小时（整点）；整点变化时自动重掷 */
    private int rollHour = -1;

    /** 10 个分类的随机物品（含完整 NBT）；空栈 = 该分类无可用物品 */
    private final ItemStack[] rolls = new ItemStack[ROLL_SLOTS];
    /** 当前选中的分类索引，-1 表示未选择 */
    private int selectedIndex = -1;
    private int clientMax;
    private int clientUsed;

    public SupplyCrateMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(id, playerInventory, data.readBlockPos(), null, -1,
                readRolls(data), data.readVarInt() - 1, data.readVarInt(), data.readVarInt());
    }

    /**
     * 服务端构造（由 SupplyCrateBlock 提供已生成的随机结果与打开玩家）
     */
    public static SupplyCrateMenu createServer(int id, Inventory playerInventory, BlockPos pos,
                                               ItemStack[] rolls, int selectedIndex, int max, int used,
                                               ServerPlayer owner) {
        int hour = owner.level() instanceof ServerLevel sl ? currentHour(sl) : -1;
        return new SupplyCrateMenu(id, playerInventory, pos, owner, hour, rolls, selectedIndex, max, used);
    }

    private SupplyCrateMenu(int id, Inventory playerInventory, BlockPos pos,
                            @Nullable ServerPlayer owner, int rollHour, ItemStack[] initialRolls,
                            int initialSelected, int initialMax, int initialUsed) {
        super(Registration.SUPPLY_CRATE_MENU.get(), id);
        this.pos = pos;
        this.ownerPlayer = owner;
        this.rollHour = rollHour;
        setRolls(initialRolls);
        this.selectedIndex = initialSelected;
        this.clientMax = initialMax;
        this.clientUsed = initialUsed;
        addDataSlot(makeDataSlot(() -> selectedIndex + 1, v -> selectedIndex = v - 1));
        addDataSlot(makeDataSlot(() -> clientMax, v -> clientMax = v));
        addDataSlot(makeDataSlot(() -> clientUsed, v -> clientUsed = v));
    }

    /**
     * 把 10 个物品写成 NBT 列表写进缓冲（开屏包与 {@link SupplyCrateRollsPacket} 共用）。
     * <p>
     * <b>这里不能用 {@code ItemStack.OPTIONAL_STREAM_CODEC}</b>：它给附魔、药水这类"引用注册表"的组件同步的是
     * <b>注册表整数 id</b>，一旦编码用的那个 RegistryAccess 里的注册表实例和物品持有的不是同一个，
     * 就会在 Netty 线程抛 {@code Can't find id for 'Reference{[minecraft:enchantment / minecraft:impaling]}'}
     * 并直接掐断连接（ATM10 实测：抽到附魔书必崩）。走 NBT 则按**注册名**书写，与注册表实例无关。
     */
    public static void writeRolls(@Nonnull RegistryFriendlyByteBuf buf, @Nullable ItemStack[] rolls) {
        HolderLookup.Provider registries = Tool.registries();
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (int i = 0; i < ROLL_SLOTS; i++) {
            ItemStack stack = (rolls != null && i < rolls.length && rolls[i] != null) ? rolls[i] : ItemStack.EMPTY;
            list.add(encodeStack(stack, ops));
        }
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("rolls", list);
        buf.writeNbt(wrapper);
    }

    /** 单个物品转 NBT；编码失败只丢这一格（返回空标签 → 客户端读成空栈），不连累整包 */
    @Nonnull
    private static CompoundTag encodeStack(@Nonnull ItemStack stack, @Nonnull DynamicOps<Tag> ops) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        try {
            return (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result()
                    .orElseGet(CompoundTag::new);
        } catch (Throwable e) {
            return new CompoundTag();
        }
    }

    /** 与 {@link #writeRolls} 对称的读法；用缓冲区自带的注册表访问器按注册名解析 */
    @Nonnull
    public static ItemStack[] readRolls(@Nonnull RegistryFriendlyByteBuf data) {
        ItemStack[] result = new ItemStack[ROLL_SLOTS];
        Arrays.fill(result, ItemStack.EMPTY);
        CompoundTag wrapper = data.readNbt();
        if (wrapper == null) {
            return result;
        }
        HolderLookup.Provider registries = data.registryAccess();
        ListTag list = wrapper.getListOrEmpty("rolls");
        for (int i = 0; i < Math.min(ROLL_SLOTS, list.size()); i++) {
            try {
                result[i] = ItemStack.OPTIONAL_CODEC
                        .parse(registries.createSerializationContext(NbtOps.INSTANCE), list.getCompoundOrEmpty(i))
                        .result().orElse(ItemStack.EMPTY);
            } catch (Throwable ignored) {
                // 单条坏数据只丢这一格
            }
        }
        return result;
    }

    /** 用一组新结果替换当前 10 格（不足或含 null 的按空处理） */
    private void setRolls(@Nullable ItemStack[] source) {
        for (int i = 0; i < ROLL_SLOTS; i++) {
            ItemStack stack = (source != null && i < source.length) ? source[i] : null;
            rolls[i] = stack == null ? ItemStack.EMPTY : stack;
        }
    }

    /** 客户端收到服务端重掷结果后调用 */
    public void applyRolls(@Nullable ItemStack[] newRolls) {
        setRolls(newRolls);
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
     * 某分类当前随机物品的栈（含 NBT），空返回 EMPTY
     */
    @Nonnull
    public ItemStack getRolledStack(int index) {
        return index >= 0 && index < ROLL_SLOTS ? rolls[index] : ItemStack.EMPTY;
    }

    // ==================== 按钮处理 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        if (id >= BUTTON_SELECT_BASE && id < BUTTON_SELECT_BASE + ROLL_SLOTS) {
            if (!getRolledStack(id).isEmpty()) {
                this.selectedIndex = id;
            }
            return true;
        }
        int remaining = SupplyData.getMax(player) - SupplyData.getUsed(player);
        if (id == BUTTON_REDEEM) {
            if (remaining < SupplyData.COST_REDEEM) {
                return false;
            }
            if (this.selectedIndex < 0 || getRolledStack(this.selectedIndex).isEmpty()) {
                return false;
            }
            // 给玩家 1 件选中的物品（带 NBT），放不下则丢到脚边
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
     * 按当前状态重掷一组物品并记录对应的整点小时，随后把结果发给客户端。
     */
    private void rollItems(ServerLevel level, Player player) {
        setRolls(SupplyRoll.roll(level, player));
        this.rollHour = currentHour(level);
        if (ownerPlayer != null) {
            PacketDistributor.sendToPlayer(ownerPlayer, new SupplyCrateRollsPacket(rolls.clone()));
        }
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
