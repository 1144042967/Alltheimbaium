package cn.sd.jrz.alltheimbaium.entity;

import static cn.sd.jrz.alltheimbaium.setup.Registration.LIQUID_FOUNTAIN_ENTITY;
import static cn.sd.jrz.alltheimbaium.setup.Registration.LIQUID_FOUNTAIN_ITEM;
import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.connection.LiquidFountainConnection;
import cn.sd.jrz.alltheimbaium.gui.LiquidFountainMenu;
import cn.sd.jrz.alltheimbaium.item.Tip;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 液体无限制造机实体。
 * <p>
 * 单种流体输入，达到配置阈值（默认 10,000,000 mB = 1 万桶）后变为无限。
 * <ul>
 *   <li>+ 槽：放入液体桶/带液容器时把液体输入机器；放入空桶/空容器时从机器抽取液体；操作完毕转移到 - 槽</li>
 *   <li>- 槽：存放处理完毕的桶/容器，玩家或管道可抽取，不可主动放入</li>
 *   <li>六面主动输出开关（GUI 中逐台修改，NBT 持久化）</li>
 *   <li>管道输入输出：IFluidHandler 未无限时可输入/输出存量，无限后只可输出</li>
 * </ul>
 */
public class LiquidFountainEntity extends BlockEntity implements MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainEntity.class);

    /** 对外流体能力（方向无关）。26.x 的能力类型是 {@code ResourceHandler<FluidResource>}，实现类见 LiquidFountainConnection */
    private final LiquidFountainConnection fluidHandler = new LiquidFountainConnection(this);

    @Nonnull
    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        return fluidHandler;
    }

    /**
     * 物品管道能力：+ 槽可插入、- 槽可抽取，保证管道单向流动（方向无关）。
     * NeoForge 不再实现 ICapabilityProvider，由 {@code registerCapabilities} 拉取。
     * <p>
     * 26.x：能力类型改成 {@code ResourceHandler<ItemResource>}，改动在事务日志里记录，回滚时恢复两个槽位。
     */
    private final SlotItemHandler itemHandler = new SlotItemHandler();

    @Nonnull
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    /** 两个槽位（0 = + 槽可插入，1 = - 槽可抽取）的新传输 API 视图 */
    private final class SlotItemHandler extends SnapshotJournal<ItemStack[]> implements ResourceHandler<ItemResource> {

        @Nonnull
        private ItemStack stackAt(int index) {
            if (index == 0) {
                return inputSlot.getStackInSlot(0);
            }
            return index == 1 ? outputSlot.getStackInSlot(0) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        @Nonnull
        public ItemResource getResource(int index) {
            ItemStack current = stackAt(index);
            return current.isEmpty() ? ItemResource.EMPTY : ItemResource.of(current);
        }

        @Override
        public long getAmountAsLong(int index) {
            return stackAt(index).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, @Nonnull ItemResource resource) {
            if (index != 0) {
                return 1;
            }
            return resource.isEmpty() ? Integer.MAX_VALUE : resource.getMaxStackSize();
        }

        @Override
        public boolean isValid(int index, @Nonnull ItemResource resource) {
            return index == 0 && inputSlot.isItemValid(0, resource.toStack(1));
        }

        @Override
        public int insert(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
            if (index != 0 || amount <= 0) {
                return 0;
            }
            ItemStack current = stackAt(0);
            if ((!current.isEmpty() && !resource.matches(current)) || !isValid(0, resource)) {
                return 0;
            }
            int existing = current.isEmpty() ? 0 : current.getCount();
            int inserted = (int) Math.min(amount, getCapacityAsLong(0, resource) - existing);
            if (inserted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            inputSlot.setStackInSlot(0, current.isEmpty()
                    ? resource.toStack(inserted)
                    : current.copyWithCount(existing + inserted));
            return inserted;
        }

        @Override
        public int extract(int index, @Nonnull ItemResource resource, int amount, @Nonnull TransactionContext transaction) {
            if (index != 1 || amount <= 0) {
                return 0;
            }
            ItemStack current = stackAt(1);
            if (!resource.matches(current)) {
                return 0;
            }
            int extracted = Math.min(amount, current.getCount());
            if (extracted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            outputSlot.setStackInSlot(0, extracted >= current.getCount()
                    ? ItemStack.EMPTY
                    : current.copyWithCount(current.getCount() - extracted));
            return extracted;
        }

        @Override
        protected ItemStack[] createSnapshot() {
            return new ItemStack[]{inputSlot.getStackInSlot(0).copy(), outputSlot.getStackInSlot(0).copy()};
        }

        @Override
        protected void revertToSnapshot(ItemStack[] snapshot) {
            inputSlot.setStackInSlot(0, snapshot[0]);
            outputSlot.setStackInSlot(0, snapshot[1]);
        }
    }

    /**
     * 机器内部流体（无限后 amount = Integer.MAX_VALUE）
     */
    public FluidStack stack = FluidStack.EMPTY;

    // + 槽（输入）：空桶或带 FLUID_HANDLER_ITEM 能力的容器，组的大小由物品自身堆叠上限决定
    public final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 空桶（vanilla 桶无流体物品能力，需特判）
            if (stack.is(Items.BUCKET)) {
                return true;
            }
            // 带液容器：机器为空或容器内流体与机器同种才接受
            // 26.x：物品的流体能力改走 ItemAccess（旧 IFluidHandlerItem 已标 forRemoval 且无新→旧适配器）
            ResourceHandler<FluidResource> handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
            if (handler == null) {
                return false;
            }
            for (int tank = 0; tank < handler.size(); tank++) {
                FluidResource resource = handler.getResource(tank);
                if (resource.isEmpty()) {
                    return true;
                }
                return getStack().isEmpty() || resource.matches(getStack());
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            return stack.getMaxStackSize();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // - 槽（输出）：只读，机器放入处理完毕的桶/容器，玩家/管道取出
    public final ItemStackHandler outputSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // 六面主动输出开关（逐台保存，GUI 可修改，默认全启用）
    public boolean transferDown = true;
    public boolean transferUp = true;
    public boolean transferNorth = true;
    public boolean transferSouth = true;
    public boolean transferWest = true;
    public boolean transferEast = true;
    /** 主动输出总开关（默认开启；GUI 按钮切换，NBT 持久化），关闭后不执行任何主动输出 */
    public boolean outputEnabled = true;

    // 上次已同步到客户端的液体指纹（避免每 tick 发送相同更新包）
    private int lastSyncAmount = -1;
    private Fluid lastSyncFluid = null;
    /** 六面主动输出开关的 NBT 键名，顺序与 Direction.values() 一致 */
    private static final String[] TRANSFER_KEYS = {"transferDown", "transferUp", "transferNorth", "transferSouth", "transferWest", "transferEast"};

    public LiquidFountainEntity(BlockPos pos, BlockState state) {
        super(LIQUID_FOUNTAIN_ENTITY.get(), pos, state);
    }


    /**
     * 服务端每 tick 调用（由方块的 ticker 触发）
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        try {
            // 检查命名空间，配置文件 auto_infinite_mods 列表中的 MOD 流体直接设为无限
            if (stack != FluidStack.EMPTY) {
                String namespace = BuiltInRegistries.FLUID.getKey(stack.getFluid()).getNamespace();
                if (LiquidFountainBlock.isAutoInfiniteMod(namespace)) {
                    stack.setAmount(Integer.MAX_VALUE);
                }
            }
            // 达到阈值后变为无限
            if (stack != FluidStack.EMPTY && stack.getAmount() >= getMax()) {
                stack.setAmount(Integer.MAX_VALUE);
            }
            // 处理 + 槽（桶/容器双向操作）
            processInputSlot();
            // 无限后向六个面主动输出（受总开关与逐面开关控制）
            if (isInfinity() && outputEnabled) {
                outputToSides();
            }
            // 只在液体指纹变化时触发客户端同步，避免每 tick 发包
            syncIfChanged();
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.serverTick error", e);
        }
    }

    /** 液体量/类型变化时触发客户端同步（setChanged），无变化则跳过，避免每 tick 发送相同更新包 */
    private void syncIfChanged() {
        if (stack != FluidStack.EMPTY) {
            int amount = stack.getAmount();
            if (amount != lastSyncAmount || stack.getFluid() != lastSyncFluid) {
                lastSyncAmount = amount;
                lastSyncFluid = stack.getFluid();
                setChanged();
                sendUpdatePacket();
            }
        } else if (lastSyncAmount != -1) {
            lastSyncAmount = -1;
            lastSyncFluid = null;
            setChanged();
            sendUpdatePacket();
        }
    }

    /**
     * 向附近的客户端玩家显式发送液体数据更新包，
     * 确保实时同步（不依赖 vanilla 隐式发包机制）。
     */
    private void sendUpdatePacket() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            double distSq = 64.0 * 64.0;
            for (ServerPlayer player : serverLevel.players()) {
                if (player.blockPosition().distSqr(worldPosition) < distSq) {
                    player.connection.send(ClientboundBlockEntityDataPacket.create(this));
                }
            }
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.sendUpdatePacket error", e);
        }
    }

    /**
     * 处理 + 槽：
     * <ul>
     *   <li>带液桶/容器 → 液体输入机器，容器移到 - 槽</li>
     *   <li>空桶 → 机器有 ≥1000 mB 时填桶，满桶移到 - 槽</li>
     *   <li>空容器 → 机器有液体时填充，容器移到 - 槽</li>
     * </ul>
     * 带液容器只输入、空容器只装出，二者互斥，避免来回倒液体。
     */
    private void processInputSlot() {
        ItemStack input = inputSlot.getStackInSlot(0);
        if (input.isEmpty()) {
            return;
        }
        // 空桶特判（vanilla 桶无 FLUID_HANDLER_ITEM 能力）
        if (input.is(Items.BUCKET)) {
            if (stack != FluidStack.EMPTY && stack.getAmount() >= 1000) {
                ItemStack filled = FluidUtil.getFilledBucket(new FluidStack(stack.getFluid(), 1000));
                if (!filled.isEmpty() && canInsertOutput(filled)) {
                    insertOutput(filled);
                    consumeOne(input);
                    stack.shrink(1000);
                    if (stack.getAmount() <= 0) {
                        stack = FluidStack.EMPTY;
                    }
                    setChanged();
                }
            }
            return;
        }
        // 带流体物品能力的容器
        // 26.x：改走 ItemAccess.forStack（直接就地改写传入的那份栈），能力类型是 ResourceHandler<FluidResource>，
        // 读写都要包在事务里，成功才 commit
        ItemStack container = input.copy();
        container.setCount(1);
        ResourceHandler<FluidResource> handler = ItemAccess.forStack(container).getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            boolean changed = false;
            FluidResource contained = handler.size() > 0 ? handler.getResource(0) : FluidResource.EMPTY;
            if (!contained.isEmpty()) {
                // 带液容器：把液体输入机器（机器为空或同种且未无限）
                if (!isInfinity() && (stack == FluidStack.EMPTY || contained.matches(stack))) {
                    int available = Tool.suitInt(handler.getAmountAsLong(0));
                    int accepted = fillMachineLimit(contained, available);
                    if (accepted > 0) {
                        int extracted = 0;
                        try (Transaction tx = Transaction.open(null)) {
                            extracted = handler.extract(0, contained, accepted, tx);
                            if (extracted > 0) {
                                tx.commit();
                            }
                        }
                        if (extracted > 0) {
                            addFluid(contained, extracted);
                            changed = true;
                        }
                    }
                }
            } else if (stack != FluidStack.EMPTY) {
                // 空容器：从机器装液体（无限时不消耗机器存量）
                int maxFill = isInfinity() ? Integer.MAX_VALUE : Tool.suitInt(stack.getAmount());
                if (maxFill > 0) {
                    int filled = 0;
                    try (Transaction tx = Transaction.open(null)) {
                        filled = handler.insert(0, FluidResource.of(stack.getFluid()), maxFill, tx);
                        if (filled > 0) {
                            tx.commit();
                        }
                    }
                    if (filled > 0) {
                        if (!isInfinity()) {
                            stack.shrink(filled);
                            if (stack.getAmount() <= 0) {
                                stack = FluidStack.EMPTY;
                            }
                        }
                        changed = true;
                    }
                }
            }
            // 操作完毕 → 容器移到 - 槽（单件处理，避免共享 NBT 的组造成输出超限）
            // ItemAccess.forStack 直接就地改写传入的那份栈，container 就是处理后的容器
            if (changed) {
                ItemStack result = container.copy();
                result.setCount(1);
                if (canInsertOutput(result)) {
                    insertOutput(result);
                    consumeOne(input);
                }
                setChanged();
            }
        }
    }

    /**
     * 机器还能接收多少该流体（类型不匹配或已无限返回 0），只计算不写入
     */
    private int fillMachineLimit(@Nonnull FluidResource resource, int requested) {
        if (resource.isEmpty() || requested <= 0 || isInfinity()) {
            return 0;
        }
        if (stack != FluidStack.EMPTY && !resource.matches(stack)) {
            return 0;
        }
        long remaining = Tool.suitInt(getMax() - stack.getAmount());
        return (int) Math.min(requested, remaining);
    }

    /**
     * 把已确认可接收的流体真正写入机器
     */
    private void addFluid(@Nonnull FluidResource resource, int amount) {
        if (amount <= 0) {
            return;
        }
        if (stack == FluidStack.EMPTY) {
            stack = new FluidStack(resource.getFluid(), amount);
        } else {
            stack.grow(amount);
        }
    }

    /**
     * 无限后向六个面（受开关控制）主动输出无限量液体
     */
    private void outputToSides() {
        Level level = getLevel();
        if (level == null || stack == FluidStack.EMPTY) {
            return;
        }
        BlockPos blockPos = getBlockPos();
        for (Direction direction : Direction.values()) {
            if (!isTransferEnabled(direction)) {
                continue;
            }
            try {
                BlockEntity entity = level.getBlockEntity(blockPos.relative(direction));
                if (entity == null) {
                    continue;
                }
                ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, entity.getBlockPos(), direction.getOpposite());
                if (handler == null) {
                    continue;
                }
                // 26.x：insertStacking 内部会开一个根事务并在结束时提交，等价于旧的 fill(..., EXECUTE)
                ResourceHandlerUtil.insertStacking(handler, FluidResource.of(stack.getFluid()), Integer.MAX_VALUE, null);
            } catch (Throwable e) {
                log.error("LiquidFountainEntity.outputToSides error", e);
            }
        }
    }

    /**
     * 机器是否已无限
     */
    public boolean isInfinity() {
        return stack != FluidStack.EMPTY && stack.getAmount() >= getMax();
    }

    /**
     * 当前流体（只读）
     */
    @Nonnull
    public FluidStack getStack() {
        return stack;
    }

    /**
     * 当前流体存量（mB），无限时返回最大值
     */
    public long getFluidAmount() {
        return stack == FluidStack.EMPTY ? 0 : stack.getAmount();
    }

    /**
     * 无限阈值（mB），由配置文件决定
     */
    public long getMax() {
        return LiquidFountainBlock.getMax();
    }

    /**
     * 是否开启主动输出（总开关）
     */
    public boolean isOutputEnabled() {
        return outputEnabled;
    }

    /**
     * 指定面是否允许主动输出
     */
    public boolean isTransferEnabled(Direction direction) {
        return switch (direction) {
            case DOWN -> transferDown;
            case UP -> transferUp;
            case NORTH -> transferNorth;
            case SOUTH -> transferSouth;
            case WEST -> transferWest;
            case EAST -> transferEast;
        };
    }

    /**
     * 设置指定面的主动输出开关（NBT 加载用）
     */
    private void setTransferEnabled(Direction direction, boolean enabled) {
        switch (direction) {
            case DOWN -> transferDown = enabled;
            case UP -> transferUp = enabled;
            case NORTH -> transferNorth = enabled;
            case SOUTH -> transferSouth = enabled;
            case WEST -> transferWest = enabled;
            case EAST -> transferEast = enabled;
        }
    }

    /**
     * 输出槽能否放入该物品
     */
    private boolean canInsertOutput(ItemStack itemStack) {
        ItemStack out = outputSlot.getStackInSlot(0);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(itemStack.getItem()) && out.getCount() + itemStack.getCount() <= outputSlot.getSlotLimit(0);
    }

    /**
     * 把一个物品放入输出槽（调用前需先通过 canInsertOutput 校验）
     */
    private void insertOutput(ItemStack itemStack) {
        ItemStack out = outputSlot.getStackInSlot(0);
        if (out.isEmpty()) {
            outputSlot.setStackInSlot(0, itemStack.copy());
        } else {
            out.grow(itemStack.getCount());
            outputSlot.setStackInSlot(0, out);
        }
    }

    /**
     * 消耗 + 槽中的 1 个物品
     */
    private void consumeOne(ItemStack input) {
        input.shrink(1);
        inputSlot.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
    }

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.alltheimbaium.liquid_fountain").withStyle(Tip.rarityColor(LIQUID_FOUNTAIN_ITEM.get()));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new LiquidFountainMenu(id, inv, worldPosition);
    }

    private static final String KEY_FLUID_ID = "fluid_id";
    private static final String KEY_FLUID_AMOUNT = "fluid_amount";
    private static final String KEY_OUTPUT_ENABLED = "outputEnabled";
    private static final String KEY_INPUT_SLOT = "inputSlot";
    private static final String KEY_OUTPUT_SLOT = "outputSlot";

    @Override
    protected void saveAdditional(@Nonnull ValueOutput output) {
        super.saveAdditional(output);
        try {
            //noinspection deprecation
            output.putString(KEY_FLUID_ID, BuiltInRegistries.FLUID.getKey(stack == FluidStack.EMPTY ? Fluids.EMPTY : stack.getFluid()).toString());
            output.putInt(KEY_FLUID_AMOUNT, stack.getAmount());
            for (Direction direction : Direction.values()) {
                output.putBoolean(TRANSFER_KEYS[direction.ordinal()], isTransferEnabled(direction));
            }
            output.putBoolean(KEY_OUTPUT_ENABLED, outputEnabled);
            // 26.x：ItemStackHandler 改实现 ValueIOSerializable，用 putChild 存取（serializeNBT 已删除）
            output.putChild(KEY_INPUT_SLOT, inputSlot);
            output.putChild(KEY_OUTPUT_SLOT, outputSlot);
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.saveAdditional error", e);
        }
    }

    @Override
    protected void loadAdditional(@Nonnull ValueInput input) {
        super.loadAdditional(input);
        try {
            String fluidRaw = input.getString(KEY_FLUID_ID).orElse(null);
            if (fluidRaw != null) {
                Fluid fluid = null;
                try {
                    //noinspection deprecation
                    fluid = BuiltInRegistries.FLUID.getValue(Identifier.tryParse(fluidRaw));
                } catch (Exception ignored) {
                }
                if (fluid != null && fluid != Fluids.EMPTY) {
                    this.stack = new FluidStack(fluid, 0);
                }
            }
            if (stack != FluidStack.EMPTY) {
                stack.setAmount(input.getIntOr(KEY_FLUID_AMOUNT, stack.getAmount()));
            }
            for (Direction direction : Direction.values()) {
                setTransferEnabled(direction, input.getBooleanOr(TRANSFER_KEYS[direction.ordinal()], isTransferEnabled(direction)));
            }
            outputEnabled = input.getBooleanOr(KEY_OUTPUT_ENABLED, outputEnabled);
            input.readChild(KEY_INPUT_SLOT, inputSlot);
            input.readChild(KEY_OUTPUT_SLOT, outputSlot);
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.loadAdditional error", e);
        }
    }

    /**
     * 客户端数据同步：区块加载/方块放置时，把服务端数据（含流体存量）发给客户端，
     * 供 BER 渲染内部液体与 GUI 展示。
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /**
     * 实时数据同步：液体存量/流体类型变化时（服务端 setChanged），向客户端发送更新包。
     */
    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
