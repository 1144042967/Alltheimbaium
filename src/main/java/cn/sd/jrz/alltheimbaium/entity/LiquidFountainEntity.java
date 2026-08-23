package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.block.LiquidFountainBlock;
import cn.sd.jrz.alltheimbaium.connection.LiquidFountainConnection;
import cn.sd.jrz.alltheimbaium.gui.LiquidFountainMenu;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
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
public class LiquidFountainEntity extends BlockEntity implements ICapabilityProvider, MenuProvider {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainEntity.class);
    private final LazyOptional<LiquidFountainConnection> fecOptional = LazyOptional.of(() -> new LiquidFountainConnection(this));
    /**
     * 物品管道能力：+ 槽可插入、- 槽可抽取，保证管道单向流动
     */
    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? inputSlot.getStackInSlot(0) : outputSlot.getStackInSlot(0);
        }

        @Override
        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == 0 ? inputSlot.insertItem(0, stack, simulate) : stack;
        }

        @Override
        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? outputSlot.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? inputSlot.getSlotLimit(0) : outputSlot.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && inputSlot.isItemValid(0, stack);
        }
    });

    /**
     * 机器内部流体（无限后 amount = Integer.MAX_VALUE）
     */
    public FluidStack stack = FluidStack.EMPTY;

    // + 槽（输入）：空桶或带 FLUID_HANDLER_ITEM 能力的容器，组的大小由物品自身堆叠上限决定
    public final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 空桶（vanilla 桶无 FLUID_HANDLER_ITEM 能力，需特判）
            if (stack.is(Items.BUCKET)) {
                return true;
            }
            // 带液容器：机器为空或容器内流体与机器同种才接受
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    FluidStack fluid = handler.getFluidInTank(tank);
                    if (fluid.isEmpty()) {
                        return true;
                    }
                    return getStack() == FluidStack.EMPTY || fluid.isFluidEqual(getStack());
                }
                return false;
            }).orElse(false);
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

    // 上次已同步到客户端的液体指纹（避免每 tick 发送相同更新包）
    private int lastSyncAmount = -1;
    private Fluid lastSyncFluid = null;
    /** 六面主动输出开关的 NBT 键名，顺序与 Direction.values() 一致 */
    private static final String[] TRANSFER_KEYS = {"transferDown", "transferUp", "transferNorth", "transferSouth", "transferWest", "transferEast"};

    public LiquidFountainEntity(BlockPos pos, BlockState state) {
        super(Registration.LIQUID_FOUNTAIN_ENTITY.get(), pos, state);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.FLUID_HANDLER) {
                return fecOptional.cast();
            }
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                return itemOptional.cast();
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    /**
     * 服务端每 tick 调用（由方块的 ticker 触发）
     */
    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
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
            // 无限后向六个面主动输出（受开关控制）
            if (isInfinity()) {
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
        // 带 FLUID_HANDLER_ITEM 能力的容器
        input.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().ifPresent(handler -> {
            boolean changed = false;
            FluidStack inTank = handler.getTanks() > 0 ? handler.getFluidInTank(0) : FluidStack.EMPTY;
            if (!inTank.isEmpty()) {
                // 带液容器：把液体输入机器（机器为空或同种且未无限）
                if (!isInfinity() && (stack == FluidStack.EMPTY || inTank.isFluidEqual(stack))) {
                    int accepted = fillMachine(inTank.copy());
                    if (accepted > 0) {
                        handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                        changed = true;
                    }
                }
            } else if (stack != FluidStack.EMPTY) {
                // 空容器：从机器装液体（无限时不消耗机器存量）
                int maxFill = isInfinity() ? Integer.MAX_VALUE : (int) Math.min(Integer.MAX_VALUE, (long) stack.getAmount());
                if (maxFill > 0) {
                    int filled = handler.fill(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
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
            if (changed) {
                ItemStack result = handler.getContainer();
                if (result.isEmpty()) {
                    result = input.copy();
                }
                result = result.copy();
                result.setCount(1);
                if (canInsertOutput(result)) {
                    insertOutput(result);
                    consumeOne(input);
                }
                setChanged();
            }
        });
    }

    /**
     * 把流体输入机器，返回接受量（类型不匹配或已无限返回 0）
     */
    private int fillMachine(FluidStack fs) {
        if (fs.isEmpty() || isInfinity()) {
            return 0;
        }
        if (stack != FluidStack.EMPTY && !stack.isFluidEqual(fs)) {
            return 0;
        }
        int maxInput = (int) Math.min(fs.getAmount(), Tool.suitInt(getMax() - stack.getAmount()));
        if (maxInput <= 0) {
            return 0;
        }
        if (stack == FluidStack.EMPTY) {
            stack = new FluidStack(fs.getFluid(), maxInput);
        } else {
            stack.grow(maxInput);
        }
        return maxInput;
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
                IFluidHandler handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).resolve().orElse(null);
                if (handler == null) {
                    continue;
                }
                FluidStack fs = stack.copy();
                fs.setAmount(Integer.MAX_VALUE);
                handler.fill(fs, IFluidHandler.FluidAction.EXECUTE);
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
        return Component.translatable("block.alltheimbaium.liquid_fountain");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player) {
        return new LiquidFountainMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            if (stack != FluidStack.EMPTY) {
                //noinspection deprecation
                nbt.putString("fluid_id", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
                nbt.putInt("fluid_amount", stack.getAmount());
            } else {
                //noinspection deprecation
                nbt.putString("fluid_id", BuiltInRegistries.FLUID.getKey(Fluids.EMPTY).toString());
                nbt.putInt("fluid_amount", stack.getAmount());
            }
            for (Direction direction : Direction.values()) {
                nbt.putBoolean(TRANSFER_KEYS[direction.ordinal()], isTransferEnabled(direction));
            }
            nbt.put("inputSlot", inputSlot.serializeNBT());
            nbt.put("outputSlot", outputSlot.serializeNBT());
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
            if (nbt.contains("fluid_id", Tag.TAG_STRING)) {
                Fluid fluid = null;
                try {
                    //noinspection deprecation
                    fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(nbt.getString("fluid_id")));
                } catch (Exception ignored) {
                }
                if (fluid != null && fluid != Fluids.EMPTY) {
                    this.stack = new FluidStack(fluid, 0);
                }
            }
            if (nbt.contains("fluid_amount", Tag.TAG_INT)) {
                if (stack != FluidStack.EMPTY) {
                    stack.setAmount(nbt.getInt("fluid_amount"));
                }
            }
            for (Direction direction : Direction.values()) {
                String key = TRANSFER_KEYS[direction.ordinal()];
                if (nbt.contains(key, Tag.TAG_BYTE)) {
                    setTransferEnabled(direction, nbt.getBoolean(key));
                }
            }
            if (nbt.contains("inputSlot", Tag.TAG_COMPOUND)) {
                inputSlot.deserializeNBT(nbt.getCompound("inputSlot"));
            }
            if (nbt.contains("outputSlot", Tag.TAG_COMPOUND)) {
                outputSlot.deserializeNBT(nbt.getCompound("outputSlot"));
            }
        } catch (Throwable e) {
            log.error("LiquidFountainEntity.load error", e);
        }
    }

    /**
     * 客户端数据同步：区块加载/方块放置时，把服务端数据（含流体存量）发给客户端，
     * 供 BER 渲染内部液体与 GUI 展示。
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

    /**
     * 实时数据同步：液体存量/流体类型变化时（服务端 setChanged），向客户端发送更新包。
     */
    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }
}
