package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 生物农场容器。
 * <p>
 * 槽位：0 = 收容/使用合一槽（未收容时放刷怪蛋/特征掉落物收容生物；已收容后放物品自动模拟右击），
 * 1~27 = 产物行虚拟槽（单击提取），28~63 = 玩家背包。
 * 数据槽同步等级/进度、收容生物、27 行物品 id+存量+权重、六面状态与总开关。
 */
public class MobFarmMenu extends AbstractContainerMenu {
    public static final int MAX_PRODUCTS = 27;

    // 按钮 ID
    public static final int BUTTON_DIR_BASE = 0;                 // 0~5：循环切换六面输出状态
    public static final int BUTTON_EXTRACT_ONE_BASE = 6;         // 6~32：单击提取 1 个
    public static final int BUTTON_EXTRACT_STACK_BASE = 33;      // 33~59：shift 提取 1 组
    public static final int BUTTON_EXTRACT_ALL_BASE = 60;        // 60~86：空格 提取到背包满
    public static final int BUTTON_OUTPUT = 87;                  // 输出总开关
    /** 88~93：右键反向循环切换六面输出状态 */
    public static final int BUTTON_DIR_REVERSE_BASE = 88;

    // 槽位号
    public static final int SLOT_SPECIAL = 0;                    // 收容/使用合一槽
    public static final int SLOT_PRODUCT_BASE = 1;               // 1~27
    public static final int SLOT_PLAYER_BASE = 28;               // 28~63

    // ==================== 帮助表（开屏包）容量 ====================
    // **编码端（MobFarmMarkerIndex.writeToBuf）与解码端必须用同一组常量**。此前编码端不封顶、
    // 解码端单方面截断：超限时没读完的标记对残留会被紧接着当成"产物行数"读，
    // 轻则 B 卡整页错位（未知实体 id 被 Forge 回退成猪），重则缓冲越界、B 卡全空。
    /** 标记对最多下发的 int 个数（每 2 个 int 为一条 物品→生物 映射） */
    public static final int HELP_MAX_MARKER_INTS = 8192;
    /** 产物行最多下发的行数 */
    public static final int HELP_MAX_ROWS = 2048;
    /** 每行最多下发的产物个数 */
    public static final int HELP_MAX_PRODUCTS_PER_ROW = 512;

    public final MobFarmEntity entity;

    // 开屏 extraData 带来的数据（客户端用 "?" 帮助；服务端为空）
    /** "掉落物→生物" 标记对 {itemId, typeId, …} */
    private final int[] markerPairs;
    /** "生物→其产物" 行数组：每行 {typeId, itemId…} */
    private final int[][] productRows;

    // 客户端镜像（由数据槽同步）
    private final int[] clientItemIds = new int[MAX_PRODUCTS];
    private final long[] clientStocks = new long[MAX_PRODUCTS];
    private final long[] clientWeights = new long[MAX_PRODUCTS];
    private final int[] clientRowModes = new int[MAX_PRODUCTS];
    private int clientToolStatus;
    private long clientLevel;
    private int clientTickCount;
    private int clientContainedId;
    private final int[] clientDirectionState = new int[6];
    private boolean clientOutputEnabled;

    /** 服务端构造（MobFarmEntity.createMenu 调用），无标记/产物表 */
    public MobFarmMenu(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, pos, new int[0], new int[0][]);
    }

    /** 客户端构造：从开屏 extraData 读取方块坐标、标记对与产物行（顺序与 MobFarmBlock.use 写入一致） */
    public MobFarmMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, data.readBlockPos(), readMarkerPairs(data), readProductRows(data));
    }

    private MobFarmMenu(int id, Inventory playerInventory, BlockPos pos, int[] markerPairs, int[][] productRows) {
        super(Registration.MOB_FARM_MENU.get(), id);
        this.markerPairs = markerPairs;
        this.productRows = productRows;
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (MobFarmEntity) blockEntity;

        // 0 收容/使用合一槽（对齐贴图右上角槽位）
        addSlot(new SlotItemHandler(entity.specialSlot, 0, 152, 23));
        // 1~27 产物行虚拟槽（3 行 × 9 列，单击/Shift/空格由 Screen 拦截发按钮）
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            addSlot(new ProductSlot(i, 8 + (i % 9) * 18, 44 + (i / 9) * 18));
        }
        // 28~63 玩家背包
        addPlayerInventory(playerInventory);

        // ===== 数据同步 =====
        // 每个值都按 16 位一块拆开传：数据槽走 writeShort，>32767 的值会被客户端读成负数
        for (int k = 0; k < 4; k++) {
            final int part = k;
            addDataSlot(makeDataSlot(() -> longChunk(entity.level, part), v -> clientLevel = setChunk(clientLevel, part, v)));
        }
        addDataSlot(makeDataSlot(() -> intChunk(suitInt(entity.tickCount), 0), v -> clientTickCount = merge32(intChunk(clientTickCount, 1), v)));
        addDataSlot(makeDataSlot(() -> intChunk(suitInt(entity.tickCount), 1), v -> clientTickCount = merge32(v, intChunk(clientTickCount, 0))));
        addDataSlot(makeDataSlot(() -> intChunk(entity.getContainedEntityId(), 0), v -> clientContainedId = merge32(intChunk(clientContainedId, 1), v)));
        addDataSlot(makeDataSlot(() -> intChunk(entity.getContainedEntityId(), 1), v -> clientContainedId = merge32(v, intChunk(clientContainedId, 0))));
        for (int i = 0; i < MAX_PRODUCTS; i++) {
            final int idx = i;
            addDataSlot(makeDataSlot(() -> intChunk(entity.getProductItemId(idx), 0), v -> clientItemIds[idx] = merge32(intChunk(clientItemIds[idx], 1), v)));
            addDataSlot(makeDataSlot(() -> intChunk(entity.getProductItemId(idx), 1), v -> clientItemIds[idx] = merge32(v, intChunk(clientItemIds[idx], 0))));
            for (int k = 0; k < 4; k++) {
                final int part = k;
                addDataSlot(makeDataSlot(() -> longChunk(entity.getProductStock(idx), part), v -> clientStocks[idx] = setChunk(clientStocks[idx], part, v)));
            }
            for (int k = 0; k < 4; k++) {
                final int part = k;
                addDataSlot(makeDataSlot(() -> longChunk(entity.getProductWeight(idx), part), v -> clientWeights[idx] = setChunk(clientWeights[idx], part, v)));
            }
            addDataSlot(makeDataSlot(() -> entity.getRowMode(idx), v -> clientRowModes[idx] = v));
        }
        addDataSlot(makeDataSlot(entity::getToolStatus, v -> clientToolStatus = v));
        for (Direction direction : Direction.values()) {
            final int idx = direction.ordinal();
            addDataSlot(makeDataSlot(() -> entity.getDirectionState(direction), v -> clientDirectionState[idx] = v));
        }
        addDataSlot(makeDataSlot(() -> entity.outputEnabled ? 1 : 0, v -> clientOutputEnabled = v != 0));
    }

    // ==================== 展示 getter（服务端读实体 / 客户端读镜像） ====================

    private boolean serverSide() {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide;
    }

    public long getLevel() {
        return serverSide() ? entity.level : clientLevel;
    }

    public int getTickCount() {
        return serverSide() ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    public int getContainedEntityId() {
        return serverSide() ? entity.getContainedEntityId() : clientContainedId;
    }

    public int getDirectionState(Direction direction) {
        return serverSide() ? entity.getDirectionState(direction) : clientDirectionState[direction.ordinal()];
    }

    public boolean isOutputEnabled() {
        return serverSide() ? entity.outputEnabled : clientOutputEnabled;
    }

    @Nonnull
    public ItemStack getProductStack(int index) {
        if (serverSide()) {
            return entity.getProductStack(index);
        }
        if (index < 0 || index >= MAX_PRODUCTS || clientItemIds[index] <= 0) {
            return ItemStack.EMPTY;
        }
        //noinspection deprecation
        return new ItemStack(BuiltInRegistries.ITEM.byId(clientItemIds[index]), 1);
    }


    public long getProductStock(int index) {
        if (serverSide()) {
            return entity.getProductStock(index);
        }
        return index >= 0 && index < MAX_PRODUCTS ? clientStocks[index] : 0;
    }

    public long getProductWeight(int index) {
        if (serverSide()) {
            return entity.getProductWeight(index);
        }
        return index >= 0 && index < MAX_PRODUCTS ? clientWeights[index] : 0;
    }

    /** 产物行展示模式（0 被动 / 1 使用槽产出中 / 2 使用槽未产出） */
    public int getRowMode(int index) {
        if (serverSide()) {
            return entity.getRowMode(index);
        }
        return index >= 0 && index < MAX_PRODUCTS ? clientRowModes[index] : 0;
    }

    /** 使用槽全局状态（0 无 / 1 缺少工具 / 2 工具不符 / 3 正常） */
    public int getToolStatus() {
        if (serverSide()) {
            return entity.getToolStatus();
        }
        return clientToolStatus;
    }

    // ==================== 标记表读取（客户端 "?" 帮助用） ====================

    /** 标记对条数（掉落物→生物 的映射数量） */
    public int markerCount() {
        return markerPairs.length / 2;
    }

    /** 第 i 条标记物物品（越界返回 null） */
    @Nullable
    public Item markerItem(int index) {
        // 需要 index*2 与 index*2+1 两个下标都合法，所以判 >= 而不是 >
        if (index < 0 || index * 2 + 1 >= markerPairs.length) {
            return null;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.byId(markerPairs[index * 2]);
    }

    /** 第 i 条对应的收容生物类型（越界返回 null） */
    @Nullable
    public EntityType<?> markerType(int index) {
        if (index < 0 || index * 2 + 1 >= markerPairs.length) {
            return null;
        }
        //noinspection deprecation
        return BuiltInRegistries.ENTITY_TYPE.byId(markerPairs[index * 2 + 1]);
    }

    // ==================== 产物表读取（第二个 "?" 帮助用） ====================

    /** 产物行数（"生物→其产物" 的映射数量） */
    public int productRowCount() {
        return productRows.length;
    }

    /** 第 i 行的生物类型（越界返回 null） */
    @Nullable
    public EntityType<?> productRowType(int row) {
        if (row < 0 || row >= productRows.length) {
            return null;
        }
        //noinspection deprecation
        return BuiltInRegistries.ENTITY_TYPE.byId(productRows[row][0]);
    }

    /** 第 i 行的产物物品数 */
    public int productRowItemCount(int row) {
        if (row < 0 || row >= productRows.length) {
            return 0;
        }
        return productRows[row].length - 1;
    }

    /** 第 i 行第 k 个产物物品（越界返回 null） */
    @Nullable
    public Item productRowItem(int row, int k) {
        if (row < 0 || row >= productRows.length || k < 0 || k + 1 >= productRows[row].length) {
            return null;
        }
        //noinspection deprecation
        return BuiltInRegistries.ITEM.byId(productRows[row][k + 1]);
    }

    /** 读取开屏 extraData 中的标记对；读损坏回退空数组 */
    private static int[] readMarkerPairs(FriendlyByteBuf data) {
        try {
            int count = Math.max(0, Math.min(data.readVarInt(), HELP_MAX_MARKER_INTS));
            int[] arr = new int[count];
            for (int i = 0; i < count; i++) {
                arr[i] = data.readVarInt();
            }
            return arr;
        } catch (Throwable e) {
            return new int[0];
        }
    }

    /** 读取开屏 extraData 中的产物行（先行数，每行 typeId + 物品数 + 物品id…）；读损坏回退空数组 */
    private static int[][] readProductRows(FriendlyByteBuf data) {
        try {
            int rows = Math.max(0, Math.min(data.readVarInt(), HELP_MAX_ROWS));
            int[][] out = new int[rows][];
            for (int r = 0; r < rows; r++) {
                int typeId = data.readVarInt();
                int count = Math.max(0, Math.min(data.readVarInt(), HELP_MAX_PRODUCTS_PER_ROW));
                int[] row = new int[count + 1];
                row[0] = typeId;
                for (int i = 0; i < count; i++) {
                    row[i + 1] = data.readVarInt();
                }
                out[r] = row;
            }
            return out;
        } catch (Throwable e) {
            return new int[0][];
        }
    }

    // ==================== 按钮处理 ====================

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (entity == null || player.level().isClientSide) {
            return false;
        }
        if (id >= BUTTON_DIR_BASE && id < BUTTON_DIR_BASE + 6) {
            entity.cycleDirectionState(Direction.values()[id - BUTTON_DIR_BASE]);
        } else if (id >= BUTTON_DIR_REVERSE_BASE && id < BUTTON_DIR_REVERSE_BASE + 6) {
            // 右键：反向循环
            entity.cycleDirectionState(Direction.values()[id - BUTTON_DIR_REVERSE_BASE], false);
        } else if (id >= BUTTON_EXTRACT_ONE_BASE && id < BUTTON_EXTRACT_ONE_BASE + MAX_PRODUCTS) {
            extract(player, id - BUTTON_EXTRACT_ONE_BASE, 1);
        } else if (id >= BUTTON_EXTRACT_STACK_BASE && id < BUTTON_EXTRACT_STACK_BASE + MAX_PRODUCTS) {
            int slot = id - BUTTON_EXTRACT_STACK_BASE;
            ItemStack template = entity.getProductStack(slot);
            long maxStack = template.isEmpty() ? 1 : template.getMaxStackSize();
            extract(player, slot, maxStack);
        } else if (id >= BUTTON_EXTRACT_ALL_BASE && id < BUTTON_EXTRACT_ALL_BASE + MAX_PRODUCTS) {
            extract(player, id - BUTTON_EXTRACT_ALL_BASE, Long.MAX_VALUE);
        } else if (id == BUTTON_OUTPUT) {
            entity.outputEnabled = !entity.outputEnabled;
        } else {
            return false;
        }
        entity.setChanged();
        return true;
    }

    /**
     * 从产物行提取最多 maxCount 件放入玩家背包；放不下时退回存量
     */
    private void extract(Player player, int slot, long maxCount) {
        if (maxCount <= 0) {
            return;
        }
        long remaining = maxCount;
        while (remaining > 0) {
            long available = entity.getProductStock(slot);
            if (available <= 0) {
                break;
            }
            int amount = (int) Math.min(available, Math.min(remaining, 64));
            long got = entity.extractItems(slot, amount);
            if (got <= 0) {
                break;
            }
            ItemStack stack = entity.getProductStack(slot).copy();
            stack.setCount((int) got);
            player.addItem(stack);
            int placed = (int) got - stack.getCount();
            if (placed < got) {
                entity.addProduct(stack.copy());
            }
            remaining -= placed;
            if (placed <= 0) {
                break;
            }
        }
        entity.setChanged();
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (entity == null) {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    /**
     * 快速转移：产物虚拟槽不可取出；合一槽可移向背包；背包可 Shift 放入合一槽
     */
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        try {
            if (index < SLOT_SPECIAL || index > SLOT_PLAYER_BASE + 35) {
                return ItemStack.EMPTY;
            }
            Slot slot = this.slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (index == SLOT_SPECIAL) {
                // 合一槽 → 背包
                if (!this.moveItemStackTo(stack, SLOT_PLAYER_BASE, SLOT_PLAYER_BASE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= SLOT_PLAYER_BASE) {
                // 背包 → 合一槽（标记/使用语义由槽决定）
                if (!this.moveItemStackTo(stack, SLOT_SPECIAL, SLOT_SPECIAL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 产物虚拟槽不可快速取出
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } catch (Throwable e) {
            // 忽略快速转移异常
        }
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // 主物品栏 3 行与快捷栏坐标对齐 PS 贴图：160/178/196 + 218
        int mainY = 160;
        int hotbarY = 218;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, mainY + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, hotbarY));
        }
    }

    /**
     * 产物行虚拟槽：getItem 从实体/同步数据计算，不可放入/取出（取出由 Screen 拦截按钮处理）
     */
    private class ProductSlot extends Slot {
        private final int index;

        ProductSlot(int index, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.index = index;
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        @Nonnull
        public ItemStack getItem() {
            return getProductStack(index);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@Nonnull Player player) {
            return false;
        }

        @Override
        public void set(@Nonnull ItemStack stack) {
        }

        @Override
        @Nonnull
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
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

    // ==================== 数据槽拆位工具 ====================
    // 数据槽在线路上走 ClientboundContainerSetDataPacket，值用 writeShort 写——**只有 16 位**。
    // 因此 32 位值要拆成 2 块、64 位值要拆成 4 块；每块按无符号 16 位传递
    // （0~65535 写出去会被读成负数，所以取块与并块都要 & 0xFFFF）。
    // 注意「拆成高低 32 位」是错的：那样每一半仍然会被截断到 16 位。

    /** 取 32 位值的第 {@code part} 个 16 位块（part 0 = 低 16 位） */
    private static int intChunk(int value, int part) {
        return (value >>> (part * 16)) & 0xFFFF;
    }

    /** 把两个 16 位块并回 32 位值 */
    private static int merge32(int high, int low) {
        return ((high & 0xFFFF) << 16) | (low & 0xFFFF);
    }

    /** 取 long 的第 {@code part} 个 16 位块（part 0 = 低 16 位，共 4 块） */
    private static int longChunk(long value, int part) {
        return (int) ((value >>> (part * 16)) & 0xFFFFL);
    }

    /** 把第 {@code part} 个 16 位块写回 long */
    private static long setChunk(long value, int part, int chunk) {
        int shift = part * 16;
        long mask = 0xFFFFL << shift;
        return (value & ~mask) | (((long) (chunk & 0xFFFF)) << shift);
    }

    /** 裁剪到 int 范围（负值取 0） */
    private static int suitInt(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }
}
